param([string]$Api='http://localhost:18080',[string]$Web='http://localhost:4300',[string]$Ai='')
$ErrorActionPreference='Stop'
$targets=@(@{name='readiness';url=$Api.TrimEnd('/')+'/actuator/health/readiness';expected='UP'},
 @{name='liveness';url=$Api.TrimEnd('/')+'/actuator/health/liveness';expected='UP'},
 @{name='frontend';url=$Web.TrimEnd('/')+'/';expected=$null})
if($Ai){$targets+=@{name='ai';url=$Ai.TrimEnd('/')+'/health';expected='ok'}}
$checks=@()
foreach($target in $targets){
 $uri=[Uri]$target.url
 if($uri.Scheme -notin @('http','https') -or $uri.UserInfo){throw 'Usa URLs sin credenciales.'}
 $timer=[Diagnostics.Stopwatch]::StartNew();$good=$false;$status=0
 try{
  $r=Invoke-WebRequest -Uri $uri -TimeoutSec 8 -SkipHttpErrorCheck
  $status=[int]$r.StatusCode;$good=$status -eq 200
  if($good -and $target.expected){$good=($r.Content|ConvertFrom-Json).status -eq $target.expected}
 }catch{$good=$false}
 $checks+=@{component=$target.name;healthy=$good;http=$status;milliseconds=$timer.ElapsedMilliseconds}
}
$checks|ConvertTo-Json
if(@($checks|Where-Object {!$_.healthy}).Count){exit 1}
