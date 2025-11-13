package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

/**
 * Component responsible for extracting link lists from submission JSON metadata.
 *
 * <p>Primarily locates and retrieves the extracted links list filename and associated file
 * metadata, facilitating the download process through the {@link FileService}.
 */
@Component
public class LinkListExtractor {

  /**
   * JSONPath to locate the filename of the link list containing the extracted links. Searches
   * subsections for type "ExtractedLinks" and the attribute named "Link List".
   */
  private static final String EXTRACTED_LINKS_JSON_PATH =
      "$.section.subsections[*][*][?(@.type=='ExtractedLinks')].attributes[?(@.name=='Link List')].value";

  private static final Logger LOGGER = LogManager.getLogger(LinkListExtractor.class.getName());

  private final FileService fileService;

  /**
   * Constructs a LinkListExtractor with the specified {@link FileService}.
   *
   * @param fileService the file service used to retrieve files based on metadata
   */
  public LinkListExtractor(FileService fileService) {
    this.fileService = fileService;
  }

  /**
   * Retrieves an {@link InputStream} for the extracted links file associated with the given
   * submission.
   *
   * <p>Uses the submission metadata JSON to determine the filename of the extracted links list,
   * builds file metadata, sets storage mode and security key if applicable, and requests the file
   * through the {@link FileService}.
   *
   * @param accession the accession identifier of the submission
   * @param relativePath the relative path to the directory containing submission files
   * @param json the JSON metadata representing the submission
   * @param isPublicStudy flag indicating if the study is public
   * @param secretKey optional secret key used for private storage access
   * @return an InputStream for the extracted links file, or {@code null} if not implemented
   * @throws FileNotFoundException if the extracted links file cannot be found
   */
  public InputStream getLinkListAsStream(
      String accession, String relativePath, JsonNode json, boolean isPublicStudy, String secretKey)
      throws FileNotFoundException {
    String extractedLinksFileName = getExtractedLinksFileName(json);
    if (extractedLinksFileName == null || extractedLinksFileName.isEmpty()) {
      // Handle case where filename is missing
      LOGGER.warn("Extracted links filename not found in submission JSON: {}", accession);
      return null;
    }

    FileMetaData fileList = new FileMetaData(accession);
    fileList.setUiRequestedPath(extractedLinksFileName);
    fileList.setRelativePath(relativePath);
    fileList.setPublic(isPublicStudy);

    Constants.File.StorageMode storageMode = getStorageMode(json);
    if (storageMode == Constants.File.StorageMode.NFS && !isPublicStudy) {
      if (secretKey == null || secretKey.isEmpty()) {
        LOGGER.debug(
            "Invalid or missing secret key during parsing private submission file list for accession: {}",
            accession);
      } else {
        fileList.setSecKey(secretKey);
      }
    }
    fileList.setStorageMode(storageMode);

    LOGGER.debug("Retrieving file list - {}", fileList);
    // Note: Actual file retrieval logic should read InputStream and return it here.
    fileService.getDownloadFile(fileList);

    return fileList.getInputStream();
  }

  /**
   * Retrieves the filename for the extracted links list from the given JSON submission structure.
   *
   * <p>This method searches the JSON data for the "Link List" attribute within a subsection of type
   * "ExtractedLinks." If found, it returns the attribute value, ensuring the result ends with the
   * ".json" extension. If none is found, the method returns {@code null}.
   *
   * @param json the JSON node representing the submission metadata
   * @return the name of the extracted links list file, guaranteed to end with ".json"; or {@code
   *     null} if no matching file is found
   */
  private String getExtractedLinksFileName(JsonNode json) {
    ReadContext jsonPathContext = JsonPath.parse(json.toString());
    List<String> values = jsonPathContext.read(EXTRACTED_LINKS_JSON_PATH);
    if (values.isEmpty()) {
      return null;
    }
    String name = values.get(0);
    // Make sure file ends with '.json'
    return name.toLowerCase().endsWith(".json") ? name : name + ".json";
  }

  /**
   * Determines the storage mode used for the files of the submission from its JSON metadata.
   *
   * @param json the JSON node representing the submission metadata
   * @return the storage mode as specified in the submission JSON
   * @throws IllegalStateException if the storage mode attribute is missing in the JSON
   */
  private Constants.File.StorageMode getStorageMode(JsonNode json) {
    if (!json.hasNonNull(Constants.Fields.STORAGE_MODE)) {
      throw new IllegalStateException(
          String.format(
              "Submission does not have a Storage Mode property (%s).",
              Constants.Fields.STORAGE_MODE));
    }
    return Constants.File.StorageMode.valueOf(json.get(Constants.Fields.STORAGE_MODE).asText());
  }
}
