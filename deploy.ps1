# ===========================================
# BJUT-ZXQ Windows 一键部署脚本
# 功能：本地构建前端 + 上传到服务器 + 远程部署
# 支持 Git 分支管理（test/main）
# ===========================================

# 服务器配置
$SERVER = "root@60.205.210.11"
$REMOTE_PATH = "/opt/bjut-zxq"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  BJUT-ZXQ Windows 部署脚本" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 选择部署模式
Write-Host "请选择部署模式:" -ForegroundColor Blue
Write-Host "  1) 完整部署 (构建前端 + 上传 + 远程部署)" -ForegroundColor Cyan
Write-Host "  2) 仅上传并重启 (不重新构建)" -ForegroundColor Cyan
Write-Host "  3) 测试分支部署 (切换到 test 分支)" -ForegroundColor Cyan
Write-Host "  4) 生产分支部署 (切换到 main 分支)" -ForegroundColor Cyan
Write-Host "  0) 退出" -ForegroundColor Cyan
Write-Host ""
$choice = Read-Host "请输入选项 (0-4, 默认1)"

if ($choice -eq "0") {
    Write-Host "再见!" -ForegroundColor Gray
    exit 0
}

$START_TIME = Get-Date

# 步骤 1: 构建前端（如果需要）
if ($choice -eq "1" -or $choice -eq "") {
    Write-Host ""
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
}

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

if ($choice -eq "1" -or $choice -eq "") {
    # 完整部署：解压 + 重新构建
    $remoteScript = @"
cd $REMOTE_PATH
rm -rf *
unzip -o /opt/deploy.zip
chmod +x deploy.sh
./deploy.sh 2
"@
} elseif ($choice -eq "2") {
    # 仅重启：不解压，直接重启
    $remoteScript = @"
cd $REMOTE_PATH
docker compose restart
"@
} elseif ($choice -eq "3") {
    # 测试分支部署
    $remoteScript = @"
cd $REMOTE_PATH
rm -rf *
unzip -o /opt/deploy.zip
chmod +x deploy.sh
./deploy.sh 6
"@
} elseif ($choice -eq "4") {
    # 生产分支部署
    $remoteScript = @"
cd $REMOTE_PATH
rm -rf *
unzip -o /opt/deploy.zip
chmod +x deploy.sh
./deploy.sh 7
"@
}

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
