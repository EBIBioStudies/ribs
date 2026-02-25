package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.DataTableColumnInfo;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.auth.Session;
import uk.ac.ebi.biostudies.auth.User;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.exceptions.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.file.Thumbnails;
import uk.ac.ebi.biostudies.service.FilePaginationService;
import uk.ac.ebi.biostudies.service.client.FileSearcherHttpClient;
import uk.ac.ebi.biostudies.service.client.IndexerHttpClient;
import uk.ac.ebi.biostudies.service.client.Study;

@Slf4j
@Service
public class FilePaginationServiceImpl implements FilePaginationService {

  private final IndexConfig indexConfig;
  private final Thumbnails thumbnails;
  private final IndexerHttpClient indexerHttpClient;
  private final FileSearcherHttpClient fileSearcherHttpClient;

  public FilePaginationServiceImpl(
      IndexConfig indexConfig,
      Thumbnails thumbnails,
      IndexerHttpClient indexerHttpClient,
      FileSearcherHttpClient fileSearcherHttpClient) {
    this.indexConfig = indexConfig;
    this.thumbnails = thumbnails;
    this.indexerHttpClient = indexerHttpClient;
    this.fileSearcherHttpClient = fileSearcherHttpClient;
  }

  /**
   * Builds the JSON payload returned by the legacy study-info endpoint.
   *
   * <p>This method retrieves basic study metadata from the Indexer service, then enriches it with
   * file column definitions, access flags, download links and section information, preserving the
   * original response shape expected by the UI.
   *
   * @param accession the requested study accession
   * @param secretKey optional secret key used to access private studies
   * @return a JSON object containing study info; empty object if the study does not exist
   * @throws SubmissionNotAccessibleException if the study exists but is not accessible
   */
  public ObjectNode getStudyInfo(String accession, String secretKey)
      throws SubmissionNotAccessibleException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode studyInfo = mapper.createObjectNode();

    // Default ordered columns: always show Name and Size first
    String[] orderedArray = {"Name", "Size"};
    ArrayNode fileColumnAttributes = mapper.createArrayNode();

    // Retrieve basic study metadata from the Indexer service
    Study indexedStudy = indexerHttpClient.getStudyInfo(accession, secretKey);
    if (indexedStudy.isEmpty()) {
      // Study not found in index: return empty JSON, as legacy behavior
      return studyInfo;
    }

    accession = indexedStudy.getAccNo();
    String relativePath = indexedStudy.getRelPath();
    String storageModeString = indexedStudy.getStorageMode();
    Constants.File.StorageMode storageMode =
        Constants.File.StorageMode.valueOf(
            StringUtils.isEmpty(storageModeString) ? "NFS" : storageModeString);

    // For private studies stored on NFS, adjust the relative path to include the secret key
    if (storageMode == Constants.File.StorageMode.NFS && !indexedStudy.isPublic()) {
      relativePath = StudyUtils.modifyRelativePathForPrivateStudies(secretKey, relativePath);
    }

    // Build file column headers from file attribute names, preserving original order
    List<String> allAtts = indexedStudy.getFileAttributeNames();
    if (allAtts == null || allAtts.isEmpty()) {
      return studyInfo;
    }

    Set<String> headerSet = new HashSet<>(Arrays.asList(orderedArray));
    List<String> orderedList = new ArrayList<>(Arrays.asList(orderedArray));
    for (String att : allAtts) {
      if (att.isEmpty() || headerSet.contains(att)) {
        continue;
      }
      headerSet.add(att);
      orderedList.add(att);
    }

    int counter = 0;
    for (String att : orderedList) {
      ObjectNode node = mapper.createObjectNode();
      node.put("name", att);
      node.put("title", att);
      node.put("visible", true);
      node.put("searchable", !att.equalsIgnoreCase("size"));
      node.put("data", att.replaceAll("[\\[\\]\\(\\)\\s]", "_").replaceAll("\\.", "\\\\."));
      node.put("defaultContent", "");
      fileColumnAttributes.add(node);

      // Inject thumbnail column after the second column if thumbnails are available
      if (counter++ == 1 && thumbnails.hasThumbnailsFolder(accession, relativePath, storageMode)) {
        fileColumnAttributes.add(getThumbnailHeader(mapper));
      }
    }

    List<String> sectionsWithFiles = indexedStudy.getSectionsWithFiles();

    // Core study info fields used by the UI
    studyInfo.set("columns", fileColumnAttributes);
    studyInfo.put(Constants.Fields.FILES, indexedStudy.getFiles());
    studyInfo.put("httpLink", indexConfig.getFtpOverHttpUrl(storageMode) + "/" + relativePath);
    studyInfo.put("ftpLink", indexConfig.getFtpDir(storageMode) + "/" + relativePath);
    studyInfo.put("globusLink", indexConfig.getGlobusUrl(storageMode) + "/" + relativePath);
    studyInfo.put("isPublic", indexedStudy.isPublic());
    studyInfo.put(Constants.Fields.RELATIVE_PATH, relativePath);
    studyInfo.put("hasZippedFolders", storageMode == Constants.File.StorageMode.FIRE);
    studyInfo.put("views", indexedStudy.getViews());
    if (indexedStudy.isHasFileParsingError()) {
      studyInfo.put("hasFileParsingError", "true");
    }

    // Release and modification timestamps (epoch millis or preformatted values from index)
    studyInfo.put("released", indexedStudy.getReleaseTime());
    studyInfo.put("modified", indexedStudy.getModificationTime());

    // Add fields only visible to owners / private submissions
    setPrivateData(studyInfo, indexedStudy, secretKey);

    try {
      if (sectionsWithFiles != null) {
        ArrayNode arrayNode = mapper.createArrayNode();
        sectionsWithFiles.forEach(arrayNode::add);
        studyInfo.set("sections", arrayNode);
      }
    } catch (Exception e) {
      log.error("Error retrieving sections with files", e);
      studyInfo.put("sections", "[]");
    }

    return studyInfo;
  }

  private ObjectNode getThumbnailHeader(ObjectMapper mapper) {
    String thumbStr = "Thumbnail";
    ObjectNode node = mapper.createObjectNode();
    node.put("name", thumbStr);
    node.put("title", thumbStr);
    node.put("visible", true);
    node.put("searchable", false);
    node.put("sortable", false);
    node.put("defaultContent", "");
    return node;
  }

  @Override
  public ObjectNode getFileList(
      String accession,
      int start,
      int pageSize,
      String search,
      int draw,
      boolean metadata,
      Map<Integer, DataTableColumnInfo> dataTableUiResult,
      String secretKey)
      throws SubmissionNotAccessibleException {

    // Convert DataTable params → indexer JSON (your new helper)
    String columnsJson;
    try {
      columnsJson = DataTableColumnInfo.toIndexerJson(dataTableUiResult);
    } catch (Exception e) {
      log.warn("Failed to convert DataTable columns to JSON: {}", e.getMessage());
      return createEmptyResponse(draw);
    }

    return fileSearcherHttpClient.searchFiles(
        accession, start, pageSize, search, draw, metadata, columnsJson, secretKey);
  }

  /** Creates empty DataTables response (RIBS fallback behavior). */
  private ObjectNode createEmptyResponse(int draw) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode empty = mapper.createObjectNode();
    empty.put(Constants.File.DRAW, draw);
    empty.put(Constants.File.RECORDTOTAL, 0L);
    empty.put(Constants.File.RECORDFILTERED, 0L);
    empty.set(Constants.File.DATA, mapper.createArrayNode());
    return empty;
  }

  /**
   * Adds the secret key to the study information JSON if the requester has permission to access
   * private study data.
   *
   * <p>This method checks access permissions using {@link #canShareStudy(ObjectNode, Study,
   * String)} and, if access is granted, adds the study's secret key to the {@code studyInfo}
   * object. The secret key enables privileged operations such as viewing unreleased data or
   * modifying the study.
   *
   * <p>The secret key is only added if it exists in the document. If access is denied or the secret
   * key field is not present, the {@code studyInfo} object remains unchanged.
   *
   * @param studyInfo the JSON object representing study metadata that will be enriched with the
   *     secret key if access is granted
   * @param indexedSubmission the indexed submission containing the indexed study data, including
   *     the secret key field
   * @param secretKey the secret key provided by the requester for validation (may be null for
   *     authentication-based access)
   */
  private void setPrivateData(ObjectNode studyInfo, Study indexedSubmission, String secretKey) {
    if (canShareStudy(studyInfo, indexedSubmission, secretKey)) {
      if (secretKey != null) {
        studyInfo.put(Constants.Fields.SECRET_KEY, indexedSubmission.getSecretKey());
      }
    }
  }

  /**
   * Determines if the current user or requester can access and share private study information.
   *
   * <p>Access is granted under the following conditions:
   *
   * <ul>
   *   <li>The provided secret key matches the document's secret key (grants access to anyone)
   *   <li>The current user is authenticated AND the study is unreleased (no release date set,
   *       release date is null, or release date is in the future)
   * </ul>
   *
   * @param studyInfo the JSON object containing study metadata, particularly the "released" field
   * @param indexedSubmission the indexed submission containing the indexed study data with the
   *     secret key
   * @param secretKey the secret key provided by the requester (may be null for public access
   *     attempts)
   * @return {@code true} if the study's private information can be shared with the requester,
   *     {@code false} otherwise
   */
  private boolean canShareStudy(ObjectNode studyInfo, Study indexedSubmission, String secretKey) {
    User currentUser = Session.getCurrentUser();

    // Anyone with the correct secret key can access the study
    String docSecretKey = indexedSubmission.getSecretKey();
    if (secretKey != null && secretKey.equals(docSecretKey)) {
      return true;
    }

    // For authenticated users, check release date conditions
    if (currentUser != null) {
      // Grant access if no release date is set or if it's null
      // (indicates study is not yet ready for public release)
      if (!studyInfo.hasNonNull("released")) {
        return true;
      }

      // Grant access if the release date is in the future
      // (study is scheduled for future release but not yet public)
      return studyInfo.get("released").asLong() > new Date().getTime();
    }

    // No access for unauthenticated users without a secret key
    return false;
  }

}
