package uk.ac.ebi.biostudies.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * DTO for Indexer service submission response. Maps only fields needed by legacy getStudyInfo().
 *
 * <p>Compatible with Spring Boot 2.6 / Jackson 2.9.9 / JDK 8.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndexedSubmissionResponse {

  @JsonProperty("accession")
  private String accession;

  @JsonProperty("relPath")
  private String relPath;

  @JsonProperty("storageMode")
  private String storageMode;

  @JsonProperty("access")
  private String access;

  @JsonProperty("views")
  private Long views;

  @JsonProperty("sectionsWithFiles")
  private List<String> sectionsWithFiles;

  @JsonProperty("fileAttributesNames")
  private List<String> fileAttributesNames;

  private boolean hasFileParsingError;

  private long releaseTime;

  private long modificationTime;

  private String secretKey;

  private long numberOfFiles;

  private long numberOfLinks;
}
