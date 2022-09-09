package com.xtbd.servicegoods.util;

import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.exception.FdfsUnsupportStorePathException;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class FastDFSClient {
    @Resource
    private FastFileStorageClient storageClient;

    @Resource
    private FdfsWebServer fdfsWebServer;

    /**
     * 上传文件
     * @param file 文件对象
     * @return 文件访问地址
     * @throws IOException
     */
    public String uploadFile(MultipartFile file) throws IOException {
        StorePath storePath = storageClient.uploadFile(file.getInputStream(),file.getSize(), FilenameUtils.getExtension(file.getOriginalFilename()),null);
        return storePath.getFullPath();
    }

    public String uploadFile(InputStream inputStream,Long fileSize,String fileExtName) throws IOException{
        StorePath storePath = storageClient.uploadFile("group1", inputStream, fileSize, fileExtName);
        return storePath.getFullPath();
    }

    /**
     * 上传文件
     * @param file 文件对象
     * @return 文件访问地址
     * @throws IOException
     */
    public String uploadFile(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream (file);
        StorePath storePath = storageClient.uploadFile(inputStream,file.length(), FilenameUtils.getExtension(file.getName()),null);
        return getResAccessUrl(storePath);
    }

    /**
     * 封装图片完整URL地址
     * @param storePath
     * @return
     */
    private String getResAccessUrl(StorePath storePath) {
        return fdfsWebServer.getWebServerUrl() + storePath.getFullPath();
    }


    /**
     * 删除文件
     * @param fileUrl 文件访问地址
     * @return
     */

    public void deleteFile(String fileUrl){
        if (StringUtils.isEmpty(fileUrl)) {
            return;
        }
        try {
            StorePath storePath = StorePath.parseFromUrl(fileUrl);
            storageClient.deleteFile(storePath.getGroup(), storePath.getPath());
        } catch (FdfsUnsupportStorePathException e) {
            System.out.println(e);
        }
    }
}
