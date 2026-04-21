$ErrorActionPreference = "Stop"

param(
    [string]$Target = "/var/www/my-site/frontend",
    [string]$Server = "ubuntu@your-server-ip"
)

Write-Host "Uploading frontend build output to $Server`:$Target"
scp -r .output/public/* "$Server`:$Target/"
