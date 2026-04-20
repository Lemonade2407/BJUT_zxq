package com.bjutzxq.server.mapper;


import com.bjutzxq.pojo.ProjectFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 项目文件 Mapper 接口
 */
@Mapper
public interface ProjectFileMapper {
    
    /**
     * 新增文件
     * @param projectFile 文件信息
     * @return 影响行数
     */
    int insert(ProjectFile projectFile);
    
    /**
     * 根据 ID 查询文件
     * @param id 文件 ID
     * @return 文件信息
     */
    ProjectFile selectById(@Param("id") Integer id);
    
    /**
     * 根据项目 ID 和父目录 ID 查询文件列表
     * @param projectId 项目 ID
     * @param parentId 父目录 ID（null 表示根目录）
     * @return 文件列表
     */
    List<ProjectFile> selectByProjectIdAndParentId(
            @Param("projectId") Integer projectId, 
            @Param("parentId") Integer parentId);
    
    /**
     * 根据项目 ID 查询所有文件
     * @param projectId 项目 ID
     * @return 文件列表
     */
    List<ProjectFile> selectByProjectId(@Param("projectId") Integer projectId);
    
    /**
     * 根据路径查询文件（用于检查重名）
     * @param projectId 项目 ID
     * @param filePath 文件路径
     * @param fileName 文件名
     * @return 文件信息
     */
    ProjectFile selectByPath(
            @Param("projectId") Integer projectId,
            @Param("filePath") String filePath,
            @Param("fileName") String fileName);
    
    /**
     * 更新文件信息
     * @param projectFile 文件信息
     * @return 影响行数
     */
    int updateById(ProjectFile projectFile);
    
    /**
     * 删除文件
     * @param id 文件 ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Integer id);
    
    /**
     * 统计项目文件数量
     * @param projectId 项目 ID
     * @return 文件数量
     */
    int countByProjectId(@Param("projectId") Integer projectId);
    
    /**
     * 根据上传者 ID 查询文件列表（用于清理用户文件）
     * @param uploaderId 上传者 ID
     * @return 文件列表
     */
    List<ProjectFile> selectByUploaderId(@Param("uploaderId") Integer uploaderId);
    
    /**
     * 删除项目的所有文件（用于覆盖上传）
     * @param projectId 项目 ID
     * @return 影响行数
     */
    int deleteByProjectId(@Param("projectId") Integer projectId);
}
