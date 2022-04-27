package com.xtbd.serviceuser.service;


import com.xtbd.Entity.User;
import com.xtbd.service.UserService;
import com.xtbd.serviceuser.mapper.UserDao;
import com.xtbd.serviceuser.util.RedisUtil;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;

@Component
@org.springframework.stereotype.Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = UserService.class,timeout = 2000)
public class UserServiceImpl implements UserService {

    @Resource
    UserDao userDao;

    @Resource
    RocketMQTemplate rocketmqTemplate;

    @Resource
    RedisUtil redisUtil;


    @Override
    public boolean addUser(User user) {
        return userDao.addUser(user);
    }

    @Override
    public boolean login(User user) {
        return userDao.findUser(user);
    }

    @Override
    public User getUserInfoByNickName(User user) {
        return userDao.getUserInfoByNickName(user.getNickName());
    }



    @Override
    public User getUserInfoById(String userId) {
        return userDao.getUserInfoById(userId);
    }


}
