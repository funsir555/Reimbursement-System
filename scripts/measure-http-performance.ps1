param([Parameter(Mandatory = $true)][string]$LogPath, [int]$MinSamples = 30)

$ErrorActionPreference = 'Stop'
$records = foreach ($line in (Get-Content -LiteralPath $LogPath -Encoding utf8)) {
    if ($line -notmatch 'http_request_completed method=(\S+) path=(\S+) status=(\d+) outcome=(\S+) duration_ms=(\d+)') { continue }
    $route = $Matches[2] -replace '/[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}', '/:id' -replace '/\d+(?=/|$)', '/:id'
    [pscustomobject]@{ Method = $Matches[1]; Route = $route; Status = [int]$Matches[3]; DurationMs = [int]$Matches[5] }
}
if (-not $records) { throw 'No http_request_completed records found; no performance conclusion can be made.' }

$groups = $records | Group-Object Method, Route
$results = foreach ($group in $groups) {
    $sorted = @($group.Group | Sort-Object DurationMs)
    $count = $sorted.Count
    $p50 = $sorted[[Math]::Ceiling($count * 0.50) - 1].DurationMs
    $p95 = $sorted[[Math]::Ceiling($count * 0.95) - 1].DurationMs
    [pscustomobject]@{
        Route = $group.Name
        Samples = $count
        P50Ms = $p50
        P95Ms = $p95
        Error5xx = @($sorted | Where-Object { $_.Status -ge 500 }).Count
        Decision = if ($count -lt $MinSamples) { 'INSUFFICIENT_SAMPLE' } else { 'REVIEW_WITH_LOAD_CONTEXT' }
    }
}
$results | Sort-Object P95Ms -Descending | Format-Table -AutoSize
