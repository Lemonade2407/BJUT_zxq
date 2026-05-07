package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 课程字典 Mapper 接口
 */
@Mapper
public interface CourseMapper {

    /**
     * 插入课程
     */
    int insert(Course course);

    /**
     * 根据 ID 删除课程
     */
    int deleteById(@Param("id") Integer id);

    /**
     * 更新课程
     */
    int update(Course course);

    /**
     * 根据 ID 查询课程
     */
    Course selectById(@Param("id") Integer id);

    /**
     * 根据课程名称查询
     */
    Course selectByCourseName(@Param("courseName") String courseName);

    /**
     * 查询所有课程
     */
    List<Course> selectAllCourses();

    /**
     * 模糊搜索课程
     */
    List<Course> searchCourses(@Param("keyword") String keyword);
}
