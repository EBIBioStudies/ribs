package uk.ac.ebi.biostudies.service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ebi.biostudies.api.util.Constants;

/**
 * Lightweight data class holding submission metadata needed for study info endpoints.
 *
 * <p>Maps directly from Indexer service `IndexedSubmission` fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Study {

  private String accNo; // accession
  private String relPath; // relativePath
  private String storageMode; // NFS/FIRE/S3
  private String access; // "public private"
  private long files;
  private long links;
  private long views;
  private List<String> sectionsWithFiles; // ["Section1", "Section2"]
  private List<String> fileAttributeNames; // ["Name", "Size", "Section", "allele_accession_id"]
  private boolean hasFileParsingError;
  private long releaseTime;
  private long modificationTime;
  private String secretKey;

  /** Empty study info (for 404 responses). */
  public static Study empty() {
    return new Study();
  }

  /** True if this represents a non-existent submission. */
  public boolean isEmpty() {
    return accNo == null;
  }

  /** True if the study has public access. */
  public boolean isPublic() {
    return access != null && access.toLowerCase().contains(Constants.PUBLIC);
  }
}
