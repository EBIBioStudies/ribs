package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.analyzer.AttributeFieldAnalyzer;
import uk.ac.ebi.biostudies.api.util.parser.AbstractParser;
import uk.ac.ebi.biostudies.api.util.parser.ParserManager;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.config.TaxonomyManager;
import uk.ac.ebi.biostudies.service.*;

import java.io.File;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.ac.ebi.biostudies.api.util.Constants.*;

/**
 * Created by ehsan on 27/02/2017.
 */


@Service
@Scope("singleton")

public class IndexServiceImpl implements IndexService {

    public static final FieldType TYPE_NOT_ANALYZED = new FieldType();
    private static final BlockingQueue<String> indexFileQueue = new LinkedBlockingQueue<>();
    public static AtomicInteger ActiveExecutorService = new AtomicInteger(0);

    static {
        TYPE_NOT_ANALYZED.setIndexOptions(IndexOptions.DOCS);
        TYPE_NOT_ANALYZED.setTokenized(false);
        TYPE_NOT_ANALYZED.setStored(true);
    }

    private final Logger logger = LogManager.getLogger(IndexServiceImpl.class.getName());
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    IndexManager indexManager;
    @Autowired
    IndexManagementService indexManagementService;
    @Autowired
    FileIndexService fileIndexService;
    @Autowired
    SearchService searchService;
    @Autowired
    TaxonomyManager taxonomyManager;
    @Autowired
    FacetService facetService;
    @Autowired
    ParserManager parserManager;
    @Autowired
    ViewCountLoader viewCountLoader;


    @Override
    public void indexAll(InputStream inputStream, boolean removeFileDocuments) throws IOException {
//        indexManagementService.close();
        Long startTime = System.currentTimeMillis();
        ExecutorService executorService = new ThreadPoolExecutor(indexConfig.getThreadCount(), indexConfig.getThreadCount(),
                60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(indexConfig.getQueueSize()), new ThreadPoolExecutor.CallerRunsPolicy());
        ActiveExecutorService.incrementAndGet();
        int counter = 0;
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(inputStreamReader);


            JsonToken token = parser.nextToken();
            while (token != null && !JsonToken.START_ARRAY.equals(token)) {
                token = parser.nextToken();
            }

            ObjectMapper mapper = new ObjectMapper();
            while (true) {
                token = parser.nextToken();
                if (!JsonToken.START_OBJECT.equals(token)) {
                    break;
                }
                if (token == null) {
                    break;
                }

                JsonNode submission = mapper.readTree(parser);
                executorService.execute(new JsonDocumentIndexer(submission, taxonomyManager, indexManager, fileIndexService, removeFileDocuments, parserManager));
                if (++counter % 10000 == 0) {
                    logger.info("{} docs indexed", counter);
                }
            }
            while (token != null && token != JsonToken.END_OBJECT) {
                token = parser.nextToken();
            }

            Map<String, String> commitData = new HashMap<>();
            commitData.put("updateTime", Long.toString(new Date().getTime()));
            indexManager.getIndexWriter().setLiveCommitData(commitData.entrySet());

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.HOURS);
            FileIndexServiceImpl.FileListThreadPool.shutdown();
            FileIndexServiceImpl.FileListThreadPool.awaitTermination(5, TimeUnit.HOURS);
            indexManager.commitTaxonomy();
            indexManager.getIndexWriter().commit();
            indexManager.getFileIndexWriter().commit();
            indexManager.refreshIndexSearcherAndReader();
            indexManager.refreshTaxonomyReader();
            logger.info("Indexing lasted {} seconds", (System.currentTimeMillis() - startTime) / 1000);
            ActiveExecutorService.decrementAndGet();
            searchService.clearStatsCache();
        } catch (Throwable error) {
            logger.error("problem in parsing partial update", error);
        } finally {
//            indexManagementService.open();
            //logger.debug("Deleting temp file {}", inputStudiesFilePath);
            //Files.delete(Paths.get(inputStudiesFilePath));
        }
    }


    @Override
    public void indexOne(JsonNode submission, boolean removeFileDocuments) throws IOException {
        //TODO: Remove executor service if not needed
        Long startTime = System.currentTimeMillis();
        ExecutorService executorService = new ThreadPoolExecutor(indexConfig.getThreadCount(), indexConfig.getThreadCount(),
                60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(indexConfig.getQueueSize()), new ThreadPoolExecutor.CallerRunsPolicy());
        ActiveExecutorService.incrementAndGet();
        executorService.execute(new JsonDocumentIndexer(submission, taxonomyManager, indexManager, fileIndexService, removeFileDocuments, parserManager));
        executorService.shutdown();
        try {
            Map<String, String> commitData = new HashMap<>();
            commitData.put("updateTime", Long.toString(new Date().getTime()));
            indexManager.getIndexWriter().setLiveCommitData(commitData.entrySet());

            executorService.awaitTermination(5, TimeUnit.HOURS);
            indexManager.commitTaxonomy();
            indexManager.getIndexWriter().commit();
            indexManager.getFileIndexWriter().commit();
            indexManager.refreshIndexSearcherAndReader();
            indexManager.refreshTaxonomyReader();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ActiveExecutorService.decrementAndGet();
        searchService.clearStatsCache();
    }

    @Override
    public void deleteDoc(String accession) throws Exception {
        if (accession == null || accession.isEmpty())
            return;
        QueryParser parser = new QueryParser(Fields.ACCESSION, new AttributeFieldAnalyzer());
        String strquery = Fields.ACCESSION + ":" + accession;
        parser.setSplitOnWhitespace(true);
        Query query = parser.parse(strquery);
        indexManager.getIndexWriter().deleteDocuments(query);
        FileIndexServiceImpl.removeFileDocuments(indexManager.getFileIndexWriter(), accession);
        indexManager.getIndexWriter().commit();
        indexManager.getFileIndexWriter().commit();
        indexManager.refreshIndexSearcherAndReader();
        searchService.clearStatsCache();
    }

    @Override
    public void clearIndex(boolean commit) throws IOException {
        indexManager.getIndexWriter().deleteAll();
        indexManager.getFileIndexWriter().deleteAll();
        indexManager.getIndexWriter().forceMergeDeletes();
        indexManager.getFileIndexWriter().forceMergeDeletes();
        if (commit) {
            indexManager.getIndexWriter().commit();
            indexManager.getFileIndexWriter().commit();
            indexManager.refreshIndexSearcherAndReader();
        }
        indexManager.resetTaxonomyWriter();
        searchService.clearStatsCache();
    }

    public synchronized String getCopiedSourceFile(String jsonFileName) throws IOException {
        File destFile = new File(System.getProperty("java.io.tmpdir"), jsonFileName);
        String sourceLocation = indexConfig.getStudiesInputFile();
        if (isNotBlank(sourceLocation)) {
            if (jsonFileName != null && !jsonFileName.isEmpty()) {
                sourceLocation = sourceLocation.replaceAll(SUBMISSIONS_JSON, jsonFileName);
            }
            File srcFile = new File(sourceLocation);
            logger.info("Making a local copy  of {} at {}", srcFile.getAbsolutePath(), destFile.getAbsolutePath());
            com.google.common.io.Files.copy(srcFile, destFile);
        }
        return destFile.getAbsolutePath();
    }

    @Override
    public void destroy() {

    }

    @Async
    public void processFileForIndexing() {
        logger.debug("Initializing File Queue for Indexing");
        while (true) {
            String filename = null;
            String inputStudiesFilePath = null;
            try {
                logger.debug("Waiting for index file");
                filename = indexFileQueue.take();
                logger.log(Level.INFO, "Started indexing {}. {} files left in the queue.", filename, indexFileQueue.size());
                boolean removeFileDocuments = true;
                if (indexManager.getIndexWriter() == null || !indexManager.getIndexWriter().isOpen()) {
                    logger.log(Level.INFO, "IndexWriter was closed trying to construct a new IndexWriter");
                    indexManager.refreshIndexWriterAndWholeOtherIndices();
                    indexManagementService.openEfoIndexAndLoadOntology();
                    Thread.sleep(30000);
                    indexFileQueue.put(filename);
                    continue;
                }
                if (filename == null || filename.isEmpty() || filename.equalsIgnoreCase(Constants.SUBMISSIONS_JSON) || filename.equalsIgnoreCase("default")) {
                    indexManagementService.closeWebsocket();
                    clearIndex(false);
                    filename = Constants.SUBMISSIONS_JSON;
                    removeFileDocuments = false;
                }
                inputStudiesFilePath = getCopiedSourceFile(filename);
                logger.info("loading view count file");
                viewCountLoader.loadViewCountFile();
                logger.info("loading view count file finished with {} entries!", ViewCountLoader.getViewCountMap().size());
                indexAll(new FileInputStream(inputStudiesFilePath), removeFileDocuments);
                logger.info("freeing view count map memory!");
                ViewCountLoader.unloadViewCountMap();
            } catch (Exception e) {
                e.printStackTrace();
                logger.log(Level.ERROR, e);
            } finally {
                indexManagementService.openWebsocket();
                try {
                    Files.delete(Paths.get(inputStudiesFilePath));
                } catch (Throwable e) {
                    logger.error("Cannot delete {}", inputStudiesFilePath);
                }
            }
            logger.log(Level.INFO, "Finished indexing {}", filename);
        }
    }

    public BlockingQueue<String> getIndexFileQueue() {
        return indexFileQueue;
    }

    public static class JsonDocumentIndexer implements Runnable {
        private final Logger logger = LogManager.getLogger(JsonDocumentIndexer.class.getName());

        private final IndexWriter writer;
        private final JsonNode json;
        private final TaxonomyManager taxonomyManager;
        private final IndexManager indexManager;
        private final FileIndexService fileIndexService;
        private final boolean removeFileDocuments;
        private final ParserManager parserManager;

        public JsonDocumentIndexer(JsonNode json, TaxonomyManager taxonomyManager, IndexManager indexManager, FileIndexService fileIndexService, boolean removeFileDocuments, ParserManager parserManager) {
            this.writer = indexManager.getIndexWriter();
            this.json = json;
            this.taxonomyManager = taxonomyManager;
            this.indexManager = indexManager;
            this.removeFileDocuments = removeFileDocuments;
            this.parserManager = parserManager;
            this.fileIndexService = fileIndexService;
        }

        @Override
        public void run() {
            Map<String, Object> valueMap = new HashMap<>();
            String accession = "";
            try {
                ReadContext jsonPathContext = JsonPath.parse(json.toString());
                accession = parserManager.getParser(Fields.ACCESSION).parse(valueMap, json, jsonPathContext);
                parserManager.getParser(Fields.SECRET_KEY).parse(valueMap, json, jsonPathContext);

                for (JsonNode fieldMetadataNode : indexManager.getIndexDetails().findValue(PUBLIC)) {//parsing common "public" facet and fields
                    AbstractParser abstractParser = parserManager.getParser(fieldMetadataNode.get("name").asText());
                    abstractParser.parse(valueMap, json, jsonPathContext);
                }
                //collections do not need more parsing
                if (valueMap.getOrDefault(Fields.TYPE, "").toString().equalsIgnoreCase("collection")) {
                    addCollectionToHierarchy(valueMap, accession);
                    updateDocument(valueMap);
                    return;
                }

                // remove repeating collections
                Set<String> collectionFacets = new HashSet<>();
                if (valueMap.containsKey(Facets.COLLECTION)) {
                    collectionFacets.addAll(Arrays.asList(valueMap.get(Facets.COLLECTION).toString().toLowerCase().split("\\" + Facets.DELIMITER)));
                    collectionFacets.remove("");
                    collectionFacets.remove(PUBLIC);
                    valueMap.put(Facets.COLLECTION, String.join(Facets.DELIMITER, collectionFacets));
                }

                for (String collectionName : collectionFacets) {
                    JsonNode collectionSpecificFields = indexManager.getIndexDetails().findValue(collectionName);
                    if (collectionSpecificFields != null) {
                        for (JsonNode fieldMetadataNode : collectionSpecificFields) {//parsing collection's facet and fields
                            AbstractParser abstractParser = parserManager.getParser(fieldMetadataNode.get("name").asText());
                            abstractParser.parse(valueMap, json, jsonPathContext);
                        }
                    }
                }
                Set<String> columnSet = new LinkedHashSet<>();

                Map<String, Object> fileValueMap = fileIndexService.indexSubmissionFiles((String) valueMap.get(Fields.ACCESSION),
                        (String) valueMap.get(Fields.RELATIVE_PATH), json, indexManager.getFileIndexWriter(), columnSet, removeFileDocuments);
                if (fileValueMap != null) {
                    valueMap.putAll(fileValueMap);
                    appendFileAttsToContent(valueMap);
                }
                valueMap.put(Constants.File.FILE_ATTS, columnSet);
                updateDocument(valueMap);

            } catch (Exception ex) {
                logger.debug("problem in parser for parsing accession: {}!", accession, ex);
            }
        }

        private void appendFileAttsToContent(Map<String, Object> valueMap) {
            StringBuilder content = new StringBuilder(valueMap.get(Fields.CONTENT).toString());
            content.append(" ").append(valueMap.get(FILE_ATT_KEY_VALUE).toString());
            valueMap.put(Fields.CONTENT, content.toString());
        }

        private void addCollectionToHierarchy(Map<String, Object> valueMap, String accession) {
            Object parent = valueMap.getOrDefault(Facets.COLLECTION, null);
            //TODO: Start - Remove this when backend supports subcollections
            if (accession.equalsIgnoreCase("JCB") || accession.equalsIgnoreCase("BioImages-EMPIAR")) {
                parent = "BioImages";
            }
            //TODO: End - Remove this when backend supports subcollections
            if (parent == null || StringUtils.isEmpty(parent.toString())) {
                indexManager.unsetCollectionParent(accession);
            } else {
                indexManager.setSubCollection(parent.toString(), accession);
            }
        }

        private void updateDocument(Map<String, Object> valueMap) throws IOException {
            Document doc = new Document();

            //TODO: replace by classes if possible
            String value;
            String prjName = (String) valueMap.get(Facets.COLLECTION);
            //updateCollectionParents(valueMap);
            addFileAttributes(doc, (Set<String>) valueMap.get(Constants.File.FILE_ATTS));
            for (String field : indexManager.getCollectionRelatedFields(prjName.toLowerCase())) {
                JsonNode curNode = indexManager.getIndexEntryMap().get(field);
                String fieldType = curNode.get(IndexEntryAttributes.FIELD_TYPE).asText();
                try {
                    switch (fieldType) {
                        case IndexEntryAttributes.FieldTypeValues.TOKENIZED_STRING:
                            value = String.valueOf(valueMap.get(field));
                            doc.add(new TextField(String.valueOf(field), value, Field.Store.YES));
                            break;
                        case IndexEntryAttributes.FieldTypeValues.UNTOKENIZED_STRING:
                            if (!valueMap.containsKey(field)) break;
                            value = String.valueOf(valueMap.get(field));
                            Field unTokenizeField = new Field(String.valueOf(field), value, TYPE_NOT_ANALYZED);
                            doc.add(unTokenizeField);
                            if (curNode.has(IndexEntryAttributes.SORTABLE) && curNode.get(IndexEntryAttributes.SORTABLE).asBoolean(false))
                                doc.add(new SortedDocValuesField(String.valueOf(field), new BytesRef(valueMap.get(field).toString())));
                            break;
                        case IndexEntryAttributes.FieldTypeValues.LONG:
                            if (!valueMap.containsKey(field) || valueMap.get(field) == null || StringUtils.isEmpty(valueMap.get(field).toString()))
                                break;
                            doc.add(new SortedNumericDocValuesField(String.valueOf(field), (Long) valueMap.get(field)));
                            doc.add(new StoredField(String.valueOf(field), valueMap.get(field).toString()));
                            doc.add(new LongPoint(String.valueOf(field), (Long) valueMap.get(field)));
                            break;
                        case IndexEntryAttributes.FieldTypeValues.FACET:
                            addFacet(valueMap.containsKey(field) && valueMap.get(field) != null ?
                                    String.valueOf(valueMap.get(field)) : null, field, doc, curNode);
                    }
                } catch (Exception ex) {
                    logger.error("field name: {} doc accession: {}", field, String.valueOf(valueMap.get(Fields.ACCESSION)), ex);
                }


            }

            Document facetedDocument = taxonomyManager.getFacetsConfig().build(indexManager.getFacetWriter(), doc);
            writer.updateDocument(new Term(Fields.ID, valueMap.get(Fields.ACCESSION).toString()), facetedDocument);

        }

        /*private void updateCollectionParents(Map<String, Object> valueMap) {
            String collection = valueMap.getOrDefault (Facets.PROJECT, "").toString();
            if (StringUtils.isEmpty(collection)) return;
            Map<String, String> collectionParentMap = indexManager.getCollectionParentMap();
            List<String> parents = new ArrayList<>();
            parents.add(collection);
            while (collectionParentMap.containsKey(collection)) {
                String parent = collectionParentMap.get(collection);
                parents.add(parent);
                collection = parent;
            }
            valueMap.put(Facets.PROJECT, StringUtils.join(parents,Facets.DELIMITER));
        }*/

        private void addFileAttributes(Document doc, Set<String> columnAtts) {
            StringBuilder allAtts = new StringBuilder("Name|Size|");
            if (columnAtts == null)
                columnAtts = new HashSet<>();
            for (String att : columnAtts)
                allAtts.append(att).append("|");
            doc.add(new StringField(Constants.File.FILE_ATTS, allAtts.toString(), Field.Store.YES));
        }

        private void addFacet(String value, String fieldName, Document doc, JsonNode facetConfig) {
            if (value == null || value.isEmpty()) {
                if (fieldName.equalsIgnoreCase(Facets.FILE_TYPE) || fieldName.equalsIgnoreCase(Facets.LINK_TYPE)
                        || (facetConfig.has(IndexEntryAttributes.FACET_TYPE) && facetConfig.get(IndexEntryAttributes.FACET_TYPE).asText().equalsIgnoreCase("boolean"))
                )
                    return;
                else
                    value = NA;
            }
            for (String subVal : org.apache.commons.lang3.StringUtils.split(value, Facets.DELIMITER)) {
                if (subVal.equalsIgnoreCase(NA) && facetConfig.has(IndexEntryAttributes.DEFAULT_VALUE)) {
                    subVal = facetConfig.get(IndexEntryAttributes.DEFAULT_VALUE).textValue();
                }
                doc.add(new FacetField(fieldName, subVal.trim().toLowerCase()));
            }
        }
    }

}
