param(
    [switch]$WriteEnvFile,
    [string]$EnvFile = ".env",
    [string]$PostgresUrlsUser = "postgres",
    [string]$PostgresUrlsPassword,
    [string]$PostgresAuthUser = "postgres",
    [string]$PostgresAuthPassword,
    [string]$PostgresAnalyticsUser = "postgres",
    [string]$PostgresAnalyticsPassword,
    [string]$RabbitMqUsername = "tinyurl",
    [string]$RabbitMqPassword,
    [string]$GrafanaAdminUser = "admin",
    [string]$GrafanaAdminPassword
)

function New-LocalSecret {
    $bytes = New-Object byte[] 24
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToHexString($bytes).ToLowerInvariant()
}

$rsa = [System.Security.Cryptography.RSA]::Create(2048)

$privateKey = [Convert]::ToBase64String($rsa.ExportPkcs8PrivateKey())
$publicKey = [Convert]::ToBase64String($rsa.ExportSubjectPublicKeyInfo())

if ([string]::IsNullOrWhiteSpace($PostgresUrlsPassword)) {
    $PostgresUrlsPassword = New-LocalSecret
}

if ([string]::IsNullOrWhiteSpace($PostgresAuthPassword)) {
    $PostgresAuthPassword = New-LocalSecret
}

if ([string]::IsNullOrWhiteSpace($PostgresAnalyticsPassword)) {
    $PostgresAnalyticsPassword = New-LocalSecret
}

if ([string]::IsNullOrWhiteSpace($RabbitMqPassword)) {
    $RabbitMqPassword = New-LocalSecret
}

if ([string]::IsNullOrWhiteSpace($GrafanaAdminPassword)) {
    $GrafanaAdminPassword = New-LocalSecret
}

Write-Host "Generated RSA keypair for local JWT signing." -ForegroundColor Green
Write-Host ""
Write-Host "PowerShell session variables:"
Write-Host "`$env:JWT_PRIVATE_KEY='$privateKey'"
Write-Host "`$env:JWT_PUBLIC_KEY='$publicKey'"
Write-Host ""
Write-Host "Kubernetes commands:"
Write-Host "kubectl create secret generic auth-jwt-signing-secret -n tinyurl --from-literal=private-key='$privateKey' --dry-run=client -o yaml | kubectl apply -f -"
Write-Host "kubectl patch configmap auth-service-config -n tinyurl --type merge -p '{""data"":{""JWT_PUBLIC_KEY"":""$publicKey""}}'"
Write-Host "kubectl patch configmap url-service-config -n tinyurl --type merge -p '{""data"":{""JWT_PUBLIC_KEY"":""$publicKey""}}'"
Write-Host "kubectl rollout restart deployment/auth-service -n tinyurl"
Write-Host "kubectl rollout restart statefulset/url-service -n tinyurl"

if ($WriteEnvFile) {
    @(
        "JWT_PRIVATE_KEY=$privateKey"
        "JWT_PUBLIC_KEY=$publicKey"
        "POSTGRES_URLS_USER=$PostgresUrlsUser"
        "POSTGRES_URLS_PASSWORD=$PostgresUrlsPassword"
        "POSTGRES_AUTH_USER=$PostgresAuthUser"
        "POSTGRES_AUTH_PASSWORD=$PostgresAuthPassword"
        "POSTGRES_ANALYTICS_USER=$PostgresAnalyticsUser"
        "POSTGRES_ANALYTICS_PASSWORD=$PostgresAnalyticsPassword"
        "RABBITMQ_USERNAME=$RabbitMqUsername"
        "RABBITMQ_PASSWORD=$RabbitMqPassword"
        "GRAFANA_ADMIN_USER=$GrafanaAdminUser"
        "GRAFANA_ADMIN_PASSWORD=$GrafanaAdminPassword"
    ) | Set-Content -Path $EnvFile -Encoding utf8

    Write-Host ""
    Write-Host "Wrote Docker Compose env file: $EnvFile" -ForegroundColor Green
    Write-Host "This file should stay local and is ignored by Git."
    Write-Host "Postgres local users: $PostgresUrlsUser, $PostgresAuthUser, $PostgresAnalyticsUser"
    Write-Host "RabbitMQ local user: $RabbitMqUsername"
    Write-Host "Grafana local admin user: $GrafanaAdminUser"
    Write-Host "Grafana local admin password: $GrafanaAdminPassword"
}
