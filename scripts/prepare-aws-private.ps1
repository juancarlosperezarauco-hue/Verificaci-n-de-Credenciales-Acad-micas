[CmdletBinding()]
param(
    [string]$Profile = 'credenciales',
    [ValidateSet('us-east-2')][string]$Region = 'us-east-2',
    [ValidatePattern('^[0-9]{12}$')][string]$ExpectedAccount = '098453331970',
    [ValidatePattern('^[a-z][a-z0-9-]{2,39}$')][string]$Deployment = 'credenciales-private',
    [switch]$Apply,
    [string]$Aws = 'aws'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$env:AWS_PAGER = ''
$env:AWS_EC2_METADATA_DISABLED = 'true'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (!(Get-Command $Aws -ErrorAction SilentlyContinue)) {
    $Aws = Join-Path $env:LOCALAPPDATA 'Programs/Amazon/AWSCLIV2/aws.exe'
    if (!(Test-Path -LiteralPath $Aws)) { throw 'Instala AWS CLI v2 e inicia sesión con aws login.' }
}
function Invoke-AwsJson {
    param([string[]]$Arguments, [string]$MissingCode = '')
    $result = & $Aws @Arguments --profile $Profile --region $Region --output json --no-cli-pager --cli-connect-timeout 10 --cli-read-timeout 30 2>&1
    if ($LASTEXITCODE -ne 0) {
        if ($MissingCode -and (($result | Out-String) -match [regex]::Escape("($MissingCode)"))) { return $null }
        throw ($result | Out-String)
    }
    if ($result) { return ($result | Out-String | ConvertFrom-Json -Depth 100) }
}
function Write-JsonFile($Value, [string]$Path) {
    [IO.File]::WriteAllText($Path, ($Value | ConvertTo-Json -Depth 40), [Text.UTF8Encoding]::new($false))
    return "file://$($Path.Replace('\','/'))"
}
function Assert-Owned($Tags) {
    if (!(@($Tags | Where-Object { $_.Key -eq 'ManagedBy' -and $_.Value -eq 'credentials-private-deploy' }).Count)) {
        throw 'Recurso existente sin la etiqueta de propiedad. Se detuvo para no modificar recursos ajenos.'
    }
}
$identity = Invoke-AwsJson @('sts','get-caller-identity')
if ($identity.Account -ne $ExpectedAccount) { throw 'La sesión AWS pertenece a otra cuenta; no se modificará.' }
Push-Location $root
try {
    $commit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or $commit -notmatch '^[0-9a-f]{40}$') { throw 'No se pudo fijar la revisión Git.' }
    if ($Apply) {
        if (& git status --porcelain) { throw 'Guarda y publica los cambios antes de crear recursos.' }
        $branch = (& git branch --show-current).Trim()
        if (!$branch) { throw 'Publica primero una rama con la revisión a desplegar.' }
        $remote = (& git ls-remote https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas.git "refs/heads/$branch" | Out-String).Trim()
        if ($LASTEXITCODE -ne 0 -or !$remote.StartsWith("$commit`t")) { throw 'La revisión local no coincide con la rama publicada en el repositorio esperado.' }
    }
    $existing = Invoke-AwsJson @('ec2','describe-instances','--filters',"Name=tag:Deployment,Values=$Deployment",'Name=instance-state-name,Values=pending,running,stopping,stopped')
    $instances = @($existing.Reservations | ForEach-Object { $_.Instances })
    if ($instances.Count) {
        if ($instances.Count -ne 1) { throw 'Hay varios servidores con esa etiqueta; revisa antes de continuar.' }
        Assert-Owned $instances[0].Tags
        Write-Output "Servidor existente conservado: $($instances[0].InstanceId) ($($instances[0].State.Name)). No se creó otro."
        return
    }
    $vpcs = Invoke-AwsJson @('ec2','describe-vpcs','--filters','Name=is-default,Values=true')
    if (@($vpcs.Vpcs).Count -ne 1) { throw 'Se necesita una VPC predeterminada existente; no se creará una red automáticamente.' }
    $vpc = $vpcs.Vpcs[0].VpcId
    $subnets = Invoke-AwsJson @('ec2','describe-subnets','--filters',"Name=vpc-id,Values=$vpc",'Name=state,Values=available')
    $tables = Invoke-AwsJson @('ec2','describe-route-tables','--filters',"Name=vpc-id,Values=$vpc")
    $subnet = $null
    foreach ($candidate in ($subnets.Subnets | Sort-Object AvailabilityZone)) {
        $routeTable = @($tables.RouteTables | Where-Object { @($_.Associations | Where-Object SubnetId -eq $candidate.SubnetId).Count })
        if (!$routeTable.Count) { $routeTable = @($tables.RouteTables | Where-Object { @($_.Associations | Where-Object Main -eq $true).Count }) }
        if (@($routeTable.Routes | Where-Object { $_.DestinationCidrBlock -eq '0.0.0.0/0' -and $_.GatewayId -like 'igw-*' -and $_.State -eq 'active' }).Count) { $subnet = $candidate.SubnetId; break }
    }
    if (!$subnet) { throw 'No hay una subred con salida a Internet. No se creará NAT ni se modificarán rutas.' }
    $amiParameter = '/aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id'
    $ami = (Invoke-AwsJson @('ssm','get-parameter','--name',$amiParameter)).Parameter.Value
    $image = (Invoke-AwsJson @('ec2','describe-images','--image-ids',$ami)).Images[0]
    if ($image.OwnerId -ne '099720109477' -or $image.Architecture -ne 'x86_64' -or $image.State -ne 'available') { throw 'La AMI no coincide con Ubuntu oficial x86_64.' }
    $offerings = Invoke-AwsJson @('ec2','describe-instance-type-offerings','--location-type','availability-zone','--filters','Name=instance-type,Values=t3.medium',"Name=location,Values=$($candidate.AvailabilityZone)")
    if (!@($offerings.InstanceTypeOfferings).Count) { throw 't3.medium no está disponible en la zona seleccionada.' }
    $planDirectory = Join-Path $root '.local-backups/aws-private'
    $null = New-Item -ItemType Directory -Path $planDirectory -Force
    $tags = @(@{Key='ManagedBy';Value='credentials-private-deploy'},@{Key='Deployment';Value=$Deployment},@{Key='Environment';Value='private-evaluation'})
    $roleName = "$Deployment-ssm"
    $trust = @{Version='2012-10-17';Statement=@(@{Effect='Allow';Principal=@{Service='ec2.amazonaws.com'};Action='sts:AssumeRole'})}
    $ssmPolicy = 'arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore'
    $bootstrap = @'
#!/usr/bin/env bash
set -euo pipefail
# Source files must remain readable by the unprivileged users in application images.
# Secret generation uses its own restrictive umask in init-private.sh.
umask 022
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends git ca-certificates
release=/opt/credenciales/releases/__COMMIT__
install -d -m 700 "$release"
git -C "$release" init
git -C "$release" remote add origin https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas.git
git -C "$release" fetch --depth 1 origin __COMMIT__
git -C "$release" checkout --detach FETCH_HEAD
test "$(git -C "$release" rev-parse HEAD)" = '__COMMIT__'
bash "$release/deploy/aws/bootstrap-host.sh"
bash "$release/deploy/aws/init-private.sh"
bash "$release/deploy/aws/start-private.sh"
'@
    $bootstrap = $bootstrap.Replace('__COMMIT__',$commit).Replace("`r`n","`n") + "`n"
    $request = @{
        ImageId=$ami;InstanceType='t3.medium';MinCount=1;MaxCount=1
        ClientToken="$Deployment-$($commit.Substring(0,16))"
        IamInstanceProfile=@{Name=$roleName}
        NetworkInterfaces=@(@{DeviceIndex=0;AssociatePublicIpAddress=$true;SubnetId=$subnet;Groups=@('RESOLVED_ON_APPLY')})
        MetadataOptions=@{HttpTokens='required';HttpEndpoint='enabled';HttpPutResponseHopLimit=1;InstanceMetadataTags='disabled'}
        CreditSpecification=@{CpuCredits='standard'}
        BlockDeviceMappings=@(@{DeviceName=$image.RootDeviceName;Ebs=@{VolumeSize=40;VolumeType='gp3';Encrypted=$true;DeleteOnTermination=$false}})
        Monitoring=@{Enabled=$false}
        UserData=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($bootstrap))
        TagSpecifications=@(@{ResourceType='instance';Tags=($tags + @{Key='Name';Value=$Deployment})},@{ResourceType='volume';Tags=$tags})
    }
    $plan = @{Account=$identity.Account;Region=$Region;GitCommit=$commit;Vpc=$vpc;Subnet=$subnet;Ingress=@();EgressPorts=@(80,443);InstanceRequest=$request;RoleTrust=$trust;ManagedPolicy=$ssmPolicy;EstimatedMonthlyUSD=37.22;EstimateHours=730;CostExcludes='Tax, transfer, snapshots; credits subject to AWS eligibility';DataRetention='Encrypted root EBS retained on termination; storage charges continue'}
    $null = Write-JsonFile $plan (Join-Path $planDirectory 'plan.json')
    Write-Output "Plan: cuenta $($identity.Account), $Region, t3.medium (2 vCPU/4 GiB), 40 GiB cifrados, sin entradas públicas."
    Write-Output "Estimación base: USD 37.22 por 730 horas. Archivo: $planDirectory/plan.json"
    if (!$Apply) { Write-Output 'Solo preparación: no se crearon recursos. -Apply crea el entorno y consume saldo/créditos AWS.'; return }

    $role = Invoke-AwsJson @('iam','get-role','--role-name',$roleName) -MissingCode NoSuchEntity
    if ($role) {
        Assert-Owned $role.Role.Tags
        $document = $role.Role.AssumeRolePolicyDocument
        $statement = @($document.Statement)
        if ($document.Version -ne '2012-10-17' -or $statement.Count -ne 1 -or
            $statement[0].Effect -ne 'Allow' -or $statement[0].Action -cne 'sts:AssumeRole' -or
            $statement[0].Principal.Service -cne 'ec2.amazonaws.com' -or
            @($statement[0].Principal.PSObject.Properties).Count -ne 1 -or
            @($statement[0].PSObject.Properties | Where-Object Name -notin @('Effect','Action','Principal','Sid')).Count) {
            throw 'La política de confianza existente difiere. Revisión manual requerida.'
        }
    }
    else { $trustFile = Write-JsonFile $trust (Join-Path $planDirectory 'trust.json'); $tagsFile = Write-JsonFile $tags (Join-Path $planDirectory 'tags.json'); $null = Invoke-AwsJson @('iam','create-role','--role-name',$roleName,'--assume-role-policy-document',$trustFile,'--tags',$tagsFile) }
    $attached = Invoke-AwsJson @('iam','list-attached-role-policies','--role-name',$roleName)
    $inline = Invoke-AwsJson @('iam','list-role-policies','--role-name',$roleName)
    if (@($attached.AttachedPolicies | Where-Object PolicyArn -ne $ssmPolicy).Count -or @($inline.PolicyNames).Count) { throw 'El rol tiene permisos adicionales inesperados. No se usará.' }
    $null = Invoke-AwsJson @('iam','attach-role-policy','--role-name',$roleName,'--policy-arn',$ssmPolicy)
    $profileObject = Invoke-AwsJson @('iam','get-instance-profile','--instance-profile-name',$roleName) -MissingCode NoSuchEntity
    if (!$profileObject) { $tagsFile = Write-JsonFile $tags (Join-Path $planDirectory 'tags.json'); $profileObject = Invoke-AwsJson @('iam','create-instance-profile','--instance-profile-name',$roleName,'--tags',$tagsFile) }
    Assert-Owned $profileObject.InstanceProfile.Tags
    if (@($profileObject.InstanceProfile.Roles | Where-Object RoleName -ne $roleName).Count) { throw 'El perfil pertenece a otro rol.' }
    if (!@($profileObject.InstanceProfile.Roles).Count) { $null = Invoke-AwsJson @('iam','add-role-to-instance-profile','--instance-profile-name',$roleName,'--role-name',$roleName) }
    $null = Invoke-AwsJson @('iam','wait','instance-profile-exists','--instance-profile-name',$roleName)
    Start-Sleep -Seconds 10
    $groups = Invoke-AwsJson @('ec2','describe-security-groups','--filters',"Name=group-name,Values=$Deployment", "Name=vpc-id,Values=$vpc")
    if (@($groups.SecurityGroups).Count) {
        $group = $groups.SecurityGroups[0]; Assert-Owned $group.Tags
        if (@($group.IpPermissions).Count) { throw 'El grupo existente tiene entradas habilitadas. No se usará.' }
        $expectedEgress = @(80,443)
        if (@($group.IpPermissionsEgress).Count -ne 2 -or @($group.IpPermissionsEgress | Where-Object { $_.IpProtocol -ne 'tcp' -or $_.FromPort -ne $_.ToPort -or $_.FromPort -notin $expectedEgress -or @($_.IpRanges).Count -ne 1 -or $_.IpRanges[0].CidrIp -ne '0.0.0.0/0' -or @($_.Ipv6Ranges).Count -or @($_.UserIdGroupPairs).Count -or @($_.PrefixListIds).Count }).Count) { throw 'Las salidas del grupo existente no coinciden con el plan.' }
        $groupId = $group.GroupId
    } else {
        $specFile = Write-JsonFile @(@{ResourceType='security-group';Tags=$tags}) (Join-Path $planDirectory 'sg-tags.json')
        $groupId = (Invoke-AwsJson @('ec2','create-security-group','--group-name',$Deployment,'--description','Private evaluation, SSM only, no inbound ports','--vpc-id',$vpc,'--tag-specifications',$specFile)).GroupId
        $group = (Invoke-AwsJson @('ec2','describe-security-groups','--group-ids',$groupId)).SecurityGroups[0]
        if (@($group.IpPermissionsEgress).Count) { $rules = Write-JsonFile @($group.IpPermissionsEgress) (Join-Path $planDirectory 'remove-egress.json'); $null = Invoke-AwsJson @('ec2','revoke-security-group-egress','--group-id',$groupId,'--ip-permissions',$rules) }
        $rules = Write-JsonFile @(@{IpProtocol='tcp';FromPort=80;ToPort=80;IpRanges=@(@{CidrIp='0.0.0.0/0';Description='Ubuntu package downloads'})},@{IpProtocol='tcp';FromPort=443;ToPort=443;IpRanges=@(@{CidrIp='0.0.0.0/0';Description='SSM, repositories and registry downloads'})}) (Join-Path $planDirectory 'egress.json')
        $null = Invoke-AwsJson @('ec2','authorize-security-group-egress','--group-id',$groupId,'--ip-permissions',$rules)
    }
    $request.NetworkInterfaces[0].Groups = @($groupId)
    $requestFile = Write-JsonFile $request (Join-Path $planDirectory 'run-instance.json')
    $created = Invoke-AwsJson @('ec2','run-instances','--cli-input-json',$requestFile)
    Write-Output "Servidor creado: $($created.Instances[0].InstanceId). Espera a que SSM esté Online y cloud-init termine."
    Write-Output 'La clave inicial queda en /etc/credenciales/bootstrap-admin.txt, accesible solo desde tu sesión administrativa.'
} finally { Pop-Location }
