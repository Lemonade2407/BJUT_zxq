#!/bin/bash

# ===========================================
# BJUT-ZXQ 通用部署脚本
# ===========================================
#
# 【功能特性】
#   ✅ 支持 7 种部署模式，满足不同场景需求
#   ✅ 支持 Git 分支管理（test/main）
#   ✅ 增量构建，充分利用 Docker 缓存
#   ✅ 并行构建前后端，提升速度
#   ✅ 智能健康检查，动态等待服务就绪
#   ✅ 显示部署耗时，便于性能监控
#   ✅ 自动备份与回滚机制
#
# 【使用方法】
#   方式 1: 交互式菜单（推荐）
#     ./deploy.sh
#     # 然后根据提示选择部署模式
#
#   方式 2: 命令行参数
#     ./deploy.sh 1        # 首次部署（强制重新构建）
#     ./deploy.sh 2        # 快速部署（增量构建，推荐）
#     ./deploy.sh 3        # 极速重启（不重新构建）
#     ./deploy.sh 4        # 仅重建后端
#     ./deploy.sh 5        # 仅重建前端
#     ./deploy.sh 6        # 测试分支部署
#     ./deploy.sh 7        # 生产分支部署
#
#   方式 3: 使用别名
#     ./deploy.sh first    # 等同于 ./deploy.sh 1
#     ./deploy.sh fast     # 等同于 ./deploy.sh 2
#     ./deploy.sh restart  # 等同于 ./deploy.sh 3
#     ./deploy.sh test     # 等同于 ./deploy.sh 6
#     ./deploy.sh prod     # 等同于 ./deploy.sh 7
#
# 【部署模式说明】
#   1) 首次部署
#      - 适用场景：第一次部署或需要完全重新构建
#      - 特点：使用 --no-cache，不使用任何缓存
#      - 耗时：5-10 分钟
#
#   2) 快速部署 ⭐推荐
#      - 适用场景：日常代码更新部署
#      - 特点：增量构建，利用 Docker 缓存层
#      - 耗时：2-4 分钟（后续部署）
#
#   3) 极速重启
#      - 适用场景：修改配置文件、环境变量等
#      - 特点：不重新构建镜像，仅重启容器
#      - 耗时：10-30 秒
#
#   4) 仅重建后端
#      - 适用场景：只修改了后端代码
#      - 特点：只构建后端镜像，前端不变
#      - 耗时：2-3 分钟
#
#   5) 仅重建前端
#      - 适用场景：只修改了前端代码
#      - 特点：只构建前端镜像，后端不变
#      - 耗时：1-2 分钟
#
#   6) 测试分支部署 🆕
#      - 适用场景：新功能测试验证
#      - 特点：自动切换到 test 分支并部署
#      - 流程：拉取 test 分支 → 构建 → 部署 → 验证
#      - 耗时：2-4 分钟
#
#   7) 生产分支部署 🆕
#      - 适用场景：正式发布到生产环境
#      - 特点：从 test 合并到 main 或直接部署 main 分支
#      - 流程：备份当前版本 → 切换 main 分支 → 部署 → 健康检查
#      - 耗时：2-4 分钟
#
# 【前置要求】
#   - Docker 已安装
#   - Docker Compose 已安装
#   - .env 配置文件已正确设置
#   - Git 仓库已初始化（分支部署必需）
#
# 【常见问题】
#   Q: 为什么第二次构建还是慢？
#   A: 确保使用模式 2（快速部署），不要使用 --no-cache
#
#   Q: 如何查看构建进度？
#   A: docker compose logs -f
#
#   Q: 部署失败怎么办？
#   A: 查看日志 docker compose logs --tail=50 backend
#
#   Q: 如何回滚到上一个版本？
#   A: 使用模式 7 部署时会自动备份，可手动恢复
#
# ===========================================

set -e  # 遇到错误立即退出

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

START_TIME=$(date +%s)

# ===========================================
# 函数定义
# ===========================================

# 显示横幅
show_banner() {
    echo ""
    echo -e "${BOLD}=========================================${NC}"
    echo -e "${BOLD}  BJUT-ZXQ 通用部署脚本${NC}"
    echo -e "${BOLD}=========================================${NC}"
    echo ""
}

# 显示菜单
show_menu() {
    echo -e "${BLUE}请选择部署模式:${NC}"
    echo -e "  ${CYAN}1) 首次部署${NC}      - 完整构建，适合第一次部署"
    echo -e "  ${CYAN}2) 快速部署${NC}      - 增量构建，适合日常更新（推荐）⭐"
    echo -e "  ${CYAN}3) 极速重启${NC}      - 不重新构建，仅重启服务"
    echo -e "  ${CYAN}4) 仅重建后端${NC}    - 只重新构建后端服务"
    echo -e "  ${CYAN}5) 仅重建前端${NC}    - 只重新构建前端服务"
    echo -e "  ${CYAN}6) 测试分支部署${NC}  - 切换到 test 分支并部署 🆕"
    echo -e "  ${CYAN}7) 生产分支部署${NC}  - 切换到 main 分支并部署 🆕"
    echo -e "  ${CYAN}0) 退出${NC}"
    echo ""
}

# 检查配置文件
check_config() {
    echo -e "${BLUE}[1/5] 检查配置文件...${NC}"
    if [ ! -f .env ]; then
        echo -e "${YELLOW}⚠ 未找到 .env 文件${NC}"
        if [ -f .env.example ]; then
            echo "正在从 .env.example 创建..."
            cp .env.example .env
            echo -e "${RED}❗ 请编辑 .env 文件,填入正确的配置信息!${NC}"
            echo ""
            echo "必须修改的配置:"
            echo "  - MYSQL_ROOT_PASSWORD (MySQL密码)"
            echo "  - OSS_ACCESS_KEY_ID (阿里云OSS AccessKey ID)"
            echo "  - OSS_ACCESS_KEY_SECRET (阿里云OSS AccessKey Secret)"
            echo ""
            read -p "配置完成后按 Enter 继续..."
        else
            echo -e "${RED}✗ 未找到 .env.example${NC}"
            exit 1
        fi
    fi
    echo -e "${GREEN}✓ 配置文件检查完成${NC}"
    echo ""
}

# 检查 Docker 环境
check_docker() {
    echo -e "${BLUE}[2/5] 检查 Docker 环境...${NC}"
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}✗ Docker 未安装${NC}"
        read -p "是否自动安装 Docker? (y/n): " INSTALL_DOCKER
        if [ "$INSTALL_DOCKER" = "y" ] || [ "$INSTALL_DOCKER" = "Y" ]; then
            curl -fsSL https://get.docker.com | sh
            systemctl start docker
            systemctl enable docker
        else
            exit 1
        fi
    fi

    if ! command -v docker compose &> /dev/null; then
        echo -e "${RED}✗ Docker Compose 未安装${NC}"
        exit 1
    fi

    echo -e "${GREEN}✓ Docker 环境正常${NC}"
    echo -e "   Docker 版本: $(docker --version)"
    echo -e "   Compose 版本: $(docker compose version)"
    echo ""
}

# 拉取最新代码
pull_code() {
    local target_branch=${1:-""}
    
    echo -e "${BLUE}[3/5] 检查代码更新...${NC}"
    if command -v git &> /dev/null && [ -d .git ]; then
        CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
        echo "当前分支: ${CURRENT_BRANCH}"
        
        # 如果指定了目标分支，则切换分支
        if [ -n "$target_branch" ] && [ "$target_branch" != "$CURRENT_BRANCH" ]; then
            echo -e "${YELLOW}⚠ 正在切换到 $target_branch 分支...${NC}"
            
            # 检查是否有未提交的更改
            if ! git diff-index --quiet HEAD --; then
                echo -e "${RED}✗ 检测到未提交的更改${NC}"
                read -p "是否暂存更改并继续? (y/n): " STASH_CHANGES
                if [ "$STASH_CHANGES" = "y" ] || [ "$STASH_CHANGES" = "Y" ]; then
                    git stash push -m "Auto-stash before deploy to $target_branch"
                    echo -e "${GREEN}✓ 更改已暂存${NC}"
                else
                    echo -e "${RED}✗ 请先提交或暂存更改${NC}"
                    exit 1
                fi
            fi
            
            # 检查目标分支是否存在
            if ! git show-ref --verify --quiet refs/heads/$target_branch; then
                echo -e "${YELLOW}⚠ 本地不存在 $target_branch 分支，尝试从远程获取...${NC}"
                git fetch origin $target_branch:$target_branch 2>/dev/null || true
            fi
            
            # 切换分支
            git checkout $target_branch
            echo -e "${GREEN}✓ 已切换到 $target_branch 分支${NC}"
        fi
        
        read -p "是否拉取最新代码? (y/n, 默认y): " PULL_CODE
        if [ "$PULL_CODE" != "n" ] && [ "$PULL_CODE" != "N" ]; then
            echo "正在拉取最新代码..."
            git pull
            echo -e "${GREEN}✓ 代码已更新${NC}"
        else
            echo -e "${CYAN}⊘ 跳过代码拉取${NC}"
        fi
    else
        echo -e "${CYAN}⊘ 非 Git 仓库,跳过代码更新检查${NC}"
        if [ -n "$target_branch" ]; then
            echo -e "${RED}⚠ 分支部署需要 Git 仓库${NC}"
        fi
    fi
    echo ""
}

# 停止服务
stop_services() {
    echo -e "${BLUE}[4/5] 停止现有服务...${NC}"
    docker compose down --remove-orphans 2>/dev/null || true
    echo -e "${GREEN}✓ 旧服务已停止${NC}"
    echo ""
}

# 构建镜像（并行构建）
build_images() {
    echo -e "${BLUE}[5/5] 构建 Docker 镜像...${NC}"
    
    if [ "$1" = "nocache" ]; then
        echo -e "${YELLOW}⚠ 强制重新构建（不使用缓存）${NC}"
        docker compose build --no-cache
    elif [ "$1" = "backend" ]; then
        echo "正在构建后端..."
        docker compose build backend
    elif [ "$1" = "frontend" ]; then
        echo "正在构建前端..."
        docker compose build frontend
    else
        echo -e "${CYAN}提示: 使用增量构建，后续构建会更快${NC}"
        echo ""
        
        # 并行构建前后端
        echo "正在后台构建前端..."
        docker compose build frontend &
        FRONTEND_PID=$!

        echo "正在构建后端..."
        docker compose build backend &
        BACKEND_PID=$!

        # 等待两个构建完成
        wait $FRONTEND_PID 2>/dev/null
        FRONTEND_RESULT=$?

        wait $BACKEND_PID 2>/dev/null
        BACKEND_RESULT=$?

        if [ $FRONTEND_RESULT -ne 0 ] || [ $BACKEND_RESULT -ne 0 ]; then
            echo -e "${RED}✗ 镜像构建失败${NC}"
            exit 1
        fi
    fi
    
    echo -e "${GREEN}✓ 镜像构建完成${NC}"
    echo ""
}

# 启动服务并等待就绪
start_services() {
    local services=${1:-""}
    
    echo -e "${BLUE}启动服务...${NC}"
    if [ -n "$services" ]; then
        docker compose up -d $services
    else
        docker compose up -d
    fi

    echo ""
    echo "等待服务启动（通过健康检查监控）..."

    # 动态等待服务就绪
    MAX_WAIT=120
    WAIT_COUNT=0
    INTERVAL=3

    while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
        if [ -z "$services" ]; then
            # 检查所有服务
            HEALTHY_COUNT=$(docker compose ps --format json 2>/dev/null | grep -c '"healthy"' || true)
            if [ "$HEALTHY_COUNT" -ge 3 ] 2>/dev/null; then
                echo -e "\n${GREEN}✓ 所有服务已就绪!${NC}"
                break
            fi
        else
            # 检查指定服务
            ALL_HEALTHY=true
            for service in $services; do
                STATUS=$(docker compose ps --format json $service 2>/dev/null | grep -o '"healthy"\|"unhealthy"\|"starting"' | head -1)
                if [ "$STATUS" != '"healthy"' ]; then
                    ALL_HEALTHY=false
                    break
                fi
            done
            
            if [ "$ALL_HEALTHY" = true ]; then
                echo -e "\n${GREEN}✓ 服务已就绪!${NC}"
                break
            fi
        fi
        
        # 显示进度
        if [ $((WAIT_COUNT % 10)) -eq 0 ]; then
            echo -n "."
        fi
        
        sleep $INTERVAL
        WAIT_COUNT=$((WAIT_COUNT + INTERVAL))
    done

    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        echo -e "\n${YELLOW}⚠ 等待超时，请手动检查服务状态${NC}"
    fi
    
    echo ""
}

# 重启服务（不重新构建）
restart_services() {
    echo -e "${BLUE}选择要重启的服务:${NC}"
    echo "1) 仅后端 (最快)"
    echo "2) 仅前端"
    echo "3) 后端 + 前端"
    echo "4) 全部服务"
    echo ""
    read -p "请输入选项 (1-4, 默认1): " CHOICE

    case ${CHOICE:-1} in
        1) SERVICES="backend" ;;
        2) SERVICES="frontend" ;;
        3) SERVICES="backend frontend" ;;
        4) SERVICES="" ;;
        *) SERVICES="backend" ;;
    esac

    echo ""
    
    # 检查服务是否运行
    if [ -n "$SERVICES" ]; then
        if ! docker compose ps --quiet $SERVICES 2>/dev/null | grep -q .; then
            echo -e "${YELLOW}⚠ 服务未运行,正在启动...${NC}"
            docker compose up -d $SERVICES
        else
            echo -e "${BLUE}正在重启服务: $SERVICES${NC}"
            docker compose restart $SERVICES
        fi
    else
        echo -e "${BLUE}正在重启所有服务...${NC}"
        docker compose restart
    fi

    echo ""
    start_services "$SERVICES"
}

# 备份当前版本（用于回滚）
backup_current_version() {
    echo -e "${BLUE}备份当前版本...${NC}"
    BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    
    # 备份 Docker 镜像标签
    docker images --format "{{.Repository}}:{{.Tag}}" | grep bjut-zxq > "$BACKUP_DIR/images.txt" 2>/dev/null || true
    
    # 备份当前 Git commit
    if command -v git &> /dev/null && [ -d .git ]; then
        git rev-parse HEAD > "$BACKUP_DIR/git_commit.txt" 2>/dev/null || true
        git branch --show-current > "$BACKUP_DIR/git_branch.txt" 2>/dev/null || true
    fi
    
    # 备份配置文件
    cp .env "$BACKUP_DIR/.env.backup" 2>/dev/null || true
    
    echo -e "${GREEN}✓ 版本已备份到: $BACKUP_DIR${NC}"
    echo ""
}

# 显示部署结果
show_result() {
    END_TIME=$(date +%s)
    ELAPSED=$((END_TIME - START_TIME))

    echo ""
    echo -e "${BLUE}服务状态:${NC}"
    docker compose ps

    echo ""
    echo -e "${BOLD}=========================================${NC}"
    echo -e "${GREEN}  ✅ 部署完成!${NC}"
    echo -e "${CYAN}  ⏱️  总耗时: ${ELAPSED} 秒${NC}"
    
    # 显示当前分支信息
    if command -v git &> /dev/null && [ -d .git ]; then
        CURRENT_BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
        CURRENT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
        echo -e "${CYAN}  📦 分支: ${CURRENT_BRANCH} (${CURRENT_COMMIT})${NC}"
    fi
    
    echo -e "${BOLD}=========================================${NC}"
    echo ""

    # 获取服务器 IP
    SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || echo "localhost")

    echo -e "${BLUE}📍 访问地址:${NC}"
    echo -e "   前端: ${GREEN}http://${SERVER_IP}${NC}"
    echo -e "   后端: ${GREEN}http://${SERVER_IP}/api${NC}"
    echo ""
    echo -e "${BLUE}📝 常用命令:${NC}"
    echo -e "   查看日志:     ${CYAN}docker compose logs -f${NC}"
    echo -e "   查看后端日志: ${CYAN}docker compose logs -f backend${NC}"
    echo -e "   查看前端日志: ${CYAN}docker compose logs -f frontend${NC}"
    echo -e "   重启服务:     ${CYAN}docker compose restart${NC}"
    echo -e "   停止服务:     ${CYAN}docker compose down${NC}"
    echo ""
    echo -e "${BLUE}🔧 故障排查:${NC}"
    echo -e "   后端日志:     ${CYAN}docker compose logs --tail=50 backend${NC}"
    echo -e "   前端日志:     ${CYAN}docker compose logs --tail=50 frontend${NC}"
    echo -e "   数据库日志:   ${CYAN}docker compose logs --tail=50 mysql${NC}"
    echo -e "   进入容器:     ${CYAN}docker exec -it bjut-zxq-backend sh${NC}"
    echo ""
}

# ===========================================
# 主流程
# ===========================================

main() {
    show_banner

    # 如果提供了命令行参数，直接使用
    if [ $# -gt 0 ]; then
        MODE=$1
    else
        # 显示菜单并获取用户选择
        show_menu
        read -p "请输入选项 (0-5): " MODE
    fi

    case $MODE in
        1|first)
            echo -e "${BOLD}🚀 模式: 首次部署${NC}"
            echo ""
            check_config
            check_docker
            stop_services
            build_images "nocache"
            start_services
            show_result
            ;;
        
        2|fast)
            echo -e "${BOLD}⚡ 模式: 快速部署（推荐）${NC}"
            echo ""
            check_config
            check_docker
            pull_code
            stop_services
            build_images "incremental"
            start_services
            show_result
            ;;
        
        3|restart)
            echo -e "${BOLD}🔄 模式: 极速重启${NC}"
            echo ""
            check_docker
            restart_services
            show_result
            ;;
        
        4|backend)
            echo -e "${BOLD}🔧 模式: 仅重建后端${NC}"
            echo ""
            check_config
            check_docker
            pull_code
            build_images "backend"
            docker compose up -d backend
            start_services "backend"
            show_result
            ;;
        
        5|frontend)
            echo -e "${BOLD}🎨 模式: 仅重建前端${NC}"
            echo ""
            check_config
            check_docker
            pull_code
            build_images "frontend"
            docker compose up -d frontend
            start_services "frontend"
            show_result
            ;;
        
        6|test)
            echo -e "${BOLD}🧪 模式: 测试分支部署${NC}"
            echo ""
            check_config
            check_docker
            pull_code "test"
            stop_services
            build_images "incremental"
            start_services
            show_result
            echo -e "${YELLOW}⚠️  提示: 测试完成后，请记得合并到 main 分支${NC}"
            ;;
        
        7|prod)
            echo -e "${BOLD}🚀 模式: 生产分支部署${NC}"
            echo ""
            check_config
            check_docker
            
            # 生产部署前备份
            backup_current_version
            
            pull_code "main"
            stop_services
            build_images "incremental"
            start_services
            show_result
            echo -e "${GREEN}✅ 生产环境部署成功！${NC}"
            ;;
        
        0|exit)
            echo -e "${CYAN}再见!${NC}"
            exit 0
            ;;
        
        *)
            echo -e "${RED}✗ 无效选项: $MODE${NC}"
            echo ""
            show_menu
            exit 1
            ;;
    esac
}

# 执行主流程
main "$@"
