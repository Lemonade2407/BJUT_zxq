package com.bjutzxq.server.controller;

import com.bjutzxq.common.Result;
import com.bjutzxq.pojo.dto.PageResult;
import com.bjutzxq.pojo.entity.Team;
import com.bjutzxq.pojo.entity.TeamApplication;
import com.bjutzxq.pojo.vo.TeamApplicationVO;
import com.bjutzxq.pojo.vo.TeamVO;
import com.bjutzxq.server.context.UserIdContext;
import com.bjutzxq.server.service.TeamApplicationService;
import com.bjutzxq.server.service.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamApplicationService appService;

    /**
     * 获取组队列表
     * GET /api/teams?pageNum=1&pageSize=12&tag=COMPETITION&status=1
     */
    @GetMapping
    public Result<PageResult<TeamVO>> getTeams(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "12") Integer pageSize,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "courseName", required = false) String courseName) {
        List<TeamVO> teams = teamService.getTeams(pageNum, pageSize, tag, status, courseName);
        long total = teamService.countAll(tag, status, courseName);
        return Result.success(new PageResult<>(teams, total, pageNum, pageSize));
    }

    /**
     * 获取单个组队详情
     * GET /api/teams/{id}
     */
    @GetMapping("/{id}")
    public Result<TeamVO> getTeam(@PathVariable Integer id) {
        TeamVO team = teamService.getTeamById(id);
        if (team == null) {
            return Result.error("组队不存在");
        }
        return Result.success(team);
    }

    /**
     * 创建组队
     * POST /api/teams
     */
    @PostMapping
    public Result<Team> createTeam(@RequestBody Map<String, Object> body) {
        Integer userId = UserIdContext.getCurrentUserId();
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        Integer currentMembers = body.get("currentMembers") != null
            ? ((Number) body.get("currentMembers")).intValue() : 1;
        Integer neededMembers = body.get("neededMembers") != null
            ? ((Number) body.get("neededMembers")).intValue() : null;
        String tag = (String) body.get("tag");
        String courseName = (String) body.get("courseName");

        Team team = teamService.createTeam(userId, title, description,
            currentMembers, neededMembers, tag, courseName);
        return Result.success("组队创建成功", team);
    }

    /**
     * 更新组队
     * PUT /api/teams/{id}
     */
    @PutMapping("/{id}")
    public Result<Void> updateTeam(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer userId = UserIdContext.getCurrentUserId();
        Team update = new Team();
        if (body.containsKey("title")) update.setTitle((String) body.get("title"));
        if (body.containsKey("description")) update.setDescription((String) body.get("description"));
        if (body.containsKey("currentMembers")) update.setCurrentMembers(((Number) body.get("currentMembers")).intValue());
        if (body.containsKey("neededMembers")) update.setNeededMembers(((Number) body.get("neededMembers")).intValue());
        if (body.containsKey("tag")) update.setTag((String) body.get("tag"));
        if (body.containsKey("courseName")) update.setCourseName((String) body.get("courseName"));
        if (body.containsKey("status")) update.setStatus(((Number) body.get("status")).intValue());

        teamService.updateTeam(id, userId, update);
        return Result.success("组队更新成功", null);
    }

    /**
     * 删除组队
     * DELETE /api/teams/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTeam(@PathVariable Integer id) {
        Integer userId = UserIdContext.getCurrentUserId();
        teamService.deleteTeam(id, userId);
        return Result.success("组队已删除", null);
    }

    /**
     * 获取我的组队
     * GET /api/teams/mine
     */
    @GetMapping("/mine")
    public Result<List<TeamVO>> getMyTeams() {
        Integer userId = UserIdContext.getCurrentUserId();
        List<TeamVO> teams = teamService.getUserTeams(userId);
        return Result.success(teams);
    }

    // ==================== 入队申请 ====================

    /**
     * 申请入队
     * POST /api/teams/{id}/apply
     */
    @PostMapping("/{id}/apply")
    public Result<TeamApplication> apply(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Integer userId = UserIdContext.getCurrentUserId();
        String message = body.getOrDefault("message", "");
        TeamApplication app = appService.apply(id, userId, message);
        return Result.success("申请已提交", app);
    }

    /**
     * 检查是否已申请
     * GET /api/teams/{id}/applied
     */
    @GetMapping("/{id}/applied")
    public Result<Boolean> hasApplied(@PathVariable Integer id) {
        Integer userId = UserIdContext.getCurrentUserId();
        boolean applied = appService.hasApplied(id, userId);
        return Result.success(applied);
    }

    /**
     * 获取组队的申请列表（组长）
     * GET /api/teams/{id}/applications
     */
    @GetMapping("/{id}/applications")
    public Result<List<TeamApplicationVO>> getApplications(@PathVariable Integer id) {
        Integer userId = UserIdContext.getCurrentUserId();
        List<TeamApplicationVO> apps = appService.getTeamApplications(id, userId);
        return Result.success(apps);
    }

    /**
     * 通过申请
     * PUT /api/teams/applications/{id}/approve
     */
    @PutMapping("/applications/{id}/approve")
    public Result<Void> approveApplication(@PathVariable Integer id) {
        Integer userId = UserIdContext.getCurrentUserId();
        appService.approve(id, userId);
        return Result.success("已通过", null);
    }

    /**
     * 拒绝申请
     * PUT /api/teams/applications/{id}/reject
     */
    @PutMapping("/applications/{id}/reject")
    public Result<Void> rejectApplication(@PathVariable Integer id) {
        Integer userId = UserIdContext.getCurrentUserId();
        appService.reject(id, userId);
        return Result.success("已拒绝", null);
    }

    /**
     * 获取我的申请记录
     * GET /api/teams/my-applications
     */
    @GetMapping("/my-applications")
    public Result<List<TeamApplicationVO>> getMyApplications() {
        Integer userId = UserIdContext.getCurrentUserId();
        List<TeamApplicationVO> apps = appService.getMyApplications(userId);
        return Result.success(apps);
    }
}
