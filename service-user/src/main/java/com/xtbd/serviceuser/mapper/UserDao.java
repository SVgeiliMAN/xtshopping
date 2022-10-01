package com.xtbd.serviceuser.mapper;

import com.xtbd.Entity.User;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserDao {
    Integer addUser(User user);

    boolean findUser(User user);

    User getUserInfoByNickName(String nickName);


    User getUserInfoById(String userId);

}

