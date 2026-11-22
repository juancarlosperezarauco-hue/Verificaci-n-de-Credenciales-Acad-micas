# An in-process AWS CLI double: these checks never contact AWS or read credentials.
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
function global:session-manager-plugin {}
function global:Invoke-AwsPrivateFixture {
    param([Parameter(ValueFromRemainingArguments)][string[]]$Values)
    $global:LASTEXITCODE=0
    switch($Values[0]) {
        sts {
            $global:AwsPrivateFixture.identityCalls++
            $account=if($global:AwsPrivateFixture.wrongAccount -or ($global:AwsPrivateFixture.expireAfterSession -and $global:AwsPrivateFixture.sessionCalls -gt 0)){'111111111111'}else{'098453331970'}
            return @{Account=$account}|ConvertTo-Json
        }
        ec2 { return @{Reservations=@(@{Instances=@(@{InstanceId='i-test-fixture'})})}|ConvertTo-Json -Depth 6 }
        ssm {
            if($Values[1] -ne 'start-session'){throw 'Unexpected SSM operation'}
            $global:AwsPrivateFixture.sessionCalls++
            if($global:AwsPrivateFixture.sessionCalls -ge 2){$global:LASTEXITCODE=130}
            return
        }
        default { throw 'Unexpected AWS fixture call' }
    }
}
try {
    $global:AwsPrivateFixture=@{sessionCalls=0;identityCalls=0}
    & "$root/scripts/connect-aws-private.ps1" -Aws Invoke-AwsPrivateFixture | Out-Null
    if($global:AwsPrivateFixture.sessionCalls -ne 1){throw 'Default mode must open exactly one session'}
    $global:AwsPrivateFixture=@{sessionCalls=0;identityCalls=0}
    & "$root/scripts/connect-aws-private.ps1" -Aws Invoke-AwsPrivateFixture -Reconnect -RetrySeconds 1 | Out-Null
    if($global:AwsPrivateFixture.sessionCalls -ne 2 -or $global:AwsPrivateFixture.identityCalls -ne 2){throw 'An ended session must reconnect after verifying the account'}
    $global:AwsPrivateFixture=@{sessionCalls=0;identityCalls=0;expireAfterSession=$true}
    $rejected=$false
    try { & "$root/scripts/connect-aws-private.ps1" -Aws Invoke-AwsPrivateFixture -Reconnect -RetrySeconds 1 | Out-Null } catch {$rejected=$true}
    if(!$rejected -or $global:AwsPrivateFixture.sessionCalls -ne 1){throw 'A changed/expired identity must stop reconnection'}
    $global:AwsPrivateFixture=@{sessionCalls=0;identityCalls=0;wrongAccount=$true}
    $rejected=$false
    try { & "$root/scripts/connect-aws-private.ps1" -Aws Invoke-AwsPrivateFixture | Out-Null } catch {$rejected=$true}
    if(!$rejected -or $global:AwsPrivateFixture.sessionCalls -ne 0){throw 'A different account must never open the tunnel'}
    Write-Output 'Four connection checks passed: one-shot, reconnect, changed identity and wrong initial account.'
} finally {
    Remove-Item Function:\Invoke-AwsPrivateFixture,Function:\session-manager-plugin
    Remove-Variable AwsPrivateFixture -Scope Global
}
