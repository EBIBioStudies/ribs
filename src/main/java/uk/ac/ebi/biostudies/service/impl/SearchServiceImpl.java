package uk.ac.ebi.biostudies.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.ac.ebi.biostudies.api.util.Constants.*;
import static uk.ac.ebi.biostudies.controller.Stats.LATEST_ENDPOINT;
import static uk.ac.ebi.biostudies.controller.Stats.STATS_ENDPOINT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.api.util.analyzer.AnalyzerManager;
import uk.ac.ebi.biostudies.api.util.analyzer.AttributeFieldAnalyzer;
import uk.ac.ebi.biostudies.auth.Session;
import uk.ac.ebi.biostudies.auth.User;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.efo.EFOExpandedHighlighter;
import uk.ac.ebi.biostudies.efo.EFOExpansionTerms;
import uk.ac.ebi.biostudies.efo.EFOQueryExpander;
import uk.ac.ebi.biostudies.service.FacetService;
import uk.ac.ebi.biostudies.service.QueryService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;

/**
 * Created by ehsan on 27/02/2017.
 */

@Service
public class SearchServiceImpl implements SearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static Cache<String, String> statsCache;
    private static Query excludeCompound;
    private static QueryParser parser;
    private static Set<String> sectionsToFilter;
    private final Logger logger = LogManager.getLogger(SearchServiceImpl.class.getName());
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    IndexManager indexManager;
    @Autowired
    EFOQueryExpander efoQueryExpander;
    @Autowired
    EFOExpandedHighlighter efoExpandedHighlighter;
    @Autowired
    FacetService facetService;
    @Autowired
    AnalyzerManager analyzerManager;
    @Autowired
    SecurityQueryBuilder securityQueryBuilder;
    @Autowired
    QueryService queryService;
    @Autowired
    FireService fireService;


    @PostConstruct
    void init() {
        parser = new QueryParser(Fields.TYPE, new AttributeFieldAnalyzer());
        parser.setSplitOnWhitespace(true);
        // TODO: move stats cache timeout to config file
        statsCache = CacheBuilder.newBuilder()
                .expireAfterAccess(720, TimeUnit.MINUTES)
                .build();
        sectionsToFilter = new HashSet<>();
        sectionsToFilter.add("author");
        sectionsToFilter.add("organisation");
        sectionsToFilter.add("organization");
        sectionsToFilter.add("publication");

        try {
            excludeCompound = parser.parse("type:compound");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    private ObjectNode applySearchOnQuery(Query query, int page, int pageSize, String sortBy, String sortOrder, boolean doHighlight, String queryString) {
        IndexReader reader = indexManager.getSearchIndexReader();
        IndexSearcher searcher = indexManager.getIndexSearcher();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();
        SortField.Type sortFieldType = extractFieldType(sortBy);
        boolean shouldReverse = extractSortOrder(sortOrder, sortBy);
        SortField sortField;
        if (sortBy.isEmpty() || RELEVANCE.equalsIgnoreCase(sortBy))
            sortField = new SortField(null, SortField.Type.SCORE, shouldReverse);
        else {
            sortField = sortFieldType == SortField.Type.LONG
                    ? new SortedNumericSortField(sortBy, sortFieldType, shouldReverse)
                    : new SortField(sortBy, sortFieldType, shouldReverse);
        }
        Sort sort = new Sort(sortField);
        if (sortBy.equalsIgnoreCase(RELEASE_DATE))
            sort = new Sort(sortField, new SortedNumericSortField(Fields.MODIFICATION_TIME, SortField.Type.LONG, shouldReverse));

        try {
            User currentUser = Session.getCurrentUser();
            pageSize = Math.min(pageSize, currentUser != null && currentUser.isSuperUser() ? Integer.MAX_VALUE : MAX_PAGE_SIZE);
            int searchResultsSize = Math.min(page * pageSize, Integer.MAX_VALUE);
            TopDocs hits = searcher.search(query, searchResultsSize, sort);
            long totalHits = hits.totalHits != null ? hits.totalHits.value : 0;
            boolean isTotalHitsExact = hits.totalHits == null || hits.totalHits.relation.equals(TotalHits.Relation.EQUAL_TO);
            long to = (long) page * pageSize > totalHits ? totalHits : (long) page * pageSize;
            response.put("page", page);
            response.put("pageSize", pageSize);
            response.put("totalHits", totalHits);
            response.put("isTotalHitsExact", isTotalHitsExact);
            response.put("sortBy", sortBy.equalsIgnoreCase(Fields.RELEASE_TIME) ? RELEASE_DATE : sortBy);
            response.put("sortOrder", shouldReverse ? SortOrder.DESCENDING : SortOrder.ASCENDING);
            if (sortBy.equalsIgnoreCase(RELEVANCE)) {
                response.put("sortOrder", !shouldReverse ? SortOrder.DESCENDING : SortOrder.ASCENDING);
            }
            if (totalHits > 0) {
                ArrayNode docs = mapper.createArrayNode();
                for (int i = (page - 1) * pageSize; i < to; i++) {
                    ObjectNode docNode = mapper.createObjectNode();
                    Document doc = reader.document(hits.scoreDocs[i].doc);
                    for (String field : indexManager.getIndexEntryMap().keySet()) {
                        JsonNode fieldData = indexManager.getIndexEntryMap().get(field);
                        if (fieldData.has(IndexEntryAttributes.RETRIEVED)
                                && fieldData.get(IndexEntryAttributes.RETRIEVED).asBoolean(false)) {
                            if (fieldData.get(IndexEntryAttributes.FIELD_TYPE).asText().equalsIgnoreCase(IndexEntryAttributes.FieldTypeValues.LONG)
                                    && doc.get(field) != null) {
                                docNode.put(field, Long.parseLong(doc.get(field)));
                            } else {
                                docNode.put(field, doc.get(field));
                            }
                        }
                    }
                    boolean isPublic = (" " + doc.get(Fields.ACCESS) + " ").toLowerCase().contains(" public ");
                    docNode.put("isPublic", isPublic);
                    if (!isPublic) {
                        docNode.put("modification_time", doc.get(Fields.MODIFICATION_TIME));
                    }

                    if (doHighlight) {
                        docNode.put(Fields.CONTENT,
                                efoExpandedHighlighter.highlightQuery(query, Fields.CONTENT,
                                        doc.get(Fields.CONTENT),
                                        true
                                )
                        );
                    }
                    docs.add(docNode);
                }
                response.set("hits", docs);
                logger.debug(hits.totalHits + " hits");
            } else if (queryString != null && !queryString.isEmpty()) {
                String[] spells = indexManager.getSpellChecker().suggestSimilar(queryString, 5);
                response.set("suggestion", mapper.valueToTree(Arrays.asList(spells)));
            }
        } catch (Throwable error) {
            logger.error("problem in searching this query {}", query, error);
        }
        return response;
    }

    private SortField.Type extractFieldType(String sortBy) {
        try {
            if (sortBy == null || sortBy.isEmpty() || sortBy.equalsIgnoreCase("relevance"))
                return SortField.Type.LONG;
            else {
                JsonNode field = indexManager.getIndexEntryMap().get(sortBy.toLowerCase());
                return field.get("fieldType").asText().toLowerCase().contains("str") ? SortField.Type.STRING : SortField.Type.LONG;
            }
        } catch (Exception e) {
            logger.debug("bad sortby value {}", sortBy);
        }
        return SortField.Type.SCORE;
    }

    private boolean extractSortOrder(String sortOrder, String sortBy) {
        if (sortOrder.isEmpty()) {
            sortOrder = (Fields.ACCESSION.equalsIgnoreCase(sortBy) || Fields.TITLE.equalsIgnoreCase(sortBy) || Fields.AUTHOR.equalsIgnoreCase(sortBy))
                    ? SortOrder.ASCENDING : SortOrder.DESCENDING;
        }
        boolean shouldReverse = (SortOrder.DESCENDING.equalsIgnoreCase(sortOrder));
        if (sortBy == null || RELEVANCE.equalsIgnoreCase(sortBy)) {
            shouldReverse = !shouldReverse;
        }
        return shouldReverse;
    }

    @Override
    public String search(String queryString, JsonNode selectedFacetsAndFields, String prjName, int page, int pageSize, String sortBy, String sortOrder) {
        boolean doHighlight = true;
        if (queryString.isEmpty() && sortBy.isEmpty()) {
            sortBy = Fields.RELEASE_DATE; // TODO: Make sure RELEASE_TIME is used correctly all over the code base
            doHighlight = false;
        } else if (sortBy.isEmpty()) {
            sortBy = RELEVANCE;
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();
        JsonNode selectedFields = selectedFacetsAndFields == null ? null : selectedFacetsAndFields.get("fields");
        JsonNode selectedFacets = selectedFacetsAndFields == null ? null : selectedFacetsAndFields.get("facets");
        Pair<Query, EFOExpansionTerms> resultPair = queryService.makeQuery(queryString, prjName, selectedFields);
        try {
            Query queryAfterSecurity = resultPair.getKey();
            if (selectedFacets != null) {
                queryAfterSecurity = facetService.applyFacets(queryAfterSecurity, selectedFacets);
                logger.debug("Lucene after facet query: {}", queryAfterSecurity.toString());
            }
            response = applySearchOnQuery(queryAfterSecurity, page, pageSize, sortBy, sortOrder, doHighlight, queryString);

            // add expansion
            EFOExpansionTerms expansionTerms = resultPair.getValue();
            if (expansionTerms != null && expansionTerms.efo.size() + expansionTerms.synonyms.size() > EFOQueryExpander.MAX_EXPANSION_TERMS) {
                response.put("tooManyExpansionTerms", true);
            } else if (expansionTerms != null) {
                ArrayNode expandedEfoTerms = mapper.createArrayNode();
                expansionTerms.efo.forEach(s -> expandedEfoTerms.add(s));
                response.set("expandedEfoTerms", expandedEfoTerms);

                ArrayNode expandedSynonymTerms = mapper.createArrayNode();
                expansionTerms.synonyms.forEach(s -> expandedSynonymTerms.add(s));
                response.set("expandedSynonyms", expandedSynonymTerms);

            }

            response.put("query", doHighlight ? queryString : null);
            response.set("facets", selectedFacets == null || selectedFacets.size() == 0 ? null : selectedFacets);
        } catch (Throwable error) {
            logger.error("problem in searching this query {}", queryString, error);
            response.put("query", queryString.equals("*:*") ? null : queryString);
        }

        return response.toString();
    }

    public String getFailedIndexingSubmissions() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();
        ArrayNode failedAccessions = mapper.createArrayNode();

        try {
            // Query: HAS_FILE_PARSING_ERROR:true
            Query query = new TermQuery(new Term(Fields.HAS_FILE_PARSING_ERROR, "true"));
            IndexSearcher searcher = indexManager.getIndexSearcher();
            IndexReader reader = indexManager.getSearchIndexReader();

            // Fetch total first
            TotalHitCountCollector countCollector = new TotalHitCountCollector();
            searcher.search(query, countCollector);
            int totalFailure = countCollector.getTotalHits();

            // Fetch top 100 only
            TopDocs hits = searcher.search(query, 100);
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document doc = reader.document(scoreDoc.doc);
                String accession = doc.get(Fields.ACCESSION);
                if (accession != null) {
                    failedAccessions.add(accession);
                }
            }

            response.put("totalFailure", totalFailure);
            response.set("failedAccessions", failedAccessions);

        } catch (Exception e) {
            logger.error("Error retrieving failed indexing submissions", e);
            response.put("error", "Internal error while retrieving failed submissions");
        }

        return response.toString();
    }



    @Override
    public InputStreamResource getStudyAsStream(String accession, String relativePath, boolean anonymise, Constants.File.StorageMode storageMode, boolean isPublicStudy, String secretKey)
            throws IOException {
        return getStudyAsStream(accession, relativePath, anonymise, storageMode, true, isPublicStudy, secretKey);
    }

    @Override
    public InputStreamResource getStudyAsStream(String accession, String relativePath, boolean anonymise, Constants.File.StorageMode storageMode, boolean fillPagetabIndex, boolean isPublicStudy, String secretKey)
            throws IOException {

        InputStream inputStream = null;
        Exception codeIOException = null;
        if (fillPagetabIndex) {
            try {
                inputStream = getPagetabFromPagetabIndex(accession.toLowerCase());
            } catch (Exception ex) {
                codeIOException = ex;
                logger.error("problem in searching pagetab index", ex);
            }
        }
        try {
            if (inputStream == null) {
                switch (storageMode) {
                    case FIRE:
                        inputStream = fireService.cloneFireS3ObjectStream(relativePath + "/" + accession + ".json");
                        break;
                    default:
                        if(!isPublicStudy) {
                            relativePath = StudyUtils.modifyRelativePathForPrivateStudies(secretKey, relativePath);
                        }
                        inputStream = new FileInputStream(Paths.get(indexConfig.getFileRootDir(), relativePath, accession + ".json").toFile());
                }
                if(inputStream!=null){//cache miss, we do not have this pagetab in index so we will add it
                    byte []buffer = inputStream.readAllBytes();
                    inputStream.close();
                    addPagetabDocument(accession, buffer);
                    inputStream = new ByteArrayInputStream(buffer);
                }
            }
        } catch (Exception exception) {
            codeIOException = exception;
        }
        if (inputStream == null)
            throw new IOException(codeIOException);


        if (anonymise) {
            inputStream = anonymisePagetab(inputStream);
        }

        return new InputStreamResource(inputStream);
    }

    public InputStream anonymisePagetab(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(inputStream);
        removePagetabSensitiveSections(json);
        inputStream.close();
        return new ByteArrayInputStream(mapper.writeValueAsBytes(json));
    }

    public String anonymisePagetab(String inputString) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(inputString);
        removePagetabSensitiveSections(json);
        return mapper.writeValueAsString(json);
    }

    private void removePagetabSensitiveSections(JsonNode json){
        JsonNode subSections = json.get("section").get("subsections");
        if (subSections!=null && subSections.isArray()) {
            Iterator<JsonNode> iterator = subSections.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                if (node.has("type") && sectionsToFilter.contains(node.get("type").textValue().toLowerCase())) {
                    iterator.remove();
                }
            }
        }
    }
    private void addPagetabDocument(String accession, byte[] pagetabContent) {
        accession = accession.toLowerCase();
        Document pagetabDoc = new Document();
        pagetabDoc.add(new StringField(Constants.Fields.ACCESSION, accession, Field.Store.YES));
        pagetabDoc.add(new StoredField(Fields.CONTENT, pagetabContent));
        try {
            indexManager.getPagetabIndexWriter().updateDocument(new Term(Fields.ACCESSION, accession), pagetabDoc);
            indexManager.getPagetabIndexWriter().commit();
        } catch (Exception exception) {
            logger.error("Problem in adding pagetab to index", exception);
        }
    }

    private InputStream getPagetabFromPagetabIndex(String accession) throws Exception {
        TopDocs pagetabResult = indexManager.getPagetabIndexSearcher().search(new TermQuery(new Term(Constants.Fields.ACCESSION, accession)), 1);
        InputStream pagetabStream = null;
        if (pagetabResult.totalHits.value == 1L) {
            Document doc = indexManager.getPagetabIndexReader().document(pagetabResult.scoreDocs[0].doc);
            BytesRef contentBytes = doc.getBinaryValue(Constants.Fields.CONTENT);
            if (contentBytes != null) {
                pagetabStream = new ByteArrayInputStream(contentBytes.bytes);
                logger.debug("pagetab retrieved from pagetab index for acc: {}", accession);
            }
        }
        return pagetabStream;
    }

    @Override
    public String getFieldStats() throws Exception {
        String responseString = statsCache.getIfPresent(STATS_ENDPOINT);
        if (responseString == null) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode response = mapper.createObjectNode();
            IndexSearcher searcher = indexManager.getIndexSearcher();
            if (searcher == null) return "{}";
            DocValuesStats.SortedLongDocValuesStats fileStats = new DocValuesStats.SortedLongDocValuesStats("files");
            searcher.search(new MatchAllDocsQuery(), new DocValuesStatsCollector(fileStats));
            response.put("files", fileStats.sum());

            DocValuesStats.SortedLongDocValuesStats linkStats = new DocValuesStats.SortedLongDocValuesStats("links");
            searcher.search(new MatchAllDocsQuery(), new DocValuesStatsCollector(linkStats));
            response.put("links", linkStats.sum());

            indexManager.getSearchIndexWriter().getLiveCommitData().forEach(entry -> {
                if (entry.getKey().equalsIgnoreCase("updateTime")) {
                    response.put("time", Long.parseLong(entry.getValue()));
                }
            });
            responseString = response.toString();
            statsCache.put(STATS_ENDPOINT, responseString);
        }
        return responseString;
    }

    @Override
    public void clearStatsCache() {
        statsCache.invalidateAll();
    }

    @Override
    public ObjectNode getSimilarStudies(String accession, String secretKey) throws Throwable {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        if (secretKey != null)
            return result;
        int maxHits = 4;
        MoreLikeThis mlt = new MoreLikeThis(indexManager.getSearchIndexReader());
        mlt.setFieldNames(new String[]{Fields.CONTENT, Fields.TITLE, Facets.COLLECTION});
        mlt.setAnalyzer(analyzerManager.getPerFieldAnalyzerWrapper());
        Integer docNumber = getDocumentNumberByAccession(accession, null);
        Query likeQuery = mlt.like(docNumber);
        Query similarityQuery = securityQueryBuilder.applySecurity(likeQuery, null);
        TopDocs mltDocs = indexManager.getIndexSearcher().search(similarityQuery, maxHits);
        ArrayNode similarStudies = mapper.createArrayNode();
        for (int i = 0; i < mltDocs.scoreDocs.length; i++) {
            ObjectNode study = mapper.createObjectNode();
            study.set(Fields.ACCESSION, mapper.valueToTree(indexManager.getSearchIndexReader().document(mltDocs.scoreDocs[i].doc).get(Fields.ACCESSION)));
            study.set(Fields.TITLE, mapper.valueToTree(indexManager.getSearchIndexReader().document(mltDocs.scoreDocs[i].doc).get(Fields.TITLE)));
            if (!study.get(Fields.ACCESSION).asText().equalsIgnoreCase(accession)) {
                similarStudies.add(study);
            }
        }

        if (mltDocs.totalHits.value > 1) {
            result.set("similarStudies", similarStudies);
        }
        return result;
    }

    @Override
    public Document getDocumentByAccession(String accession, String secretKey) throws SubmissionNotAccessibleException {
        Integer docNumber = getDocumentNumberByAccession(accession, secretKey);
        if (docNumber == null) {
            if (isDocumentPresent(accession)) {
                throw new SubmissionNotAccessibleException();
            }
            return null;
        }
        try {
            return indexManager.getSearchIndexReader().document(docNumber);
        } catch (IOException ex) {
            logger.error("Problem retrieving " + accession, ex);
        }
        return null;
    }

    @Override
    public Document getDocumentByAccessionAndType(String accession, String secretKey, String type) throws SubmissionNotAccessibleException {
        Document document = getDocumentByAccession(accession, secretKey);
        if (document == null || !document.get(Fields.TYPE).equalsIgnoreCase(type.trim()))
            return null;
        return document;
    }


    private Integer getDocumentNumberByAccession(String accession, String secretKey) {
        QueryParser parser = new QueryParser(Fields.ACCESSION, new AttributeFieldAnalyzer());
        parser.setSplitOnWhitespace(true);
        Query query = null;
        try {
            query = parser.parse(Fields.ACCESSION + ":" + accession);
            Query result = securityQueryBuilder.applySecurity(query, secretKey);
            TopDocs topDocs = indexManager.getIndexSearcher().search(result, 1);
            if (topDocs.totalHits.value == 1) {
                return topDocs.scoreDocs[0].doc;
            }
        } catch (Throwable ex) {
            logger.error("Problem in checking security", ex);
        }
        return null;
    }

    @Override
    public boolean isDocumentPresent(String accession) {
        QueryParser parser = new QueryParser(Fields.ACCESSION, new AttributeFieldAnalyzer());
        parser.setSplitOnWhitespace(true);
        Query query;
        try {
            query = parser.parse(Fields.ACCESSION + ":" + accession);
            TopDocs topDocs = indexManager.getIndexSearcher().search(query, 1);
            return topDocs.totalHits.value == 1;
        } catch (Throwable ex) {
            logger.error("Problem in checking security", ex);
        }
        return false;
    }

    public String getLatestStudies() throws Exception {
        boolean isLoggedIn = Session.getCurrentUser() != null;
        String responseString = null;
        if (!isLoggedIn) {
            responseString = statsCache.getIfPresent(LATEST_ENDPOINT);
            if (responseString == null) {
                responseString = search(URLDecoder.decode(Constants.Fields.TYPE + ":study", String.valueOf(UTF_8)),
                        null, null, 1, 5, Constants.Fields.RELEASE_TIME,
                        Constants.SortOrder.DESCENDING);
                statsCache.put(LATEST_ENDPOINT, responseString);
            }
        } else {
            return search(URLDecoder.decode(Constants.Fields.TYPE + ":study", String.valueOf(UTF_8)),
                    null, null, 1, 5, Constants.Fields.RELEASE_TIME,
                    Constants.SortOrder.DESCENDING);
        }

        return responseString;
    }

    public boolean isDocumentInCollection(Document submissionDoc, String collection) {
        return StringUtils.isEmpty(collection) || submissionDoc.get("collection").contains(collection.toLowerCase());
    }
}
