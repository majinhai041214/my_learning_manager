$ErrorActionPreference = "Stop"

param(
    [string]$Target = "/var/www/my-site/backend",
    [string]$Server = "ubuntu@your-server-ip"
)

Write-Host "Packaging Spring Boot backend..."
mvn -f ../backend/pom.xml clean package -DskipTests

Write-Host "Uploading backend jar to $Server`:$Target"
scp ../backend/target/website-backend-0.0.1-SNAPSHOT.jar "$Server`:$Target/app.jar"
