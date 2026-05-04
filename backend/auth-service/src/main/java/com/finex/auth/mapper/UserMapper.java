package com.finex.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.finex.auth.entity.User;
import com.finex.auth.support.UserDepartmentRelationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            <script>
            SELECT user_id AS userId, dept_id AS deptId
            FROM sys_user_department_rel
            WHERE user_id IN
            <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
            </foreach>
            ORDER BY user_id ASC, dept_id ASC, id ASC
            </script>
            """)
    List<UserDepartmentRelationRecord> selectDepartmentRelationsByUserIds(@Param("userIds") Collection<Long> userIds);

    @Select("""
            <script>
            SELECT DISTINCT user_id
            FROM sys_user_department_rel
            WHERE dept_id IN
            <foreach collection="deptIds" item="deptId" open="(" separator="," close=")">
                #{deptId}
            </foreach>
            ORDER BY user_id ASC
            </script>
            """)
    List<Long> selectUserIdsByDepartmentIds(@Param("deptIds") Collection<Long> deptIds);

    @Select("""
            SELECT COUNT(1)
            FROM sys_user_department_rel
            WHERE dept_id = #{deptId}
            """)
    long countDepartmentRelationsByDepartmentId(@Param("deptId") Long deptId);

    @Delete("""
            DELETE FROM sys_user_department_rel
            WHERE user_id = #{userId}
            """)
    int deleteDepartmentRelationsByUserId(@Param("userId") Long userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_department_rel (user_id, dept_id, created_at, updated_at)
            VALUES
            <foreach collection="deptIds" item="deptId" separator=",">
                (#{userId}, #{deptId}, NOW(), NOW())
            </foreach>
            </script>
            """)
    int insertDepartmentRelations(@Param("userId") Long userId, @Param("deptIds") Collection<Long> deptIds);
}
