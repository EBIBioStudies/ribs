package uk.ac.ebi.biostudies.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LinkListExtractorTest {
  private FileService fileService;
  @Autowired private LinkListExtractor linkListExtractor;

  @Test
  public void testIndexExtractedLinkList() throws JsonProcessingException, FileNotFoundException {
    String submission =
        "{"
            + "\"accno\":\"S-BSST2687\","
            + "\"attributes\":["
            + "{\"name\":\"Title\",\"value\":\"Submission Title\"},"
            + "{\"name\":\"ReleaseDate\",\"value\":\"2108-02-08\"}"
            + "],"
            + "\"storageMode\": \"NFS\","
            + "\"section\":{"
            + "\"accno\":\"S-BSST2687\","
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
            + "\"value\":\"extracted_links_small.json\""
            + "}"
            + "]"
            + "}"
            + "]]"
            + "},"
            + "\"type\":\"submission\""
            + "};";
    String accession = "S-BSST2690";
    String relPath = "S-BSST/690/S-BSST2690";
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(submission);
    boolean isPublicStudy = true;
    String secretKey = "x";
    InputStream inputStream =
        linkListExtractor.getLinkListAsStream(
            accession, relPath, jsonNode, isPublicStudy, secretKey);
    assertNotNull(inputStream);
  }
}
