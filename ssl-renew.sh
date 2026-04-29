#!/bin/bash
# SSL 证书自动续期脚本
# 将此脚本添加到 crontab 中，每月执行一次

echo "开始更新 SSL 证书..."

# 停止 Docker 容器（释放 80 端口）
cd /path/to/BJUT_zxq
docker-compose down

# 更新证书
certbot renew --nginx

# 重新启动 Docker 容器
docker-compose up -d

echo "SSL 证书更新完成！"
echo "下次检查日期: $(date -d '+90 days' '+%Y-%m-%d')"
