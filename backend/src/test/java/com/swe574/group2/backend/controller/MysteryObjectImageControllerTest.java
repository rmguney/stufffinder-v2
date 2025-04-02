package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.entity.MysteryObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MysteryObjectImageControllerTest {

    @Mock
    private MysteryObjectRepository mysteryObjectRepository;

    @InjectMocks
    private MysteryObjectImageController mysteryObjectImageController;

    private MysteryObject mysteryObject;
    private MultipartFile imageFile;

    @BeforeEach
    public void setUp() {
        mysteryObject = new MysteryObject();
        mysteryObject.setId(1L);

        imageFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    @Test
    public void testUploadImage_Success() throws Exception {
        // Arrange
        when(mysteryObjectRepository.findById(1L)).thenReturn(Optional.of(mysteryObject));
        when(mysteryObjectRepository.save(mysteryObject)).thenReturn(mysteryObject);

        // Act
        ResponseEntity<Map<String, String>> response = mysteryObjectImageController.uploadImage(1L, imageFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("message", "Image uploaded successfully!");

        verify(mysteryObjectRepository).findById(1L);
        verify(mysteryObjectRepository).save(mysteryObject);
        assertThat(mysteryObject.getImageUrl()).isNotNull();
    }

    @Test
    public void testUploadImage_ObjectNotFound() {
        // Arrange
        when(mysteryObjectRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Map<String, String>> response = mysteryObjectImageController.uploadImage(1L, imageFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("message", "Mystery Object not found");

        verify(mysteryObjectRepository).findById(1L);
        verify(mysteryObjectRepository, never()).save(any());
    }

    @Test
    public void testUploadImage_FailedToUpload() throws IOException {
        // Arrange
        MultipartFile mockFailingFile = mock(MultipartFile.class);
        when(mysteryObjectRepository.findById(1L)).thenReturn(Optional.of(mysteryObject));
        when(mockFailingFile.getBytes()).thenThrow(new IOException("File read error"));

        // Act
        ResponseEntity<Map<String, String>> response = mysteryObjectImageController.uploadImage(1L, mockFailingFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("message", "Failed to upload image");

        verify(mysteryObjectRepository).findById(1L);
        verify(mysteryObjectRepository, never()).save(any());
    }
}
