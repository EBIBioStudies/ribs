package uk.ac.ebi.biostudies.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.lucene.index.IndexWriter;

/**
 * Defines the contract for indexing link lists.
 *
 * <p>A link list is an externalized table of links that can be attached to a section through the
 * special Link List attribute. The value of this attribute is the name of the file containing the
 * links. Implementations of this interface should provide logic to index the links represented in
 * this format.
 */
public interface LinkListIndexService {
  /**
   * Locates and indexes all link lists attached to the given submission, except for the "Extracted
   * Links" list, which is treated separately.
   *
   * @param accession the accession identifier of the submission
   * @param relativePath the path (relative to the submission) where link list files are located
   * @param json the JSON metadata for the submission
   * @param writer the index writer used to perform indexing
   */
  void indexSubmissionLinkLists(
      String accession, String relativePath, JsonNode json, IndexWriter writer);

  /**
   * Indexes the "Extracted Links" list associated with the specified submission.
   *
   * <p>This method locates the file containing extracted links, parses it, and updates the search
   * index accordingly.
   *
   * @param accession the accession identifier for the submission
   * @param relativePath the path (relative to the submission) to the extracted links file
   * @param json the metadata of the submission as a JSON node
   * @param writer the index writer used for indexing operations
   * @param isPublicStudy flag indicating whether the study is public; affects access control
   * @param secretKey optional secret key used for accessing private study files; can be null or empty if not applicable
   */
  void indexExtractedLinkList(
      String accession,
      String relativePath,
      JsonNode json,
      IndexWriter writer,
      boolean isPublicStudy,
      String secretKey);
}
