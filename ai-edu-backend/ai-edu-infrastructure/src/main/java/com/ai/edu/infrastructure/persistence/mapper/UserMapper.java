package com.ai.edu.infrastructure.persistence.mapper;

import com.ai.edu.domain.user.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE username = #{username}")
    Optional<User> selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_user WHERE phone = #{phone}")
    Optional<User> selectByPhone(@Param("phone") String phone);

    @Select("SELECT COUNT(*) > 0 FROM t_user WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) > 0 FROM t_user WHERE phone = #{phone}")
    boolean existsByPhone(@Param("phone") String phone);
}