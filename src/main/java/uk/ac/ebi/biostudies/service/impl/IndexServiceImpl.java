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
import org.apache.xpath.operations.Bool;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
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

import java.io.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.ac.ebi.biostudies.api.util.Constants.*;

/**
 * Created by ehsan on 27/02/2017.
 */


@Service
@Scope("singleton")

public class IndexServiceImpl implements IndexService {

    public static final FieldType TYPE_NOT_ANALYZED = new FieldType();
    static {
        TYPE_NOT_ANALYZED.setIndexOptions(IndexOptions.DOCS);
        TYPE_NOT_ANALYZED.setTokenized(false);
        TYPE_NOT_ANALYZED.setStored(true);
    }

    public static AtomicInteger ActiveExecutorService = new AtomicInteger(0);

    private Logger logger = LogManager.getLogger(IndexServiceImpl.class.getName());

    private static  BlockingQueue<String> indexFileQueue;

    private static AtomicBoolean closed = new AtomicBoolean(false);

    @Autowired
    IndexConfig indexConfig;

    @Autowired
    IndexManager indexManager;

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
    private RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @Override
    public void afterPropertiesSet() {
        indexFileQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public synchronized boolean isClosed() {
        return closed.get();
    }

    @Override
    public synchronized void close() {
        rabbitListenerEndpointRegistry.getListenerContainer(PartialUpdateListener.PARTIAL_UPDATE_LISTENER).stop();
        closed.compareAndSet(false, true);
    }

    @Override
    public synchronized boolean open() {
        rabbitListenerEndpointRegistry.getListenerContainer(PartialUpdateListener.PARTIAL_UPDATE_LISTENER).start();
        return closed.compareAndSet(true, false);
    }

    @Override
    public void indexAll(InputStream inputStream, boolean removeFileDocuments) throws IOException {

        Long startTime = System.currentTimeMillis();
        ExecutorService executorService = new ThreadPoolExecutor(indexConfig.getThreadCount(), indexConfig.getThreadCount(),
                60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(indexConfig.getQueueSize()), new ThreadPoolExecutor.CallerRunsPolicy());
        ActiveExecutorService.incrementAndGet();
        int counter = 0;
        try (InputStreamReader inputStreamReader = new InputStreamReader( inputStream , "UTF-8")) {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(inputStreamReader);


            JsonToken token = parser.nextToken();
            while (!JsonToken.START_ARRAY.equals(token)) {
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
                if(++counter % 10000==0) {
                    logger.info("{} docs indexed", counter);
                }
            }
            Map<String,String> commitData = new HashMap<>();
            while(token!=JsonToken.END_OBJECT){
                if(token.name().equalsIgnoreCase("field_name")) {
                    String key = parser.getText();
                    token = parser.nextToken();
                    commitData.put(key,token.isNumeric() ? Long.toString(parser.getLongValue()) : parser.getText());

                }
                token = parser.nextToken();
            }

            indexManager.getIndexWriter().setLiveCommitData(commitData.entrySet());
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.HOURS);
            taxonomyManager.commitTaxonomy();
            indexManager.getIndexWriter().commit();
            indexManager.refreshIndexSearcherAndReader();
            taxonomyManager.refreshTaxonomyReader();
            logger.info("Indexing lasted {} seconds", (System.currentTimeMillis()-startTime)/1000);
            ActiveExecutorService.decrementAndGet();
            searchService.clearStatsCache();
        }
        catch (Throwable error){
            logger.error("problem in parsing partial update", error);
        } finally {
            //logger.debug("Deleting temp file {}", inputStudiesFilePath);
            //Files.delete(Paths.get(inputStudiesFilePath));
        }
    }


    @Override
    public void indexOne(JsonNode submission, boolean removeFileDocuments) throws IOException {

        Long startTime = System.currentTimeMillis();
        ExecutorService executorService = new ThreadPoolExecutor(indexConfig.getThreadCount(), indexConfig.getThreadCount(),
                60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(indexConfig.getQueueSize()), new ThreadPoolExecutor.CallerRunsPolicy());
        ActiveExecutorService.incrementAndGet();
        executorService.execute(new JsonDocumentIndexer(submission, taxonomyManager, indexManager, fileIndexService, removeFileDocuments, parserManager));
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.HOURS);
            taxonomyManager.commitTaxonomy();
            indexManager.getIndexWriter().commit();
            indexManager.refreshIndexSearcherAndReader();
            taxonomyManager.refreshTaxonomyReader();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Indexing lasted {} seconds", (System.currentTimeMillis()-startTime)/1000);
        ActiveExecutorService.decrementAndGet();
        searchService.clearStatsCache();
    }

    @Override
    public void deleteDoc(String accession) throws Exception{
        if(accession==null || accession.isEmpty())
            return;
        QueryParser parser = new QueryParser(Fields.ACCESSION, new AttributeFieldAnalyzer());
        String strquery = Fields.ACCESSION+":"+accession;
        parser.setSplitOnWhitespace(true);
        Query query = parser.parse(strquery);
        indexManager.getIndexWriter().deleteDocuments(query);
        indexManager.getIndexWriter().commit();
        indexManager.refreshIndexSearcherAndReader();
        searchService.clearStatsCache();
    }

    @Override
    public void clearIndex(boolean commit) throws IOException {
        indexManager.getIndexWriter().deleteAll();
        indexManager.getIndexWriter().forceMergeDeletes();
        if(commit) {
            indexManager.getIndexWriter().commit();
            indexManager.refreshIndexSearcherAndReader();
        }
        taxonomyManager.resetTaxonomyWriter();
        searchService.clearStatsCache();
    }

    public synchronized String getCopiedSourceFile(String jsonFileName) throws IOException {
        File destFile = new File(System.getProperty("java.io.tmpdir"), jsonFileName);
        String sourceLocation = indexConfig.getStudiesInputFile();
        if (isNotBlank(sourceLocation)) {
            if (jsonFileName != null && !jsonFileName.isEmpty()) {
                sourceLocation = sourceLocation.replaceAll(STUDIES_JSON_FILE, jsonFileName);
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

    public static class JsonDocumentIndexer implements Runnable {
        private Logger logger = LogManager.getLogger(JsonDocumentIndexer.class.getName());

        private IndexWriter writer;
        private JsonNode json;
        private TaxonomyManager taxonomyManager;
        private IndexManager indexManager;
        private FileIndexService fileIndexService;
        private boolean removeFileDocuments;
        private ParserManager parserManager;

        public JsonDocumentIndexer(JsonNode json,TaxonomyManager taxonomyManager, IndexManager indexManager, FileIndexService fileIndexService, boolean removeFileDocuments, ParserManager parserManager) {
            this.writer = indexManager.getIndexWriter();
            this.json = json;
            this.taxonomyManager = taxonomyManager;
            this.indexManager = indexManager;
            this.removeFileDocuments = removeFileDocuments;
            this.parserManager = parserManager;
            this.fileIndexService = fileIndexService;
        }

        @Override
        public void run(){
            Map<String, Object> valueMap = new HashMap<>();
            String accession="";
            try {
                ReadContext jsonPathContext = JsonPath.parse(json.toString());
                accession = parserManager.getParser(Fields.ACCESSION).parse(valueMap, json, jsonPathContext);
                parserManager.getParser(Fields.SECRET_KEY).parse(valueMap, json, jsonPathContext);

                for(JsonNode fieldMetadataNode:indexManager.getIndexDetails().findValue(PUBLIC)){//parsing common "public" facet and fields
                    AbstractParser abstractParser = parserManager.getParser(fieldMetadataNode.get("name").asText());
                    abstractParser.parse(valueMap, json, jsonPathContext);
                }
                //collections do not need more parsing
                if(valueMap.getOrDefault(Fields.TYPE,"").toString().equalsIgnoreCase("collection"))
                {
                    addCollectionToHierarchy(valueMap, accession);
                    updateDocument(valueMap);
                    return;
                }

                // remove repeating collections
                Set<String> collectionFacets = new HashSet<>();
                if (valueMap.containsKey(Facets.PROJECT)) {
                    collectionFacets.addAll(Arrays.asList(valueMap.get(Facets.PROJECT).toString().toLowerCase().split("\\" + Facets.DELIMITER)));
                    //TODO: Remvoe this when collection field is okay
                    collectionFacets.addAll(Arrays.stream(valueMap.get(Fields.ACCESS).toString().toLowerCase().split(" ")).filter(f -> !f.contains("@") && !f.startsWith("#") && !f.equalsIgnoreCase(PUBLIC)).map( f-> f.replaceAll("\\d","")).collect(Collectors.toList()));
                    collectionFacets.remove("");
                    valueMap.put(Facets.PROJECT, String.join(Facets.DELIMITER, collectionFacets));
                }

                for (String collectionName:  collectionFacets ) {
                    JsonNode collectionSpecificFields = indexManager.getIndexDetails().findValue(collectionName);
                    if(collectionSpecificFields != null) {
                        for (JsonNode fieldMetadataNode : collectionSpecificFields) {//parsing collection's facet and fields
                            AbstractParser abstractParser = parserManager.getParser(fieldMetadataNode.get("name").asText());
                            abstractParser.parse(valueMap, json, jsonPathContext);
                        }
                    }
                }
                Set<String> columnSet = new LinkedHashSet<>();

                Map<String, Object> fileValueMap = fileIndexService.indexSubmissionFiles((String) valueMap.get(Fields.ACCESSION), (String) valueMap.get(Fields.RELATIVE_PATH), json, writer, columnSet, removeFileDocuments);
                if (fileValueMap!=null) {
                    valueMap.putAll(fileValueMap);
                }

                valueMap.put(Constants.File.FILE_ATTS, columnSet);
                updateDocument(valueMap);

            }catch (Exception ex){
                logger.debug("problem in parser for parsing accession: {}!", accession, ex);
            }
        }

        private void addCollectionToHierarchy(Map<String, Object> valueMap, String accession) {
            Object parent =valueMap.getOrDefault (Facets.PROJECT, null);
            //TODO: Start - Remove this when backend supports subcollections
            if (accession.equalsIgnoreCase("JCB") || accession.equalsIgnoreCase("BioImages-EMPIAR")) {
                parent="BioImages";
            }
            //TODO: End - Remove this when backend supports subcollections
            if (parent==null || StringUtils.isEmpty(parent.toString())) {
                indexManager.unsetCollectionParent(accession);
            } else {
                indexManager.setSubCollection(parent.toString(), accession);
            }
        }

        private void updateDocument(Map<String, Object> valueMap) throws IOException {
            Document doc = new Document();

            //TODO: replace by classes if possible
            String value;
            String prjName = (String)valueMap.get(Facets.PROJECT);
            //updateCollectionParents(valueMap);
            addFileAttributes(doc, (Set<String>) valueMap.get(Constants.File.FILE_ATTS));
            for (String field: indexManager.getCollectionRelatedFields(prjName.toLowerCase())) {
                JsonNode curNode = indexManager.getIndexEntryMap().get(field);
                String fieldType = curNode.get(IndexEntryAttributes.FIELD_TYPE).asText();
                try{
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
                            if(curNode.has (IndexEntryAttributes.SORTABLE) && curNode.get(IndexEntryAttributes.SORTABLE).asBoolean(false))
                                doc.add( new SortedDocValuesField(String.valueOf(field), new BytesRef( valueMap.get(field).toString())));
                            break;
                        case IndexEntryAttributes.FieldTypeValues.LONG:
                            if (!valueMap.containsKey(field) || StringUtils.isEmpty(valueMap.get(field).toString()) ) break;
                            doc.add(new SortedNumericDocValuesField(String.valueOf(field), (Long) valueMap.get(field)));
                            doc.add(new StoredField(String.valueOf(field), valueMap.get(field).toString()));
                            doc.add( new LongPoint(String.valueOf(field), (Long) valueMap.get(field)  ));
                            break;
                        case IndexEntryAttributes.FieldTypeValues.FACET:
                            addFacet(valueMap.containsKey(field) && valueMap.get(field)!=null ?
                                    String.valueOf(valueMap.get(field)) : null, field, doc, curNode);
                    }
                }catch(Exception ex){
                    logger.error("field name: {} doc accession: {}", field.toString(), String.valueOf(valueMap.get(Fields.ACCESSION)), ex);
                }


            }

            Document facetedDocument = taxonomyManager.getFacetsConfig().build(taxonomyManager.getTaxonomyWriter() ,doc);
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

        private void addFileAttributes(Document doc, Set<String> columnAtts){
            StringBuilder allAtts = new StringBuilder("Name|Size|");
            if(columnAtts==null)
                columnAtts = new HashSet<>();
            for(String att:columnAtts)
                allAtts.append(att).append("|");
            doc.add(new StringField(Constants.File.FILE_ATTS, allAtts.toString(),Field.Store.YES));
        }

        private void addFacet(String value, String fieldName, Document doc, JsonNode facetConfig){
            if(value==null || value.isEmpty()){
                if(fieldName.equalsIgnoreCase(Facets.FILE_TYPE) || fieldName.equalsIgnoreCase(Facets.LINK_TYPE)
                        || (facetConfig.has(IndexEntryAttributes.FACET_TYPE) && facetConfig.get(IndexEntryAttributes.FACET_TYPE).asText().equalsIgnoreCase("boolean"))
                )
                    return;
                else
                    value = NA;
            }
            for(String subVal: org.apache.commons.lang3.StringUtils.split(value, Facets.DELIMITER)) {
                if(subVal.equalsIgnoreCase(NA) && facetConfig.has(IndexEntryAttributes.DEFAULT_VALUE)){
                    subVal = facetConfig.get(IndexEntryAttributes.DEFAULT_VALUE).textValue();
                }
                doc.add(new FacetField(fieldName, subVal.trim().toLowerCase()));
            }
        }
    }

    @Async
    public void processFileForIndexing() {
        logger.debug("Initializing File Queue for Indexing");
        while (true) {
            String filename = null;
            try {
                filename = indexFileQueue.take();
                logger.log(Level.INFO, "Started indexing {}. {} files left in the queue.", filename, indexFileQueue.size());
                boolean removeFileDocuments = true;
                if(indexManager.getIndexWriter()==null || !indexManager.getIndexWriter().isOpen()){
                    logger.log(Level.INFO,"IndexWriter was closed trying to construct a new IndexWriter");
                    indexManager.refreshIndexWriterAndWholeOtherIndices();
                    Thread.sleep(30000);
                    indexFileQueue.put(filename);
                    continue;
                }
                if (filename == null || filename.isEmpty() || filename.equalsIgnoreCase(Constants.STUDIES_JSON_FILE) || filename.equalsIgnoreCase("default"))  {
                    clearIndex(false);
                    filename = Constants.STUDIES_JSON_FILE;
                    removeFileDocuments = false;
                }
                String inputStudiesFilePath = getCopiedSourceFile(filename);
                indexAll(new FileInputStream(inputStudiesFilePath), removeFileDocuments);
            } catch (Exception e) {
                e.printStackTrace();
                logger.log(Level.ERROR, e);
            }
            logger.log(Level.INFO, "Finished indexing {}", filename);
        }
    }

    public BlockingQueue<String> getIndexFileQueue() {
        return indexFileQueue;
    }

}
