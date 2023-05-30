package com.example.awsstorage.servicetest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import com.example.awsstorage.service.S3FileService;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

class S3FileServiceTest {

    @Mock
    private S3Client s3Client;

    private S3FileService s3FileService;
    private String bucketName = "test-bucket";
    private String userName = "test-user";
    private String filename = "test-file";

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        s3FileService = new S3FileService(s3Client);
        s3FileService.setBucketName(bucketName);
    }

    @Test
    void saveFile_UploadsFileSuccessfully() throws IOException {
        // Arrange
        String filename = "test-file.txt";
        InputStream fileStream = new ByteArrayInputStream("test content".getBytes());
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/plain", fileStream);
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(putObjectResponse);

        // Act
        String result = s3FileService.saveFile(file, userName);

        // Assert
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals("File uploaded", result);
    }

    @Test
    void downloadFile_ShouldThrowRuntimeException_WhenFileNotFound() {
        // Arrange
        String filename = "test.txt";
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> s3FileService.downloadFile(userName, filename));
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void deleteFile_ShouldDeleteFileFromS3() {
        // Arrange
        String filename = "test.txt";
        DeleteObjectResponse deleteObjectResponse = DeleteObjectResponse.builder().build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(deleteObjectResponse);

        // Act
        boolean result = s3FileService.deleteFile(userName, filename);

        // Assert
        assertTrue(result);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_ShouldReturnFalse_WhenFileNotFound() {
        // Arrange
        String filename = "test.txt";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        // Act
        boolean result = s3FileService.deleteFile(userName, filename);

        // Assert
        assertFalse(result);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void listAllFiles_ShouldReturnListOfFiles() {
        // Arrange
        String prefix = userName + "/";
        ListObjectsV2Response listObjectsV2Response = ListObjectsV2Response.builder()
                .contents(Arrays.asList(
                        S3Object.builder().key(prefix + "file1.txt").build(),
                        S3Object.builder().key(prefix + "file2.txt").build()
                ))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsV2Response);

        // Act
        List<String> result = s3FileService.listAllFiles(userName);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(prefix + "file1.txt"));
        assertTrue(result.contains(prefix + "file2.txt"));
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void deleteFile_FileExists_ReturnsTrue() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        boolean result = s3FileService.deleteFile(userName, filename);

        assertTrue(result);
        verify(s3Client).headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(userName + "/" + filename)
                .build());
        verify(s3Client).deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(userName + "/" + filename)
                .build());
    }

    @Test
    void deleteFile_FileDoesNotExist_ReturnsFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.class);

        boolean result = s3FileService.deleteFile(userName, filename);

        assertFalse(result);
        verify(s3Client).headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(userName + "/" + filename)
                .build());
        verifyNoMoreInteractions(s3Client);
    }

    @Test
    void listAllFiles_ReturnsListOfFiles() {
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Arrays.asList(
                        S3Object.builder().key(userName + "/file1").build(),
                        S3Object.builder().key(userName + "/file2").build()
                ))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(response);

        List<String> files = s3FileService.listAllFiles(userName);

        assertEquals(2, files.size());
        assertTrue(files.contains(userName + "/file1"));
        assertTrue(files.contains(userName + "/file2"));
        verify(s3Client).listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(userName + "/")
                .build());
    }

}
