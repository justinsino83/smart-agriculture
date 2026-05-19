package com.weiming.smartag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.weiming.smartag.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码（明文，实际应为加密后的密码）
     * @return 用户信息，验证失败返回null
     */
    User login(String username, String password);
}
