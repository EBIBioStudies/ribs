package uk.ac.ebi.biostudies.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.ExtractedLink;
import uk.ac.ebi.biostudies.service.LinkListIndexService;
import uk.ac.ebi.biostudies.service.TextMiningLinkUpdater;

@RunWith(MockitoJUnitRunner.class)
public class LinkListIndexServiceImplTest {

  private static final String SECRET_KEY = "secret_key";
  private static final String ACCESSION = "S-BIAD2225";
  private static final String REL_PATH = "S-BIAD/225/S-BIAD2225";
  @Mock private LinkListExtractor linkListExtractor;
  @Mock private TextMiningLinkUpdater textMiningLinkUpdater;
  @Mock private IndexWriter indexWriter;
  private LinkListIndexService instance;

  @Before
  public void setUp() {
    instance = new LinkListIndexServiceImpl(linkListExtractor, textMiningLinkUpdater);
  }

  @Test
  public void testIndexExtractedLinkList_withValidLinks() throws IOException {
    // Arrange
    JsonNode submissionJson = createTestSubmissionJson();
    Document ownerDocument = createTestDocument(ACCESSION);

    try (InputStream is = loadExtractedLinksStreamByFileName("singleNodeExtractedLinks.json")) {
      assertNotNull(is, "Test resource not found");

      when(linkListExtractor.getLinkListAsStream(
          anyString(), anyString(), any(JsonNode.class), anyBoolean(), anyString()))
          .thenReturn(is);

      // Act
      instance.indexExtractedLinkList(
          ACCESSION, ownerDocument, REL_PATH, submissionJson, indexWriter, true, SECRET_KEY);

      // Assert - verify textMiningLinkUpdater was called with the document
      ArgumentCaptor<String> accessionCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
      ArgumentCaptor<List> linksCaptor = ArgumentCaptor.forClass(List.class);

      verify(textMiningLinkUpdater, times(1))
          .indexExtractedLinks(
              accessionCaptor.capture(), documentCaptor.capture(), linksCaptor.capture());

      // Verify correct values were passed
      assertEquals(ACCESSION, accessionCaptor.getValue());
      assertSame(ownerDocument, documentCaptor.getValue());
      assertNotNull(linksCaptor.getValue());
      assertFalse(linksCaptor.getValue().isEmpty());
    }
  }

  @Test
  public void testIndexExtractedLinkList_withNoExtractedLinksFile() throws IOException {
    // Arrange
    JsonNode submissionJson = createTestSubmissionJson();
    Document ownerDocument = createTestDocument(ACCESSION);

    when(linkListExtractor.getLinkListAsStream(
        anyString(), anyString(), any(JsonNode.class), anyBoolean(), anyString()))
        .thenReturn(null);

    // Act
    instance.indexExtractedLinkList(
        ACCESSION, ownerDocument, REL_PATH, submissionJson, indexWriter,true, SECRET_KEY);

    // Assert - verify textMiningLinkUpdater was NOT called
    verify(textMiningLinkUpdater, never())
        .indexExtractedLinks(anyString(), any(Document.class), any());
  }

  @Test
  public void testIndexExtractedLinkList_withNullDocument() {
    // Arrange
    JsonNode submissionJson = createTestSubmissionJson();

    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () ->
            instance.indexExtractedLinkList(
                ACCESSION, null, REL_PATH, submissionJson, indexWriter,true, SECRET_KEY));
  }

  @Test
  public void testIndexExtractedLinkList_withFileNotFoundException() throws FileNotFoundException {
    // Arrange
    JsonNode submissionJson = createTestSubmissionJson();
    Document ownerDocument = createTestDocument(ACCESSION);

    when(linkListExtractor.getLinkListAsStream(
        anyString(), anyString(), any(JsonNode.class), anyBoolean(), anyString()))
        .thenThrow(new FileNotFoundException("File not found"));

    // Act - should not throw, just log the error
    assertDoesNotThrow(
        () ->
            instance.indexExtractedLinkList(
                ACCESSION, ownerDocument, REL_PATH, submissionJson, indexWriter,true, SECRET_KEY));

    // Assert - verify textMiningLinkUpdater was NOT called
    verify(textMiningLinkUpdater, never())
        .indexExtractedLinks(anyString(), any(Document.class), any());
  }

  @Test
  public void testIndexExtractedLinkList_withPrivateSubmission() throws IOException {
    // Arrange
    JsonNode submissionJson = createTestSubmissionJson();
    Document ownerDocument = createTestDocument(ACCESSION);

    try (InputStream is = loadExtractedLinksStreamByFileName("singleNodeExtractedLinks.json")) {
      when(linkListExtractor.getLinkListAsStream(
          eq(ACCESSION), eq(REL_PATH), any(JsonNode.class), eq(false), eq(SECRET_KEY)))
          .thenReturn(is);

      // Act
      instance.indexExtractedLinkList(
          ACCESSION, ownerDocument, REL_PATH, submissionJson, indexWriter, false, SECRET_KEY);

      // Assert - verify correct parameters were passed to extractor
      verify(linkListExtractor, times(1))
          .getLinkListAsStream(ACCESSION, REL_PATH, submissionJson, false, SECRET_KEY);
    }
  }

  /**
   * Creates a test submission JSON structure.
   */
  private JsonNode createTestSubmissionJson() {
    String submission =
        "{"
            + "\"accno\":\"S-EPMC12345\","
            + "\"attributes\":["
            + "{\"name\":\"Title\",\"value\":\"Submission Title\"},"
            + "{\"name\":\"ReleaseDate\",\"value\":\"2108-02-08\"}"
            + "],"
            + "\"section\":{"
            + "\"accno\":\"S-EPMC12345\","
            + "\"type\":\"Study\","
            + "\"attributes\":["
            + "{\"name\":\"Title\",\"value\":\"Submission Title\"}"
            + "],"
            + "\"subsections\":[["
            + "{"
            + "\"accno\":\"ExtractedLinks_Section\","
            + "\"type\":\"ExtractedLinks\","
            + "\"attributes\":["
            + "{"
            + "\"name\":\"Link List\","
            + "\"value\":\"S-EPMC12345_extracted_links.json\""
            + "}"
            + "]"
            + "}"
            + "]]"
            + "},"
            + "\"type\":\"submission\""
            + "}";

    try {
      return new ObjectMapper().readTree(submission);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to create test JSON", e);
    }
  }

  /**
   * Creates a test Lucene document with the given accession.
   */
  private Document createTestDocument(String accession) {
    Document doc = new Document();
    doc.add(new StringField(Constants.Fields.ACCESSION, accession, Field.Store.YES));
    doc.add(new StringField(Constants.Fields.ID, accession, Field.Store.YES));
    return doc;
  }

  /**
   * Loads an InputStream to the extracted links file located under the classpath resource folder.
   *
   * @param fileName the name of the extracted links file (expected to be inside "LinkListExtractor"
   *     folder)
   * @return InputStream for the resource
   * @throws IllegalStateException if the file is not found
   */
  private InputStream loadExtractedLinksStreamByFileName(String fileName) {
    String resourcePath = "LinkListExtractor/" + fileName;
    InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
    if (is == null) {
      throw new IllegalStateException("Extracted links file resource not found: " + resourcePath);
    }
    return is;
  }
}
