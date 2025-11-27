package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for LinkListExtractor.
 */
@ExtendWith(MockitoExtension.class)
class LinkListExtractorTest {

  @Mock
  private FileService fileService;

  private LinkListExtractor linkListExtractor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    linkListExtractor = new LinkListExtractor(fileService);
    objectMapper = new ObjectMapper();
  }

  // ========== getLinkListAsStream Tests ==========

  @Test
  void getLinkListAsStream_withValidPublicSubmission_shouldReturnInputStream() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.json";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, true, null);

    // Assert
    assertNotNull(result);
    assertEquals(expectedStream, result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService, times(1)).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertEquals(accession, capturedMetadata.getAccession());
    assertEquals(filename, capturedMetadata.getUiRequestedPath());
    assertEquals(relativePath, capturedMetadata.getRelativePath());
    assertTrue(capturedMetadata.isPublic());
    assertEquals(Constants.File.StorageMode.NFS, capturedMetadata.getStorageMode());
  }

  @Test
  void getLinkListAsStream_withValidPrivateSubmissionAndSecretKey_shouldSetSecretKey() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.json";
    String secretKey = "secret123";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, false, secretKey);

    // Assert
    assertNotNull(result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertEquals(secretKey, capturedMetadata.getSecKey());
    assertFalse(capturedMetadata.isPublic());
  }

  @Test
  void getLinkListAsStream_withPrivateSubmissionAndNoSecretKey_shouldNotSetSecretKey() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.json";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, false, null);

    // Assert
    assertNotNull(result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertNull(capturedMetadata.getSecKey());
  }

  @Test
  void getLinkListAsStream_withPrivateSubmissionAndEmptySecretKey_shouldNotSetSecretKey() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.json";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, false, "");

    // Assert
    assertNotNull(result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertNull(capturedMetadata.getSecKey());
  }

  @Test
  void getLinkListAsStream_withFireStorageMode_shouldNotRequireSecretKey() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.json";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.FIRE);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, false, null);

    // Assert
    assertNotNull(result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertEquals(Constants.File.StorageMode.FIRE, capturedMetadata.getStorageMode());
    assertNull(capturedMetadata.getSecKey());
  }

  @Test
  void getLinkListAsStream_withMissingFilename_shouldReturnNull() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    JsonNode json = createJsonWithoutExtractedLinks();

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, true, null);

    // Assert
    assertNull(result);
    verify(fileService, never()).getDownloadFile(any(FileMetaData.class));
  }

  @Test
  void getLinkListAsStream_withEmptyFilename_shouldReturnNull() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    JsonNode json = createValidJsonWithExtractedLinks("", Constants.File.StorageMode.NFS);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, true, null);

    // Assert
    assertNull(result);
    verify(fileService, never()).getDownloadFile(any(FileMetaData.class));
  }

  @Test
  void getLinkListAsStream_withFilenameWithoutJsonExtension_shouldAddJsonExtension() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, true, null);

    // Assert
    assertNotNull(result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertEquals("extracted_links.json", capturedMetadata.getUiRequestedPath());
  }

  @Test
  void getLinkListAsStream_withFilenameAlreadyHavingJsonExtension_shouldNotDuplicate() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.json";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, true, null);

    // Assert
    assertNotNull(result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertEquals("extracted_links.json", capturedMetadata.getUiRequestedPath());
  }

  @Test
  void getLinkListAsStream_withMixedCaseJsonExtension_shouldNotDuplicate() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.JSON";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    InputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
    setupFileServiceMock(expectedStream);

    // Act
    InputStream result = linkListExtractor.getLinkListAsStream(
        accession, relativePath, json, true, null);

    // Assert
    assertNotNull(result);

    // Verify and capture
    ArgumentCaptor<FileMetaData> captor = ArgumentCaptor.forClass(FileMetaData.class);
    verify(fileService).getDownloadFile(captor.capture());

    FileMetaData capturedMetadata = captor.getValue();
    assertEquals("extracted_links.JSON", capturedMetadata.getUiRequestedPath());
  }

  @Test
  void getLinkListAsStream_withFileNotFoundException_shouldPropagateException() throws Exception {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    String filename = "extracted_links.json";
    JsonNode json = createValidJsonWithExtractedLinks(filename, Constants.File.StorageMode.NFS);

    doThrow(new FileNotFoundException("File not found"))
        .when(fileService).getDownloadFile(any(FileMetaData.class));

    // Act & Assert
    assertThrows(FileNotFoundException.class, () ->
        linkListExtractor.getLinkListAsStream(accession, relativePath, json, true, null));
  }

  @Test
  void getLinkListAsStream_withMissingStorageMode_shouldThrowIllegalStateException() {
    // Arrange
    String accession = "S-BSST2690";
    String relativePath = "/path/to/files";
    JsonNode json = createJsonWithoutStorageMode();

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
        linkListExtractor.getLinkListAsStream(accession, relativePath, json, true, null));

    assertTrue(exception.getMessage().contains("Storage Mode"));
  }

  // ========== Helper Methods ==========

  /**
   * Creates a valid JSON structure with extracted links section.
   */
  private JsonNode createValidJsonWithExtractedLinks(String filename, Constants.File.StorageMode storageMode) {
    String jsonString = "{\n" +
        "  \"accession\": \"S-BSST2690\",\n" +
        "  \"" + Constants.Fields.STORAGE_MODE + "\": \"" + storageMode.name() + "\",\n" +
        "  \"section\": {\n" +
        "    \"type\": \"Study\",\n" +
        "    \"sections\": [\n" +
        "      {\n" +
        "        \"type\": \"ExtractedLinks\",\n" +
        "        \"linkList\": {\n" +
        "          \"fileName\": \"" + filename + "\"\n" +
        "        }\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "}";

    try {
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create test JSON", e);
    }
  }

  /**
   * Creates JSON without extracted links section.
   */
  private JsonNode createJsonWithoutExtractedLinks() {
    String jsonString = "{\n" +
        "  \"accession\": \"S-BSST2690\",\n" +
        "  \"" + Constants.Fields.STORAGE_MODE + "\": \"NFS\",\n" +
        "  \"section\": {\n" +
        "    \"type\": \"Study\",\n" +
        "    \"sections\": []\n" +
        "  }\n" +
        "}";

    try {
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create test JSON", e);
    }
  }

  /**
   * Creates JSON without storage mode field.
   */
  private JsonNode createJsonWithoutStorageMode() {
    String jsonString = "{\n" +
        "  \"accession\": \"S-BSST2690\",\n" +
        "  \"section\": {\n" +
        "    \"type\": \"Study\",\n" +
        "    \"sections\": [\n" +
        "      {\n" +
        "        \"type\": \"ExtractedLinks\",\n" +
        "        \"linkList\": {\n" +
        "          \"fileName\": \"extracted_links.json\"\n" +
        "        }\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "}";

    try {
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create test JSON", e);
    }
  }

  /**
   * Sets up the fileService mock to set an InputStream on the FileMetaData when called.
   */
  private void setupFileServiceMock(InputStream inputStream) throws FileNotFoundException {
    doAnswer(invocation -> {
      FileMetaData metadata = invocation.getArgument(0);
      metadata.setInputStream(inputStream);
      return null;
    }).when(fileService).getDownloadFile(any(FileMetaData.class));
  }
}
