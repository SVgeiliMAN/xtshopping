package com.xtbd.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.InputStream;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Image implements Serializable {

    Integer id;
    Integer goodsId;
    String imageUrl;
    Long size;
    String extName;
    byte[] bytes;
}
