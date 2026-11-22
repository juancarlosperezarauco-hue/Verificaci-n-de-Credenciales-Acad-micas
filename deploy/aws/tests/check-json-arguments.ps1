# Load only the serialization helper; never execute the provisioning entry point.
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$ast = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $root 'scripts/prepare-aws-private.ps1'), [ref]$null, [ref]$null)
$function = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Write-JsonFile' }, $true)
. ([scriptblock]::Create($function.Extent.Text))
$path = [IO.Path]::GetTempFileName()
try {
    # The AWS CLI list parameters must retain [] even for one security-group tag/rule.
    $null = Write-JsonFile @(@{ResourceType='security-group';Tags=@(@{Key='Environment';Value='private-evaluation'})}) $path
    $text = [IO.File]::ReadAllText($path).Trim()
    if (!$text.StartsWith('[')) { throw 'Single-item AWS list serialized as an object' }
    $value = ConvertFrom-Json -InputObject $text -NoEnumerate
    if ($value.Count -ne 1 -or $value[0].ResourceType -ne 'security-group') { throw 'Invalid list payload' }
    $null = Write-JsonFile @{ImageId='ami-fixture';MinCount=1;MaxCount=1} $path
    if (![IO.File]::ReadAllText($path).Trim().StartsWith('{')) { throw 'AWS request object serialized as list' }
    Write-Output 'AWS CLI object and single-item list payloads validated.'
} finally { Remove-Item -LiteralPath $path }
