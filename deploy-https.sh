#!/bin/bash
# ProjecTree知享圈 - HTTPS 一键部署脚本
# 使用方法: bash deploy-https.sh

set -e  # 遇到错误立即退出

echo "=========================================="
echo "  ProjecTree知享圈 HTTPS 部署脚本"
echo "=========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否以 root 权限运行
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}请使用 sudo 运行此脚本${NC}"
    echo "用法: sudo bash deploy-https.sh"
    exit 1
fi

# 获取项目路径
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo -e "${YELLOW}步骤 1: 检查系统环境...${NC}"

# 检测操作系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$NAME
else
    OS="unknown"
fi

echo "检测到操作系统: $OS"

# 安装 Certbot
echo ""
echo -e "${YELLOW}步骤 2: 安装 Certbot...${NC}"

case $OS in
    *"Ubuntu"*|*"Debian"*)
        apt update -y
        apt install -y certbot python3-certbot-nginx
        ;;
    *"CentOS"*|*"Red Hat"*|*"Fedora"*)
        yum install -y certbot python3-certbot-nginx
        ;;
    *)
        echo -e "${RED}不支持的操作系统: $OS${NC}"
        echo "请手动安装 certbot"
        exit 1
        ;;
esac

echo -e "${GREEN}✓ Certbot 安装成功${NC}"

# 创建 SSL 目录
echo ""
echo -e "${YELLOW}步骤 3: 创建 SSL 证书目录...${NC}"
mkdir -p ./ssl
chmod 755 ./ssl
echo -e "${GREEN}✓ SSL 目录创建成功${NC}"

# 停止 Docker 容器
echo ""
echo -e "${YELLOW}步骤 4: 停止 Docker 容器（释放 80 端口）...${NC}"
docker-compose down
echo -e "${GREEN}✓ Docker 容器已停止${NC}"

# 获取 SSL 证书
echo ""
echo -e "${YELLOW}步骤 5: 获取 SSL 证书...${NC}"
echo ""
echo "请输入你的邮箱地址（用于证书到期提醒）:"
read -p "邮箱: " EMAIL

certbot certonly --standalone \
    -d bjut-zxq.cn \
    -d www.bjut-zxq.cn \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    --non-interactive

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ SSL 证书获取成功${NC}"
else
    echo -e "${RED}✗ SSL 证书获取失败${NC}"
    echo "请检查："
    echo "  1. 域名 DNS 解析是否正确"
    echo "  2. 80 端口是否被占用"
    echo "  3. 防火墙是否开放 80 端口"
    exit 1
fi

# 复制证书文件
echo ""
echo -e "${YELLOW}步骤 6: 复制证书文件...${NC}"
cp /etc/letsencrypt/live/bjut-zxq.cn/fullchain.pem ./ssl/
cp /etc/letsencrypt/live/bjut-zxq.cn/privkey.pem ./ssl/

# 设置权限
chmod 644 ./ssl/fullchain.pem
chmod 600 ./ssl/privkey.pem

echo -e "${GREEN}✓ 证书文件已复制到 ./ssl/ 目录${NC}"

# 启动 Docker 容器
echo ""
echo -e "${YELLOW}步骤 7: 启动 Docker 容器...${NC}"
docker-compose up -d

# 等待容器启动
echo "等待容器启动..."
sleep 5

# 检查容器状态
echo ""
echo -e "${YELLOW}步骤 8: 检查容器状态...${NC}"
docker-compose ps

# 测试 HTTPS
echo ""
echo -e "${YELLOW}步骤 9: 测试 HTTPS 连接...${NC}"
if curl -sI https://bjut-zxq.cn | grep -q "200 OK"; then
    echo -e "${GREEN}✓ HTTPS 配置成功！${NC}"
else
    echo -e "${YELLOW}⚠ HTTPS 可能需要几分钟才能完全生效${NC}"
fi

# 显示证书信息
echo ""
echo -e "${YELLOW}证书信息:${NC}"
echo | openssl s_client -connect bjut-zxq.cn:443 2>/dev/null | openssl x509 -noout -dates || echo "证书信息暂时无法获取，请稍后检查"

# 设置自动续期
echo ""
echo -e "${YELLOW}步骤 10: 配置自动续期...${NC}"

# 创建续期脚本
cat > ./ssl-renew.sh << 'EOF'
#!/bin/bash
# SSL 证书自动续期脚本

echo "开始更新 SSL 证书..."

# 进入项目目录
cd "$(dirname "$0")"

# 停止 Docker 容器
docker-compose down

# 更新证书
certbot renew --non-interactive

# 复制新证书
cp /etc/letsencrypt/live/bjut-zxq.cn/fullchain.pem ./ssl/
cp /etc/letsencrypt/live/bjut-zxq.cn/privkey.pem ./ssl/

# 设置权限
chmod 644 ./ssl/fullchain.pem
chmod 600 ./ssl/privkey.pem

# 重启 Docker 容器
docker-compose up -d

echo "SSL 证书更新完成！"
EOF

chmod +x ./ssl-renew.sh

# 添加到 crontab
(crontab -l 2>/dev/null | grep -v "ssl-renew.sh"; echo "0 3 1 * * $PROJECT_DIR/ssl-renew.sh >> /var/log/ssl-renew.log 2>&1") | crontab -

echo -e "${GREEN}✓ 自动续期已配置（每月1号凌晨3点执行）${NC}"

# 完成
echo ""
echo "=========================================="
echo -e "${GREEN}  🎉 HTTPS 部署完成！${NC}"
echo "=========================================="
echo ""
echo "访问地址:"
echo "  - https://bjut-zxq.cn"
echo "  - https://www.bjut-zxq.cn"
echo ""
echo "重要提示:"
echo "  1. 确保证书文件安全，不要泄露 privkey.pem"
echo "  2. 证书有效期为 90 天，已配置自动续期"
echo "  3. 可以使用 'sudo certbot certificates' 查看证书信息"
echo "  4. 建议访问 https://www.ssllabs.com/ssltest/ 测试 SSL 等级"
echo ""
echo "如有问题，请查看日志:"
echo "  - Docker 日志: docker-compose logs -f"
echo "  - Certbot 日志: /var/log/letsencrypt/letsencrypt.log"
echo ""
