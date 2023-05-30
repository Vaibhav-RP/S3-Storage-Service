package com.example.awsstorage.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class S3FileService implements IFileService {

    @Value("${bucketName}")
    private String bucketName;

    private final S3Client s3;

    public S3FileService(S3Client s3) {
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
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .metadata(Collections.singletonMap("Content-Length", String.valueOf(file.getSize())))
                    .build();
            s3.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
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
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try (ResponseInputStream<GetObjectResponse> response = s3.getObject(request)) {
            return response.readAllBytes();
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
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        try {
            s3.deleteObject(request);
            return true;
        } catch (SdkException e) {
            throw e;
        }
    }
    
    
    private boolean doesFileExist(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (SdkException e) {
            throw e;
        }
    }
    
    
    @Override
    public List<String> listAllFiles(String userName) {
        String prefix = userName + "/";
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();
        ListObjectsV2Response response = s3.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }
    

}
