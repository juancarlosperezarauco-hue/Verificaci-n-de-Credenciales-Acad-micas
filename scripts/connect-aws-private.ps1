param(
    [string]$Profile = 'credenciales',
    [ValidateSet('Application','Mail')][string]$Service = 'Application',
    [ValidatePattern('^[a-z][a-z0-9-]{2,39}$')][string]$Deployment = 'credenciales-private',
    [string]$Aws = 'aws'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
if (!(Get-Command $Aws -ErrorAction SilentlyContinue)) {
    $Aws = Join-Path $env:LOCALAPPDATA 'Programs/Amazon/AWSCLIV2/aws.exe'
    if (!(Test-Path -LiteralPath $Aws)) { throw 'Falta AWS CLI v2.' }
}
if (!(Get-Command session-manager-plugin -ErrorAction SilentlyContinue)) {
    $pluginDir = 'C:/Program Files/Amazon/SessionManagerPlugin/bin'
    if (Test-Path "$pluginDir/session-manager-plugin.exe") { $env:PATH = "$pluginDir;$env:PATH" }
    else { throw 'Instala el complemento oficial Session Manager: https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html' }
}
$common = @('--profile',$Profile,'--region','us-east-2','--no-cli-pager')
$identity = & $Aws sts get-caller-identity @common --output json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0 -or $identity.Account -ne '098453331970') { throw 'Comprueba la sesión y la cuenta AWS esperada.' }
$result = & $Aws ec2 describe-instances @common --filters "Name=tag:Deployment,Values=$Deployment" 'Name=tag:ManagedBy,Values=credentials-private-deploy' 'Name=instance-state-name,Values=running' --output json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar el servidor.' }
$instances = @($result.Reservations | ForEach-Object { $_.Instances })
if ($instances.Count -ne 1) { throw 'Debe existir exactamente un servidor de pruebas en ejecución.' }
$port = if ($Service -eq 'Application') { '4302' } else { '8126' }
Write-Output "Mantén esta ventana abierta y entra a http://localhost:$port"
& $Aws ssm start-session @common --target $instances[0].InstanceId --document-name AWS-StartPortForwardingSession --parameters "portNumber=$port,localPortNumber=$port"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir el túnel. Comprueba SSM Online y que el puerto local esté libre.' }
