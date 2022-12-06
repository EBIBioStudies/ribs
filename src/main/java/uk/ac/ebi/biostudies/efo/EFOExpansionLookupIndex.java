package uk.ac.ebi.biostudies.efo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.IndexManager;

import java.io.IOException;
import java.util.*;

/**
 * Created by ehsan on 02/03/2017.
 */

@Service
public class EFOExpansionLookupIndex {

    private Logger logger = LogManager.getLogger(EFOExpansionLookupIndex.class.getName());


    @Autowired
    IndexManager indexManager;



    private static final int MAX_INDEX_HITS = 16;

    public EFOExpansionTerms getExpansionTerms(Query origQuery) throws IOException {
        EFOExpansionTerms expansion = new EFOExpansionTerms();

        if (DirectoryReader.indexExists(indexManager.getEfoIndexDirectory())) {

            try{
                IndexSearcher searcher = indexManager.getEfoIndexSearcher();
                Query q = overrideQueryField(origQuery, Constants.QEXPAND.TERM);

                TopDocs hits = searcher.search(q, MAX_INDEX_HITS);
                //this.logger.debug("Expansion lookup for query [{}] returned [{}] hits", q.toString(), hits.totalHits);

                for (ScoreDoc d : hits.scoreDocs) {
                    Document doc = searcher.doc(d.doc);
                    String[] terms = doc.getValues(Constants.QEXPAND.TERM);
                    String[] efo = doc.getValues(Constants.QEXPAND.EFO);
                    //this.logger.debug("Synonyms [{}], EFO Terms [{}]", StringUtils.join(terms, ", "), StringUtils.join(efo, ", "));
                    if (0 != terms.length) {
                        expansion.synonyms.addAll(Arrays.asList(terms));
                    }

                    if (0 != efo.length) {
                        expansion.efo.addAll(Arrays.asList(efo));
                    }
                }
            }catch (Exception ex){
                logger.error("problem in expanding terms for query {}", origQuery.toString(), ex);
            }
        }else
        {
            logger.info("No EFO index have found to expand key words!");
        }

        return expansion;
    }

    public Set<String> getReverseExpansion(String text) throws IOException {
        Set<String> reverseExpansion = new HashSet<>();

        if (null != text && DirectoryReader.indexExists(indexManager.getEfoIndexDirectory())) {

            try {
                IndexSearcher searcher = indexManager.getEfoIndexSearcher();

                // step 1: split terms
                String[] terms = text.split("\\s+");

                for (int termIndex = 0; termIndex < terms.length; ++termIndex) {
                    BooleanQuery.Builder qb = new BooleanQuery.Builder();

                    Term t = new Term(Constants.QEXPAND.ALL, terms[termIndex]);
                    qb.add(new TermQuery(t), BooleanClause.Occur.SHOULD);

                    for (int phraseLength = 4; phraseLength <= 2; --phraseLength) {
                        if (termIndex + phraseLength > terms.length) {
                            continue;
                        }
                        PhraseQuery.Builder pqb = new PhraseQuery.Builder();
                        for (int phraseTermIndex = 0; phraseTermIndex < phraseLength; ++phraseTermIndex) {
                            t = new Term(Constants.QEXPAND.ALL, terms[termIndex + phraseTermIndex]);
                            pqb.add(t);
                        }
                        qb.add(pqb.build(), BooleanClause.Occur.SHOULD);
                    }
                    Query q = qb.build();
                    TopDocs hits = searcher.search(q, MAX_INDEX_HITS);
                    //this.logger.debug("Expansion lookup for query [{}] returned [{}] hits", q.toString(), hits.totalHits);

                    for (ScoreDoc d : hits.scoreDocs) {
                        Document doc = searcher.doc(d.doc);
                        String[] reverseTerms = doc.getValues(Constants.QEXPAND.TERM);
                        //this.logger.debug("Synonyms [{}], EFO Terms [{}]", StringUtils.join(terms, ", "), StringUtils.join(efo, ", "));
                        if (0 != reverseTerms.length) {
                            reverseExpansion.addAll(Arrays.asList(reverseTerms));
                        }
                    }
                }
            }catch (Exception ex){
                logger.error("", ex);
            }
        }

        return reverseExpansion;
    }



    private Query overrideQueryField(Query origQuery, String fieldName) {
        Query query = new TermQuery(new Term(""));

        try {
            if (origQuery instanceof PrefixQuery) {
                Term term = ((PrefixQuery) origQuery).getPrefix();
                query = new PrefixQuery(new Term(fieldName, term.text()));
            } else if (origQuery instanceof WildcardQuery) {
                Term term = ((WildcardQuery) origQuery).getTerm();
                query = new WildcardQuery(new Term(fieldName, term.text()));
            } else if (origQuery instanceof TermRangeQuery) {
                TermRangeQuery trq = (TermRangeQuery) origQuery;
                query = new TermRangeQuery(fieldName, trq.getLowerTerm(), trq.getUpperTerm(), trq.includesLower(), trq.includesUpper());
            } else if (origQuery instanceof FuzzyQuery) {
                Term term = ((FuzzyQuery) origQuery).getTerm();
                query = new FuzzyQuery(new Term(fieldName, term.text()));
            } else if (origQuery instanceof TermQuery) {
                Term term = ((TermQuery) origQuery).getTerm();
                query = new TermQuery(new Term(fieldName, term.text()));
            } else if (origQuery instanceof PhraseQuery) {
                Term[] terms = ((PhraseQuery) origQuery).getTerms();
                StringBuilder text = new StringBuilder();
                for (Term t : terms) {
                    text.append(t.text()).append(' ');
                }
                query = new TermQuery(new Term(fieldName, text.toString().trim()));
            } else {
                this.logger.error("Unsupported query type [{}]", origQuery.getClass().getCanonicalName());
            }
        } catch (Exception x) {
            this.logger.error("Caught an exception:", x);
        }


        return query;
    }


}
