package com.ai.edu.domain.user.model.aggregate;

import com.ai.edu.domain.user.model.entity.User;
import lombok.Getter;

/**
 * 用户聚合根
 */
@Getter
public class UserAggregate {

    private final User user;

    public UserAggregate(User user) {
        this.user = user;
    }

    public static UserAggregate create(String username, String password, String realName, String role) {
        User user = User.create(username, password, realName, role);
        return new UserAggregate(user);
    }

    public Long getId() {
        return user.getId();
    }

    public String getUsername() {
        return user.getUsername();
    }

    public String getRole() {
        return user.getRole();
    }
}