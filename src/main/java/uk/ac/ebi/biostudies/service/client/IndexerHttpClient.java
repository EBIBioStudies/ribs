package uk.ac.ebi.biostudies.service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biostudies.exceptions.IndexerServiceUnavailableException;
import uk.ac.ebi.biostudies.exceptions.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.service.IndexClient;

/**
 * HTTP client implementation for the Indexer service using RestTemplate.
 *
 * <p>Maps HTTP responses to {@link IndexClient} behavior:
 *
 * <ul>
 *   <li>200 OK → {@link Study} instance
 *   <li>404 Not Found → empty {@link Study}
 *   <li>403 Forbidden → {@link SubmissionNotAccessibleException}
 *   <li>5xx/Timeouts → {@link IndexerServiceUnavailableException}
 * </ul>
 */
@Slf4j
@Component
public class IndexerHttpClient implements IndexClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  public IndexerHttpClient(
      RestTemplate restTemplate,
      @Value("${indexer.service.url:http://localhost:8090}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
  }

  /** {@inheritDoc} */
  @Override
  public Study getStudyInfo(String accession, String secretKey)
      throws SubmissionNotAccessibleException, IndexerServiceUnavailableException {
    return getStudyInfoInternal(accession, secretKey, null);
  }

  /** {@inheritDoc} */
  @Override
  public Study getStudyInfo(String accession, String secretKey, String type)
      throws SubmissionNotAccessibleException, IndexerServiceUnavailableException {
    return getStudyInfoInternal(accession, secretKey, type);
  }

  /** Internal implementation shared by both public methods. */
  private Study getStudyInfoInternal(String accession, String secretKey, String type) {
    try {
      StringBuilder urlBuilder =
          new StringBuilder(baseUrl).append("api/submissions/").append(accession);

      if (secretKey != null) {
        urlBuilder.append("?secretKey=").append(secretKey);
      }
      if (type != null) {
        urlBuilder.append(type.isEmpty() ? "" : "?type=").append(type.trim().toLowerCase());
      }

      String url = urlBuilder.toString();
      IndexedSubmissionResponse response =
          restTemplate.getForObject(url, IndexedSubmissionResponse.class);

      if (response == null) {
        log.debug("Empty response for accession {}", accession);
        return new Study(); // Empty study
      }

      return toStudyInfo(response);

    } catch (HttpClientErrorException.NotFound e) {
      log.debug("Study {} not found", accession);
      return new Study(); // 404 → empty Study

    } catch (HttpClientErrorException.Forbidden e) {
      log.debug("Access denied for study {}", accession);
      throw new SubmissionNotAccessibleException("Study " + accession + " is not accessible");

    } catch (RestClientException e) {
      log.error("Indexer service error for {}: {}", accession, e.getMessage());
      throw new IndexerServiceUnavailableException(
          "Indexer service unavailable for " + accession, e);

    } catch (Exception e) {
      log.error("Unexpected error fetching study {}", accession, e);
      throw new IndexerServiceUnavailableException("Failed to fetch study " + accession, e);
    }
  }

  /**
   * Maps Indexer DTO to RIBS Study model.
   *
   * <p>Populates all fields required by {@code getStudyInfo()} legacy logic.
   */
  private Study toStudyInfo(IndexedSubmissionResponse response) {
    Study study = new Study();
    study.setAccNo(response.getAccession());
    study.setAccess(response.getAccess());
    study.setRelPath(response.getRelPath());
    study.setStorageMode(response.getStorageMode());
    study.setSectionsWithFiles(response.getSectionsWithFiles());
    study.setFileAttributeNames(response.getFileAttributesNames());
    study.setHasFileParsingError(response.isHasFileParsingError());
    study.setReleaseTime(response.getReleaseTime());
    study.setModificationTime(response.getModificationTime());
    study.setSecretKey(response.getSecretKey());
    study.setFiles(response.getNumberOfFiles());
    study.setLinks(response.getNumberOfLinks());
    return study;
  }
}
