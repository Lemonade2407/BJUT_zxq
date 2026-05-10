package com.bjutzxq.server.service;

import com.bjutzxq.common.BusinessException;
import com.bjutzxq.pojo.entity.Team;
import com.bjutzxq.pojo.vo.TeamVO;
import com.bjutzxq.server.mapper.TeamMapper;
import com.bjutzxq.server.util.DtoConverter;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeamService {

    @Autowired
    private TeamMapper teamMapper;

    /**
     * 创建组队
     */
    @Transactional(rollbackFor = Exception.class)
    public Team createTeam(Integer userId, String title, String description,
                           Integer currentMembers, Integer neededMembers,
                           String tag, String courseName) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("组队标题不能为空");
        }
        if (neededMembers == null || neededMembers < 2) {
            throw new IllegalArgumentException("需要成员数量至少为2");
        }
        Team team = new Team();
        team.setUserId(userId);
        team.setTitle(title.trim());
        team.setDescription(description != null ? description.trim() : "");
        team.setCurrentMembers(currentMembers != null ? currentMembers : 1);
        team.setNeededMembers(neededMembers);
        team.setTag(tag != null ? tag.toUpperCase() : "PROJECT");
        team.setCourseName(courseName);
        team.setStatus(1);
        teamMapper.insert(team);
        log.info("组队创建成功，ID: {}", team.getId());
        return team;
    }

    /**
     * 获取组队列表（分页）
     */
    public List<TeamVO> getTeams(Integer pageNum, Integer pageSize, String tag, Integer status, String courseName) {
        PageHelper.startPage(pageNum, pageSize);
        List<Team> teams = teamMapper.selectAll(tag, status, courseName);
        return DtoConverter.toTeamVOList(teams, batchLoadUsers(teams));
    }

    /**
     * 获取用户的组队
     */
    public List<TeamVO> getUserTeams(Integer userId) {
        List<Team> teams = teamMapper.selectByUserId(userId);
        return DtoConverter.toTeamVOList(teams, batchLoadUsers(teams));
    }

    /**
     * 获取单个组队
     */
    public TeamVO getTeamById(Integer id) {
        Team team = teamMapper.selectById(id);
        if (team == null) return null;
        return DtoConverter.toTeamVO(team, batchLoadUsers(List.of(team)));
    }

    /**
     * 更新组队
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTeam(Integer teamId, Integer userId, Team update) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(404, "组队不存在");
        }
        if (!team.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能修改自己的组队");
        }
        if (update.getTitle() != null) team.setTitle(update.getTitle());
        if (update.getDescription() != null) team.setDescription(update.getDescription());
        if (update.getCurrentMembers() != null) team.setCurrentMembers(update.getCurrentMembers());
        if (update.getNeededMembers() != null) team.setNeededMembers(update.getNeededMembers());
        if (update.getTag() != null) team.setTag(update.getTag().toUpperCase());
        if (update.getCourseName() != null) team.setCourseName(update.getCourseName());
        if (update.getStatus() != null) team.setStatus(update.getStatus());
        teamMapper.updateById(team);
        log.info("组队更新成功，ID: {}", teamId);
    }

    /**
     * 删除组队
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeam(Integer teamId, Integer userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(404, "组队不存在");
        }
        if (!team.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的组队");
        }
        teamMapper.deleteById(teamId);
        log.info("组队删除成功，ID: {}", teamId);
    }

    /**
     * 管理员删除组队
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteTeam(Integer id) {
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw new BusinessException(404, "组队不存在");
        }
        teamMapper.deleteById(id);
        log.info("管理员删除组队，ID: {}", id);
    }

    /**
     * 更新组队状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, Integer status) {
        Team team = teamMapper.selectById(id);
        if (team == null) {
            throw new BusinessException(404, "组队不存在");
        }
        team.setStatus(status);
        teamMapper.updateById(team);
    }

    /**
     * 统计总数
     */
    public long countAll(String tag, Integer status, String courseName) {
        return teamMapper.countAll(tag, status, courseName);
    }

    private Map<Integer, Map<String, Object>> batchLoadUsers(List<Team> teams) {
        if (teams.isEmpty()) return new java.util.HashMap<>();
        List<Integer> userIds = teams.stream().map(Team::getUserId).distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) return new java.util.HashMap<>();
        List<Map<String, Object>> users = teamMapper.selectUserBatch(userIds);
        return users.stream().collect(Collectors.toMap(u -> (Integer) u.get("id"), u -> u));
    }
}
