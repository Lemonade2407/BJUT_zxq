#!/usr/bin/env bash
# ===========================================
# ProjecTree 本地开发启动脚本（bash / git-bash / WSL）
#
# 用法（默认一键全栈 Docker）：
#   ./dev.sh           启动全部 4 个容器（MySQL+Redis+后端+前端）
#   ./dev.sh rebuild   代码变更后重建镜像并启动
#   ./dev.sh down      停止全部容器
#   ./dev.sh logs      跟踪容器日志
#   ./dev.sh db        仅启动 MySQL+Redis 容器
#   ./dev.sh backend   本地原生跑后端（mvn，热更新，可选）
#   ./dev.sh frontend  本地原生跑前端（npm dev，可选）
# ===========================================
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="docker-compose.local.yml"
ACTION="${1:-all}"

check_docker() {
  if ! docker info >/dev/null 2>&1; then
    echo "Docker 未运行，请先启动 Docker Desktop" >&2
    exit 1
  fi
}

up_stack() {
  check_docker
  echo "[up] 构建并启动全栈容器（MySQL/Redis/后端/前端）..."
  docker compose -f "$ROOT/$COMPOSE" up -d --build || exit 1
  echo ""
  echo "✅ 已全部启动（Docker 容器）"
  echo "   前端: http://localhost:8080 （默认账号 admin / 123456）"
  echo "   后端: 容器内部，经 Nginx 代理 /api"
  echo "   数据库: localhost:3306   Redis: localhost:6379"
  echo "常用命令: ./dev.sh down / logs / rebuild"
}

down_stack() {
  check_docker
  echo "[down] 停止全部容器..."
  docker compose -f "$ROOT/$COMPOSE" down
  echo "[down] 已停止"
}

logs_stack() {
  check_docker
  docker compose -f "$ROOT/$COMPOSE" logs -f --tail=100
}

start_db() {
  check_docker
  echo "[db] 启动 MySQL+Redis 容器..."
  docker compose -f "$ROOT/$COMPOSE" up -d mysql redis
  echo "[db] 等待 MySQL 就绪..."
  for i in $(seq 1 30); do
    if [ "$(docker inspect -f "{{.State.Health.Status}}" bjut-zxq-mysql-local 2>/dev/null)" = "healthy" ]; then
      break
    fi
    sleep 2
  done
  echo "[db] 完成。MySQL: localhost:3306  Redis: localhost:6379"
}

# ===== 原生开发模式（可选，热更新用）=====

load_env() {
  if [ ! -f "$ROOT/.env" ]; then
    echo "缺少 .env，请先执行: cp .env.example .env 并填写配置" >&2
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "$ROOT/.env"
  set +a
  echo "✓ 已从 .env 加载环境变量"
}

start_backend() {
  load_env
  echo "[backend] 刷新 common/pojo 依赖到本地仓库..."
  (cd "$ROOT" && mvn -q -pl common,pojo -am install "-Dmaven.test.skip=true")
  echo "[backend] 启动后端 http://localhost:8080 （Ctrl+C 停止）"
  (cd "$ROOT/server" && mvn spring-boot:run "-Dmaven.test.skip=true")
}

start_frontend() {
  echo "[frontend] 启动前端 http://localhost:5173 （Ctrl+C 停止）"
  (cd "$ROOT/Vue" && npm run dev)
}

case "${ACTION}" in
  all|up|rebuild) up_stack ;;
  down) down_stack ;;
  logs) logs_stack ;;
  db) start_db ;;
  backend) start_backend ;;
  frontend) start_frontend ;;
  *) echo "未知操作: ${ACTION}（可用 all / rebuild / down / logs / db / backend / frontend）" ;;
esac
