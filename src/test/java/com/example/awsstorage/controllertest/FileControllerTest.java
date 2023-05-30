package com.example.awsstorage.controllertest;

import com.example.awsstorage.controller.FileController;
import com.example.awsstorage.service.S3FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FileControllerTest {

    @Mock
    private S3FileService s3Service;

    private FileController fileController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        fileController = new FileController();
        fileController.setS3Service(s3Service); 
    }

    @Test
    void upload_ReturnsFileSavedMessage() {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "test content".getBytes());
        String userName = "test-user";
        when(s3Service.saveFile(file, userName)).thenReturn("File uploaded");

        // Act
        String result = fileController.upload(file, userName);

        // Assert
        assertEquals("File uploaded", result);
        verify(s3Service).saveFile(file, userName);
    }

    @Test
    void download_ReturnsFileBytes_WhenFileExists() {
        // Arrange
        String userName = "test-user";
        String filename = "test.txt";
        byte[] fileBytes = "test content".getBytes();
        when(s3Service.downloadFile(userName, filename)).thenReturn(fileBytes);

        // Act
        ResponseEntity<?> response = fileController.download(userName, filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.ALL_VALUE, response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename=test.txt", response.getHeaders().get("Content-Disposition").get(0));
        assertEquals(fileBytes, response.getBody());
        verify(s3Service).downloadFile(userName, filename);
    }

    @Test
    void download_ReturnsFileNotFound_WhenFileDoesNotExist() {
        // Arrange
        String userName = "test-user";
        String filename = "test.txt";
        when(s3Service.downloadFile(userName, filename)).thenThrow(new RuntimeException());

        // Act
        ResponseEntity<?> response = fileController.download(userName, filename);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("File not found", response.getBody());
        verify(s3Service).downloadFile(userName, filename);
    }

    @Test
    void deleteFile_ReturnsFileDeleted_WhenFileExists() {
        // Arrange
        String userName = "test-user";
        String filename = "test.txt";
        when(s3Service.deleteFile(userName, filename)).thenReturn(true);

        // Act
        ResponseEntity<String> response = fileController.deleteFile(userName, filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("File deleted", response.getBody());
        verify(s3Service).deleteFile(userName, filename);
    }

    @Test
    void deleteFile_ReturnsFileOrUserNotFound_WhenFileDoesNotExist() {
        // Arrange
        String userName = "test-user";
        String filename = "test.txt";
        when(s3Service.deleteFile(userName, filename)).thenReturn(false);

        // Act
        ResponseEntity<String> response = fileController.deleteFile(userName, filename);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("File or user not found", response.getBody());
        verify(s3Service).deleteFile(userName, filename);
    }

    @Test
    void getAllFiles_ReturnsListOfFiles() {
        // Arrange
        String userName = "test-user";
        List<String> files = Arrays.asList("file1.txt", "file2.txt");
        when(s3Service.listAllFiles(userName)).thenReturn(files);

        // Act
        ResponseEntity<List<String>> response = fileController.getAllFiles(userName);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(files, response.getBody());
        verify(s3Service).listAllFiles(userName);
    }

    @Test
    void getAllFiles_ReturnsNotFound_WhenNoFilesFound() {
        // Arrange
        String userName = "test-user";
        when(s3Service.listAllFiles(userName)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<String>> response = fileController.getAllFiles(userName);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Collections.singletonList("No files found for the user."), response.getBody());
        verify(s3Service).listAllFiles(userName);
    }
}
