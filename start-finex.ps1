[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)]
  [string]$RootPath,
  [Parameter(Mandatory = $true)]
  [string]$RunDir
)

$ErrorActionPreference = 'Stop'

function Normalize-Text {
  param([string]$Value)
  if ([string]::IsNullOrWhiteSpace($Value)) {
    return ''
  }
  return $Value.Trim()
}

function Join-SearchTokens {
  param([string[]]$Parts)
  $tokens = @()
  foreach ($part in $Parts) {
    $value = Normalize-Text $part
    if ($value) {
      $tokens += $value
    }
  }
  return $tokens
}

function Test-ContainsAllTokens {
  param(
    [string]$Text,
    [string[]]$Tokens
  )

  $source = Normalize-Text $Text
  if (-not $source) {
    return $false
  }

  foreach ($token in $Tokens) {
    $value = Normalize-Text $token
    if (-not $value) {
      continue
    }
    if ($source.IndexOf($value, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
      return $false
    }
  }
  return $true
}

function Get-ProcessSnapshot {
  param([int]$ProcessId)

  $process = $null
  $cim = $null

  try {
    $process = Get-Process -Id $ProcessId -ErrorAction Stop
  } catch {
    $process = $null
  }

  try {
    $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction Stop
  } catch {
    $cim = $null
  }

  if (-not $process -and -not $cim) {
    return $null
  }

  $name = ''
  if ($process -and $process.ProcessName) {
    $name = [string]$process.ProcessName
  } elseif ($cim -and $cim.Name) {
    $name = [string]$cim.Name
  }

  $commandLine = ''
  if ($cim -and $cim.CommandLine) {
    $commandLine = [string]$cim.CommandLine
  }

  $windowTitle = ''
  if ($process -and $process.MainWindowTitle) {
    $windowTitle = [string]$process.MainWindowTitle
  }

  return [pscustomobject]@{
    Id              = $ProcessId
    Name            = $name
    CommandLine     = $commandLine
    MainWindowTitle = $windowTitle
    SearchText      = ($name + ' ' + $commandLine + ' ' + $windowTitle)
  }
}

function Get-ListeningProcessIds {
  param([int]$Port)

  $processIds = @()
  $lines = netstat -ano -p tcp | Select-String 'LISTENING'
  foreach ($line in $lines) {
    $columns = (($line.ToString() -replace '^\s+', '') -split '\s+')
    if ($columns.Count -lt 5) {
      continue
    }

    $localAddress = $columns[1]
    $state = $columns[3]
    $processIdText = $columns[4]

    if ($state -ne 'LISTENING') {
      continue
    }

    if ($localAddress -notmatch (":{0}$" -f [regex]::Escape([string]$Port))) {
      continue
    }

    $parsedProcessId = 0
    if ([int]::TryParse($processIdText, [ref]$parsedProcessId)) {
      if ($parsedProcessId -gt 0) {
        $processIds += $parsedProcessId
      }
    }
  }

  return $processIds | Sort-Object -Unique
}

function Wait-ForProcessExit {
  param(
    [int]$ProcessId,
    [int]$TimeoutSeconds = 15
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (-not (Get-ProcessSnapshot -ProcessId $ProcessId)) {
      return $true
    }
    Start-Sleep -Milliseconds 500
  }
  return (-not (Get-ProcessSnapshot -ProcessId $ProcessId))
}

function Wait-ForPortRelease {
  param(
    [int]$Port,
    [int]$TimeoutSeconds = 15
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (-not (Get-ListeningProcessIds -Port $Port)) {
      return $true
    }
    Start-Sleep -Milliseconds 500
  }
  return (-not (Get-ListeningProcessIds -Port $Port))
}

function Stop-ProcessTree {
  param(
    [int]$ProcessId,
    [string]$Reason
  )

  if (-not (Get-ProcessSnapshot -ProcessId $ProcessId)) {
    return
  }

  Write-Host ("[finex] Stopping process {0} ({1})" -f $ProcessId, $Reason)
  & cmd.exe /c "taskkill /PID $ProcessId /T /F" | Out-Null
  $taskKillExitCode = $LASTEXITCODE
  if ($taskKillExitCode -ne 0 -and $taskKillExitCode -ne 128 -and $taskKillExitCode -ne 255) {
    throw ("Failed to stop process {0}. taskkill exit code: {1}" -f $ProcessId, $taskKillExitCode)
  }
}

function Read-PidFileValue {
  param([string]$PidFile)

  if (-not (Test-Path -LiteralPath $PidFile)) {
    return $null
  }

  $raw = Normalize-Text (Get-Content -LiteralPath $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
  $processId = 0
  if (-not [int]::TryParse($raw, [ref]$processId)) {
    return $null
  }
  if ($processId -le 0) {
    return $null
  }
  return $processId
}

function Remove-PidFileIfExists {
  param([string]$PidFile)
  if (Test-Path -LiteralPath $PidFile) {
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
  }
}

function Test-ServiceShellOwnership {
  param($Service, [int]$ProcessId)
  $snapshot = Get-ProcessSnapshot -ProcessId $ProcessId
  if (-not $snapshot) {
    return $false
  }
  return (Test-ContainsAllTokens -Text $snapshot.SearchText -Tokens $Service.ShellMatchTokens)
}

function Test-ServiceListenerOwnership {
  param($Service, [int]$ProcessId)
  $snapshot = Get-ProcessSnapshot -ProcessId $ProcessId
  if (-not $snapshot) {
    return $false
  }
  return (Test-ContainsAllTokens -Text $snapshot.SearchText -Tokens $Service.ListenerMatchTokens)
}

function Start-ServiceShell {
  param($Service)

  $commandText = "title {0} && cd /d ""{1}"" && {2}" -f $Service.Title, $Service.WorkingDirectory, $Service.StartCommand
  $process = Start-Process -FilePath 'cmd.exe' -ArgumentList '/K', $commandText -WorkingDirectory $Service.WorkingDirectory -PassThru
  Set-Content -LiteralPath $Service.PidFile -Value $process.Id -Encoding ASCII
  Write-Host ("[finex] Started {0} shell pid={1} port={2}" -f $Service.Title, $process.Id, $Service.Port)
}

function Ensure-AdminWebDependencies {
  param([string]$WorkingDirectory)

  $nodeModulesPath = Join-Path $WorkingDirectory 'node_modules'
  if (Test-Path -LiteralPath $nodeModulesPath) {
    return
  }

  Write-Host '[finex] admin-web dependencies are missing. Installing them before starting Vite...'
  Push-Location $WorkingDirectory
  try {
    if (Test-Path -LiteralPath (Join-Path $WorkingDirectory 'package-lock.json')) {
      & npm ci --no-audit --no-fund
    } else {
      & npm install --no-audit --no-fund
    }

    if ($LASTEXITCODE -ne 0) {
      throw "npm dependency install failed with exit code $LASTEXITCODE"
    }
  } finally {
    Pop-Location
  }
}

$root = (Resolve-Path -LiteralPath $RootPath).Path
$runDirectory = (New-Item -ItemType Directory -Force -Path $RunDir).FullName

$authServiceWorkingDirectory = Join-Path $root 'backend\auth-service'
$gatewayWorkingDirectory = Join-Path $root 'backend\gateway'
$adminWebWorkingDirectory = Join-Path $root 'frontend\admin-web'

$services = @(
  [pscustomobject]@{
    Name                = 'auth-service'
    Title               = 'finex-auth-service'
    Port                = [int]$env:FINEX_AUTH_SERVICE_PORT
    WorkingDirectory    = $authServiceWorkingDirectory
    StartCommand        = 'mvn spring-boot:run'
    PidFile             = Join-Path $runDirectory 'finex-auth-service.pid'
    ShellMatchTokens    = Join-SearchTokens @('title finex-auth-service', $authServiceWorkingDirectory, 'mvn spring-boot:run')
    ListenerMatchTokens = Join-SearchTokens @((Join-Path $authServiceWorkingDirectory 'target\classes'), 'com.finex.auth.AuthApplication')
  },
  [pscustomobject]@{
    Name                = 'gateway'
    Title               = 'finex-gateway'
    Port                = [int]$env:FINEX_GATEWAY_PORT
    WorkingDirectory    = $gatewayWorkingDirectory
    StartCommand        = 'mvn spring-boot:run'
    PidFile             = Join-Path $runDirectory 'finex-gateway.pid'
    ShellMatchTokens    = Join-SearchTokens @('title finex-gateway', $gatewayWorkingDirectory, 'mvn spring-boot:run')
    ListenerMatchTokens = Join-SearchTokens @((Join-Path $gatewayWorkingDirectory 'target\classes'), 'com.finex.gateway.GatewayApplication')
  },
  [pscustomobject]@{
    Name                = 'admin-web'
    Title               = 'finex-admin-web'
    Port                = [int]$env:FINEX_FRONTEND_PORT
    WorkingDirectory    = $adminWebWorkingDirectory
    StartCommand        = 'npm run dev'
    PidFile             = Join-Path $runDirectory 'finex-admin-web.pid'
    ShellMatchTokens    = Join-SearchTokens @('title finex-admin-web', $adminWebWorkingDirectory, 'npm run dev')
    ListenerMatchTokens = Join-SearchTokens @($adminWebWorkingDirectory, 'vite\bin\vite.js')
  }
)

Write-Host ("[finex] Runtime directory: {0}" -f $runDirectory)

foreach ($service in $services) {
  $trackedProcessId = Read-PidFileValue -PidFile $service.PidFile
  if (-not $trackedProcessId) {
    Remove-PidFileIfExists -PidFile $service.PidFile
    continue
  }

  if (Test-ServiceShellOwnership -Service $service -ProcessId $trackedProcessId) {
    Stop-ProcessTree -ProcessId $trackedProcessId -Reason ("tracked {0} shell" -f $service.Name)
    [void](Wait-ForProcessExit -ProcessId $trackedProcessId -TimeoutSeconds 15)
  } else {
    Write-Host ("[finex] Ignoring stale pid file for {0}: {1}" -f $service.Name, $trackedProcessId)
  }

  Remove-PidFileIfExists -PidFile $service.PidFile
}

$conflicts = @()

foreach ($service in $services) {
  $listenerProcessIds = Get-ListeningProcessIds -Port $service.Port
  foreach ($listenerProcessId in $listenerProcessIds) {
    if (Test-ServiceListenerOwnership -Service $service -ProcessId $listenerProcessId) {
      Stop-ProcessTree -ProcessId $listenerProcessId -Reason ("existing {0} listener on port {1}" -f $service.Name, $service.Port)
      continue
    }

    $snapshot = Get-ProcessSnapshot -ProcessId $listenerProcessId
    $conflicts += [pscustomobject]@{
      Service     = $service.Name
      Port        = $service.Port
      ProcessId   = $listenerProcessId
      ProcessName = if ($snapshot -and $snapshot.Name) { $snapshot.Name } else { 'unknown' }
      CommandLine = if ($snapshot -and $snapshot.CommandLine) { $snapshot.CommandLine } else { '' }
    }
  }

  if (-not (Wait-ForPortRelease -Port $service.Port -TimeoutSeconds 20)) {
    $remainingListener = Get-ListeningProcessIds -Port $service.Port | Select-Object -First 1
    $snapshot = if ($remainingListener) { Get-ProcessSnapshot -ProcessId $remainingListener } else { $null }
    $conflicts += [pscustomobject]@{
      Service     = $service.Name
      Port        = $service.Port
      ProcessId   = if ($remainingListener) { $remainingListener } else { 0 }
      ProcessName = if ($snapshot -and $snapshot.Name) { $snapshot.Name } else { 'unknown' }
      CommandLine = if ($snapshot -and $snapshot.CommandLine) { $snapshot.CommandLine } else { '' }
    }
  }
}

if ($conflicts.Count -gt 0) {
  $distinctConflicts = $conflicts | Sort-Object Port, ProcessId -Unique
  Write-Host '[finex] The following ports are occupied by non-FinEx processes:'
  foreach ($conflict in $distinctConflicts) {
    Write-Host ("[finex]   port={0} service={1} pid={2} name={3}" -f $conflict.Port, $conflict.Service, $conflict.ProcessId, $conflict.ProcessName)
    if ($conflict.CommandLine) {
      Write-Host ("[finex]     command={0}" -f $conflict.CommandLine)
    }
  }
  Write-Host '[finex] Close the external process or change the port in backend\.env.local.cmd, then run start-finex.bat again.'
  exit 1
}

foreach ($service in $services) {
  if ($service.Name -eq 'admin-web') {
    Ensure-AdminWebDependencies -WorkingDirectory $service.WorkingDirectory
  }
  Start-ServiceShell -Service $service
}

Write-Host '[finex] FinEx services are starting in separate windows.'
