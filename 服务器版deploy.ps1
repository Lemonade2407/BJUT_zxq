# ===========================================
# BJUT-ZXQ One-Click Deployment Script
# Function: Local Build + Docker Deployment
# ===========================================

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  BJUT-ZXQ Deployment Script" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$START_TIME = Get-Date

# Step 1: Check Docker status
Write-Host "[1/4] Checking Docker status..." -ForegroundColor Blue
try {
    docker info | Out-Null
    Write-Host "[OK] Docker is running" -ForegroundColor Green
} catch {
    Write-Host "[Error] Docker is not running, please start Docker Desktop first" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 2: Stop old containers
Write-Host "[2/4] Stopping old containers..." -ForegroundColor Blue
docker compose down
Write-Host "[OK] Old containers stopped" -ForegroundColor Green
Write-Host ""

# Step 3: Rebuild and start services
Write-Host "[3/4] Rebuilding and starting services..." -ForegroundColor Blue
docker compose up -d --build
Write-Host "[OK] Services started" -ForegroundColor Green
Write-Host ""

# Step 4: Wait for services to be ready
Write-Host "[4/4] Waiting for services to be ready..." -ForegroundColor Blue
Write-Host "Waiting for backend to start (about 30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Check service status (使用简单的文本解析，避免 JSON 编码问题)
$servicesText = docker compose ps --format "table {{.Service}}\t{{.State}}\t{{.Status}}"
Write-Host ""
Write-Host "Service Status:" -ForegroundColor Cyan
$servicesText | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
Write-Host ""

# 简单检查是否有 running 状态
$runningCount = ($servicesText | Select-String "running" | Measure-Object).Count
$serviceCount = ($servicesText | Where-Object { $_ -match "^(backend|frontend|mysql|redis)" } | Measure-Object).Count

if ($runningCount -ge $serviceCount) {
    Write-Host "[OK] All services are running" -ForegroundColor Green
} else {
    Write-Host "[Warning] Some services may not be running" -ForegroundColor Yellow
    Write-Host "   Check logs: docker compose logs" -ForegroundColor Cyan
}

Write-Host ""

# Calculate elapsed time
$END_TIME = Get-Date
$ELAPSED = ($END_TIME - $START_TIME).TotalSeconds
$ELAPSED_ROUNDED = [Math]::Round($ELAPSED, 0)

Write-Host "=========================================" -ForegroundColor Green
Write-Host "  [OK] Deployment Complete!" -ForegroundColor Green
Write-Host "  [Time] Total time: $ELAPSED_ROUNDED seconds" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "[Info] Access URLs:" -ForegroundColor Yellow
Write-Host "   Local: http://localhost" -ForegroundColor Cyan
Write-Host "   LAN: http://$(Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.InterfaceAlias -notlike '*Loopback*' -and $_.IPAddress -like '192.168.*' -or $_.IPAddress -like '172.*' -or $_.IPAddress -like '10.*'} | Select-Object -First 1 -ExpandProperty IPAddress)" -ForegroundColor Cyan
Write-Host ""
Write-Host "[Help] Common commands:" -ForegroundColor Yellow
Write-Host "   View logs: docker compose logs -f" -ForegroundColor Cyan
Write-Host "   Restart services: docker compose restart" -ForegroundColor Cyan
Write-Host "   Stop services: docker compose down" -ForegroundColor Cyan
Write-Host ""
