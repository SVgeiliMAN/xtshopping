package com.xtbd.Entity;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
public class Goods implements Serializable {
    Integer goodsId;
    String  goodsName;
    Double  goodsPrice;
    Integer belongTo;
    Integer count;
    Boolean onSale;
    //销量
    Integer sales;
    Date    createTime;
    Date    updateTime;
    String  unit;
    //折扣
    Double  discount;
    //商品热度
    Integer hot;

}
