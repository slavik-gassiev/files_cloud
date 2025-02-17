package com.slava.service;

import com.slava.dto.*;
import com.slava.exception.FileNotFoundException;
import com.slava.repository.CustomFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private CustomFileRepository fileRepository;

    @InjectMocks
    private FileService fileService;

    @Test
    void moveFile_callsRepositoryWithCorrectArgs() {
        MoveFileDto dto = new MoveFileDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("source/path/");
        dto.setTargetPath("target/path/");
        dto.setFileName("file.txt");

        fileService.moveFile(dto);

        // Проверяем, что в репозиторий переданы корректные аргументы
        verify(fileRepository).moveFile("test-bucket", "source/path/", "target/path/file.txt");
    }

    @Test
    void renameFile_callsRepositoryWithCorrectArgs() {
        RenameFileDto dto = new RenameFileDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("some/path/file.txt");
        dto.setFileName("file.txt");
        dto.setNewFileName("renamed.txt");

        fileService.renameFile(dto);

        // newTargetPath = "some/path/file.txt".replace("file.txt", "renamed.txt") => "some/path/renamed.txt"
        verify(fileRepository).moveFile("test-bucket", "some/path/file.txt", "some/path/renamed.txt");
    }

    @Test
    void deleteFile_callsRepositoryDeleteFile() {
        DeleteFileDto dto = new DeleteFileDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("some/path/file.txt");

        fileService.deleteFile(dto);

        verify(fileRepository).deleteFile("test-bucket", "some/path/file.txt");
    }

    @Test
    void downloadFile_success() {
        String bucketName = "test-bucket";
        String objectName = "path/to/file.txt";
        byte[] fileContent = "Hello World".getBytes();

        // Мокаем возвращение InputStream
        when(fileRepository.downloadFile(bucketName, objectName))
                .thenReturn(Optional.of(new ByteArrayInputStream(fileContent)));

        byte[] result = fileService.downloadFile(bucketName, objectName);

        assertArrayEquals(fileContent, result);
    }

    @Test
    void downloadFile_fileNotFound_throwsException() {
        when(fileRepository.downloadFile("test-bucket", "missing/file.txt"))
                .thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class,
                () -> fileService.downloadFile("test-bucket", "missing/file.txt"));
    }

    @Test
    void listFolderContents_filtersAndMapsObjects() {
        when(fileRepository.listObjects("test-bucket", "folder/"))
                .thenReturn(List.of(
                        "folder/",               // Папка, совпадает с префиксом
                        "folder/subfolder/",     // Папка внутри folder
                        "folder/file1.txt",      // Файл внутри folder
                        "folder/subfolder/file2.txt" // Файл внутри subfolder
                ));

        List<FileFolderDto> result = fileService.listFolderContents("test-bucket", "folder/");

        // Согласно логике:
        // - "folder/" => relativePath = "" -> отфильтровывается (empty)
        // - "folder/subfolder/" => relativePath = "subfolder/" -> папка (т.к. заканчивается на "/")
        //   и она проходит фильтр, т.к. индекс '/' == length-1
        // - "folder/file1.txt" => relativePath = "file1.txt" -> файл (не содержит "/")
        // - "folder/subfolder/file2.txt" => relativePath = "subfolder/file2.txt"
        //   содержит "/", индекс = 9, length-1 = 16 -> не проходит
        //
        // Итого останутся "folder/subfolder/" и "folder/file1.txt"
        assertEquals(2, result.size());

        FileFolderDto folderDto = result.stream()
                .filter(FileFolderDto::isFolder)
                .findFirst().orElseThrow();
        FileFolderDto fileDto = result.stream()
                .filter(dto -> !dto.isFolder())
                .findFirst().orElseThrow();

        assertEquals("subfolder/", folderDto.getName());
        assertEquals("folder/subfolder/", folderDto.getPath());
        assertTrue(folderDto.isFolder());

        assertEquals("file1.txt", fileDto.getName());
        assertEquals("folder/file1.txt", fileDto.getPath());
        assertFalse(fileDto.isFolder());
    }

    @Test
    void uploadFile_callsRepositoryWithCorrectArgs() {
        UploadFileDto dto = new UploadFileDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("folder/");
        dto.setFileName("file.txt");
        dto.setContent("Hello".getBytes());
        dto.setContentType("text/plain");

        fileService.uploadFile(dto);

        verify(fileRepository).uploadFile(
                eq("test-bucket"),
                eq("folder/file.txt"),
                any(ByteArrayInputStream.class),
                eq((long) "Hello".getBytes().length),
                eq("text/plain")
        );
    }

    @Test
    void ensureBucketExists_bucketDoesNotExist_createsBucket() {
        when(fileRepository.bucketExists("test-bucket")).thenReturn(false);

        fileService.ensureBucketExists("test-bucket");

        verify(fileRepository).createBucket("test-bucket");
    }

    @Test
    void ensureBucketExists_bucketExists_noCreateBucketCall() {
        when(fileRepository.bucketExists("test-bucket")).thenReturn(true);

        fileService.ensureBucketExists("test-bucket");

        verify(fileRepository, never()).createBucket("test-bucket");
    }

    @Test
    void getParentPathForFile_returnsExpected() {
        String fullPath = "some/path/to/file.txt";
        String parentPath = fileService.getParentPathForFile(fullPath);

        assertEquals("some/path/to", parentPath);
    }

    @Test
    void getBreadcrumbLinks_returnsListOfSegments() {
        String path = "some/path/to/";
        List<String> breadcrumbLinks = fileService.getBreadcrumbLinks(path);

        // Логика:
        // segment=some => fullPath=some/ => breadcrumbLinks=["some/"]
        // segment=path => fullPath=some/path/ => breadcrumbLinks=["some/", "some/path/"]
        // segment=to => fullPath=some/path/to/ => breadcrumbLinks=["some/", "some/path/", "some/path/to/"]
        assertEquals(3, breadcrumbLinks.size());
        assertEquals("some/", breadcrumbLinks.get(0));
        assertEquals("some/path/", breadcrumbLinks.get(1));
        assertEquals("some/path/to/", breadcrumbLinks.get(2));
    }

    @Test
    void getPathSegments_splitsCorrectly() {
        String path = "some/path/to/file.txt";
        String[] segments = fileService.getPathSegments(path);

        assertArrayEquals(new String[] {"some", "path", "to", "file.txt"}, segments);
    }
}
