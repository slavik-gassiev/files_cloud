package com.slava.service;

import com.slava.dto.*;
import com.slava.exception.FolderDownloadException;
import com.slava.repository.CustomFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock
    private CustomFileRepository fileRepository;

    @Mock
    private FileService fileService;

    @InjectMocks
    private FolderService folderService;

    @Test
    void createFolder_callsRepositoryCreateFolder() {
        CreateFolderDto dto = new CreateFolderDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("some/path/");
        dto.setFolderName("newFolder");

        folderService.createFolder(dto);

        // createFolderDto.setSourcePath(...) => "some/path/newFolder"
        verify(fileRepository).createFolder("test-bucket", "some/path/newFolder");
    }

    @Test
    void deleteFolder_callsRepositoryDeleteFolder() {
        DeleteFileDto dto = new DeleteFileDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("some/path/folder/");

        folderService.deleteFolder(dto);

        verify(fileRepository).deleteFolder("test-bucket", "some/path/folder/");
    }

    @Test
    void renameFolder_callsMoveFolderWithCorrectArgs() {
        RenameFileDto dto = new RenameFileDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("some/path/oldName/");
        dto.setFileName("oldName");
        dto.setNewFileName("newName");

        folderService.renameFolder(dto);

        // newTargetPath = "some/path/oldName/".replace("oldName", "newName") => "some/path/newName/"
        verify(fileRepository).moveFolder("test-bucket", "some/path/oldName/", "some/path/newName/");
    }

    @Test
    void moveFolder_callsMoveFolderWithCorrectArgs() {
        MoveFileDto dto = new MoveFileDto();
        dto.setBucketName("test-bucket");
        dto.setSourcePath("old/path/");
        dto.setTargetPath("new/path/");
        dto.setFileName("folder/");

        folderService.moveFolder(dto);

        // newTargetPath = "new/path/" + "folder/" => "new/path/folder/"
        verify(fileRepository).moveFolder("test-bucket", "old/path/", "new/path/folder/");
    }

    @Test
    void downloadFolderAsZip_returnsZipBytes() {
        when(fileRepository.listObjects("test-bucket", "folder/"))
                .thenReturn(List.of("folder/file1.txt", "folder/file2.txt"));

        // Имитируем, что FileService скачивает файлы
        when(fileService.downloadFile("test-bucket", "folder/file1.txt"))
                .thenReturn("file1 content".getBytes());
        when(fileService.downloadFile("test-bucket", "folder/file2.txt"))
                .thenReturn("file2 content".getBytes());

        byte[] zipBytes = folderService.downloadFolderAsZip("test-bucket", "folder/");

        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        // При желании можно распаковать zipBytes и проверить состав архива подробнее
    }

    @Test
    void downloadFolderAsZip_throwsFolderDownloadException_onIOException() {
        // Если внутри цикла или при записи в ZipOutputStream возникнет исключение,
        // мы ожидаем FolderDownloadException. Упростим: пусть fileService выбросит RuntimeException,
        // а FolderService "поймает" IOException при записи в ZIP (симуляция).
        when(fileRepository.listObjects("test-bucket", "folder/"))
                .thenReturn(List.of("folder/file1.txt"));

        when(fileService.downloadFile("test-bucket", "folder/file1.txt"))
                .thenThrow(new RuntimeException("Simulated IO error"));

        assertThrows(FolderDownloadException.class, () ->
                folderService.downloadFolderAsZip("test-bucket", "folder/"));
    }

    @Test
    void listOnlyFolders_filtersOutNonFolders() {
        // Возвращаем набор объектов, часть из которых не заканчивается на "/"
        when(fileRepository.listObjects("test-bucket", ""))
                .thenReturn(List.of("folder1/", "folder2/", "folder2/file.txt", "file2.txt"));

        List<FileFolderDto> result = folderService.listOnlyFolders("test-bucket");

        // Ожидаем, что в результате только folder1/ и folder2/
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> "folder1/".equals(dto.getName())));
        assertTrue(result.stream().anyMatch(dto -> "folder2/".equals(dto.getName())));
    }

    @Test
    void getParentPathForFolder_removesLastSegment() {
        // Если путь заканчивается на "/", убираем её, потом обрезаем до последнего слэша
        String path = "some/path/folder/";
        String parentPath = folderService.getParentPathForFolder(path);
        assertEquals("some/path/", parentPath);

        // Если нет ни одного слэша => вернётся ""
        path = "folder";
        parentPath = folderService.getParentPathForFolder(path);
        assertEquals("", parentPath);
    }

    @Test
    void extractFolderName_handlesEmptyOrNull() {
        // Пустой или null => "Root"
        assertEquals("Root", folderService.extractFolderName(null));
        assertEquals("Root", folderService.extractFolderName(""));

        // Убираем конечный слэш, возвращаем часть после последнего слэша
        assertEquals("folder", folderService.extractFolderName("some/path/folder/"));
        assertEquals("folder", folderService.extractFolderName("folder"));
    }
}
