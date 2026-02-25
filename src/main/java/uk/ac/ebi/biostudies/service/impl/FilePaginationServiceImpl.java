package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.DataTableColumnInfo;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.auth.Session;
import uk.ac.ebi.biostudies.auth.User;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.exceptions.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.file.Thumbnails;
import uk.ac.ebi.biostudies.service.FilePaginationService;
import uk.ac.ebi.biostudies.service.client.FileSearcherHttpClient;
import uk.ac.ebi.biostudies.service.client.IndexerHttpClient;
import uk.ac.ebi.biostudies.service.client.Study;

@Slf4j
@Service
public class FilePaginationServiceImpl implements FilePaginationService {

  @Autowired IndexManager indexManager;
  @Autowired IndexConfig indexConfig;
  @Autowired Thumbnails thumbnails;
  @Autowired IndexerHttpClient indexerHttpClient;
  @Autowired
  FileSearcherHttpClient fileSearcherHttpClient;

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

//  @Override
//  public ObjectNode getFileList(
//      String accession,
//      int start,
//      int pageSize,
//      String search,
//      int draw,
//      boolean metadata,
//      Map<Integer, DataTableColumnInfo> dataTableUiResult,
//      String secretKey)
//      throws SubmissionNotAccessibleException {
//    ObjectMapper mapper = new ObjectMapper();
//    IndexSearcher searcher = indexManager.getFileIndexSearcher();
//    QueryParser parser = new QueryParser(Constants.Fields.ACCESSION, new KeywordAnalyzer());
//    IndexReader reader = indexManager.getFileIndexReader();
//    ObjectNode studyInfo = getStudyInfo(accession, secretKey);
//    long totalFiles = studyInfo.get(Constants.Fields.FILES).asLong();
//    if (studyInfo == null) return mapper.createObjectNode();
//    ArrayNode columns = (ArrayNode) studyInfo.get("columns");
//    try {
//      List<SortField> allSortedFields = new ArrayList<>();
//      List<DataTableColumnInfo> searchedColumns = new ArrayList<>();
//      for (DataTableColumnInfo ftInfo : dataTableUiResult.values()) {
//        if (ftInfo.getDir() != null && !ftInfo.getName().equalsIgnoreCase("x")) {
//          allSortedFields.add(
//              ftInfo.getName().equalsIgnoreCase("size")
//                  ? new SortedNumericSortField(
//                      ftInfo.getName(),
//                      SortField.Type.LONG,
//                      ftInfo.getDir().equalsIgnoreCase("desc"))
//                  : new SortField(
//                      ftInfo.getName(),
//                      SortField.Type.STRING,
//                      ftInfo.getDir().equalsIgnoreCase("desc")));
//        }
//        if (ftInfo.getSearchValue() != null && !ftInfo.getSearchValue().isEmpty()) {
//          searchedColumns.add(ftInfo);
//        }
//      }
//      if (allSortedFields.isEmpty())
//        allSortedFields.add(new SortField(Constants.File.POSITION, SortField.Type.LONG, false));
//      Sort sort = new Sort(allSortedFields.toArray(new SortField[allSortedFields.size()]));
//      Query query = parser.parse(Constants.File.OWNER + ":" + accession);
//      if (search != null && !search.isEmpty() && hasUnescapedDoubleQuote(search)) {
//        query = phraseSearch(search, query);
//      } else if (search != null && !search.isEmpty() && !search.trim().equalsIgnoreCase("**")) {
//        search = modifySearchText(search);
//        query = applySearch(search, query, columns);
//      }
//      if (searchedColumns.size() > 0) query = applyPerFieldSearch(searchedColumns, query);
//      TopDocs hits = searcher.search(query, Integer.MAX_VALUE, sort);
//      ObjectNode response = mapper.createObjectNode();
//      response.put(Constants.File.DRAW, draw);
//      response.put(Constants.File.RECORDTOTAL, totalFiles);
//      response.put(Constants.File.RECORDFILTERED, hits.totalHits.value);
//      if (hits.totalHits.value >= 0) {
//        if (pageSize == -1) pageSize = Integer.MAX_VALUE;
//        ArrayNode docs = mapper.createArrayNode();
//        for (int i = start; i < start + pageSize && i < hits.totalHits.value; i++) {
//          ObjectNode docNode = mapper.createObjectNode();
//          Document doc = reader.document(hits.scoreDocs[i].doc);
//          if (metadata) {
//            for (JsonNode field : columns) {
//              String fName = field.get("name").asText();
//              docNode.put(
//                  field.get("name").asText().replaceAll("[\\[\\]\\(\\)\\s]", "_"),
//                  doc.get(fName) == null ? "" : doc.get(fName));
//            }
//          }
//          docNode.put(
//              Constants.File.PATH,
//              doc.get(Constants.File.PATH) == null ? "" : doc.get(Constants.File.PATH));
//          docNode.put(
//              Constants.File.TYPE,
//              doc.get(Constants.File.IS_DIRECTORY) == null
//                  ? "file"
//                  : doc.get(Constants.File.IS_DIRECTORY).equalsIgnoreCase("true")
//                      ? "directory"
//                      : "file");
//          docNode.put(
//              Constants.File.SIZE.toLowerCase(), Long.parseLong(doc.get(Constants.File.SIZE)));
//          docs.add(docNode);
//        }
//        response.set(Constants.File.DATA, docs);
//        return response;
//      }
//
//    } catch (Exception ex) {
//      log.debug("problem in file atts preparation", ex);
//    }
//    return mapper.createObjectNode();
//  }

  @Override
  public ObjectNode getFileList(
      String accession,
      int start,
      int pageSize,
      String search,
      int draw,
      boolean metadata,
      Map<Integer, DataTableColumnInfo> dataTableUiResult,
      String secretKey) throws SubmissionNotAccessibleException {

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

  private Query phraseSearch(String search, Query query) throws Exception {
    // Todo these lines will be removed after Ahmad update val part of valqual with OR clauses
    search = search.replaceAll(" or ", " OR ");
    search = search.replaceAll(" and ", " AND ");
    search = search.replaceAll(" not ", " NOT ");

    BooleanQuery.Builder finalQueryBuilder = new BooleanQuery.Builder();
    QueryParser keywordParser = new QueryParser(Constants.File.NAME, new KeywordAnalyzer());
    Query phrasedQuery = keywordParser.parse(search);
    finalQueryBuilder.add(phrasedQuery, BooleanClause.Occur.MUST);
    finalQueryBuilder.add(query, BooleanClause.Occur.MUST);
    return finalQueryBuilder.build();
  }

  private Query applySearch(String search, Query firstQuery, ArrayNode columns) {
    BooleanQuery.Builder builderSecond = new BooleanQuery.Builder();
    //        BooleanClause.Occur[] occurs = new  BooleanClause.Occur[columns.size()];
    String[] fields = new String[columns.size()];
    try {
      int counter = 0;
      for (JsonNode field : columns) {
        String fName = field.get("name").asText();
        fields[counter] = fName;
        //               occurs[counter] = BooleanClause.Occur.SHOULD;
        counter++;
      }
      MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new KeywordAnalyzer());

      parser.setAllowLeadingWildcard(true);
      // parser.setLowercaseExpandedTerms(false);
      Query tempSmallQuery = parser.parse(StudyUtils.escape(search));
      log.debug(tempSmallQuery.toString());
      builderSecond.add(firstQuery, BooleanClause.Occur.MUST);
      builderSecond.add(tempSmallQuery, BooleanClause.Occur.MUST);
    } catch (ParseException e) {
      log.debug("File Searchable Query Parser Exception", e);
    }
    log.debug("query is: {}", builderSecond.build().toString());
    return builderSecond.build();
  }

  private String modifySearchText(String search) {
    search = search.toLowerCase();
    String[] tokens = search.split(" ");
    String newQuery = "";
    if (tokens != null) {
      for (String token : tokens) {
        token = " *" + token + "* ";
        newQuery = newQuery + token;
      }
    }
    if (newQuery.contains(" *and* ")) newQuery = newQuery.replaceAll(" \\*and\\* ", " AND ");
    if (newQuery.contains(" *or* ")) newQuery = newQuery.replaceAll(" \\*or\\* ", " OR ");
    if (newQuery.contains(" *not* ")) newQuery = newQuery.replaceAll(" \\*not\\* ", " NOT ");
    return newQuery;
  }

  private Query applyPerFieldSearch(
      List<DataTableColumnInfo> searchedColumns, Query originalQuery) {
    BooleanQuery.Builder logicQueryBuilder = new BooleanQuery.Builder();
    logicQueryBuilder.add(originalQuery, BooleanClause.Occur.MUST);
    for (DataTableColumnInfo info : searchedColumns) {
      QueryParser parser = new QueryParser(info.getName(), new KeywordAnalyzer());
      parser.setAllowLeadingWildcard(true);
      try {
        Query query =
            parser.parse(
                StudyUtils.escape(
                    (info.getName().equalsIgnoreCase("section")
                        ? info.getSearchValue().toLowerCase()
                        : modifySearchText(info.getSearchValue()))));
        logicQueryBuilder.add(query, BooleanClause.Occur.MUST);
      } catch (ParseException e) {
        log.debug("problem in search term {}", info.getSearchValue(), e);
      }
    }
    return logicQueryBuilder.build();
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
   * @param indexedSubmission the indexed submission containing the indexed study data with the secret key
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

  private boolean hasUnescapedDoubleQuote(String search) {
    return search.replaceAll("\\Q\\\"\\E", "").contains("\"");
  }
}
