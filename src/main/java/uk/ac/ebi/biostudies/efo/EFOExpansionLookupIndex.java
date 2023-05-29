package uk.ac.ebi.biostudies.efo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.IndexManager;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by ehsan on 02/03/2017.
 */

@Service
public class EFOExpansionLookupIndex {

    private static final int MAX_INDEX_HITS = 16;
    private final Logger logger = LogManager.getLogger(EFOExpansionLookupIndex.class.getName());

    @Autowired
    IndexManager indexManager;

    public EFOExpansionTerms getExpansionTerms(Query origQuery) throws IOException {
        EFOExpansionTerms expansion = new EFOExpansionTerms();

        if (DirectoryReader.indexExists(indexManager.getEfoIndexDirectory())) {

            try {
                IndexSearcher searcher = indexManager.getEfoIndexSearcher();
                Query q = overrideQueryField(origQuery);

                TopDocs hits = searcher.search(q, MAX_INDEX_HITS);

                for (ScoreDoc d : hits.scoreDocs) {
                    Document doc = searcher.doc(d.doc);
                    String[] terms = doc.getValues(Constants.QEXPAND.TERM);
                    String[] efo = doc.getValues(Constants.QEXPAND.EFO);
                    if (0 != terms.length) {
                        expansion.synonyms.addAll(Arrays.asList(terms));
                    }

                    if (0 != efo.length) {
                        expansion.efo.addAll(Arrays.asList(efo));
                    }
                }
            } catch (Exception ex) {
                logger.error("problem in expanding terms for query {}", origQuery.toString(), ex);
            }
        } else {
            logger.info("No EFO index have found to expand key words!");
        }

        return expansion;
    }

    private Query overrideQueryField(Query origQuery) {
        Query query = new TermQuery(new Term(""));

        try {
            if (origQuery instanceof PrefixQuery) {
                Term term = ((PrefixQuery) origQuery).getPrefix();
                query = new PrefixQuery(new Term(Constants.QEXPAND.TERM, term.text()));
            } else if (origQuery instanceof WildcardQuery) {
                Term term = ((WildcardQuery) origQuery).getTerm();
                query = new WildcardQuery(new Term(Constants.QEXPAND.TERM, term.text()));
            } else if (origQuery instanceof TermRangeQuery) {
                TermRangeQuery trq = (TermRangeQuery) origQuery;
                query = new TermRangeQuery(Constants.QEXPAND.TERM, trq.getLowerTerm(), trq.getUpperTerm(), trq.includesLower(), trq.includesUpper());
            } else if (origQuery instanceof FuzzyQuery) {
                Term term = ((FuzzyQuery) origQuery).getTerm();
                query = new FuzzyQuery(new Term(Constants.QEXPAND.TERM, term.text()));
            } else if (origQuery instanceof TermQuery) {
                Term term = ((TermQuery) origQuery).getTerm();
                query = new TermQuery(new Term(Constants.QEXPAND.TERM, term.text()));
            } else if (origQuery instanceof PhraseQuery) {
                Term[] terms = ((PhraseQuery) origQuery).getTerms();
                StringBuilder text = new StringBuilder();
                for (Term t : terms) {
                    text.append(t.text()).append(' ');
                }
                query = new TermQuery(new Term(Constants.QEXPAND.TERM, text.toString().trim()));
            } else {
                this.logger.error("Unsupported query type [{}]", origQuery.getClass().getCanonicalName());
            }
        } catch (Exception x) {
            this.logger.error("Caught an exception:", x);
        }

        return query;
    }


}
