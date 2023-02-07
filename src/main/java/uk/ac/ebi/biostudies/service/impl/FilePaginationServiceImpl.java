package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
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
import uk.ac.ebi.biostudies.config.SecurityConfig;
import uk.ac.ebi.biostudies.file.Thumbnails;
import uk.ac.ebi.biostudies.service.FilePaginationService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;

import java.io.IOException;
import java.util.*;

@Service
public class FilePaginationServiceImpl implements FilePaginationService {

    private final Logger logger = LogManager.getLogger(FilePaginationServiceImpl.class.getName());
    @Autowired
    IndexManager indexManager;
    @Autowired
    SearchService searchService;
    @Autowired
    SecurityConfig securityConfig;
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    Thumbnails thumbnails;

    public ObjectNode getStudyInfo(String accession, String secretKey) throws SubmissionNotAccessibleException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode studyInfo = mapper.createObjectNode();

        String[] orderedArray = {"Name", "Size"};
        ArrayNode fileColumnAttributes = mapper.createArrayNode();
        Document doc = searchService.getDocumentByAccession(accession, secretKey);
        if (doc == null) return studyInfo;
        accession = doc.get(Constants.Fields.ACCESSION);
        String relativePath = doc.get(Constants.Fields.RELATIVE_PATH);
        String attFiles = doc.get(Constants.File.FILE_ATTS);
        String storageModeString = doc.get(Constants.Fields.STORAGE_MODE);
        Constants.File.StorageMode storageMode = Constants.File.StorageMode.valueOf(StringUtils.isEmpty(storageModeString) ? "NFS" : storageModeString);

        if (attFiles == null) return studyInfo;
        String[] allAtts = attFiles.split("\\|");
        Set<String> headerSet = new HashSet<>(Arrays.asList(orderedArray));
        List<String> orderedList = new ArrayList<>(Arrays.asList(orderedArray));
        for (String att : allAtts) {
            if (att.isEmpty() || headerSet.contains(att))
                continue;
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
            node.put("data", att.replaceAll("[\\[\\]\\(\\)\\s]", "_").replaceAll("\\.","\\\\."));
            node.put("defaultContent", "");
            fileColumnAttributes.add(node);
            if (counter++ == 1 && thumbnails.hasThumbnailsFolder(accession, relativePath, storageMode)) {
                fileColumnAttributes.add(getThumbnailHeader(mapper));
            }
        }

        String sectionsWithFiles = doc.get(Constants.Fields.SECTIONS_WITH_FILES);
        studyInfo.set("columns", fileColumnAttributes);
        studyInfo.put(Constants.Fields.FILES, Long.parseLong(doc.get(Constants.Fields.FILES)));
        studyInfo.put("ftpLink", indexConfig.getFtpDir() + storageModeString.toLowerCase() + "/" + doc.get(Constants.Fields.RELATIVE_PATH));
        studyInfo.put("globusLink", indexConfig.getGlobusUrl() + storageModeString.toLowerCase() + "/" + doc.get(Constants.Fields.RELATIVE_PATH));
        studyInfo.put("isPublic", (" " + doc.get(Constants.Fields.ACCESS) + " ").toLowerCase().contains(" public "));
        studyInfo.put(Constants.Fields.RELATIVE_PATH, relativePath);
        studyInfo.put("hasZippedFolders",storageMode == Constants.File.StorageMode.FIRE);

        setDates(studyInfo, doc);
        setPrivateData(studyInfo, doc, secretKey);

        try {
            if (sectionsWithFiles != null) {
                studyInfo.set("sections", mapper.readTree("[\"" +
                        sectionsWithFiles.replaceAll(" ", "\",\"")
                        + "\"]"));
            }
        } catch (Exception e) {
            logger.error("Error retrieving sections with files");
            studyInfo.put("sections", "[]");
        }
        return studyInfo;
    }

    private void setDates(ObjectNode studyInfo, Document doc) {
        try {
            studyInfo.put("released", Long.parseLong(doc.get(Constants.Fields.RELEASE_TIME)));
        } catch (Throwable error) {
            logger.error("Invalid release date for {}: {} ", doc.get(Constants.Fields.ACCESSION), doc.get(Constants.Fields.RELEASE_TIME));
        }

        try {
            studyInfo.put("modified", Long.parseLong(doc.get(Constants.Fields.MODIFICATION_TIME)));
        } catch (Throwable error) {
            logger.error("Invalid modification date for {}: {} ", doc.get(Constants.Fields.ACCESSION), doc.get(Constants.Fields.MODIFICATION_TIME));
        }
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

    private Document getFileDocument(String accession, String path) throws ParseException, IOException {
        IndexSearcher searcher = indexManager.getFileIndexSearcher();
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        QueryParser keywordParser = new QueryParser(Constants.File.NAME, new KeywordAnalyzer());
        queryBuilder.add(keywordParser.parse(Constants.File.OWNER + ":" + accession), BooleanClause.Occur.MUST);
        queryBuilder.add(keywordParser.parse(Constants.File.PATH + ":" + StudyUtils.escape(path)), BooleanClause.Occur.MUST);
        TopDocs hits = searcher.search(queryBuilder.build(), Integer.MAX_VALUE);
        return hits.totalHits.value > 0 ? searcher.getIndexReader().document(hits.scoreDocs[0].doc) : null;
    }

    @Override
    public ObjectNode getFileList(String accession, int start, int pageSize, String search, int draw, boolean metadata, Map<Integer, DataTableColumnInfo> dataTableUiResult, String secretKey) throws SubmissionNotAccessibleException {
        ObjectMapper mapper = new ObjectMapper();
        IndexSearcher searcher = indexManager.getFileIndexSearcher();
        QueryParser parser = new QueryParser(Constants.Fields.ACCESSION, new KeywordAnalyzer());
        IndexReader reader = indexManager.getFileIndexReader();
        ObjectNode studyInfo = getStudyInfo(accession, secretKey);
        long totalFiles = studyInfo.get(Constants.Fields.FILES).asLong();
        if (studyInfo == null) return mapper.createObjectNode();
        ArrayNode columns = (ArrayNode) studyInfo.get("columns");
        try {
            List<SortField> allSortedFields = new ArrayList<>();
            List<DataTableColumnInfo> searchedColumns = new ArrayList<>();
            for (DataTableColumnInfo ftInfo : dataTableUiResult.values()) {
                if (ftInfo.getDir() != null && !ftInfo.getName().equalsIgnoreCase("x")) {
                    allSortedFields.add(ftInfo.getName().equalsIgnoreCase("size") ? new SortedNumericSortField(ftInfo.getName(), SortField.Type.LONG, ftInfo.getDir().equalsIgnoreCase("desc"))
                            : new SortField(ftInfo.getName(), SortField.Type.STRING, ftInfo.getDir().equalsIgnoreCase("desc")));
                }
                if (ftInfo.getSearchValue() != null && !ftInfo.getSearchValue().isEmpty()) {
                    searchedColumns.add(ftInfo);
                }
            }
            if (allSortedFields.isEmpty())
                allSortedFields.add(new SortField(Constants.File.NAME, SortField.Type.STRING, false));
            Sort sort = new Sort(allSortedFields.toArray(new SortField[allSortedFields.size()]));
            Query query = parser.parse(Constants.File.OWNER + ":" + accession);
            if (search != null && !search.isEmpty() && hasUnescapedDoubleQuote(search)) {
                query = phraseSearch(search, query);
            } else if (search != null && !search.isEmpty() && !search.trim().equalsIgnoreCase("**")) {
                search = modifySearchText(search);
                query = applySearch(search, query, columns);
            }
            if (searchedColumns.size() > 0)
                query = applyPerFieldSearch(searchedColumns, query);
            TopDocs hits = searcher.search(query, Integer.MAX_VALUE, sort);
            ObjectNode response = mapper.createObjectNode();
            response.put(Constants.File.DRAW, draw);
            response.put(Constants.File.RECORDTOTAL, totalFiles);
            response.put(Constants.File.RECORDFILTERED, hits.totalHits.value);
            if (hits.totalHits.value >= 0) {
                if (pageSize == -1) pageSize = Integer.MAX_VALUE;
                ArrayNode docs = mapper.createArrayNode();
                for (int i = start; i < start + pageSize && i < hits.totalHits.value; i++) {
                    ObjectNode docNode = mapper.createObjectNode();
                    Document doc = reader.document(hits.scoreDocs[i].doc);
                    if (metadata) {
                        for (JsonNode field : columns) {
                            String fName = field.get("name").asText();
                            docNode.put(field.get("name").asText().replaceAll("[\\[\\]\\(\\)\\s]", "_"), doc.get(fName) == null ? "" : doc.get(fName));
                        }
                    }
                    docNode.put(Constants.File.PATH, doc.get(Constants.File.PATH) == null ? "" : doc.get(Constants.File.PATH));
                    docNode.put(Constants.File.TYPE, doc.get(Constants.File.IS_DIRECTORY) == null ? "file" : doc.get(Constants.File.IS_DIRECTORY).equalsIgnoreCase("true") ? "directory" : "file");
                    docs.add(docNode);
                }
                response.set(Constants.File.DATA, docs);
                return response;
            }


        } catch (Exception ex) {
            logger.debug("problem in file atts preparation", ex);
        }
        return mapper.createObjectNode();
    }


    private Query phraseSearch(String search, Query query) throws Exception {
        //Todo these lines will be removed after Ahmad update val part of valqual with OR clauses
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
            //parser.setLowercaseExpandedTerms(false);
            Query tempSmallQuery = parser.parse(StudyUtils.escape(search));
            logger.debug(tempSmallQuery);
            builderSecond.add(firstQuery, BooleanClause.Occur.MUST);
            builderSecond.add(tempSmallQuery, BooleanClause.Occur.MUST);
        } catch (ParseException e) {
            logger.debug("File Searchable Query Parser Exception", e);
        }
        logger.debug("query is: {}", builderSecond.build().toString());
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
        if (newQuery.contains(" *and* "))
            newQuery = newQuery.replaceAll(" \\*and\\* ", " AND ");
        if (newQuery.contains(" *or* "))
            newQuery = newQuery.replaceAll(" \\*or\\* ", " OR ");
        if (newQuery.contains(" *not* "))
            newQuery = newQuery.replaceAll(" \\*not\\* ", " NOT ");
        return newQuery;
    }

    private Query applyPerFieldSearch(List<DataTableColumnInfo> searchedColumns, Query originalQuery) {
        BooleanQuery.Builder logicQueryBuilder = new BooleanQuery.Builder();
        logicQueryBuilder.add(originalQuery, BooleanClause.Occur.MUST);
        for (DataTableColumnInfo info : searchedColumns) {
            QueryParser parser = new QueryParser(info.getName(), new KeywordAnalyzer());
            parser.setAllowLeadingWildcard(true);
            try {
                Query query = parser.parse(StudyUtils.escape((info.getName().equalsIgnoreCase("section") ? info.getSearchValue().toLowerCase() : modifySearchText(info.getSearchValue()))));
                logicQueryBuilder.add(query, BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                logger.debug("problem in search term {}", info.getSearchValue(), e);
            }
        }
        return logicQueryBuilder.build();
    }

    private void setPrivateData(ObjectNode studyInfo, Document doc, String secretKey) {
        User currentUser = Session.getCurrentUser();
        if (studyInfo.has("released") && studyInfo.get("released").asLong() > new Date().getTime() && currentUser != null
            || (doc.get("seckey").equals(secretKey) )
        ) {
            IndexableField key = doc.getField(Constants.Fields.SECRET_KEY);
            if (key != null) {
                studyInfo.put(Constants.Fields.SECRET_KEY, key.stringValue());
            }
        }
    }

    private void generateDownloadAllFilePaths(TopDocs hits, ArrayNode filePaths, IndexReader reader) {
        for (int i = 0; i < hits.totalHits.value; i++) {
            try {
                filePaths.add(reader.document(i).get(Constants.File.PATH));
            } catch (IOException e) {
                logger.error("problem in reading lucene document {}", i, e);
            }
        }

    }

    private boolean hasUnescapedDoubleQuote(String search) {
        return search.replaceAll("\\Q\\\"\\E", "").contains("\"");
    }
}
