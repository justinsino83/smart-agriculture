package com.weiming.smartag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weiming.smartag.entity.User;
import com.weiming.smartag.mapper.UserMapper;
import com.weiming.smartag.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
               .eq(User::getPassword, password)
               .eq(User::getStatus, 1);

        User user = getOne(wrapper);
        if (user != null) {
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            updateById(user);
            log.info("用户登录成功: {}", username);
        } else {
            log.warn("用户登录失败: {}", username);
        }
        return user;
    }
}
