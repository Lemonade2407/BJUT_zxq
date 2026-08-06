# ProjecTree AI 选题与组队助手 · 设计方案

> 版本：v1.0（MVP）　日期：2026-08-04　状态：已确认待实施
> 选型结论：内嵌聊天入口 + DeepSeek API + 薄客户端 + Function Calling + 仅平台内数据（MVP 不做 RAG）

## 1. 背景与目标

知享圈（ProjecTree）已具备用户、项目（课设/毕设/竞赛/个人）、组队广场、标签、课程字典等数据。
本方案在其上新增一个 **AI 助手 Agent**，帮助用户：

1. **选题**：结合用户的专业、技术栈、课程、兴趣与工作量预算，推荐课设/竞赛/毕设选题，并给出功能拆解、技术选型与里程碑。
2. **组队**：根据用户画像与要求，从组队广场的「招募中」队伍中做匹配打分并解释理由，一键跳转现有申请流程。

### 1.1 MVP 明确不做

| 不做 | 原因 |
|---|---|
| 外部爬虫 / 外部资讯聚合 | 范围大、时效性维护成本高，二期再议 |
| RAG / 向量检索 | 数据是结构化表，function calling 工具检索已满足；触发条件见 §10 |
| 本地模型 / Ollama | 无 GPU，云端 DeepSeek 足够 |
| 语音 / 多轮长对话记忆持久化复杂化 | 会话落库即可，不做向量记忆 |

## 2. 总体架构

```
Vue 前端（新增 /ai 聊天页）
   │ fetch + ReadableStream（SSE）
   ▼
POST /api/ai/chat ── JWT 拦截器（复用现有）→ Redis 限流
   ▼
┌───────────────────────────────────────────────┐
│ AiAgentService（Agent 编排循环，Java 21 虚拟线程）│
│ 1. 构建 System Prompt（含用户画像）+ 历史消息      │
│ 2. 调 DeepSeek chatStream（流式 + tool_calls）   │
│ 3. 内容增量实时转发给前端（SSE delta 事件）         │
│ 4. 若返回 tool_calls → 执行 AgentTools →          │
│    结果回填为 tool 消息 → 回到第 2 步（最多 N 轮）   │
│ 5. 结束 → SSE done 事件                          │
└───────────────────────────────────────────────┘
   ▼
AgentTools（@Component，薄工具注册表）
  search_projects / search_teams / get_user_profile
  get_hot_tags / get_courses / get_project_detail
   ▼
现有 MySQL：project / team / tag / course / user / star / watch / project_tag
（MyBatis + PageHelper，完全复用）
```

**核心原则**：Agent 不编造数据，所有推荐都基于工具从库里查到的真实记录；LLM 只负责理解意图、结构化搜索、打分与组织话术。

## 3. 技术选型与理由

| 决策点 | 选择 | 理由 |
|---|---|---|
| 框架 | **薄客户端**（Spring `RestClient` + Jackson 手写约 200 行） | 不用 Spring AI：Agent 循环、流式解析、工具调用本就是本项目核心，自写完全可控、零版本兼容风险；Spring AI 不覆盖的部分（用户画像注入、匹配打分、会话落库、限流）反正要自写 |
| 模型接入 | **DeepSeek API**，`https://api.deepseek.com/chat/completions`，`deepseek-chat` | OpenAI 兼容（流式 + function calling 均支持），中文好、成本低 |
| 检索路线 | **Function Calling 工具检索**（SQL 查询 → 结果截断 → 喂给 LLM） | 数据是结构化表，工具即检索；不需要 embedding 向量库 |
| 数据来源 | 仅平台内数据 | 复用现有 6 张业务表 + 用户画像 |
| 流式通信 | SSE（`SseEmitter` + `text/event-stream`） | EventSource 不支持 POST，前端用 `fetch + ReadableStream` 手解析 |
| 会话存储 | MySQL 两表（`ai_conversation` / `ai_message`） | 与现有 MyBatis 模式一致，可审计、可展示，优于内存/Redis 方案 |
| 并发 | Java 21 虚拟线程执行 Agent 循环 | 阻塞式 HTTP 长连接在虚拟线程上零成本，代码同步简洁 |

## 4. 后端设计

### 4.1 模块结构（`server/.../server/ai/`）

```
ai/
├── AiProperties.java        # @ConfigurationProperties("ai.deepseek")
├── DeepSeekClient.java      # 薄客户端：chatStream / chat
├── AgentTools.java          # 工具注册表 + 6 个工具实现
├── AiAgentService.java      # Agent 编排循环 + SSE 事件发射
├── AiPrompts.java           # System Prompt 模板 + 工具 JSON Schema 定义
├── AiRateLimiter.java       # Redis 限流（每分钟/每日）
└── model/                   # DeepSeek 请求/响应 DTO
    ├── ChatMessage.java
    ├── ToolCall.java
    ├── ToolDefinition.java
    ├── ChatCompletionRequest.java
    ├── ChatCompletionResponse.java
    ├── Choice.java
    └── Delta.java
```

### 4.2 DeepSeek 薄客户端

- `RestClient`（`spring-web` 内置）+ `JdkClientHttpRequestFactory`，读取超时 120s。
- 请求体：`model`、`messages`、`tools`（function 定义）、`stream:true`、`temperature:0.7`。
- 流式解析：逐行读 `data:` 事件，跳过 `[DONE]`；累计 `delta.content` 与 `delta.tool_calls`（按 `index` 拼接 `function.arguments` 分片）；`finish_reason=tool_calls` 判定工具轮结束。
- 返回 `StreamAccumulator`（本轮累计 content + 累计 tool_calls），content 增量通过回调实时外发。

### 4.3 工具定义（Function Calling JSON Schema）

所有工具参数均用 JSON Schema（`object`），由 `AiPrompts` 统一构建。工具响应内容为 JSON 字符串，工具层负责截断（默认 3000 字符）。

| 工具 | 参数 | 说明 |
|---|---|---|
| `search_projects` | `{ keyword?, project_type?, course_name?, tags?, min_stars?, sort?, limit? }` | 查项目（公开可见），用于选题灵感、去重、相似先例；SQL 动态 WHERE + 排序 + LIMIT |
| `search_teams` | `{ keyword?, team_tag?, course_name?, status? }` | 查组队广场，默认 `status=1`（招募中） |
| `get_user_profile` | `{ }` | 当前用户：班级、bio、参与项目（类型+标签）、点赞/关注项目标签 → 推导技能与兴趣 |
| `get_hot_tags` | `{ limit? }` | 平台热门标签（按使用次数） |
| `get_courses` | `{ }` | 课程字典全量（课设选题用） |
| `get_project_detail` | `{ id }` | 项目全貌（描述 + 标签 + 统计）用于深度参考 |

### 4.4 Agent 编排循环（AiAgentService）

```
streamChat(userId, request, emitter):
  messages = [system(用户画像), ...history(最近40条), user(本次消息)]
  for round in 1..maxToolRounds(6):
     acc = deepseek.chatStream(messages, tools, delta -> emitter.sendDelta(delta))
     落库 assistant 消息（含 tool_calls JSON）
     if acc.toolCalls 为空:
         break                       # 这是最终回答
     对每个 toolCall:
         result = tools.execute(name, arguments)   # 异常→返回错误JSON
         messages.add(tool 消息)；落库
  emitter.sendDone(conversationId)
```

- 对话持久化：用户首条消息时若无 `conversationId` 则新建会话并生成标题（取首条消息前 20 字）。
- 历史重建需完整保留 `tool_call_id` 配对，保证 DeepSeek API 校验通过。
- 异常：工具执行失败不回退，把错误信息作为 tool 内容返回给模型，由模型兜底话术。

### 4.5 SSE 协议

Controller：`POST /api/ai/chat`，`produces=text/event-stream`，`SseEmitter(timeout=5min)`。

| 事件 | data 内容 | 说明 |
|---|---|---|
| `delta` | `{"type":"delta","content":"..."}` | 模型回答增量 |
| `done` | `{"type":"done","conversationId":1}` | 本次回答结束 |
| `error` | `{"type":"error","message":"..."}` | 异常（限流/上游/解析） |

### 4.6 限流与成本控制

- Redis `INCR`：`ai:rl:{userId}:{MMddHHmm}` 上限 10 次/分钟；`ai:rl:{userId}:{yyyyMMdd}` 上限 200 次/天。超限返回 `error` 事件。
- 工具结果截断 3000 字符；历史窗口截取最近 40 条；`maxToolRounds=6` 防失控。

## 5. 两大业务流程

### 5.1 选题推荐

```
用户："帮我推荐一个 Java 课设选题"
  → get_user_profile（技能/兴趣）→ get_courses（确认课程）
  → search_projects(course_name)（已有项目→去重+灵感）→ get_hot_tags
  → LLM 输出 3-5 个选题，每个含：
      题目 / 核心功能 / 技术栈 / 难度·工作量 / 亮点 / 里程碑（周计划）
  → 用户追问细化（"换个简单点的"）→ 模型按画像调整
  → 用户可要求查相似先例（search_projects）或找相关队伍（search_teams）
```

### 5.2 组队推荐

```
用户："帮我找适合我的竞赛队伍"
  → get_user_profile（技能标签）
  → search_teams(team_tag=COMPETITION, status=1)  ← 规则预筛
  → LLM 精排：每个队伍给 匹配度评分 / 契合理由 / 缺口分析
  → 用户点队伍 → 前端直接调现有 POST /api/teams/{id}/apply
     （Agent 可辅助生成申请留言）→ 走现有 WebSocket 入队通知
```

## 6. Prompt 设计（AiPrompts）

System Prompt 固定部分：

- **角色**：知享圈 AI 选题与组队助手，面向北京工业大学学生。
- **行为规则**：
  1. 先调用工具获取真实数据，绝不编造平台内不存在的项目/队伍；
  2. 信息不足先追问（课程名、想参加的竞赛、技术栈、时间预算），不要瞎猜；
  3. 选题输出结构：题目/核心功能/技术栈/难度·工作量/亮点/里程碑；
  4. 组队输出结构：队伍 + 匹配度评分 + 理由 + 缺口分析；
  5. 推荐引导原创，不推荐抄袭/代写/侵权内容；
  6. 中文回答，用 Markdown 排版。

动态部分：用户画像摘要（班级、bio、技能标签、参与/点赞项目类型），每次请求重建。

## 7. 前端设计

- 新增页面 `Vue/src/components/ai/AiChat.vue`，路由 `/ai`，顶部导航加入口图标。
- 流式渲染：`fetch(url, {method:'POST', body})` + `response.body.getReader()` 逐行解 SSE，增量追加；消息用现有 `marked` 渲染 Markdown。
- 功能：建议 prompt 快捷按钮（"帮我选个竞赛选题"/"找适合我的队伍"）、会话历史侧栏（新建/删除/切换）、流式打字光标、错误重试。
- API 模块 `Vue/src/api/ai.js`。

## 8. 数据库表（追加，不破坏现有）

```sql
CREATE TABLE ai_conversation (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL COMMENT '用户ID',
  title VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
  INDEX idx_conv_user (user_id)
) COMMENT='AI 会话表';

CREATE TABLE ai_message (
  id INT AUTO_INCREMENT PRIMARY KEY,
  conversation_id INT NOT NULL,
  role VARCHAR(20) NOT NULL COMMENT 'system/user/assistant/tool',
  content MEDIUMTEXT COMMENT '文本内容',
  tool_calls JSON COMMENT 'assistant 的工具调用',
  tool_call_id VARCHAR(100) COMMENT 'tool 消息对应的调用ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id) ON DELETE CASCADE,
  INDEX idx_msg_conv (conversation_id)
) COMMENT='AI 会话消息表';
```

## 9. API 清单

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/ai/chat` | 发送消息，SSE 流式返回（`conversationId` 可空，首条自动建会话） |
| GET | `/api/ai/conversations` | 当前用户的会话列表 |
| GET | `/api/ai/conversations/{id}/messages` | 会话历史（前端展示用） |
| DELETE | `/api/ai/conversations/{id}` | 删除会话（级联删消息） |

## 10. 安全与成本

- 端点依赖现有 JWT 拦截器（`/api/**` 默认保护），`UserIdContext` 取当前用户。
- 用户输入进 LLM 前做长度上限（如 2000 字符）与基础清理；Prompt 不含任何密钥。
- DeepSeek Key 走环境变量 `DEEPSEEK_API_KEY`，不落库不提交。
- 限流见 §4.6；工具结果截断控制 token 成本。

## 11. 实施计划（约 2 周）

| 阶段 | 内容 | 工期 |
|---|---|---|
| P0 准备 | Key 申请；建表 SQL；`.env`/`application.yml` 配置；连通性测试 | 0.5天 |
| P1 Agent 骨架 | DeepSeekClient（流式+工具解析）；AgentTools + Schema；AiAgentService；SSE Controller；限流 | 2-3天 |
| P2 选题 | Prompt 工程化；选题输出结构化；交互式追问 | 2-3天 |
| P3 组队 | search_teams；匹配打分；推荐理由；一键申请复用 | 2-3天 |
| P4 前端 | 聊天页；SSE 流式；Markdown；会话历史；建议按钮 | 3-4天 |
| P5 打磨 | 错误重连；空态；成本压测；e2e 手测 | 2天 |

## 12. 风险与演进

| 风险 | 缓解 |
|---|---|
| token 成本 | 限流 + 结果截断 + 高频问题缓存 |
| 模型偶发畸形 tool_call | 解析容错 + 自动重试一轮 |
| 推荐质量依赖 Prompt | 预留 Prompt 迭代空间，输出结构字段化便于评测 |
| 平台数据量小时推荐空洞 | 二期加「手动维护灵感库」/ 外部资讯，届时再评估 RAG |

**RAG 触发条件**（MVP 不引入）：① 项目/灵感库成为大量自由文本且需语义相似召回；② 接入外部非结构化资讯。届时仅需把 `search_projects` 的相似召回替换为 embedding+向量库，工具签名与前端不变。
