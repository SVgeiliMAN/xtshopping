package com.xtbd.service;

import com.xtbd.Entity.User;

public interface UserService {

    boolean addUser(User user);

    boolean login(User user);

    User getUserInfoByNickName(User user);

    User getUserInfoById(String userId);




}
