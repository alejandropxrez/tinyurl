param(
    [string]$Email = "resilience-smoke-$(Get-Date -Format 'yyyyMMddHHmmss')@example.com",
    [string]$Password = "Password123!",
    [string]$OriginalUrl = "https://www.google.com",
    [string]$AuthBaseUrl = "http://localhost:8082",
    [string]$UrlBaseUrl = "http://localhost:8081",
    [string]$AnalyticsBaseUrl = "http://localhost:8083"
)

$ErrorActionPreference = "Stop"

function Wait-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $client = [System.Net.Sockets.TcpClient]::new()
        try {
            $connection = $client.BeginConnect($HostName, $Port, $null, $null)
            if ($connection.AsyncWaitHandle.WaitOne(1000)) {
                $client.EndConnect($connection)
                return
            }
        } catch {
            if ((Get-Date) -ge $deadline) {
                throw "Timed out waiting for ${HostName}:${Port}"
            }
        } finally {
            $client.Close()
        }

        if ((Get-Date) -ge $deadline) {
            throw "Timed out waiting for ${HostName}:${Port}"
        }

        Start-Sleep -Seconds 2
    } while ($true)
}

function Wait-AppPorts {
    Write-Host "Waiting for public app ports"
    Wait-TcpPort -HostName "localhost" -Port 8081
    Wait-TcpPort -HostName "localhost" -Port 8082
    Wait-TcpPort -HostName "localhost" -Port 8083
}

function Wait-ComposeService {
    param(
        [string]$Service,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $status = docker compose ps $Service --format "{{.Status}}"
        if ($status -match "healthy|Up") {
            return
        }

        if ((Get-Date) -ge $deadline) {
            throw "Timed out waiting for Compose service $Service"
        }

        Start-Sleep -Seconds 2
    } while ($true)
}

function Invoke-WithComposeServiceStopped {
    param(
        [string]$Service,
        [scriptblock]$Action
    )

    docker compose stop $Service | Out-Null
    try {
        & $Action
    } finally {
        docker compose start $Service | Out-Null
        Wait-ComposeService -Service $Service
        Wait-AppPorts
    }
}

function Wait-ForRabbitRecovery {
    param([hashtable]$Headers)

    $deadline = (Get-Date).AddSeconds(60)
    do {
        try {
            $probeUrl = New-ShortUrl -Headers $Headers
            Invoke-Redirect -ShortCode $probeUrl.shortCode
            return
        } catch {
            if ((Get-Date) -ge $deadline) {
                throw
            }
            Start-Sleep -Seconds 2
        }
    } while ($true)
}

function New-AuthHeaders {
    $jsonHeaders = @{ "Content-Type" = "application/json" }

    $registerBody = @{
        email = $Email
        password = $Password
    } | ConvertTo-Json

    Invoke-RestMethod `
        -Method Post `
        -Uri "$AuthBaseUrl/api/v1/auth/register" `
        -Headers $jsonHeaders `
        -Body $registerBody | Out-Null

    $loginBody = @{
        email = $Email
        password = $Password
    } | ConvertTo-Json

    $login = Invoke-RestMethod `
        -Method Post `
        -Uri "$AuthBaseUrl/api/v1/auth/login" `
        -Headers $jsonHeaders `
        -Body $loginBody

    return @{
        Authorization = "Bearer $($login.accessToken)"
        "Content-Type" = "application/json"
    }
}

function New-ShortUrl {
    param([hashtable]$Headers)

    $body = @{
        originalUrl = $OriginalUrl
    } | ConvertTo-Json

    return Invoke-RestMethod `
        -Method Post `
        -Uri "$UrlBaseUrl/api/v1/urls" `
        -Headers $Headers `
        -Body $body
}

function Invoke-Redirect {
    param([string]$ShortCode)

    try {
        Invoke-WebRequest `
            -Uri "$UrlBaseUrl/$ShortCode" `
            -MaximumRedirection 0 `
            -ErrorAction Stop | Out-Null
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 302) {
            throw
        }
    }
}

function Get-ClickCount {
    param([string]$ShortCode)

    $response = Invoke-RestMethod `
        -Method Get `
        -Uri "$AnalyticsBaseUrl/api/v1/analytics/urls/$ShortCode/clicks"

    return $response.clicks
}

Wait-AppPorts

$headers = New-AuthHeaders

Write-Host "1. Baseline: all infrastructure up"
$baselineUrl = New-ShortUrl -Headers $headers
Invoke-Redirect -ShortCode $baselineUrl.shortCode
Start-Sleep -Seconds 2
$baselineClicks = Get-ClickCount -ShortCode $baselineUrl.shortCode
Write-Host "   Created $($baselineUrl.shortCode); analytics clicks: $baselineClicks"

Write-Host "2. RabbitMQ outage: redirect should still work, analytics may miss the event"
Invoke-WithComposeServiceStopped -Service "rabbitmq" -Action {
    Invoke-Redirect -ShortCode $baselineUrl.shortCode
    Write-Host "   Redirect survived while RabbitMQ was stopped"
}
Wait-ForRabbitRecovery -Headers $headers

Write-Host "3. Redis outage: URL creation and redirect should still work"
Invoke-WithComposeServiceStopped -Service "redis" -Action {
    $redisDownUrl = New-ShortUrl -Headers $headers
    Invoke-Redirect -ShortCode $redisDownUrl.shortCode
    Write-Host "   Created and redirected $($redisDownUrl.shortCode) while Redis was stopped"
}

Write-Host "Done. Check Prometheus/Grafana for Resilience4j and custom error metrics."
