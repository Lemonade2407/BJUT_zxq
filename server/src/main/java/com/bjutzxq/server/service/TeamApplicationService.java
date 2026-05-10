package com.bjutzxq.server.service;

import com.bjutzxq.common.NotificationType;
import com.bjutzxq.pojo.entity.TeamApplication;
import com.bjutzxq.pojo.entity.Team;
import com.bjutzxq.pojo.entity.User;
import com.bjutzxq.pojo.vo.TeamApplicationVO;
import com.bjutzxq.server.mapper.TeamApplicationMapper;
import com.bjutzxq.server.mapper.TeamMapper;
import com.bjutzxq.server.mapper.UserMapper;
import com.bjutzxq.server.util.DtoConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeamApplicationService {

    @Autowired
    private TeamApplicationMapper appMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Transactional(rollbackFor = Exception.class)
    public TeamApplication apply(Integer teamId, Integer applicantId, String message) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) throw new RuntimeException("组队不存在");
        if (team.getUserId().equals(applicantId)) throw new RuntimeException("不能申请自己的组队");
        TeamApplication existing = appMapper.selectByTeamAndUser(teamId, applicantId);
        if (existing != null) throw new RuntimeException("您已经申请过该组队");
        TeamApplication app = new TeamApplication();
        app.setTeamId(teamId);
        app.setApplicantId(applicantId);
        app.setMessage(message);
        app.setStatus(0);
        appMapper.insert(app);
        log.info("入队申请成功，申请ID: {}, 组队ID: {}", app.getId(), teamId);

        // 通知组长
        try {
            User applicant = userMapper.selectById(applicantId);
            String applicantName = applicant != null ? applicant.getUsername() : "未知用户";
            notificationService.createNotification(
                team.getUserId(), applicantId, null,
                NotificationType.TEAM_APPLICATION,
                applicantName + " 申请加入你的组队「" + team.getTitle() + "」"
            );
        } catch (Exception e) {
            log.warn("创建组队申请通知失败：{}", e.getMessage());
        }

        return app;
    }

    public List<TeamApplicationVO> getTeamApplications(Integer teamId, Integer userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) throw new RuntimeException("组队不存在");
        if (!team.getUserId().equals(userId)) throw new RuntimeException("只有组长可以查看申请列表");
        List<TeamApplication> apps = appMapper.selectByTeamId(teamId);
        return DtoConverter.toTeamApplicationVOList(apps, batchLoadUsers(apps));
    }

    public List<TeamApplicationVO> getMyApplications(Integer userId) {
        List<TeamApplication> apps = appMapper.selectByApplicantId(userId);
        return DtoConverter.toTeamApplicationVOList(apps, batchLoadUsers(apps));
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer appId, Integer userId) {
        TeamApplication app = appMapper.selectById(appId);
        if (app == null) throw new RuntimeException("申请不存在");
        Integer creatorId = appMapper.getTeamCreatorId(app.getTeamId());
        if (!creatorId.equals(userId)) throw new RuntimeException("只有组长可以审核");
        if (app.getStatus() != 0) throw new RuntimeException("该申请已处理");
        appMapper.updateStatus(appId, 1);
        // 更新组队已有成员数
        Team team = teamMapper.selectById(app.getTeamId());
        team.setCurrentMembers(team.getCurrentMembers() + 1);
        // 如果满员则更新状态
        if (team.getCurrentMembers() >= team.getNeededMembers()) {
            team.setStatus(2);
        }
        teamMapper.updateById(team);
        log.info("申请已通过，申请ID: {}", appId);

        try {
            notificationService.createNotification(
                app.getApplicantId(), userId, null,
                NotificationType.TEAM_APPLICATION,
                "你加入组队「" + team.getTitle() + "」的申请已通过"
            );
        } catch (Exception e) {
            log.warn("发送通过通知失败：{}", e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer appId, Integer userId) {
        TeamApplication app = appMapper.selectById(appId);
        if (app == null) throw new RuntimeException("申请不存在");
        Integer creatorId = appMapper.getTeamCreatorId(app.getTeamId());
        if (!creatorId.equals(userId)) throw new RuntimeException("只有组长可以审核");
        if (app.getStatus() != 0) throw new RuntimeException("该申请已处理");
        appMapper.updateStatus(appId, 2);
        log.info("申请已拒绝，申请ID: {}", appId);

        try {
            Team team = teamMapper.selectById(app.getTeamId());
            notificationService.createNotification(
                app.getApplicantId(), userId, null,
                NotificationType.TEAM_APPLICATION,
                "你加入组队「" + team.getTitle() + "」的申请已被拒绝"
            );
        } catch (Exception e) {
            log.warn("发送拒绝通知失败：{}", e.getMessage());
        }
    }

    public boolean hasApplied(Integer teamId, Integer userId) {
        return appMapper.selectByTeamAndUser(teamId, userId) != null;
    }

    private Map<Integer, Map<String, Object>> batchLoadUsers(List<TeamApplication> apps) {
        if (apps.isEmpty()) return new java.util.HashMap<>();
        List<Integer> userIds = apps.stream().map(TeamApplication::getApplicantId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) return new java.util.HashMap<>();
        List<Map<String, Object>> users = appMapper.selectUserBatch(userIds);
        return users.stream().collect(Collectors.toMap(u -> (Integer) u.get("id"), u -> u));
    }
}
