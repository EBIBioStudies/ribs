package uk.ac.ebi.biostudies.service;

import uk.ac.ebi.biostudies.exceptions.IndexerServiceUnavailableException;
import uk.ac.ebi.biostudies.exceptions.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.service.client.Study;

/**
 * Client interface for the Indexer service, providing access to indexed submission metadata.
 *
 * <p>This interface abstracts HTTP calls to the Indexer service, mapping responses to the {@link
 * Study} data model. Implementations must handle service failures and map HTTP errors to
 * appropriate business exceptions.
 *
 * <p><strong>Deployment requirement:</strong> The Indexer service must be available at {@code
 * ${indexer.service.url}}. RIBS cannot function without it post-migration.
 *
 * <p><strong>Error handling:</strong>
 *
 * <ul>
 *   <li><b>404 Not Found</b> → returns {@link Study#isEmpty() == true}
 *   <li><b>403 Forbidden</b> → throws {@link SubmissionNotAccessibleException}
 *   <li><b>500/Timeouts</b> → throws {@link IndexerServiceUnavailableException}
 * </ul>
 */
public interface IndexClient {

  /**
   * Retrieves basic metadata for a submission by accession.
   *
   * <p>Used by legacy endpoints like {@code /api/v1/studies/{accession}/info} to obtain file
   * counts, paths, access status, and section information.
   *
   * @param accession the submission accession (e.g. {@code S-BSST1432})
   * @param secretKey optional secret key for accessing unreleased/private submissions
   * @return study metadata, or empty {@link Study} if not found
   * @throws SubmissionNotAccessibleException if submission exists but caller lacks permission
   * @throws IndexerServiceUnavailableException if Indexer service is down/unreachable
   */
  Study getStudyInfo(String accession, String secretKey)
      throws SubmissionNotAccessibleException, IndexerServiceUnavailableException;

  /**
   * Retrieves basic metadata for a submission by accession and type.
   *
   * <p>Validates that the submission matches the expected {@code type} (case-insensitive, trimmed).
   * Used when the caller knows the submission type (e.g. "Study") and wants to enforce it.
   *
   * @param accession the submission accession (e.g. {@code S-BSST1432})
   * @param secretKey optional secret key for accessing unreleased/private submissions
   * @param type expected submission type (e.g. "Study", "Project")
   * @return study metadata matching the type, or empty {@link Study} if not found/mismatched
   * @throws SubmissionNotAccessibleException if submission exists but caller lacks permission
   * @throws IndexerServiceUnavailableException if Indexer service is down/unreachable
   */
  Study getStudyInfo(String accession, String secretKey, String type)
      throws SubmissionNotAccessibleException, IndexerServiceUnavailableException;
}
