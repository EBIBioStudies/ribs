package uk.ac.ebi.biostudies.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.biostudies.exceptions.SubmissionNotAccessibleException;

/**
 * HTTP client for indexer service file search endpoints.
 *
 * <p>Handles DataTables parameter conversion and indexer service calls from RIBS.
 */
@Slf4j
@Component
public class FileSearcherHttpClient {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  private final String baseUrl;

  public FileSearcherHttpClient(RestTemplate restTemplate, ObjectMapper objectMapper, @Value("${indexer.service.url:http://localhost:8090}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
  }

  /** Calls indexer file search endpoint with DataTables parameters. */
  public ObjectNode searchFiles(
      String accession,
      Integer start,
      Integer pageSize,
      String search,
      Integer draw,
      boolean metadata,
      String columnsJson,
      String secretKey) { // ← Add secretKey passthrough

    String url = baseUrl + "api/v1/files/search";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("accession", accession);
    params.add("start", start.toString());
    params.add("pageSize", pageSize.toString());
    params.add("search", search != null ? search : "");
    if (draw != null) params.add("draw", draw.toString());
    params.add("metadata", Boolean.toString(metadata));
    params.add("columns", columnsJson != null ? columnsJson : "");
    if (secretKey != null) params.add("secretKey", secretKey); // ← Passthrough

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    try {
      log.debug("Calling indexer: {}?accession={}", url, accession);
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, request, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        return objectMapper.readValue(response.getBody(), ObjectNode.class);
      } else if (response.getStatusCode().value() == 403) {
        throw new SubmissionNotAccessibleException("Study not accessible");
      } else {
        log.warn("Indexer returned {}: {}", response.getStatusCode(), response.getBody());
        return objectMapper.createObjectNode(); // Empty response on error
      }

    } catch (Exception e) {
      log.error("Indexer call failed for {}: {}", accession, e.getMessage());
      return objectMapper.createObjectNode(); // fallback
    }
  }
}
