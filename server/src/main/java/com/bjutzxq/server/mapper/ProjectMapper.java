package com.bjutzxq.server.mapper;


import com.bjutzxq.pojo.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;


/**
 * 项目 Mapper 接口
 */
@Mapper
public interface ProjectMapper {
    /**
     * 新增项目
     * @param project 项目信息
     * @return 影响行数
     */
    int insert(Project project);
    /**
     * 根据 ID 查询项目
     * @param id 项目 ID
     * @return 项目信息
     */
    Project selectById(@Param("id") Integer id);
    /**
     * 根据名称查询项目
     * @param name 项目名称
     * @return 项目信息
     */
    List<Project> selectByName(@Param("name") String name);
    
    /**
     * 统计符合条件的项目总数（按名称）
     * @param name 项目名称
     * @return 项目总数
     */
    int countByName(@Param("name") String name);
    
    /**
     * 根据用户 ID 查询项目
     * @param userId 用户 ID
     * @return 项目信息
     */
    List<Project> selectByUserId(@Param("userId") Integer userId);
    
    /**
     * 统计用户的项目总数
     * @param userId 用户 ID
     * @return 项目总数
     */
    int countByUserId(@Param("userId") Integer userId);

    /**
     * 根据 ID 更新项目
     * @param project 项目信息
     * @return 影响行数
     */
    int updateById(Project project);
    /**
     * 根据 ID 删除项目
     * @param id 项目 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 统计项目总数
     * @return 项目总数
     */
    int countAll();
    
    /**
     * 查询所有公开项目
     * @return 公开项目列表
     */
    List<Project> selectPublicProjects();
    
    /**
     * 统计公开项目总数
     * @return 项目总数
     */
    int countPublicProjects();
    
    /**
     * 根据用户 ID 删除所有项目
     * @param userId 用户 ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Integer userId);
    
    /**
     * 根据标签 ID 查询项目
     * @param tagId 标签 ID
     * @return 项目列表
     */
    List<Project> selectByTagId(@Param("tagId") Integer tagId);
    
    /**
     * 统计标签下的项目总数
     * @param tagId 标签 ID
     * @return 项目总数
     */
    int countByTagId(@Param("tagId") Integer tagId);
    
    /**
     * 获取热门项目（按浏览量排序）
     * @param limit 限制数量
     * @return 热门项目列表
     */
    List<Project> selectTrendingProjects(@Param("limit") Integer limit);
    
    /**
     * 增加项目浏览量
     * @param id 项目 ID
     * @return 影响行数
     */
    int incrementViewCount(@Param("id") Integer id);
    
    /**
     * 增加项目下载次数
     * @param id 项目 ID
     * @return 影响行数
     */
    int incrementDownloadCount(@Param("id") Integer id);
    
    /**
     * 增加项目点赞数
     * @param id 项目 ID
     * @return 影响行数
     */
    int incrementStarCount(@Param("id") Integer id);
    
    /**
     * 减少项目点赞数
     * @param id 项目 ID
     * @return 影响行数
     */
    int decrementStarCount(@Param("id") Integer id);
    
    /**
     * 增加项目关注数
     * @param id 项目 ID
     * @return 影响行数
     */
    int incrementWatchCount(@Param("id") Integer id);
    
    /**
     * 减少项目关注数
     * @param id 项目 ID
     * @return 影响行数
     */
    int decrementWatchCount(@Param("id") Integer id);
    
    /**
     * 根据用户 ID 查询项目 ID 列表（用于清理 Git 仓库）
     * @param userId 用户 ID
     * @return 项目 ID 列表
     */
    List<Integer> selectProjectIdsByUserId(@Param("userId") Integer userId);
    
    /**
     * 根据项目 ID 列表批量查询项目
     * @param projectIds 项目 ID 列表
     * @return 项目列表
     */
    List<Project> selectByIds(@Param("projectIds") List<Integer> projectIds);
    
    /**
     * 根据项目类型查询项目
     * @param projectType 项目类型
     * @param courseName 课程名称（可选）
     * @param thesisType 毕设类型（可选）
     * @return 项目列表
     */
    List<Project> selectByProjectType(
        @Param("projectType") String projectType,
        @Param("courseName") String courseName,
        @Param("thesisType") String thesisType
    );
    
    /**
     * 按班级和课程筛选项目（用于教学管理）
     * @param className 班级名称（可选，支持模糊查询）
     * @param courseName 课程名称（可选）
     * @param projectType 项目类型（可选）
     * @return 项目列表
     */
    List<Project> selectByClassAndCourse(
        @Param("className") String className,
        @Param("courseName") String courseName,
        @Param("projectType") String projectType
    );
    
    /**
     * 统计按班级和课程筛选的项目总数
     * @param className 班级名称（可选，支持模糊查询）
     * @param courseName 课程名称（可选）
     * @param projectType 项目类型（可选）
     * @return 项目总数
     */
    int countByClassAndCourse(
        @Param("className") String className,
        @Param("courseName") String courseName,
        @Param("projectType") String projectType
    );
    
    /**
     * 获取按班级和课程筛选的所有项目ID（用于批量下载）
     * @param className 班级名称（可选，支持模糊查询）
     * @param courseName 课程名称（可选）
     * @param projectType 项目类型（可选）
     * @return 项目ID列表
     */
    List<Integer> selectIdsByClassAndCourse(
        @Param("className") String className,
        @Param("courseName") String courseName,
        @Param("projectType") String projectType
    );

    int sumDownloadsByUserId(@Param("userId") Integer userId);
    java.util.List<java.util.Map<String, Object>> countByType();
    int countByMonth(@Param("month") String month);
    java.util.List<java.util.Map<String, Object>> countByUserIdGroupByType(@Param("userId") Integer userId);
    int countByUserIdAndMonth(@Param("userId") Integer userId, @Param("month") String month);
}