package uk.ac.ebi.biostudies.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.lucene.index.IndexWriter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.biostudies.service.LinkListIndexService;

@RunWith(MockitoJUnitRunner.class)
public class LinkListIndexServiceImplTest {

  @Mock private LinkListExtractor linkListExtractor;
  @Mock private IndexWriter indexWriter;

  private LinkListIndexService instance;

  private static final String SECRET_KEY = "secret_key";

  @Before
  public void setUp() {
    instance = new LinkListIndexServiceImpl(linkListExtractor);
  }

  @Test
  public void testIndexExtractedLinkList() throws FileNotFoundException, JsonProcessingException {
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
            + "};";
    String accession = "S-BIAD2225";
    String relPath = "S-BIAD/225/S-BIAD2225";

    JsonNode submissionJson = new ObjectMapper().readTree(submission);

    try (InputStream is = loadExtractedLinksStreamByFileName("singleNodeExtractedLinks.json")) {
      assertNotNull(is, "Test resource not found");

      when(linkListExtractor.getLinkListAsStream(
              anyString(), anyString(), any(), anyBoolean(), anyString()))
          .thenReturn(is);

      instance.indexExtractedLinkList(accession, relPath, submissionJson, indexWriter, true, SECRET_KEY);
    } catch (IOException e) {
      fail("Unexpected IOException: " + e.getMessage());
    }
  }

  /**
   * Loads an InputStream to the extracted links file located under the classpath resource folder.
   *
   * @param fileName the name of the extracted links file (expected to be inside "LinkListExtractor"
   *     folder)
   * @return InputStream for the resource, or {@code null} if the file is not found
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
