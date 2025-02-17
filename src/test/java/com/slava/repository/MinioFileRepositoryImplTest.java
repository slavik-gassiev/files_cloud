package com.slava.repository;

import com.slava.exception.FileException;
import io.minio.*;
import io.minio.messages.Item;
import io.minio.messages.DeleteObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Если хотите отключить UnnecessaryStubbingException, раскомментируйте:
// @MockitoSettings(strictness = Strictness.LENIENT)
class MinioFileRepositoryImplTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MinioFileRepositoryImpl fileRepository;

    // -------------------------
    // UPLOAD FILE
    // -------------------------
    @Test
    void uploadFile_success() throws Exception {
        // putObject(...) возвращает ObjectWriteResponse, поэтому нельзя doNothing().
        ObjectWriteResponse mockResponse = mock(ObjectWriteResponse.class);
        doReturn(mockResponse).when(minioClient).putObject(any(PutObjectArgs.class));

        String bucket = "bucket";
        String objectName = "file.txt";
        byte[] content = "Hello".getBytes();
        ByteArrayInputStream stream = new ByteArrayInputStream(content);
        long size = content.length;
        String contentType = "text/plain";

        fileRepository.uploadFile(bucket, objectName, stream, size, contentType);

        // Проверяем, что putObject вызван с правильными аргументами
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertEquals(bucket, args.bucket());
        assertEquals(objectName, args.object());
        assertEquals(contentType, args.contentType());
    }

    @Test
    void uploadFile_failure_throwsFileException() throws Exception {
        // Если метод не декларирует Exception, лучше использовать RuntimeException
        doThrow(new RuntimeException("Error")).when(minioClient).putObject(any(PutObjectArgs.class));

        assertThrows(FileException.class, () ->
                fileRepository.uploadFile("bucket", "file.txt",
                        new ByteArrayInputStream("data".getBytes()),
                        4, "text/plain"));
    }

    // -------------------------
    // DOWNLOAD FILE
    // -------------------------
    @Test
    void downloadFile_success() throws Exception {
        // getObject(...) возвращает GetObjectResponse
        GetObjectResponse mockResponse = mock(GetObjectResponse.class);
        doReturn(mockResponse).when(minioClient).getObject(any(GetObjectArgs.class));

        Optional<InputStream> result = fileRepository.downloadFile("bucket", "file.txt");
        assertTrue(result.isPresent());
        // У нас mockResponse тоже является InputStream
        assertEquals(mockResponse, result.get());
    }

    @Test
    void downloadFile_failure_returnsEmptyOptional() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).getObject(any(GetObjectArgs.class));

        Optional<InputStream> result = fileRepository.downloadFile("bucket", "file.txt");
        assertFalse(result.isPresent());
    }

    // -------------------------
    // DELETE FILE
    // -------------------------
    @Test
    void deleteFile_success() throws Exception {
        // removeObject(...) — метод void
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        fileRepository.deleteFile("bucket", "file.txt");

        ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        RemoveObjectArgs args = captor.getValue();
        assertEquals("bucket", args.bucket());
        assertEquals("file.txt", args.object());
    }

    @Test
    void deleteFile_failure_throwsFileException() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThrows(FileException.class, () ->
                fileRepository.deleteFile("bucket", "file.txt"));
    }

    // -------------------------
    // COPY FILE
    // -------------------------
    @Test
    void copyFile_success() throws Exception {
        // copyObject(...) возвращает ObjectWriteResponse
        ObjectWriteResponse mockResponse = mock(ObjectWriteResponse.class);
        doReturn(mockResponse).when(minioClient).copyObject(any(CopyObjectArgs.class));

        fileRepository.copyFile("bucket", "source.txt", "target.txt");

        ArgumentCaptor<CopyObjectArgs> captor = ArgumentCaptor.forClass(CopyObjectArgs.class);
        verify(minioClient).copyObject(captor.capture());
        CopyObjectArgs args = captor.getValue();
        assertEquals("bucket", args.bucket());
        assertEquals("target.txt", args.object());

        // Проверяем источник
        CopySource sourceArg = args.source();
        assertEquals("bucket", sourceArg.bucket());
        assertEquals("source.txt", sourceArg.object());
    }

    @Test
    void copyFile_failure_throwsFileException() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).copyObject(any(CopyObjectArgs.class));

        assertThrows(FileException.class, () ->
                fileRepository.copyFile("bucket", "source.txt", "target.txt"));
    }

    // -------------------------
    // LIST OBJECTS
    // -------------------------
    @Test
    void listObjects_success() throws Exception {
        String bucket = "bucket";
        String prefix = "prefix/";

        // Мокаем объекты Item
        Item item1 = mock(Item.class);
        when(item1.objectName()).thenReturn("prefix/file1.txt");

        Item item2 = mock(Item.class);
        when(item2.objectName()).thenReturn("prefix/file2.txt");

        // Мокаем Result<Item>
        Result<Item> result1 = mock(Result.class);
        when(result1.get()).thenReturn(item1);
        Result<Item> result2 = mock(Result.class);
        when(result2.get()).thenReturn(item2);

        // Возвращаем iterable
        Iterable<Result<Item>> iterable = List.of(result1, result2);
        doReturn(iterable).when(minioClient).listObjects(any(ListObjectsArgs.class));

        List<String> objects = fileRepository.listObjects(bucket, prefix);
        assertEquals(2, objects.size());
        assertTrue(objects.contains("prefix/file1.txt"));
        assertTrue(objects.contains("prefix/file2.txt"));
    }

    @Test
    void listObjects_failure_throwsFileException() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).listObjects(any(ListObjectsArgs.class));

        assertThrows(FileException.class, () ->
                fileRepository.listObjects("bucket", "prefix/"));
    }

    // -------------------------
    // BUCKET EXISTS
    // -------------------------
    @Test
    void bucketExists_success() throws Exception {
        doReturn(true).when(minioClient).bucketExists(any(BucketExistsArgs.class));

        boolean exists = fileRepository.bucketExists("bucket");
        assertTrue(exists);
    }

    @Test
    void bucketExists_failure_throwsFileException() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).bucketExists(any(BucketExistsArgs.class));

        assertThrows(FileException.class, () ->
                fileRepository.bucketExists("bucket"));
    }

    // -------------------------
    // CREATE BUCKET
    // -------------------------
    @Test
    void createBucket_success() throws Exception {
        // makeBucket(...) — метод void
        doNothing().when(minioClient).makeBucket(any(MakeBucketArgs.class));

        fileRepository.createBucket("bucket");

        ArgumentCaptor<MakeBucketArgs> captor = ArgumentCaptor.forClass(MakeBucketArgs.class);
        verify(minioClient).makeBucket(captor.capture());
        MakeBucketArgs args = captor.getValue();
        assertEquals("bucket", args.bucket());
    }

    @Test
    void createBucket_failure_throwsFileException() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).makeBucket(any(MakeBucketArgs.class));

        assertThrows(FileException.class, () ->
                fileRepository.createBucket("bucket"));
    }

    // -------------------------
    // CREATE FOLDER
    // -------------------------
    @Test
    void createFolder_callsUploadFile() throws Exception {
        // Репозиторий внутри createFolder вызывает uploadFile, которая сама вызывает minioClient.putObject.
        // Можно заспайть сам репозиторий и "пропустить" реальный вызов uploadFile.
        MinioFileRepositoryImpl spyRepo = spy(fileRepository);

        // uploadFile(...) в MinioFileRepositoryImpl — это void.
        doNothing().when(spyRepo).uploadFile(anyString(), anyString(), any(InputStream.class), anyLong(), anyString());

        spyRepo.createFolder("bucket", "folder");

        // Проверяем, что в итоге вызвана uploadFile с нужными аргументами
        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(spyRepo).uploadFile(eq("bucket"), objectNameCaptor.capture(), any(InputStream.class), eq(0L), eq("application/x-directory"));
        String objectName = objectNameCaptor.getValue();
        assertTrue(objectName.endsWith("/"), "Папка должна оканчиваться на '/'");
    }

    // -------------------------
    // DELETE FOLDER
    // -------------------------
    @Test
    void deleteFolder_callsDeleteFileForEachObject() throws Exception {
        String bucket = "bucket";
        String folderPath = "folder/";

        // Мокаем объекты
        Item item1 = mock(Item.class);
        when(item1.objectName()).thenReturn("folder/file1.txt");
        Item item2 = mock(Item.class);
        when(item2.objectName()).thenReturn("folder/file2.txt");

        Result<Item> res1 = mock(Result.class);
        when(res1.get()).thenReturn(item1);
        Result<Item> res2 = mock(Result.class);
        when(res2.get()).thenReturn(item2);

        doReturn(List.of(res1, res2)).when(minioClient).listObjects(any(ListObjectsArgs.class));
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        fileRepository.deleteFolder(bucket, folderPath);

        // Ожидаем 2 вызова removeObject
        verify(minioClient, times(2)).removeObject(any(RemoveObjectArgs.class));
    }

    // -------------------------
    // MOVE FOLDER
    // -------------------------
    @Test
    void moveFolder_callsCopyAndDeleteForEachObject() throws Exception {
        String bucket = "bucket";
        String sourcePath = "source/";
        String targetPath = "target/";

        // Два объекта в папке
        Item item1 = mock(Item.class);
        when(item1.objectName()).thenReturn("source/file1.txt");
        Item item2 = mock(Item.class);
        when(item2.objectName()).thenReturn("source/file2.txt");

        Result<Item> res1 = mock(Result.class);
        when(res1.get()).thenReturn(item1);
        Result<Item> res2 = mock(Result.class);
        when(res2.get()).thenReturn(item2);

        doReturn(List.of(res1, res2)).when(minioClient).listObjects(any(ListObjectsArgs.class));

        // copyObject возвращает ObjectWriteResponse
        doReturn(mock(ObjectWriteResponse.class)).when(minioClient).copyObject(any(CopyObjectArgs.class));
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        fileRepository.moveFolder(bucket, sourcePath, targetPath);

        verify(minioClient, times(2)).copyObject(any(CopyObjectArgs.class));
        verify(minioClient, times(2)).removeObject(any(RemoveObjectArgs.class));
    }

    // -------------------------
    // MOVE FILE
    // -------------------------
    @Test
    void moveFile_callsCopyAndDelete() throws Exception {
        String bucket = "bucket";
        String sourcePath = "file.txt";
        String targetPath = "newfile.txt";

        // copyObject => ObjectWriteResponse
        doReturn(mock(ObjectWriteResponse.class)).when(minioClient).copyObject(any(CopyObjectArgs.class));
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        fileRepository.moveFile(bucket, sourcePath, targetPath);

        verify(minioClient).copyObject(any(CopyObjectArgs.class));
        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
