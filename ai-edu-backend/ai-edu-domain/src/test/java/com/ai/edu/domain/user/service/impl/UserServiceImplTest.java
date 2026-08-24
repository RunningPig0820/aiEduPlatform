package com.ai.edu.domain.user.service.impl;

import com.ai.edu.common.util.PasswordUtil;
import com.ai.edu.domain.user.model.entity.User;
import com.ai.edu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：组织域创建成员（教师/学生/家长）时，默认密码必须哈希存储。
 * 历史 bug：直接存明文 password123，导致 BCrypt.checkpw 失败、该用户永远无法登录。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_hashesDefaultPassword_forTeacher() {
        when(userRepository.existsByPhone("13800000001")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> (User) inv.getArgument(0));

        userService.createUser("张三", "13800000001", "TEACHER", null);

        User saved = captureSavedUser();
        String stored = saved.getPassword();
        assertNotEquals("password123", stored, "默认密码必须哈希存储，不能是明文");
        assertTrue(PasswordUtil.matches("password123", stored), "存储的哈希应能用 password123 校验通过");
    }

    @Test
    void createUser_hashesDefaultPassword_forStudent() {
        when(userRepository.existsByPhone("13800000002")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> (User) inv.getArgument(0));

        userService.createUser("李四", "13800000002", "STUDENT", "110101199001011234");

        User saved = captureSavedUser();
        String stored = saved.getPassword();
        assertNotEquals("password123", stored, "默认密码必须哈希存储，不能是明文");
        assertTrue(PasswordUtil.matches("password123", stored), "存储的哈希应能用 password123 校验通过");
    }

    private User captureSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }
}
