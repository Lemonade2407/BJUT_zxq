package com.bjutzxq.server.ai;

import com.bjutzxq.pojo.entity.Course;
import com.bjutzxq.pojo.entity.Project;
import com.bjutzxq.pojo.entity.ProjectTag;
import com.bjutzxq.pojo.entity.Tag;
import com.bjutzxq.pojo.entity.Team;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.server.mapper.CourseMapper;
import com.bjutzxq.server.mapper.ProjectMapper;
import com.bjutzxq.server.mapper.ProjectTagMapper;
import com.bjutzxq.server.mapper.TagMapper;
import com.bjutzxq.server.mapper.TeamMapper;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.mapper.WatchMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 工具注册表：所有工具返回 JSON 字符串，直接作为 function calling 结果喂回模型。
 * 内部完全复用现有 Mapper，不编造数据。
 */
@Component
public class AgentTools {

    private final ProjectMapper projectMapper;
    private final TeamMapper teamMapper;
    private final TagMapper tagMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final ProjectTagMapper projectTagMapper;
    private final WatchMapper watchMapper;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public AgentTools(ProjectMapper projectMapper, TeamMapper teamMapper, TagMapper tagMapper,
                      CourseMapper courseMapper, UserMapper userMapper, ProjectTagMapper projectTagMapper,
                      WatchMapper watchMapper, AiProperties properties) {
        this.projectMapper = projectMapper;
        this.teamMapper = teamMapper;
        this.tagMapper = tagMapper;
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.projectTagMapper = projectTagMapper;
        this.watchMapper = watchMapper;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 统一执行入口
     */
    public String execute(String name, String argumentsJson, Integer userId) {
        Map<String, Object> args = parseArgs(argumentsJson);
        switch (name) {
            case "search_projects":
                return searchProjects(args);
            case "search_teams":
                return searchTeams(args);
            case "get_user_profile":
                return getUserProfile(userId);
            case "get_hot_tags":
                return getHotTags(args);
            case "get_courses":
                return getCourses();
            case "get_project_detail":
                return getProjectDetail(args);
            default:
                return json(Map.of("error", "未知工具: " + name));
        }
    }

    // ==================== 工具实现 ====================

    private String searchProjects(Map<String, Object> args) {
        int limit = Math.min(intArg(args, "limit", 10), 15);
        List<Project> projects = projectMapper.searchForAi(
                strArg(args, "keyword"),
                strArg(args, "project_type"),
                strArg(args, "course_name"),
                intArg(args, "min_stars", null),
                strArg(args, "sort"),
                limit);
        if (projects.isEmpty()) {
            return json(Map.of("total", 0, "projects", List.of()));
        }
        Map<Integer, List<String>> tagNamesByProject = loadTagNamesByProject(
                projects.stream().map(Project::getId).collect(Collectors.toList()));
        Map<Integer, String> ownerNames = loadOwnerNames(projects);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Project p : projects) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("project_type", p.getProjectType());
            m.put("course_name", p.getCourseName());
            m.put("description", truncate(p.getDescription(), 200));
            m.put("tags", tagNamesByProject.getOrDefault(p.getId(), List.of()));
            m.put("star_count", p.getStarCount());
            m.put("view_count", p.getViewCount());
            m.put("owner", ownerNames.get(p.getOwnerId()));
            list.add(m);
        }
        return json(Map.of("total", list.size(), "projects", list));
    }

    private String searchTeams(Map<String, Object> args) {
        int limit = Math.min(intArg(args, "limit", 10), 15);
        Integer status = args.containsKey("status") ? intArg(args, "status", null) : 1;
        List<Team> teams = teamMapper.searchForAi(
                strArg(args, "keyword"),
                strArg(args, "team_tag"),
                strArg(args, "course_name"),
                status,
                limit);
        if (teams.isEmpty()) {
            return json(Map.of("total", 0, "teams", List.of()));
        }
        List<Integer> creatorIds = teams.stream().map(Team::getUserId).distinct().collect(Collectors.toList());
        Map<Integer, String> names = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getUsername() == null ? "" : u.getUsername()));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Team t : teams) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("description", truncate(t.getDescription(), 200));
            m.put("tag", t.getTag());
            m.put("course_name", t.getCourseName());
            m.put("current_members", t.getCurrentMembers());
            m.put("needed_members", t.getNeededMembers());
            m.put("status", t.getStatus());
            m.put("creator", names.get(t.getUserId()));
            list.add(m);
        }
        return json(Map.of("total", list.size(), "teams", list));
    }

    private String getUserProfile(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return json(Map.of("error", "用户不存在"));
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", user.getUsername());
        profile.put("real_name", user.getRealName());
        profile.put("class_name", user.getClassName());
        profile.put("bio", user.getBio());
        profile.put("role", user.getRole() == null ? null : user.getRole().name());

        List<Project> myProjects = projectMapper.selectByUserId(userId);
        profile.put("my_project_count", myProjects.size());
        profile.put("my_project_type_distribution",
                projectMapper.countByUserIdGroupByType(userId));

        List<String> skillTags = new ArrayList<>();
        if (!myProjects.isEmpty()) {
            Map<Integer, List<String>> tags = loadTagNamesByProject(
                    myProjects.stream().map(Project::getId).collect(Collectors.toList()));
            tags.values().forEach(skillTags::addAll);
        }
        profile.put("skill_tags", skillTags.stream().distinct().collect(Collectors.toList()));

        List<Integer> watchedIds = watchMapper.selectProjectIdsByUserId(userId);
        List<String> interestTags = new ArrayList<>();
        if (!watchedIds.isEmpty()) {
            List<Project> watched = projectMapper.selectByIds(watchedIds);
            if (!watched.isEmpty()) {
                Map<Integer, List<String>> tags = loadTagNamesByProject(
                        watched.stream().map(Project::getId).collect(Collectors.toList()));
                tags.values().forEach(interestTags::addAll);
            }
        }
        profile.put("interest_tags", interestTags.stream().distinct().collect(Collectors.toList()));
        return json(profile);
    }

    private String getHotTags(Map<String, Object> args) {
        int limit = Math.min(intArg(args, "limit", 20), 50);
        List<Map<String, Object>> list = tagMapper.selectHotTags(limit).stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", t.getName());
            m.put("category", t.getCategory());
            m.put("usage_count", t.getUsageCount());
            return m;
        }).collect(Collectors.toList());
        return json(list);
    }

    private String getCourses() {
        List<String> names = courseMapper.selectAllCourses().stream()
                .map(Course::getCourseName)
                .collect(Collectors.toList());
        return json(names);
    }

    private String getProjectDetail(Map<String, Object> args) {
        Integer id = intArg(args, "id", null);
        if (id == null) {
            return json(Map.of("error", "缺少项目 id"));
        }
        Project p = projectMapper.selectById(id);
        if (p == null || p.getVisibility() == null || p.getVisibility() != 1) {
            return json(Map.of("error", "项目不存在或不可见"));
        }
        List<Integer> tagIds = projectTagMapper.selectTagIdsByProjectId(id);
        List<String> tagNames = tagIds.isEmpty() ? List.of()
                : tagMapper.selectByIds(tagIds).stream().map(Tag::getName).collect(Collectors.toList());
        String owner = p.getOwnerId() == null ? null
                : (userMapper.selectById(p.getOwnerId()) != null
                        ? userMapper.selectById(p.getOwnerId()).getUsername() : null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("project_type", p.getProjectType());
        m.put("course_name", p.getCourseName());
        m.put("tags", tagNames);
        m.put("star_count", p.getStarCount());
        m.put("view_count", p.getViewCount());
        m.put("download_count", p.getDownloadCount());
        m.put("owner", owner);
        return json(m);
    }

    // ==================== 辅助方法 ====================

    private Map<Integer, List<String>> loadTagNamesByProject(List<Integer> projectIds) {
        Map<Integer, List<String>> result = new HashMap<>();
        if (projectIds.isEmpty()) {
            return result;
        }
        List<ProjectTag> rels = projectTagMapper.selectByProjectIds(projectIds);
        List<Integer> tagIds = rels.stream().map(ProjectTag::getTagId).distinct().collect(Collectors.toList());
        Map<Integer, String> nameById = tagIds.isEmpty() ? Map.of()
                : tagMapper.selectByIds(tagIds).stream()
                        .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        for (ProjectTag rel : rels) {
            result.computeIfAbsent(rel.getProjectId(), k -> new ArrayList<>())
                    .add(nameById.getOrDefault(rel.getTagId(), ""));
        }
        return result;
    }

    private Map<Integer, String> loadOwnerNames(List<Project> projects) {
        List<Integer> ownerIds = projects.stream().map(Project::getOwnerId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getUsername() == null ? "" : u.getUsername(),
                        (a, b) -> a));
    }

    private Map<String, Object> parseArgs(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JsonProcessingException e) {
            throw new AiException("工具参数解析失败: " + json, e);
        }
    }

    private String json(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new AiException("工具结果序列化失败", e);
        }
    }

    private String strArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? null : v.toString();
    }

    private Integer intArg(Map<String, Object> args, String key, Integer def) {
        Object v = args.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
