package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.ExtractedLink;
import uk.ac.ebi.biostudies.service.LinkListIndexService;
import uk.ac.ebi.biostudies.service.TextMiningLinkUpdater;

/**
 * Implementation of {@link LinkListIndexService} responsible for indexing link lists associated
 * with submissions.
 *
 * <p>This class handles locating and processing both general link lists and the special "Extracted
 * Links" list attached to submissions. It reads extracted links data from JSON files, parses each
 * link entry, and facilitates indexing through a provided {@link IndexWriter}.
 *
 * <p>The extracted links are parsed efficiently using Jackson’s streaming API to handle large files
 * without excessive memory consumption. Detailed logging and exception handling ensure robust
 * processing and error reporting.
 *
 * <p>Usage typically involves invoking {@link #indexSubmissionLinkLists(String, String, JsonNode,
 * IndexWriter)} to index all link lists, with special handling in {@link
 * #indexExtractedLinkList(String, String, JsonNode, IndexWriter, boolean, String)} for the
 * extracted links list.
 *
 * @see LinkListIndexService
 * @see ExtractedLink
 * @see LinkListExtractor
 */
@Component
public class LinkListIndexServiceImpl implements LinkListIndexService {

  private static final Logger LOGGER =
      LogManager.getLogger(LinkListIndexServiceImpl.class.getName());

  private final LinkListExtractor linkListExtractor;
  private final TextMiningLinkUpdater textMiningLinkUpdater;

  public LinkListIndexServiceImpl(
      LinkListExtractor linkListExtractor, TextMiningLinkUpdater textMiningLinkUpdater) {
    this.linkListExtractor = linkListExtractor;
    this.textMiningLinkUpdater = textMiningLinkUpdater;
  }

  /**
   * Locates and indexes all link lists attached to the given submission, except for the "Extracted
   * Links" list, which is treated separately.
   *
   * @param accession the accession identifier of the submission
   * @param relativePath the relative path where link lists are located
   * @param json the submission metadata as a JSON node
   * @param writer the index writer to perform the indexing
   */
  @Override
  public void indexSubmissionLinkLists(
      String accession, String relativePath, JsonNode json, IndexWriter writer) {
    // Implementation pending
  }

  /**
   * Indexes the "Extracted Links" list associated with the specified submission.
   *
   * <p>This method reads the extracted links file linked to the submission, parses its content, and
   * processes each extracted link for indexing.
   *
   * @param accession the accession identifier of the submission
   * @param ownerDocument the document that was just indexed (may not be committed yet)
   * @param relativePath the path relative to the submission where the extracted links file is
   *     located
   * @param json submission metadata as a JSON node
   * @param writer the index writer used for indexing operations
   * @param isPublicStudy true if the study is public, impacting access control
   * @param secretKey an optional secret key for private study access; may be null
   */
  @Override
  public void indexExtractedLinkList(
      String accession,
      Document ownerDocument,
      String relativePath,
      JsonNode json,
      IndexWriter writer,
      boolean isPublicStudy,
      String secretKey) {
    LOGGER.info("Indexing extracted link lists for accession {}", accession);

    if (accession == null || accession.trim().isEmpty()) {
      throw new IllegalArgumentException("Accession cannot be null or empty");
    }
    if (ownerDocument == null) {
      throw new IllegalArgumentException("Owner document cannot be null");
    }

    try {
      List<ExtractedLink> extractedLinks =
          readExtractedLinks(accession, relativePath, json, isPublicStudy, secretKey);

      if (!extractedLinks.isEmpty()) {
        textMiningLinkUpdater.indexExtractedLinks(accession, ownerDocument, extractedLinks);
      }

    } catch (FileNotFoundException fnfe) {
      LOGGER.error("Unable to find extracted link list file for accession {}", accession, fnfe);
    } catch (Exception e) {
      LOGGER.error("Exception while processing extracted links for accession {}", accession, e);
    }
  }

  /**
   * Reads extracted links from the input stream provided by the {@link LinkListExtractor}.
   *
   * @param accession the accession identifier of the submission
   * @param relativePath relative path to the extracted links file
   * @param json submission metadata JSON
   * @param isPublicStudy indicates if the study is public
   * @param secretKey secret key for private access, may be null
   * @return list of extracted links parsed from the extracted links file
   * @throws IOException if an error occurs while reading or parsing the input stream
   */
  private List<ExtractedLink> readExtractedLinks(
      String accession, String relativePath, JsonNode json, boolean isPublicStudy, String secretKey)
      throws IOException {
    List<ExtractedLink> allLinks = new ArrayList<>();

    InputStream input =
        linkListExtractor.getLinkListAsStream(
            accession, relativePath, json, isPublicStudy, secretKey);

    if (input == null) {
      // No extracted links file for the accession
      return allLinks;
    }

    populateLinks(input, allLinks);
    return allLinks;
  }

  /**
   * Parses extracted links from the given input stream and populates the provided list.
   *
   * @param extractedLinksInputStream InputStream of the extracted links JSON array
   * @param allLinks list to populate with parsed extracted links
   * @throws IOException if an error occurs during JSON parsing
   */
  private void populateLinks(InputStream extractedLinksInputStream, List<ExtractedLink> allLinks)
      throws IOException {
    try (InputStreamReader inputStreamReader =
        new InputStreamReader(extractedLinksInputStream, StandardCharsets.UTF_8)) {
      JsonFactory factory = new JsonFactory();
      JsonParser parser = factory.createParser(inputStreamReader);

      JsonToken token = parser.nextToken();
      // Move forward until start of array
      while (token != null && !JsonToken.START_ARRAY.equals(token)) {
        token = parser.nextToken();
      }

      ObjectMapper mapper = new ObjectMapper();
      while (true) {
        token = parser.nextToken();
        if (!JsonToken.START_OBJECT.equals(token)) {
          break;
        }
        JsonNode node = mapper.readTree(parser);
        ExtractedLink extractedLink = jsonNodeToExtractedLink(node);
        allLinks.add(extractedLink);
      }
    }
  }

  /**
   * Converts a JSON node representation of an extracted link into an {@link ExtractedLink} object.
   *
   * @param jsonNode JSON node containing attributes of a single extracted link
   * @return ExtractedLink object populated with type, value, and filename/link
   * @throws IllegalArgumentException if required attributes are missing or {@code jsonNode} is null
   */
  private ExtractedLink jsonNodeToExtractedLink(JsonNode jsonNode) {
    if (jsonNode == null) {
      throw new IllegalArgumentException("jsonNode is null");
    }

    JsonNode attributes = jsonNode.get("attributes");

    String typeValue = null;
    String valueValue = null;
    String filenameValue = null;
    String linkValue = null;

    if (attributes != null && attributes.isArray()) {
      for (JsonNode attr : attributes) {
        String name = attr.get("name").asText();
        String value = attr.get("value").asText();

        switch (name) {
          case "type":
            typeValue = value;
            break;
          case "value":
            valueValue = value;
            break;
          case "filename":
            filenameValue = value;
            break;
        }
      }
    }

    linkValue = jsonNode.path("url").asText();

    if (linkValue == null) {
      throw new IllegalArgumentException("linkValue is null");
    }

    if (typeValue == null) {
      throw new IllegalArgumentException("typeValue is null");
    }
    if (valueValue == null) {
      throw new IllegalArgumentException("valueValue is null");
    }
    if (filenameValue == null) {
      throw new IllegalArgumentException("filenameValue is null");
    }

    ExtractedLink extractedLink = new ExtractedLink();
    extractedLink.setType(typeValue);
    extractedLink.setValue(valueValue);
    extractedLink.setLink(linkValue);

    extractedLink.setFileName(filenameValue);

    return extractedLink;
  }
}
