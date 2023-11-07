package uk.ac.ebi.biostudies.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.SnapshotAwareDirectoryTaxonomyWriter;
import uk.ac.ebi.biostudies.api.util.analyzer.AnalyzerManager;
import uk.ac.ebi.biostudies.api.util.analyzer.LowercaseAnalyzer;
import uk.ac.ebi.biostudies.api.util.parser.ParserManager;
import uk.ac.ebi.biostudies.service.impl.IndexTransferer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Created by ehsan on 27/02/2017.
 */
@Component
@Scope("singleton")
public class IndexManager implements DisposableBean {

    private final Logger logger = LogManager.getLogger(IndexManager.class.getName());
    private final Map<String, JsonNode> indexEntryMap = new LinkedHashMap<>();
    private final Map<String, Set<String>> collectionRelatedFields = new LinkedHashMap<>();
    private final Map<String, List<String>> subCollectionMap = new LinkedHashMap<>();
    private final Set<String> privateFields = new HashSet<>();
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    EFOConfig eFOConfig;
    @Autowired
    TaxonomyManager taxonomyManager;
    @Autowired
    AnalyzerManager analyzerManager;
    @Autowired
    ParserManager parserManager;
    @Autowired
    IndexTransferer indexTransferer;
    private IndexReader searchIndexReader;
    private IndexReader fileIndexReader;
    private IndexReader extractedLinkIndexReader;
    private IndexReader pagetabIndexReader;
    private IndexSearcher indexSearcher;
    private IndexSearcher fileIndexSearcher;
    private IndexSearcher extractedLinkIndexSearcher;
    private IndexSearcher pagetabIndexSearcher;
    private IndexWriter searchIndexWriter;
    private IndexWriter fileIndexWriter;
    private IndexWriter extractedLinkIndexWriter;
    private IndexWriter pagetabIndexWriter;
    private Directory searchIndexDirectory, fileIndexDirectory, extractedLinkIndexDirectory, pagetabDirectory;
    private IndexWriterConfig searchIndexWriterConfig, fileIndexWriterConfig, extractedLinkIndexWriterConfig, pagetabIndexWriterConfig;
    private SnapshotDeletionPolicy mainIndexSnapShot, fileIndexSnapShot, extractedLinkIndexSnapShot, pagetabIndexSnapShot;
    private SnapshotDeletionPolicy efoIndexSnapShot;
    private Directory efoIndexDirectory;
    private IndexReader efoIndexReader;
    private IndexSearcher efoIndexSearcher;
    private IndexWriter efoIndexWriter;
    private SpellChecker spellChecker;
    private JsonNode indexDetails;
    private DrillSideways drillSideways;
    private SnapshotAwareDirectoryTaxonomyWriter facetWriter;
    private TaxonomyReader facetReader;
    private Directory taxoDirectory;

    public void refreshIndexWriterAndWholeOtherIndices() {
        InputStream indexJsonFile = this.getClass().getClassLoader().getResourceAsStream("collection-fields.json");
        indexDetails = readJson(indexJsonFile);
        fillAllFields();
        analyzerManager.init(indexEntryMap);
        parserManager.init(indexEntryMap);
        try {
            //TODO: Start - Remove this when backend supports subcollections
            setSubCollection("BioImages", "JCB");
            setSubCollection("BioImages", "BioImages-EMPIAR");
            //TODO: End - Remove this when backend supports subcollections
            taxonomyManager.init(indexEntryMap.values());
            openIndicesWritersAndSearchers();
            drillSideways = new DrillSideways(indexSearcher, taxonomyManager.getFacetsConfig(), facetReader);
        } catch (Throwable error) {
            logger.error("Problem in reading lucene indices", error);
        }
    }

    public void resetTaxonomyWriter() throws IOException {
        try {
            facetWriter.commit();
            facetWriter.close();
        } catch (Exception ex) {
            logger.error(ex);
        }
        facetWriter = new SnapshotAwareDirectoryTaxonomyWriter(taxoDirectory, IndexWriterConfig.OpenMode.CREATE);
    }

    public void openIndicesWritersAndSearchers() {
        try {
            openMainIndex();
            openEfoIndex();
            openFacetIndex();
            if (spellChecker == null) {
                spellChecker = new SpellChecker(FSDirectory.open(Paths.get(indexConfig.getSpellcheckerLocation())));
                spellChecker.indexDictionary(new LuceneDictionary(searchIndexReader, Constants.Fields.CONTENT), new IndexWriterConfig(), false);
            }
        } catch (Throwable error) {
            logger.error(error);
        }

    }

    public void commitIndices() {
        try {
            searchIndexWriter.commit();
            fileIndexWriter.commit();
            extractedLinkIndexWriter.commit();
            efoIndexWriter.commit();
            facetWriter.commit();
            pagetabIndexWriter.commit();
        } catch (Exception ex) {
            logger.error("problem in committing indices", ex);

        }
    }

    public void closeIndices() {
        try {
            searchIndexReader.close();
            fileIndexReader.close();
            extractedLinkIndexReader.close();
            efoIndexReader.close();
            facetReader.close();
            pagetabIndexReader.close();
            searchIndexWriter.close();
            fileIndexWriter.close();
            efoIndexWriter.close();
            facetWriter.close();
            pagetabIndexWriter.close();
        } catch (Exception ex) {
            logger.error("problem in closing indices", ex);
        }
    }

    public void takeIndexSnapShotForBackUp() throws IOException {
        IndexCommit mainSnapShot = null, facetSnapshot = null, efoSnapShot = null, fileSnapShot = null, linkSnapShot = null, pagetabSnapShot = null;
        try {
            mainSnapShot = mainIndexSnapShot.snapshot();
            indexTransferer.copyIndexFromSnapShot(mainSnapShot.getFileNames(), indexConfig.getIndexDirectory(), indexConfig.getIndexBackupDirectory() + "/submission");
            facetSnapshot = facetWriter.getDeletionPolicy().snapshot();
            indexTransferer.copyIndexFromSnapShot(facetSnapshot.getFileNames(), indexConfig.getFacetDirectory(), indexConfig.getIndexBackupDirectory() + "/taxonomy");
            efoSnapShot = efoIndexSnapShot.snapshot();
            indexTransferer.copyIndexFromSnapShot(efoSnapShot.getFileNames(), eFOConfig.getIndexLocation(), indexConfig.getIndexBackupDirectory() + "/efo");
            fileSnapShot = fileIndexSnapShot.snapshot();
            indexTransferer.copyIndexFromSnapShot(fileSnapShot.getFileNames(), indexConfig.getFileIndexDirectory(), indexConfig.getIndexBackupDirectory() + "/files");
            linkSnapShot = extractedLinkIndexSnapShot.snapshot();
            indexTransferer.copyIndexFromSnapShot(linkSnapShot.getFileNames(), indexConfig.getExtractedLinkIndexDirectory(), indexConfig.getIndexBackupDirectory() + "/links");
            pagetabSnapShot = pagetabIndexSnapShot.snapshot();
            indexTransferer.copyIndexFromSnapShot(pagetabSnapShot.getFileNames(), indexConfig.getPageTabDirectory(), indexConfig.getIndexBackupDirectory() + "/pagetab");
        } catch (Exception ex) {
            logger.error("problem in taking snapshot from main index", ex);
            throw ex;
        } finally {
            try {
                if (mainSnapShot != null)
                    mainIndexSnapShot.release(mainSnapShot);
                if (facetSnapshot != null)
                    facetWriter.getDeletionPolicy().release(facetSnapshot);
                if (efoSnapShot != null)
                    efoIndexSnapShot.release(efoSnapShot);
                if (fileSnapShot != null)
                    fileIndexSnapShot.release(fileSnapShot);
                if (linkSnapShot != null)
                    extractedLinkIndexSnapShot.release(linkSnapShot);
                if (pagetabSnapShot != null)
                    pagetabIndexSnapShot.release(pagetabSnapShot);
            } catch (Exception ex) {
                logger.error("problem in releasing snapshot lock", ex);
            }
        }
    }

    public boolean copyBackupToLocal() {
        try {
            indexTransferer.copyIndexFromNetworkFileSystemToLocal(indexConfig.getIndexBackupDirectory() + "/submission", indexConfig.getIndexDirectory());
            indexTransferer.copyIndexFromNetworkFileSystemToLocal(indexConfig.getIndexBackupDirectory() + "/taxonomy", indexConfig.getFacetDirectory());
            indexTransferer.copyIndexFromNetworkFileSystemToLocal(indexConfig.getIndexBackupDirectory() + "/efo", eFOConfig.getIndexLocation());
            indexTransferer.copyIndexFromNetworkFileSystemToLocal(indexConfig.getIndexBackupDirectory() + "/files", indexConfig.getFileIndexDirectory());
            indexTransferer.copyIndexFromNetworkFileSystemToLocal(indexConfig.getIndexBackupDirectory() + "/links", indexConfig.getExtractedLinkIndexDirectory());
            indexTransferer.copyIndexFromNetworkFileSystemToLocal(indexConfig.getIndexBackupDirectory() + "/pagetab", indexConfig.getPageTabDirectory());
        } catch (Exception ex) {
            logger.fatal("problem in copying remote index to local file system, INDEX's STATE IS INVALID", ex);
            return false;
        }
        return true;
    }

    private void openFacetIndex() {
        taxonomyManager.init(indexEntryMap.values());
        boolean shouldRefresh = false;
        try {
            taxoDirectory = FSDirectory.open(Paths.get(indexConfig.getFacetDirectory()));
            if (facetWriter == null || facetWriter.isOpen() == false) {
                facetWriter = new SnapshotAwareDirectoryTaxonomyWriter(taxoDirectory, IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                shouldRefresh = true;
            }
            if (facetReader == null || shouldRefresh)
                facetReader = new DirectoryTaxonomyReader(facetWriter);
        } catch (Throwable e) {
            logger.error("can not create taxonomy writer or reader", e);
        }
    }

    public void openMainIndex() throws Throwable {
        String indexDir = indexConfig.getIndexDirectory();
        searchIndexDirectory = FSDirectory.open(Paths.get(indexDir));
        fileIndexDirectory = FSDirectory.open(Paths.get(indexConfig.getFileIndexDirectory()));
        extractedLinkIndexDirectory = FSDirectory.open(Paths.get(indexConfig.getExtractedLinkIndexDirectory()));
        pagetabDirectory = FSDirectory.open(Paths.get(indexConfig.getPageTabDirectory()));
        searchIndexWriterConfig = new IndexWriterConfig(analyzerManager.getPerFieldAnalyzerWrapper());
        searchIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        fileIndexWriterConfig = new IndexWriterConfig(analyzerManager.getPerFieldAnalyzerWrapper());
        fileIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        extractedLinkIndexWriterConfig = new IndexWriterConfig(analyzerManager.getPerFieldAnalyzerWrapper());
        extractedLinkIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        pagetabIndexWriterConfig = new IndexWriterConfig(analyzerManager.getPerFieldAnalyzerWrapper());
        pagetabIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        mainIndexSnapShot = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        fileIndexSnapShot = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        extractedLinkIndexSnapShot = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        pagetabIndexSnapShot = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        searchIndexWriterConfig.setIndexDeletionPolicy(mainIndexSnapShot);
        fileIndexWriterConfig.setIndexDeletionPolicy(fileIndexSnapShot);
        extractedLinkIndexWriterConfig.setIndexDeletionPolicy(extractedLinkIndexSnapShot);
        pagetabIndexWriterConfig.setIndexDeletionPolicy(pagetabIndexSnapShot);
        if (searchIndexWriter == null || searchIndexWriter.isOpen() == false)
            searchIndexWriter = new IndexWriter(getSearchIndexDirectory(), getSearchIndexWriterConfig());
        if (fileIndexWriter == null || fileIndexWriter.isOpen() == false)
            fileIndexWriter = new IndexWriter(fileIndexDirectory, fileIndexWriterConfig);
        if (extractedLinkIndexWriter == null || extractedLinkIndexWriter.isOpen() == false)
            extractedLinkIndexWriter = new IndexWriter(extractedLinkIndexDirectory, extractedLinkIndexWriterConfig);
        if (pagetabIndexWriter == null || pagetabIndexWriter.isOpen() == false)
            pagetabIndexWriter = new IndexWriter(pagetabDirectory, pagetabIndexWriterConfig);
        if (searchIndexReader != null)
            searchIndexReader.close();
        if (fileIndexReader != null)
            fileIndexReader.close();
        if (extractedLinkIndexReader != null)
            extractedLinkIndexReader.close();
        if (pagetabIndexReader != null)
            pagetabIndexReader.close();
        searchIndexReader = DirectoryReader.open(searchIndexWriter);
        fileIndexReader = DirectoryReader.open(fileIndexWriter);
        extractedLinkIndexReader = DirectoryReader.open(extractedLinkIndexWriter);
        pagetabIndexReader = DirectoryReader.open(pagetabIndexWriter);
        indexSearcher = new IndexSearcher(searchIndexReader);
        fileIndexSearcher = new IndexSearcher(fileIndexReader);
        extractedLinkIndexSearcher = new IndexSearcher(extractedLinkIndexReader);
        pagetabIndexSearcher = new IndexSearcher(pagetabIndexReader);
    }

    public void openEfoIndex() throws Throwable {
        IndexWriterConfig efoIndexWriterConfig = new IndexWriterConfig(new LowercaseAnalyzer());
        efoIndexSnapShot = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        efoIndexWriterConfig.setIndexDeletionPolicy(efoIndexSnapShot);
        efoIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        setEfoIndexDirectory(FSDirectory.open(Paths.get(eFOConfig.getIndexLocation())));
        if (efoIndexWriter == null || efoIndexWriter.isOpen() == false)
            efoIndexWriter = new IndexWriter(getEfoIndexDirectory(), efoIndexWriterConfig);
        if (efoIndexReader != null)
            efoIndexReader.close();
        efoIndexReader = DirectoryReader.open(efoIndexWriter);
        efoIndexSearcher = new IndexSearcher(efoIndexReader);

    }


    public void destroy() {

    }

    private JsonNode readJson(InputStream inp) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode actualObj = mapper.readTree(inp);
            return actualObj;
        } catch (IOException e) {
            logger.error("Problem in reading collection-fields.json", e);
        }
        return null;
    }

    public void refreshIndexSearcherAndReader() {

        try {
            searchIndexReader.close();
            fileIndexReader.close();
            extractedLinkIndexReader.close();
            pagetabIndexReader.close();
            searchIndexReader = DirectoryReader.open(searchIndexWriter);
            fileIndexReader = DirectoryReader.open(fileIndexWriter);
            extractedLinkIndexReader = DirectoryReader.open(extractedLinkIndexWriter);
            pagetabIndexReader = DirectoryReader.open(pagetabIndexWriter);
            indexSearcher = new IndexSearcher(searchIndexReader);
            fileIndexSearcher = new IndexSearcher(fileIndexReader);
            extractedLinkIndexSearcher = new IndexSearcher(extractedLinkIndexReader);
            pagetabIndexSearcher = new IndexSearcher(pagetabIndexReader);
            drillSideways = new DrillSideways(indexSearcher, taxonomyManager.getFacetsConfig(), facetReader);
        } catch (Exception ex) {
            logger.error("Problem in refreshing index", ex);
        }
    }

    public void refreshTaxonomyReader() {
        try {
            if (facetWriter == null || facetWriter.isOpen() == false)
                facetWriter = new SnapshotAwareDirectoryTaxonomyWriter(taxoDirectory, IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            facetReader = new DirectoryTaxonomyReader(facetWriter);
        } catch (IOException e) {
            logger.error("problem in refreshing taxonomy", e);
        }
    }

    public Map<String, JsonNode> getIndexEntryMap() {
        return indexEntryMap;
    }

    public void commitTaxonomy() {
        try {
            facetWriter.commit();
        } catch (IOException e) {
            logger.error("problem in commiting taxonomy writer", e);
        }
    }

    private void fillAllFields() {
        Iterator<String> fieldNames = indexDetails.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            Set<String> curPrjRelatedFields = new LinkedHashSet<>();
            JsonNode curFieldsArray = indexDetails.get(key);
            for (JsonNode curField : curFieldsArray) {
                indexEntryMap.put(curField.get("name").asText(), curField);
                curPrjRelatedFields.add(curField.get("name").asText());
                if (curField.has(Constants.IndexEntryAttributes.PRIVATE) && curField.get(Constants.IndexEntryAttributes.PRIVATE).asBoolean())
                    privateFields.add(curField.get(Constants.IndexEntryAttributes.NAME).asText());
            }
            collectionRelatedFields.put(key, curPrjRelatedFields);
        }
        collectionRelatedFields.keySet().forEach(s -> {
            if (s.equalsIgnoreCase(Constants.PUBLIC)) return;
            collectionRelatedFields.get(s).addAll(collectionRelatedFields.get(Constants.PUBLIC));
        });

    }


    public Set<String> getCollectionRelatedFields(String prjName) {
        if (!collectionRelatedFields.containsKey(prjName))
            return collectionRelatedFields.get(Constants.PUBLIC);
        else
            return collectionRelatedFields.get(prjName);
    }

    public IndexReader getSearchIndexReader() {
        return searchIndexReader;
    }

    public IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }

    public IndexWriter getSearchIndexWriter() {
        return searchIndexWriter;
    }

    public Directory getSearchIndexDirectory() {
        return searchIndexDirectory;
    }

    public IndexWriterConfig getSearchIndexWriterConfig() {
        return searchIndexWriterConfig;
    }

    public Directory getEfoIndexDirectory() {
        return efoIndexDirectory;
    }

    public void setEfoIndexDirectory(Directory efoIndexDirectory) {
        this.efoIndexDirectory = efoIndexDirectory;
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

    public DirectoryTaxonomyWriter getFacetWriter() {
        return facetWriter;
    }

    public TaxonomyReader getFacetReader() {
        return facetReader;
    }

    public DrillSideways getDrillSideways() {
        return drillSideways;
    }

    public Set<String> getPrivateFields() {
        return privateFields;
    }

    public void copyBackupToRemote() throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "-c", indexConfig.getIndexSyncCommand());
        Process process = builder.start();
        Executors.newSingleThreadExecutor().submit(() ->
                new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines()
                        .forEach(s -> logger.debug(s)));
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Error running remote copying script");
        }
    }

    public String getBackUpSyncTime() {
        StringBuilder syncFileAddress = new StringBuilder();
        String fname = indexConfig.getIndexSyncBackupFile();
        if (fname == null || fname.isEmpty()) {
            syncFileAddress.append(indexConfig.getIndexBackupDirectory()).append("/").append(Constants.LATEST_INDEX_SYNC_TIME_FILE);
        } else {
            syncFileAddress.append(fname);
        }
        try {
            Path path = Path.of(syncFileAddress.toString());
            String content = Files.readString(path);
            return content;
        } catch (Exception ex) {
            logger.error("sync file not found");
        }
        return "0";
    }


    public IndexSearcher getFileIndexSearcher() {
        return fileIndexSearcher;
    }

    public IndexWriter getFileIndexWriter() {
        return fileIndexWriter;
    }

    public IndexReader getFileIndexReader() {
        return fileIndexReader;
    }

    public IndexSearcher getExtractedLinkIndexSearcher() {
        return extractedLinkIndexSearcher;
    }

    public IndexWriter getExtractedLinkIndexWriter() {
        return extractedLinkIndexWriter;
    }

    public IndexReader getExtractedLinkIndexReader() {
        return extractedLinkIndexReader;
    }

    public IndexReader getPagetabIndexReader() {
        return pagetabIndexReader;
    }

    public IndexSearcher getPagetabIndexSearcher() {
        return pagetabIndexSearcher;
    }

    public IndexWriter getPagetabIndexWriter() {
        return pagetabIndexWriter;
    }
}
