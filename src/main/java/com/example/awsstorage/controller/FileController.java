package com.example.awsstorage.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import com.example.awsstorage.service.S3FileService;

@RestController
public class FileController {

    @Autowired
    private S3FileService s3Service;

    public void setS3Service(S3FileService s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file, @RequestParam("userName") String userName) {
        return s3Service.saveFile(file, userName);
    }

    @GetMapping("/download/{userName}/{filename}")
    public ResponseEntity<?> download(@PathVariable("userName") String userName, @PathVariable("filename") String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", MediaType.ALL_VALUE);
        headers.add("Content-Disposition", "attachment; filename=" + filename);
        try {
            byte[] bytes = s3Service.downloadFile(userName, filename);
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
    }
    
    @DeleteMapping("/delete/{userName}/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable("userName") String userName, @PathVariable("filename") String filename) {
        boolean isDeleted = s3Service.deleteFile(userName, filename);
        if (isDeleted) {
            return ResponseEntity.ok("File deleted");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File or user not found");
        }
    }

    @GetMapping("/list/{userName}")
    public ResponseEntity<List<String>> getAllFiles(@PathVariable("userName") String userName) {
        List<String> files = s3Service.listAllFiles(userName);
        
        if (files.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonList("No files found for the user."));
        }
        return ResponseEntity.ok(files);
    }

    
}
