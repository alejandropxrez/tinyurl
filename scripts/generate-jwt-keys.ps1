param(
    [switch]$WriteEnvFile,
    [string]$EnvFile = ".env"
)

$rsa = [System.Security.Cryptography.RSA]::Create(2048)

$privateKey = [Convert]::ToBase64String($rsa.ExportPkcs8PrivateKey())
$publicKey = [Convert]::ToBase64String($rsa.ExportSubjectPublicKeyInfo())

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
    ) | Set-Content -Path $EnvFile -Encoding utf8

    Write-Host ""
    Write-Host "Wrote Docker Compose env file: $EnvFile" -ForegroundColor Green
    Write-Host "This file should stay local and is ignored by Git."
}
