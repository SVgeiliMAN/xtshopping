package com.xtbd.Entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class Seller implements Serializable {
    Integer sellerId;
    String nickName;
    String password;
    String phone;
    String identity;

}