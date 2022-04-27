package com.xtbd.Entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Order implements Serializable {
    Integer orderId;
    Integer userId;
    Integer sellerId;
    Double price;

    Integer goodsId;
    Integer num;
    Date createTime;
    Boolean isPay;
    String address;

    String status;
    Boolean userDeleted;
    Boolean sellerDeleted;
}
