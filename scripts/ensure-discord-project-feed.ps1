[CmdletBinding()]
param(
    [string]$BotPath,
    [switch]$StatusOnly
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$localConfigPath = Join-Path $projectRoot "rotclient-dev.local.json"

function Get-ConfiguredBotPath {
    if ($BotPath) {
        return $BotPath
    }
    if ($env:ROT_CLIENT_DISCORD_BOT_PATH) {
        return $env:ROT_CLIENT_DISCORD_BOT_PATH
    }
    if (Test-Path -LiteralPath $localConfigPath -PathType Leaf) {
        $localConfig = Get-Content -Raw -LiteralPath $localConfigPath |
            ConvertFrom-Json
        if ($localConfig.discordBotPath) {
            return [string]$localConfig.discordBotPath
        }
    }

    $driveRoot = [System.IO.Path]::GetPathRoot($projectRoot)
    $candidates = @(
        (Join-Path $driveRoot "Discord bot"),
        (Join-Path (Split-Path $projectRoot -Parent) "Discord bot")
    )
    return $candidates |
        Where-Object { Test-Path -LiteralPath $_ -PathType Container } |
        Select-Object -First 1
}

function Read-JsonFile([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $null
    }
    try {
        return Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Test-ProcessAlive($ProcessId) {
    if (-not $ProcessId) {
        return $false
    }
    return $null -ne (Get-Process -Id ([int]$ProcessId) -ErrorAction SilentlyContinue)
}

$configuredPath = Get-ConfiguredBotPath
if (-not $configuredPath) {
    Write-Warning "Discord bot path is not configured. Set ROT_CLIENT_DISCORD_BOT_PATH or create rotclient-dev.local.json."
    exit 2
}

$botRoot = (Resolve-Path -LiteralPath $configuredPath).Path
$entryPoint = Join-Path $botRoot "src\index.js"
$packageFile = Join-Path $botRoot "package.json"
$environmentFile = Join-Path $botRoot ".env"
$dependencies = Join-Path $botRoot "node_modules"
$healthPath = Join-Path $botRoot "data\health.json"
$bootstrapPath = Join-Path $botRoot "data\rotclient-dev-bootstrap.json"

foreach ($required in @($entryPoint, $packageFile, $environmentFile, $dependencies)) {
    if (-not (Test-Path -LiteralPath $required)) {
        Write-Error "Discord bot prerequisite is missing: $required"
    }
}

$health = Read-JsonFile $healthPath
$bootstrap = Read-JsonFile $bootstrapPath
$runningPid = if (Test-ProcessAlive $health.pid) {
    [int]$health.pid
} elseif (Test-ProcessAlive $bootstrap.pid) {
    [int]$bootstrap.pid
} else {
    $null
}

if ($runningPid) {
    Write-Output "Rot Client Discord feed bot is already running (PID $runningPid)."
    exit 0
}

if ($StatusOnly) {
    Write-Output "Rot Client Discord feed bot is not running."
    exit 1
}

$node = Get-Command node -ErrorAction SilentlyContinue
if (-not $node) {
    Write-Error "Node.js is not available on PATH."
}

$logDirectory = Join-Path $botRoot "logs"
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$stdout = Join-Path $logDirectory "rotclient-dev-$timestamp.stdout.log"
$stderr = Join-Path $logDirectory "rotclient-dev-$timestamp.stderr.log"

$process = Start-Process `
    -FilePath $node.Source `
    -ArgumentList "src/index.js" `
    -WorkingDirectory $botRoot `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -PassThru

$bootstrapState = [ordered]@{
    pid = $process.Id
    status = "starting"
    startedAt = (Get-Date).ToUniversalTime().ToString("o")
}
New-Item -ItemType Directory -Path (Split-Path $bootstrapPath -Parent) -Force |
    Out-Null
[System.IO.File]::WriteAllText(
    $bootstrapPath,
    (($bootstrapState | ConvertTo-Json) + [Environment]::NewLine),
    [System.Text.UTF8Encoding]::new($false))

Start-Sleep -Milliseconds 750
if (-not (Test-ProcessAlive $process.Id)) {
    Write-Error "Discord bot exited during startup. Review the local bot logs."
}

Write-Output "Rot Client Discord feed bot started in the background (PID $($process.Id))."
Write-Output "The feed will publish roadmap changes locally and GitHub changes after they are pushed."
