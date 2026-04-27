# ===========================================
# BJUT-ZXQ Windows 一键部署脚本
# 功能：本地构建前端 + 上传到服务器 + 远程部署
# ===========================================

# 服务器配置
$SERVER = "root@60.205.210.11"
$REMOTE_PATH = "/opt/bjut-zxq"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  BJUT-ZXQ Windows 部署脚本" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$START_TIME = Get-Date

# 步骤 1: 构建前端
Write-Host "[1/4] 构建前端..." -ForegroundColor Blue
Set-Location "$PSScriptRoot\Vue"

if (Test-Path "node_modules") {
    npm run build
} else {
    Write-Host "未找到 node_modules，先安装依赖..." -ForegroundColor Yellow
    npm ci
    npm run build
}

Set-Location $PSScriptRoot
Write-Host "✓ 前端构建完成" -ForegroundColor Green
Write-Host ""

# 步骤 2: 压缩文件
Write-Host "[2/4] 压缩项目文件..." -ForegroundColor Blue
if (Test-Path "deploy.zip") {
    Remove-Item deploy.zip -Force
}

# 排除不必要的文件
$excludeFiles = @(
    "node_modules",
    ".git",
    "logs",
    "*.log",
    "deploy.zip",
    ".env"
)

Compress-Archive -Path * -DestinationPath deploy.zip -Force -CompressionLevel Optimal
Write-Host "✓ 压缩完成" -ForegroundColor Green
Write-Host ""

# 步骤 3: 上传到服务器
Write-Host "[3/4] 上传到服务器..." -ForegroundColor Blue
scp deploy.zip "${SERVER}:/opt/"
Write-Host "✓ 上传完成" -ForegroundColor Green
Write-Host ""

# 步骤 4: 远程执行部署
Write-Host "[4/4] 远程部署..." -ForegroundColor Blue

# 完整部署：解压 + 重新构建（不拉取Git代码）
$remoteScript = @"
cd $REMOTE_PATH
rm -rf *
unzip -o /opt/deploy.zip
chmod +x deploy.sh
./deploy.sh 4
"@

ssh $SERVER $remoteScript

# 清理临时文件
Remove-Item deploy.zip -Force

# 计算耗时
$END_TIME = Get-Date
$ELAPSED = ($END_TIME - $START_TIME).TotalSeconds

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "  ✅ 部署完成!" -ForegroundColor Green
Write-Host ("  ⏱️  总耗时: {0:N0} 秒" -f $ELAPSED) -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📍 访问地址: http://60.205.210.11" -ForegroundColor Yellow
Write-Host ""
