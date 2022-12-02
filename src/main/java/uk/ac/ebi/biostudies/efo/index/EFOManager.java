package uk.ac.ebi.biostudies.efo.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.EFOConfig;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.efo.EFOLoader;
import uk.ac.ebi.biostudies.efo.EFONode;
import uk.ac.ebi.biostudies.efo.IEFO;
import uk.ac.ebi.biostudies.efo.StringTools;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import static uk.ac.ebi.biostudies.api.util.Constants.OWL;
import static uk.ac.ebi.biostudies.service.impl.IndexServiceImpl.TYPE_NOT_ANALYZED;

@Service
public class EFOManager {
    @Autowired
    IndexManager indexManager;
    @Autowired
    EFOConfig efoConfig;
    @Autowired
    EFOExpanderIndex efoExpanderIndex;

    private IEFO efo;


    private static final Logger LOGGER = LogManager.getLogger(EFOManager.class.getName());

    final static int MAX_LIMIT = 200;


    @PostConstruct
    public void createEfoIndex(){
        try{
            if(indexManager.getEfoIndexDirectory()==null)
                indexManager.openEfoIndex();
            if (!DirectoryReader.indexExists(indexManager.getEfoIndexDirectory())) {
                loadEfo();
                buildIndex(true);
            }
        }catch (Throwable exception){
            LOGGER.error("Problem in creating EFO init index", exception);
        }
    }

    public String getKeywordsPlus(String query, String field, int limit){
        if(limit>MAX_LIMIT)
            limit=MAX_LIMIT;
        int totalHit = 0;
        StringBuilder resultStr = new StringBuilder();
        TopDocs topResult = null;
        try {
            if(field==null || field.isEmpty())
                field=OWL.ALL;
            if(field.equalsIgnoreCase(OWL.ALL) || field.equalsIgnoreCase(OWL.TERM)) {
                QueryParser parser = new QueryParser(OWL.TERM, new KeywordAnalyzer());
                query = modifyQuery(query);
                Query myQuery = parser.parse(query);
                topResult = indexManager.getEfoIndexSearcher().search(myQuery, limit, new Sort(new SortField(OWL.TERM, SortField.Type.STRING, false)));
                totalHit = (int) topResult.totalHits.value;
                serialaizeSearchResult(topResult, resultStr);
            }
            if(field.equalsIgnoreCase(OWL.ALTERNATIVE_TERMS) || field.equalsIgnoreCase(OWL.CONTENT) || (topResult!=null && topResult.totalHits.value<limit && field.equalsIgnoreCase(OWL.ALL)))
                addAlternativeTerms(query, resultStr, limit-totalHit);
        }catch (Exception exception){
            LOGGER.error(exception);
        }
        return resultStr.toString();
    }

    private String modifyQuery(String query){
        if(query.contains("\"") || query.indexOf("AND")>=0 || query.indexOf("OR")>=0 || query.contains("*"))
            return query;

        query = query + "*";
        return  query;
    }

    private void addAlternativeTerms(String strQuery, StringBuilder resultStr, int limit) throws Exception {
        QueryParser parser = new QueryParser(OWL.ALTERNATIVE_TERMS, new KeywordAnalyzer());
        Query query = parser.parse(strQuery);
        TopDocs topResults = indexManager.getEfoIndexSearcher().search(query, limit, new Sort(new SortField(OWL.ALTERNATIVE_TERMS, SortField.Type.STRING, false)));
        for (ScoreDoc scoreDoc : topResults.scoreDocs) {
            Document response = indexManager.getEfoIndexReader().document(scoreDoc.doc);
            resultStr.append(response.get(OWL.ALTERNATIVE_TERMS)).append("|t|content\n");
        }
    }

    public String getEfoTree(String query){
        StringBuilder resultStr = new StringBuilder();
        try {
            TopDocs topResult = indexManager.getEfoIndexSearcher().search(new TermQuery(new Term(OWL.FATHER, query.toLowerCase())), Integer.MAX_VALUE);
            serialaizeSearchResult(topResult, resultStr);
        }catch (Exception exception){
            LOGGER.debug("efotree exception: ", exception);
        }
        return resultStr.toString();
    }

    private void serialaizeSearchResult(TopDocs topResults, StringBuilder resultStr) throws Exception{
        for (ScoreDoc scoreDoc : topResults.scoreDocs)
        {
            Document response = indexManager.getEfoIndexReader().document(scoreDoc.doc);
            resultStr.append(response.get(OWL.TERM)).append("|o|");
            if(response.get(OWL.CHILDRERN)!=null)
                resultStr.append(response.get(OWL.ID));
            resultStr.append("\n");
        }
    }

    public void loadEfo(){
        Long time = System.currentTimeMillis();
        try {
            File efoFile = new File(efoConfig.getOwlFilename());
            if (!efoFile.exists()) {
                String efoBuiltinSource = efoConfig.getLocalOwlFilename();
                try (InputStream is = new ClassPathResource(efoBuiltinSource).getInputStream()) {
                    Files.copy( is, efoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            LOGGER.info("Loading EFO ontology from [{}]", efoFile.getPath());

            try (InputStream ontologyStream = new FileInputStream(efoFile)) {
                this.efo = removeIgnoredClasses(new EFOLoader().load(ontologyStream), efoConfig.getIgnoreListFilename());
                LOGGER.info("EFO loading completed");
            }
        }catch (Exception ex){
            LOGGER.error("Problem in loading EFO! ", ex);
        }
        LOGGER.debug((System.currentTimeMillis()-time)+" milliseconds last to load RDF file");
    }

    public void buildIndex(boolean byForce) throws Throwable{
        if(!byForce && DirectoryReader.indexExists(indexManager.getEfoIndexDirectory()))
            return;
        if(efo==null)
            return;
        IndexWriter indexWriter = indexManager.getEfoIndexWriter();
        indexWriter.deleteAll();
        Set<String>allTermSet = new HashSet<>();
        addNodeAndChildren(this.efo.getMap().get(IEFO.ROOT_ID), indexWriter, allTermSet);
        addMainIndexTerms(indexWriter, allTermSet);
        allTermSet.clear();
        efoExpanderIndex.setEFO(efo);
        efoExpanderIndex.buildIndex(indexWriter);
        indexWriter.commit();
        indexManager.openEfoIndex();
        LOGGER.info("EFO indexing finished!");
    }

    private void addNodeAndChildren(EFONode node, IndexWriter indexWriter, Set<String>allTermSet) {

        if (null != node) {
            addNodeToIndex(node, indexWriter, allTermSet);
            for (EFONode child : node.getChildren()) {
                addNodeAndChildren(child, indexWriter, allTermSet);
            }
        }
    }

    private void addMainIndexTerms(IndexWriter indexWriter, Set<String> allTermSet){
        final int minFreq = 10;
        String termStr;
        try {
            IndexReader reader = indexManager.getIndexReader();
            Terms terms = MultiTerms.getTerms(reader, Constants.Fields.CONTENT);
            if (null != terms) {
                TermsEnum iterator = terms.iterator();
                BytesRef byteRef;
                while ((byteRef = iterator.next()) != null) {
                    termStr = byteRef.utf8ToString();
                    if(termStr.length()<4 || allTermSet.contains(termStr.toLowerCase()))
                        continue;
                    if (iterator.docFreq() >= minFreq) {
                        allTermSet.add(termStr.toLowerCase());
                        Document document = new Document();
                        document.add(new StringField(OWL.ALTERNATIVE_TERMS, termStr.toLowerCase(), Field.Store.NO));
                        document.add(new SortedDocValuesField(OWL.ALTERNATIVE_TERMS, new BytesRef(termStr)));
                        document.add(new StoredField(OWL.ALTERNATIVE_TERMS, termStr));
                        indexWriter.addDocument(document);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("getTerms problem", ex);
        }
    }

    private void addNodeToIndex(EFONode node, IndexWriter indexWriter, Set<String>allTermSet){
        if(allTermSet.contains(node.getTerm()))
            return;
        allTermSet.add(node.getTerm());
        Document document = new Document();
        document.add(new Field(OWL.ID, node.getId().toLowerCase(), TYPE_NOT_ANALYZED));
        if(node.getEfoUri()!=null)
            document.add(new Field(OWL.EFOID, node.getEfoUri().toLowerCase(), TYPE_NOT_ANALYZED));
        if(node.getTerm()!=null) {
            String curTerm = node.getTerm().toLowerCase();
            document.add(new StringField(OWL.TERM, curTerm, Field.Store.NO));
            document.add(new SortedDocValuesField(OWL.TERM, new BytesRef(curTerm)));
            document.add(new StoredField(OWL.TERM, curTerm));
        }
        if(node.getAlternativeTerms()!=null)
            node.getAlternativeTerms().forEach(term->
            {
                if(term!=null) {
                    document.add(new StringField(OWL.ALTERNATIVE_TERMS, term.toLowerCase(), Field.Store.NO));
                    document.add(new SortedDocValuesField(OWL.ALTERNATIVE_TERMS, new BytesRef(term)));
                    document.add(new StoredField(OWL.ALTERNATIVE_TERMS, term));
                }
            });
        if(node.getParents()!=null)
            node.getParents().forEach(term-> {
                if(term.getId()!=null)
                    document.add(new Field(OWL.FATHER, term.getId().toLowerCase(), TYPE_NOT_ANALYZED));
            });
        if(node.getChildren()!=null)
            node.getChildren().forEach(term -> {
                if(term.getId()!=null)
                    document.add(new Field(OWL.CHILDRERN, term.getId().toLowerCase(), TYPE_NOT_ANALYZED));
            });
        try {
            indexWriter.addDocument(document);
        }catch (Exception exception){
            LOGGER.error("Problem in indexing EFONode: {}", node.getId(), exception);
        }
    }

    public IEFO removeIgnoredClasses(IEFO efo, String ignoreListFileLocation) throws IOException {
        if (null != ignoreListFileLocation) {
            try (InputStream is = (new ClassPathResource(ignoreListFileLocation)).getInputStream()) {
                Set<String> ignoreList = StringTools.streamToStringSet(is, "UTF-8");

                LOGGER.debug("Loaded EFO ignored classes from [{}]", ignoreListFileLocation);
                for (String id : ignoreList) {
                    if (null != id && !"".equals(id) && !id.startsWith("#") && efo.getMap().containsKey(id)) {
                        removeEFONode(efo, id);
                    }
                }
            }
        }
        return efo;
    }

    private void removeEFONode(IEFO efo, String nodeId) {
        EFONode node = efo.getMap().get(nodeId);
        // step 1: for all parents remove node as a child
        for (EFONode parent : node.getParents()) {
            parent.getChildren().remove(node);
        }
        // step 2: for all children remove node as a parent; is child node has no other parents, remove it completely
        for (EFONode child : node.getChildren()) {
            child.getParents().remove(node);
            if (0 == child.getParents().size()) {
                removeEFONode(efo, child.getId());
            }
        }

        // step 3: remove node from efo map
        efo.getMap().remove(nodeId);
        LOGGER.debug("Removed [{}] -> [{}]", node.getId(), node.getTerm());
    }

    public IEFO getEfo() {
        return efo;
    }
}
