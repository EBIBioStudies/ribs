package uk.ac.ebi.biostudies.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.analyzer.LowercaseAnalyzer;
import uk.ac.ebi.biostudies.api.util.analyzer.AnalyzerManager;
import uk.ac.ebi.biostudies.api.util.parser.ParserManager;
import uk.ac.ebi.biostudies.efo.Autocompletion;
import uk.ac.ebi.biostudies.service.IndexService;
import uk.ac.ebi.biostudies.service.impl.efo.Ontology;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by ehsan on 27/02/2017.
 */
@Component
@Scope("singleton")
public class IndexManager implements InitializingBean, DisposableBean {

    private Logger logger = LogManager.getLogger(IndexManager.class.getName());
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;
    private IndexWriter indexWriter;
    private Directory indexDirectory;
    private IndexWriterConfig indexWriterConfig;
    private Directory efoIndexDirectory;
    private IndexReader efoIndexReader;
    private IndexSearcher efoIndexSearcher;
    private IndexWriter efoIndexWriter;
    private Map<String, JsonNode> indexEntryMap = new LinkedHashMap<>();
    private Map<String, Set<String>> collectionRelatedFields = new LinkedHashMap<>();
    private SpellChecker spellChecker;
    private JsonNode indexDetails;
    private Map<String, List<String>> subCollectionMap = new LinkedHashMap<>();

    @Autowired
    IndexConfig indexConfig;
    @Autowired
    EFOConfig eFOConfig;
    @Autowired
    Ontology ontology;
    @Autowired
    TaxonomyManager taxonomyManager;
    @Autowired
    AnalyzerManager analyzerManager;
    @Autowired
    Autocompletion autocompletion;
    @Autowired
    IndexService indexService;
    @Autowired
    ParserManager parserManager;

    @Override
    public void afterPropertiesSet() { logger.debug("Initializing IndexManager");
        refreshIndexWriterAndWholeOtherIndices();
        indexService.processFileForIndexing();
    }

    public void refreshIndexWriterAndWholeOtherIndices(){
        InputStream indexJsonFile = this.getClass().getClassLoader().getResourceAsStream("collection-fields.json");
        indexDetails = readJson(indexJsonFile);
        fillAllFields();
        analyzerManager.init(indexEntryMap);
        parserManager.init(indexEntryMap);
        String indexDir = indexConfig.getIndexDirectory();
        try {
            //TODO: Start - Remove this when backend supports subcollections
            setSubCollection("BioImages","JCB" );
            setSubCollection("BioImages", "BioImages-EMPIAR");
            //TODO: End - Remove this when backend supports subcollections
            indexDirectory = FSDirectory.open(Paths.get(indexDir));
            indexWriterConfig = new IndexWriterConfig(analyzerManager.getPerFieldAnalyzerWrapper());
            getIndexWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            indexWriter = new IndexWriter(getIndexDirectory(), getIndexWriterConfig());
            indexReader = DirectoryReader.open(indexWriter);
            indexSearcher = new IndexSearcher(getIndexReader());
            IndexWriterConfig efoIndexWriterConfig = new IndexWriterConfig(new LowercaseAnalyzer());
            efoIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            efoIndexDirectory = FSDirectory.open(Paths.get(eFOConfig.getIndexLocation()));
            loadEFO(efoIndexWriterConfig);
            if(efoIndexWriter==null)
                efoIndexWriter = new IndexWriter(getEfoIndexDirectory(), efoIndexWriterConfig);
            efoIndexReader = DirectoryReader.open(efoIndexWriter);
            efoIndexSearcher = new IndexSearcher(getEfoIndexReader());
            taxonomyManager.init(indexEntryMap.values());
            //autocompletion.rebuild();
            spellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexConfig.getSpellcheckerLocation())));
            spellChecker.indexDictionary(new LuceneDictionary(getIndexReader(), Constants.Fields.CONTENT), new IndexWriterConfig(), false);
        }catch (Throwable error){
            logger.error("Problem in reading lucene indices",error);
        }
    }


    private void loadEFO(IndexWriterConfig efoIndexWriterConfig) throws Exception{
        if(!DirectoryReader.indexExists(efoIndexDirectory)){
            try (InputStream resourceInputStream = (new ClassPathResource(eFOConfig.getLocalOwlFilename())).getInputStream()){
                efoIndexWriter = new IndexWriter(getEfoIndexDirectory(), efoIndexWriterConfig);
                ontology.update(resourceInputStream);
                logger.info("EFO loading completed");
            } catch (Exception ex){
                logger.error("EFO file not found", ex);
            }
        }
    }

    public void destroy(){

    }

    private JsonNode readJson(InputStream inp){
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode actualObj = mapper.readTree(inp);
            return actualObj;
        } catch (IOException e) {
            logger.error("problem in reading projec-field.json", e);
        }
        return null;
    }

    public void refreshIndexSearcherAndReader(){

        try {
            indexReader.close();
            indexReader = DirectoryReader.open(indexWriter);
            indexSearcher = new IndexSearcher(indexReader);
        }
        catch (Exception ex){
            logger.error("problem in refreshing index", ex);
        }
    }

    public Map<String, JsonNode> getIndexEntryMap(){
        return indexEntryMap;
    }

    private void fillAllFields(){
        Iterator<String> fieldNames = indexDetails.fieldNames();
        while(fieldNames.hasNext()){
            String key = fieldNames.next();
            Set<String> curPrjRelatedFields = new LinkedHashSet<>();
            JsonNode curFieldsArray = indexDetails.get(key);
            for(JsonNode curField:curFieldsArray){
                indexEntryMap.put(curField.get("name").asText(), curField);
                curPrjRelatedFields.add(curField.get("name").asText());
            }
            collectionRelatedFields.put(key, curPrjRelatedFields);
        }
        collectionRelatedFields.keySet().forEach(s -> {
            if (s.equalsIgnoreCase(Constants.PUBLIC)) return;
            collectionRelatedFields.get(s).addAll(collectionRelatedFields.get(Constants.PUBLIC));
        });

    }


    public Set<String> getCollectionRelatedFields(String prjName){
        if(!collectionRelatedFields.containsKey(prjName))
            return collectionRelatedFields.get(Constants.PUBLIC);
        else
            return collectionRelatedFields.get(prjName);
    }

    public IndexReader getIndexReader() {
        return indexReader;
    }

    public IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }

    public IndexWriter getIndexWriter() {
        return indexWriter;
    }

    public Directory getIndexDirectory() {
        return indexDirectory;
    }

    public IndexWriterConfig getIndexWriterConfig() {
        return indexWriterConfig;
    }

    public Directory getEfoIndexDirectory() {
        return efoIndexDirectory;
    }

    public IndexReader getEfoIndexReader() {
        return efoIndexReader;
    }

    public IndexSearcher getEfoIndexSearcher() {
        return efoIndexSearcher;
    }

    public IndexWriter getEfoIndexWriter() {
        return efoIndexWriter;
    }

    public SpellChecker getSpellChecker() {
        return spellChecker;
    }

    public JsonNode getIndexDetails() {
        return indexDetails;
    }

    public Map<String, List<String>> getSubCollectionMap() {
        return subCollectionMap;
    }

    public void setSubCollection(String parent, String subcollection) {
        parent = parent.toLowerCase();
        if (!subCollectionMap.containsKey(parent)) {
            subCollectionMap.put(parent, Lists.newArrayList(subcollection));
        } else {
            subCollectionMap.get(parent).add(subcollection);
        }
    }

    public void unsetCollectionParent(String collection) {
        final String lowerCaseCollection = collection.toLowerCase();
        subCollectionMap.entrySet().stream().filter(entry -> entry.getValue().contains(lowerCaseCollection))
                .forEach(entry -> {
                    entry.getValue().remove(lowerCaseCollection);
                    if (entry.getValue().size() == 0) {
                        subCollectionMap.remove(entry.getKey());
                    }
                });
    }

}
