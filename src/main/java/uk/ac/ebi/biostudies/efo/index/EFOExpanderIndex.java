package uk.ac.ebi.biostudies.efo.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.EFOConfig;
import uk.ac.ebi.biostudies.efo.EFONode;
import uk.ac.ebi.biostudies.efo.IEFO;
import uk.ac.ebi.biostudies.efo.SynonymsFileReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class EFOExpanderIndex {
    private static final Logger LOGGER = LogManager.getLogger(EFOExpanderIndex.class);

    @Autowired
    EFOConfig efoConfig;
    private Map<String, Set<String>> customSynonyms;
    private IEFO efo;


    public void setCustomSynonyms(Map<String, Set<String>> synonyms) {
        this.customSynonyms = synonyms;
    }

    public void buildIndex(IndexWriter indexWriter) throws IOException, InterruptedException {
        try {
            loadCustomSynonyms();
            LOGGER.debug("Building expansion lookup index");
            addNodeAndChildren(getEFO().getMap().get(IEFO.ROOT_ID), indexWriter);
            addCustomSynonyms(indexWriter);
            indexWriter.commit();
            LOGGER.debug("Building completed");
        }catch (Exception ex){
            LOGGER.error("problem in creating efo index",ex);
        }
    }

    private void loadCustomSynonyms() {
        String synFileLocation = efoConfig.getSynonymFilename();
        if (null != synFileLocation) {
            try (InputStream resourceInputStream = (new ClassPathResource(synFileLocation)).getInputStream()){
                Map<String, Set<String>> synonyms = new SynonymsFileReader(new InputStreamReader(resourceInputStream)).readSynonyms();
                setCustomSynonyms(synonyms);
                LOGGER.debug("Loaded custom synonyms from [{}]", synFileLocation);
            } catch (Exception ex){
                LOGGER.error("could not open synonyms file", ex);
            }
        }
    }

    private void addCustomSynonyms(IndexWriter w) throws IOException, InterruptedException {
        // here we add all custom synonyms so those that weren't added during EFO processing
        //  get a chance to be included, too. don't worry about duplication, dupes will be removed during retrieval
        if (null != this.customSynonyms) {
            Set<String> addedTerms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (String term : this.customSynonyms.keySet()) {
                if (!addedTerms.contains(term)) {
                    Document d = new Document();
                    addIndexField(d, Constants.QEXPAND.TERM, term.toLowerCase(), false);
                    Set<String> syns = this.customSynonyms.get(term);
                    for (String syn : syns) {
                        addIndexField(d, Constants.QEXPAND.EFO, syn, false);
                    }
                    w.addDocument(d);
                    addedTerms.addAll(syns);
                }
            }
        }
    }

    private void addNodeAndChildren(EFONode node, IndexWriter w) throws IOException, InterruptedException {
        if (null != node) {
            addNodeToIndex(node, w);
            for (EFONode child : node.getChildren()) {
                addNodeAndChildren(child, w);
            }
        }
    }

    private void addNodeToIndex(EFONode node, IndexWriter w) throws IOException, InterruptedException {
        String term = node.getTerm();

        if (null != term && !isStopTerm(term)) {
            Set<String> synonyms = node.getAlternativeTerms();

            // if the node represents organizational class, just include its synonyms, but not children
            Set<String> childTerms =
                    node.isOrganizationalClass()
                            ? new HashSet<String>()
                            : getEFO().getTerms(node.getId(), IEFO.INCLUDE_CHILDREN);

            // here we add custom synonyms to EFO synonyms/child terms and their synonyms
            Set<String> tempAltTerm = new HashSet<>(synonyms);
            if (null != this.customSynonyms) {
                for (String syn : tempAltTerm) {
                    if (null != syn) {
                        synonyms.add(syn);
                        if(this.customSynonyms.containsKey(syn))
                            synonyms.addAll(this.customSynonyms.get(syn));

                    }
                }

                if (this.customSynonyms.containsKey(term)) {
                    synonyms.addAll(this.customSynonyms.get(term));
                }

                for (String child : new HashSet<>(childTerms)) {
                    if (null != child && this.customSynonyms.containsKey(child)) {
                        childTerms.addAll(this.customSynonyms.get(child));
                    }
                }
            }
            if (synonyms.contains(term)) {
                synonyms.remove(term);
            }

            // just to remove ridiculously long terms/synonyms from the list


            if (synonyms.size() > 0 || childTerms.size() > 0) {

                Document d = new Document();

                int terms = 0, efoChildren = 0;

                for (String syn : synonyms) {
                    if (!isStopExpansionTerm(syn)) {
                        addIndexField(d, Constants.QEXPAND.TERM, syn.toLowerCase(), false);
                        addIndexField(d, Constants.QEXPAND.EFO, syn.toLowerCase(), false);
                        terms++;
                    }
                }

                for (String efoTerm : childTerms) {
                    if (!isStopExpansionTerm(efoTerm)) {
                        addIndexField(d, Constants.QEXPAND.EFO, efoTerm.toLowerCase(), false);
                        efoChildren++;
                    }
                }

                if (!isStopExpansionTerm(term)) {
                    addIndexField(d, Constants.QEXPAND.TERM, term.toLowerCase(), false);
                    terms++;
                }

                if (terms > 1 || (1 == terms && efoChildren > 0)) {
                    w.addDocument(d);
                }
            }
        }
    }


    private void addIndexField(Document document, String name, String value, boolean shouldAnalyze) {
        value = value.replaceAll("[^\\d\\w-]", " ").toLowerCase();
        if(shouldAnalyze){
            document.add(new TextField(name, value, Field.Store.YES));
        }
        else{
            document.add(new StringField(name, value, Field.Store.YES));
        }
    }

    private boolean isStopTerm(String str) {
        return null == str || efoConfig.getStopWordsSet().contains(str.toLowerCase());
    }

    private boolean isStopExpansionTerm(String str) {
        return isStopTerm(str) || str.length() < 3 || str.matches(".*(\\s\\(.+\\)|\\s\\[.+\\]|,\\s|\\s-\\s|/|NOS).*");
    }

    public IEFO getEFO() {
        return efo;
    }

    public void setEFO(IEFO iefo) {
        this.efo = iefo;
    }
}
