package com.xtbd.Entity;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;

@Data
public class User implements Serializable {
    Integer userId;
    String nickName;
    String password;
    String phone;
    String identity;
    ArrayList<Integer> shoppingTrolley;
}
