package com.example.awsstorage.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    String saveFile(MultipartFile file,String userName);
    byte[] downloadFile(String userName,String filename);
    boolean deleteFile(String userName,String filename);
    List<String> listAllFiles(String userName);
}