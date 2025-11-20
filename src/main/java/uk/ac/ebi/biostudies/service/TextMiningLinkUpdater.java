package uk.ac.ebi.biostudies.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.ExtractedLink;
import uk.ac.ebi.biostudies.config.IndexManager;

/**
 * Component responsible for indexing text mining extracted links into Lucene indices. Maintains
 * both a main document index and a separate link index organized by file.
 */
@Component
public class TextMiningLinkUpdater {
  private static final Logger LOGGER = LogManager.getLogger(TextMiningLinkUpdater.class.getName());

  private final IndexManager indexManager;

  public TextMiningLinkUpdater(IndexManager indexManager) {
    this.indexManager = indexManager;
  }

  /**
   * Indexes extracted links for a given accession by updating both the main document index and a
   * separate link index organized by file.
   *
   * <p>This method performs the following operations:
   *
   * <ol>
   *   <li>Searches for an existing document with the specified accession number
   *   <li>If found, updates the main index with the extracted links
   *   <li>Classifies links by their source file
   *   <li>Adds each file's links to a separate link index
   * </ol>
   *
   * @param accession the accession number of the document to associate with the links; must not be
   *     null
   * @param ownerDocument the document that was just indexed (may not be committed yet)
   * @param links the list of extracted links to index; must not be null or empty
   * @throws IllegalArgumentException if accession or links is null or empty
   * @throws IllegalStateException if there's an error accessing the index
   */
  public void indexExtractedLinks(
      String accession, Document ownerDocument, List<ExtractedLink> links) {
    if (accession == null || accession.trim().isEmpty()) {
      throw new IllegalArgumentException("Accession cannot be null or empty");
    }
    if (ownerDocument == null) {
      throw new IllegalArgumentException("Owner document cannot be null");
    }
    if (links == null || links.isEmpty()) {
      LOGGER.debug("No links to index for accession: {}", accession);
      return; // Not an error - just no links to process
    }

    String normalizedAccession = accession.toLowerCase();

    try {
      addToMainIndex(ownerDocument, links);
      indexLinksByFile(normalizedAccession, links);
      LOGGER.debug("Accession: {} - {} links indexed successfully", accession, links.size());

    } catch (IOException e) {
      LOGGER.error("I/O error while indexing links for accession: {}", accession, e);
      throw new IllegalStateException("Failed to access index for accession: " + accession, e);
    }
  }

  /**
   * Indexes links organized by their source files.
   *
   * @param normalizedAccession the normalized (lowercase) accession number
   * @param links the list of extracted links
   * @throws IOException if there's an error writing to the index
   */
  private void indexLinksByFile(String normalizedAccession, List<ExtractedLink> links)
      throws IOException {
    Map<String, List<ExtractedLink>> linksPerFileMap = classifyLinksPerFile(links);

    for (Map.Entry<String, List<ExtractedLink>> entry : linksPerFileMap.entrySet()) {
      addToLinkIndex(normalizedAccession, entry.getKey(), entry.getValue());
    }
  }

  /**
   * Classifies extracted links by their source file name.
   *
   * @param links the list of extracted links
   * @return a map where keys are file names and values are lists of links from that file
   */
  private Map<String, List<ExtractedLink>> classifyLinksPerFile(List<ExtractedLink> links) {
    Map<String, List<ExtractedLink>> linksPerFile = new HashMap<>();
    for (ExtractedLink link : links) {
      String fileName = link.getFileName();
      if (fileName == null) {
        fileName = "unknown";
      }
      linksPerFile.computeIfAbsent(fileName, k -> new ArrayList<>()).add(link);
    }
    return linksPerFile;
  }

  /**
   * Updates the main index by adding extracted link content to the document.
   * The document is already fully indexed from the pipeline, so we only add link content.
   *
   * @param document the owner document to update (already indexed)
   * @param allLinks the list of extracted links to add
   * @throws IOException if there's an error writing to the index
   */
  public void addToMainIndex(Document document, List<ExtractedLink> allLinks) throws IOException {
    // Only add extracted link content to the document

    for (ExtractedLink extractedLink : allLinks) {
      String value = extractedLink.getValue();
      String type = extractedLink.getType();

      if (value != null && !value.trim().isEmpty()) {
        document.add(new TextField(Constants.Fields.CONTENT, value.toLowerCase(), Field.Store.YES));
      }
      if (type != null && !type.trim().isEmpty()) {
        document.add(new TextField(Constants.Fields.CONTENT, type, Field.Store.YES));
      }
    }

    // Update the document in the index
    String accessionValue = document.get(Constants.Fields.ACCESSION);
    if (accessionValue == null) {
      throw new IllegalStateException("Document missing required accession field");
    }

    indexManager
        .getSearchIndexWriter()
        .updateDocument(
            new Term(Constants.Fields.ACCESSION, accessionValue.toLowerCase()),
            document);
  }

  /**
   * Adds extracted links to the link index for a given accession and file. Creates a unique digest
   * key from the accession and filename combination, removes any existing links with that key, and
   * indexes the new links.
   *
   * @param accession the accession number (already normalized/lowercase)
   * @param fileName the name of the file containing the links
   * @param allLinks the list of extracted links to index
   * @throws IOException if there's an error writing to the index
   */
  public void addToLinkIndex(String accession, String fileName, List<ExtractedLink> allLinks)
      throws IOException {

    if (allLinks == null || allLinks.isEmpty()) {
      LOGGER.debug("No links to index for accession: {}, file: {}", accession, fileName);
      return;
    }

    String digestKey = generateDigestKey(accession, fileName);

    // Remove existing links for this key
    TermQuery digestTermQuery = new TermQuery(new Term(Constants.Link.KEY, digestKey));
    long deletedCount = indexManager.getExtractedLinkIndexWriter().deleteDocuments(digestTermQuery);
    LOGGER.debug("Deleted {} existing links for key: {}", deletedCount, digestKey);

    // Index new links
    int counter = 0;
    for (ExtractedLink extractedLink : allLinks) {
      counter++;
      Document document =
          createLinkDocument(accession, fileName, digestKey, counter, extractedLink);
      indexManager.getExtractedLinkIndexWriter().addDocument(document);
    }

    LOGGER.debug("Indexed {} links for accession: {}, file: {}", counter, accession, fileName);
  }

  /**
   * Generates a hex-encoded MD5 digest key from accession and filename. Creates a new MessageDigest
   * instance for thread safety.
   *
   * <p>Note: MD5 is used for generating unique composite keys, not for security purposes. It
   * provides adequate collision resistance for this use case.
   *
   * @param accession the accession number
   * @param fileName the file name
   * @return hex-encoded MD5 digest string (32 hexadecimal characters)
   */
  private String generateDigestKey(String accession, String fileName) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digestBytes = md.digest((accession + fileName).getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder(digestBytes.length * 2);
      for (byte b : digestBytes) {
        hexString.append(String.format("%02x", b));
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 algorithm not available", e);
    }
  }

  /**
   * Creates a Lucene document for an extracted link.
   *
   * @param accession the accession number
   * @param fileName the file name
   * @param digestKey the unique digest key
   * @param counter the link counter for unique ID generation
   * @param extractedLink the extracted link data
   * @return populated Lucene document
   */
  private Document createLinkDocument(
      String accession,
      String fileName,
      String digestKey,
      int counter,
      ExtractedLink extractedLink) {
    Document document = new Document();

    document.add(new StringField(Constants.Fields.ID, accession + "_" + counter, Field.Store.YES));
    document.add(new StringField(Constants.Link.KEY, digestKey, Field.Store.YES));
    document.add(new StringField(Constants.File.OWNER, accession, Field.Store.YES));

    addSearchableField(document, Constants.File.FILENAME, fileName);
    addSearchableField(document, Constants.Link.TYPE, extractedLink.getType());
    addSearchableField(document, Constants.Link.VALUE, extractedLink.getValue());

    String link = extractedLink.getLink();
    if (link != null) {
      document.add(new StringField(Constants.Link.URL, link, Field.Store.YES));
    }

    return document;
  }

  /**
   * Adds a field with case-sensitive and case-insensitive indexing plus storage.
   *
   * @param document the document to add fields to
   * @param fieldName the field name constant
   * @param value the field value (may be null)
   */
  private void addSearchableField(Document document, String fieldName, String value) {
    if (value == null) {
      value = "";
    }

    document.add(new StringField(fieldName, value, Field.Store.NO));
    document.add(new StringField(fieldName, value.toLowerCase(), Field.Store.NO));
    document.add(new StoredField(fieldName, value));
    document.add(new SortedDocValuesField(fieldName, new BytesRef(value)));
  }
}
