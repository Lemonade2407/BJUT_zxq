package com.bjutzxq.server.ai;

import com.bjutzxq.server.ai.model.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * System Prompt 模板与工具定义（function calling JSON Schema）
 */
public final class AiPrompts {

    private AiPrompts() {
    }

    public static String systemPrompt(String profileJson) {
        return """
                你是「知享圈」AI 选题与组队助手，服务于北京工业大学的课程设计与学科竞赛学生。

                你主要做两件事：
                1. 选题推荐：结合用户的专业、技术栈、兴趣、课程与工作量预算，推荐课设/竞赛/毕设选题。
                2. 组队推荐：根据用户画像与要求，从组队广场「招募中」的队伍中匹配并打分。

                == 硬性规则 ==
                1. 必须先调用工具查询真实数据，绝不编造平台上不存在的项目或队伍；工具返回 total=0 就如实说没找到，不要脑补。
                2. 选题/组队类问题，必须先调用至少一个检索工具（search_projects / search_teams / get_courses / get_hot_tags）拿到真实数据后再回答；未调用任何检索工具就直接给出推荐，属于违规。
                3. 信息不足时先向用户追问（课程名、想参加的竞赛、技术栈、时间预算），不要替用户瞎猜。
                4. 选题推荐输出结构：每个选题包含【题目 / 核心功能 / 技术栈 / 难度与工作量 / 亮点 / 里程碑（周计划）】，一次给 3-5 个并按契合度排序。
                5. 组队推荐输出结构：每个队伍包含【匹配度评分(0-100) / 匹配理由（结合用户技能标签）/ 缺口分析（队伍还缺什么角色）】，按匹配度降序，并提示可点击「申请入队」。
                6. 只推荐原创、合规内容，不推荐抄袭、代写、侵权选题。
                7. 始终用中文回答，Markdown 排版；引用平台内项目/队伍时带上对应链接入口提示。

                == 当前用户画像（来自工具，缺失即未知） ==
                %s
                """.formatted(profileJson == null ? "（无画像数据）" : profileJson);
    }

    public static List<ToolDefinition> toolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();
        tools.add(function("search_projects",
                "检索平台公开项目，用于选题灵感、去重、找相似先例。返回项目列表（含标签与热度）。",
                Map.of(
                        "keyword", str("关键词，匹配项目名/描述"),
                        "project_type", str("项目类型: COURSE(课设)/THESIS(毕设)/COMPETITION(竞赛)/PERSONAL(个人)/OTHER"),
                        "course_name", str("课程名称（课设选题时推荐传入）"),
                        "min_stars", num("最低收藏数，用于筛选高质量先例"),
                        "sort", str("排序: stars(默认)/views/recent"),
                        "limit", num("返回条数，默认10，最多15"))));
        tools.add(function("search_teams",
                "检索组队广场的队伍，默认只看「招募中」。返回队伍列表（含人数、类型、创建人）。",
                Map.of(
                        "keyword", str("关键词，匹配队伍标题/简介"),
                        "team_tag", str("组队类型: COMPETITION(竞赛)/PROJECT(项目)/COURSE(课设)"),
                        "course_name", str("课程名称"),
                        "status", num("状态: 1-招募中(默认)，0-已结束，2-已满员"),
                        "limit", num("返回条数，默认10，最多15"))));
        tools.add(function("get_user_profile",
                "获取当前用户画像：班级、简介、角色、项目类型分布、技能标签、兴趣标签（用于定制推荐）。",
                Map.of()));
        tools.add(function("get_hot_tags",
                "获取平台热门标签（按使用次数），用于了解热门技术栈与选题方向。",
                Map.of("limit", num("返回条数，默认20，最多50"))));
        tools.add(function("get_courses",
                "获取平台课程字典全量，用于确认课设选题对应的课程。",
                Map.of()));
        tools.add(function("get_project_detail",
                "获取单个项目的完整信息（完整描述、标签、热度），用于深入了解某个参考先例。",
                Map.of("id", num("项目 ID（必填）"))));
        return tools;
    }

    private static ToolDefinition function(String name, String description, Map<String, Object> properties) {
        ToolDefinition t = new ToolDefinition();
        ToolDefinition.ToolFunction fn = new ToolDefinition.ToolFunction();
        fn.setName(name);
        fn.setDescription(description);
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        fn.setParameters(parameters);
        t.setFunction(fn);
        return t;
    }

    private static Map<String, Object> str(String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "string");
        m.put("description", desc);
        return m;
    }

    private static Map<String, Object> num(String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "integer");
        m.put("description", desc);
        return m;
    }
}
