# ===========================================
# ProjecTree 本地开发启动脚本（Windows PowerShell）
#
# 用法（默认一键全栈 Docker）：
#   ./dev.ps1          启动全部 4 个容器（MySQL+Redis+后端+前端）
#   ./dev.ps1 rebuild  代码变更后重建镜像并启动
#   ./dev.ps1 down     停止全部容器
#   ./dev.ps1 logs     跟踪容器日志
#   ./dev.ps1 db       仅启动 MySQL+Redis 容器
#   ./dev.ps1 backend  本地原生跑后端（mvn，热更新，可选）
#   ./dev.ps1 frontend 本地原生跑前端（npm dev，可选）
#
# 前置要求：已启动 Docker Desktop；已安装 JDK 21、Maven、Node 20+（原生模式需要）。
# ===========================================
param(
    [string]$Action = "all"
)

$root = $PSScriptRoot
$ComposeFile = "docker-compose.local.yml"

function Check-Docker {
    docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Docker 未运行，请先启动 Docker Desktop" -ForegroundColor Red
        exit 1
    }
}

function Up-Stack {
    Check-Docker
    Write-Host "[up] 构建并启动全栈容器（MySQL/Redis/后端/前端）..." -ForegroundColor Cyan
    docker compose -f (Join-Path $root $ComposeFile) up -d --build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[up] 启动失败，见上方日志" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
    Write-Host "✅ 已全部启动（Docker 容器）" -ForegroundColor Green
    Write-Host "   前端: http://localhost:8080 （默认账号 admin / 123456）" -ForegroundColor White
    Write-Host "   后端: 容器内部，经 Nginx 代理 /api" -ForegroundColor White
    Write-Host "   数据库: localhost:3306   Redis: localhost:6379" -ForegroundColor White
    Write-Host "常用命令: ./dev.ps1 down / logs / rebuild" -ForegroundColor Yellow
}

function Down-Stack {
    Check-Docker
    Write-Host "[down] 停止全部容器..." -ForegroundColor Cyan
    docker compose -f (Join-Path $root $ComposeFile) down
    Write-Host "[down] 已停止" -ForegroundColor Green
}

function Logs-Stack {
    Check-Docker
    docker compose -f (Join-Path $root $ComposeFile) logs -f --tail=100
}

function Start-Db {
    Check-Docker
    Write-Host "[db] 启动 MySQL+Redis 容器..." -ForegroundColor Cyan
    docker compose -f (Join-Path $root $ComposeFile) up -d mysql redis
    # 等待 MySQL 健康
    Write-Host "[db] 等待 MySQL 就绪..." -ForegroundColor Cyan
    for ($i = 0; $i -lt 30; $i++) {
        $status = docker inspect -f "{{.State.Health.Status}}" bjut-zxq-mysql-local 2>$null
        if ($status -eq "healthy") { break }
        Start-Sleep -Seconds 2
    }
    Write-Host "[db] 完成。MySQL: localhost:3306  Redis: localhost:6379" -ForegroundColor Green
}

# ===== 原生开发模式（可选，热更新用）=====

function Load-Env {
    $envFile = Join-Path $root ".env"
    if (-not (Test-Path $envFile)) {
        Write-Host "缺少 .env，请先执行: cp .env.example .env 并填写配置" -ForegroundColor Red
        exit 1
    }
    Get-Content $envFile | Where-Object { $_ -match '^\s*[^#;].*=' } | ForEach-Object {
        $kv = $_ -split '=', 2
        Set-Item -Path "env:$($kv[0].Trim())" -Value $kv[1].Trim()
    }
    Write-Host "✓ 已从 .env 加载环境变量" -ForegroundColor Green
}

function Start-Backend {
    Load-Env
    Write-Host "[backend] 刷新 common/pojo 依赖到本地仓库..." -ForegroundColor Cyan
    Push-Location $root
    # 注意：PowerShell 会拆分未加引号的 -D 参数，必须加引号
    mvn -q -pl common,pojo -am install "-Dmaven.test.skip=true"
    Pop-Location
    Write-Host "[backend] 启动后端 http://localhost:8080 （Ctrl+C 停止）" -ForegroundColor Green
    Push-Location (Join-Path $root "server")
    mvn spring-boot:run "-Dmaven.test.skip=true"
    Pop-Location
}

function Start-Frontend {
    Write-Host "[frontend] 启动前端 http://localhost:5173 （Ctrl+C 停止）" -ForegroundColor Green
    Push-Location (Join-Path $root "Vue")
    if (-not (Test-Path "node_modules")) {
        Write-Host "[frontend] 首次运行，先安装依赖..." -ForegroundColor Yellow
        npm ci
    }
    npm run dev
    Pop-Location
}

switch ($Action.ToLower()) {
    "all"     { Up-Stack }
    "up"      { Up-Stack }
    "rebuild" { Up-Stack }
    "down"    { Down-Stack }
    "logs"    { Logs-Stack }
    "db"      { Start-Db }
    "backend" { Start-Backend }
    "frontend"{ Start-Frontend }
    default   { Write-Host "未知操作: $Action（可用 all / rebuild / down / logs / db / backend / frontend）" -ForegroundColor Red }
}
