package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.DataTableColumnInfo;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.service.ExtractedLinkPaginationService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;

import java.util.*;

@Component
public class ExtractedLinkPaginationServiceImpl implements ExtractedLinkPaginationService {
    private final Logger logger = LogManager.getLogger(ExtractedLinkPaginationServiceImpl.class.getName());
    static String[] linkColumns = {Constants.Link.TYPE, Constants.Link.VALUE, Constants.Link.URL, Constants.File.FILENAME};

    @Autowired
    SearchService searchService;
    @Autowired
    IndexManager indexManager;
    @Override
    public ObjectNode getExtractedLinkList(String accession, int start, int pageSize, String search, int draw, boolean metadata, Map<Integer, DataTableColumnInfo> dataTableUiResult, String secretKey) throws SubmissionNotAccessibleException {

        ObjectMapper mapper = new ObjectMapper();
        IndexSearcher searcher = indexManager.getExtractedLinkIndexSearcher();
        QueryParser parser = new QueryParser(Constants.Fields.ACCESSION, new KeywordAnalyzer());
        IndexReader reader = indexManager.getExtractedLinkIndexReader();
        Document linkDoc = searchService.getDocumentByAccession(accession, secretKey);
        long totalLinks = getTotalLinks(accession);
        if (linkDoc == null) return mapper.createObjectNode();
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
                allSortedFields.add(new SortField(Constants.File.POSITION, SortField.Type.LONG, false));
            Sort sort = new Sort(allSortedFields.toArray(new SortField[allSortedFields.size()]));
            Query query = parser.parse(Constants.File.OWNER + ":" + accession.toLowerCase());
            if (search != null && !search.isEmpty() && hasUnescapedDoubleQuote(search)) {
                query = phraseSearch(search, query);
            } else if (search != null && !search.isEmpty() && !search.trim().equalsIgnoreCase("**")) {
                search = modifySearchText(search);
                query = applySearch(search, query, linkColumns);
            }
            if (searchedColumns.size() > 0)
                query = applyPerFieldSearch(searchedColumns, query);
            TopDocs hits = searcher.search(query, Integer.MAX_VALUE, sort);
            ObjectNode response = mapper.createObjectNode();
            response.put(Constants.File.DRAW, draw);
            response.put(Constants.File.RECORDTOTAL, totalLinks);
            response.put(Constants.File.RECORDFILTERED, hits.totalHits.value);
            if (hits.totalHits.value >= 0) {
                if (pageSize == -1) pageSize = Integer.MAX_VALUE;
                ArrayNode docs = mapper.createArrayNode();
                for (int i = start; i < start + pageSize && i < hits.totalHits.value; i++) {
                    ObjectNode docNode = mapper.createObjectNode();
                    Document respLink = reader.document(hits.scoreDocs[i].doc);
                    docNode.put(Constants.Link.TYPE, respLink.get(Constants.Link.TYPE) == null ? "" : respLink.get(Constants.Link.TYPE));
                    docNode.put(Constants.Link.VALUE, respLink.get(Constants.Link.VALUE) == null ? "" : respLink.get(Constants.Link.VALUE));
                    docNode.put(Constants.Link.URL, respLink.get(Constants.Link.URL) == null ? "" : respLink.get(Constants.Link.URL));
                    docNode.put(Constants.File.FILENAME, respLink.get(Constants.File.FILENAME) == null ? "" : respLink.get(Constants.File.FILENAME));

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

    private Query applySearch(String search, Query firstQuery, String[] columns) {
        BooleanQuery.Builder builderSecond = new BooleanQuery.Builder();
        try{
            MultiFieldQueryParser parser = new MultiFieldQueryParser(columns, new KeywordAnalyzer());
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

    private long getTotalLinks(String accession){
        try{
            Query query = new TermQuery(new Term(Constants.File.OWNER, accession.toLowerCase()));
            TopDocs resultsDoc = indexManager.getExtractedLinkIndexSearcher().search(query, Integer.MAX_VALUE);
            return resultsDoc.totalHits.value;
        }catch (Exception exception){
            logger.error(exception);
        }
        return 0;
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


    private boolean hasUnescapedDoubleQuote(String search) {
        return search.replaceAll("\\Q\\\"\\E", "").contains("\"");
    }
}
