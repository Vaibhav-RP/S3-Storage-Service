package com.example.awsstorage.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;

@Service
public class S3FileService implements IFileService {

    @Value("${bucketName}")
    private String bucketName;

    private final AmazonS3 s3;

    public S3FileService(AmazonS3 s3) {
        this.s3 = s3;
    }

    @Override
    public String saveFile(MultipartFile file, String userName) {
        String originalFilename = file.getOriginalFilename();
        String key = userName + "/" + originalFilename;
        int count = 0;
        int maxTries = 3;
        try {
            InputStream inputStream = file.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            s3.putObject(bucketName, key, inputStream, metadata);
            return "File uploaded";
        } catch (IOException e) {
            if (++count == maxTries) throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public byte[] downloadFile(String userName, String filename) {
        String key = userName + "/" + filename;
        if (!doesFileExist(key)) {
            throw new RuntimeException("File not found");
        }
        S3Object object = s3.getObject(bucketName, key);
        S3ObjectInputStream objectContent = object.getObjectContent();
        try {
            return IOUtils.toByteArray(objectContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteFile(String userName, String filename) {
        String key = userName + "/" + filename;
        if (!doesFileExist(key)) {
            return false;
        }
        try {
            s3.deleteObject(bucketName, key);
            return true;
        } catch (AmazonS3Exception e) {
            throw e;
        }
    }
    
    private boolean doesFileExist(String key) {
        try {
            s3.getObjectMetadata(bucketName, key);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                return false;
            } else {
                throw e;
            }
        }
    }
    
    @Override
    public List<String> listAllFiles(String userName) {
        String prefix = userName + "/";
        ListObjectsV2Result listObjectsV2Result = s3.listObjectsV2(bucketName, prefix);
        return listObjectsV2Result.getObjectSummaries()
                .stream()
                .map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());
    }

}
