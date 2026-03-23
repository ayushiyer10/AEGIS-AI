$ErrorActionPreference = "Stop"

param(
    [string]$AgentExePath = "C:\Users\Ayush Iyer\Desktop\Anti Cheat\dist\windows\agent\AEGISAntiCheatAgent.exe",
    [string]$TaskName = "AEGISAntiCheatAgent",
    [switch]$Remove
)

if ($Remove) {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue
    Write-Host "Removed startup task: $TaskName"
    exit 0
}

if (-not (Test-Path $AgentExePath)) {
    throw "Agent executable not found: $AgentExePath"
}

$action = New-ScheduledTaskAction -Execute $AgentExePath
$trigger = New-ScheduledTaskTrigger -AtLogOn
$settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit (New-TimeSpan -Hours 0) -AllowStartIfOnBatteries

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Description "Starts AEGIS anti-cheat tray agent at user logon." `
    -Force | Out-Null

Write-Host "Registered startup task: $TaskName"
Write-Host "Agent path: $AgentExePath"
