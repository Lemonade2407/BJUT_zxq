package com.bjutzxq.server.mapper;

import com.bjutzxq.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    User selectByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象
     */
    User selectByEmail(@Param("email") String email);
    
    /**
     * 根据身份标识号查询用户（学生为学号，教师为职工号）
     * @param employeeId 身份标识号
     * @return 用户对象
     */
    User selectByEmployeeId(@Param("employeeId") String employeeId);
    
    /**
     * 根据 ID 查询用户
     * @param id 用户 ID
     * @return 用户对象
     */
    User selectById(@Param("id") Integer id);
    
    /**
     * 插入用户
     * @param user 用户对象
     * @return 影响行数
     */
    int insert(User user);
    
    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 影响行数
     */
    int update(User user);
    
    /**
     * 删除用户
     * @param id 用户 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 查询所有用户（分页）
     * @return 用户列表
     */
    java.util.List<User> selectAll();
    
    /**
     * 根据关键词搜索用户（用户名、邮箱、学号）
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    java.util.List<User> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 根据角色查询用户列表
     * @param role 角色
     * @return 用户列表
     */
    java.util.List<User> selectByRole(@Param("role") com.bjutzxq.common.Role role);
    
    /**
     * 根据角色和关键词搜索用户
     * @param role 角色
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    java.util.List<User> searchByRoleAndKeyword(
        @Param("role") com.bjutzxq.common.Role role,
        @Param("keyword") String keyword
    );
    
    /**
     * 统计用户总数
     * @return 用户总数
     */
    int countAll();
    
    /**
     * 批量查询用户信息（用于优化 N+1 查询）
     * @param ids 用户 ID 列表
     * @return 用户列表
     */
    java.util.List<User> selectBatchIds(@Param("ids") java.util.List<Integer> ids);
}
