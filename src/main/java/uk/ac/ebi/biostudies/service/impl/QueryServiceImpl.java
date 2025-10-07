package uk.ac.ebi.biostudies.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.BioStudiesQueryParser;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.analyzer.AnalyzerManager;
import uk.ac.ebi.biostudies.api.util.analyzer.AttributeFieldAnalyzer;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.config.TaxonomyManager;
import uk.ac.ebi.biostudies.efo.EFOExpansionTerms;
import uk.ac.ebi.biostudies.efo.EFOQueryExpander;
import uk.ac.ebi.biostudies.service.QueryService;

@Service
public class QueryServiceImpl implements QueryService {

    private static Query typeFilterQuery;
    private final Logger logger = LogManager.getLogger(QueryServiceImpl.class.getName());
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    IndexManager indexManager;
    @Autowired
    EFOQueryExpander efoQueryExpander;
    @Autowired
    AnalyzerManager analyzerManager;
    @Autowired
    SecurityQueryBuilder securityQueryBuilder;
    @Autowired
    TaxonomyManager taxonomyManager;

    @PostConstruct
    void init() {
        QueryParser parser = new QueryParser(Constants.Fields.TYPE, new AttributeFieldAnalyzer());
        parser.setSplitOnWhitespace(true);
        try {
            typeFilterQuery = parser.parse(indexConfig.getTypeFilterQuery());
        } catch (ParseException e) {
            logger.error(e);
        }
    }

    @Override
    public Pair<Query, EFOExpansionTerms> makeQuery(String queryString, String collectionName, JsonNode selectedFields) {
        String[] fields = indexConfig.getIndexFields();
        Analyzer analyzer = analyzerManager.getPerFieldAnalyzerWrapper();
        QueryParser parser = new BioStudiesQueryParser(fields, analyzer, indexManager);

        if (StringUtils.isEmpty(queryString)) {
            queryString = "*:*";
        }

        Pair<Query, EFOExpansionTerms> finalQuery = null;
        try {
            Query query = parser.parse(queryString);
            Query expandedQuery = null;
            Pair<Query, EFOExpansionTerms> queryEFOExpansionTermsPair = expandQuery(query);
            if (selectedFields != null) {
                expandedQuery = applySelectedFields((ObjectNode) selectedFields, queryEFOExpansionTermsPair.getKey(), parser);
            }
            expandedQuery = (expandedQuery == null ? queryEFOExpansionTermsPair.getKey() : expandedQuery);

            if (!queryString.toLowerCase().contains("type:")) {
                if (selectedFields != null && !selectedFields.has("type")) {
                    expandedQuery = applyTypeFilter(expandedQuery);
                }
            }

            if (!StringUtils.isEmpty(collectionName) && !collectionName.equalsIgnoreCase(Constants.PUBLIC)) {
                expandedQuery = applyCollectionFilter(expandedQuery, collectionName.toLowerCase());
            }
            Query queryAfterSecurity = securityQueryBuilder.applySecurity(expandedQuery);
            logger.trace("Lucene query: {}", queryAfterSecurity.toString());
            finalQuery = new MutablePair<>(queryAfterSecurity, queryEFOExpansionTermsPair.getValue());
        } catch (Throwable ex) {
            logger.error(ex);
        }
        return finalQuery;
    }

    private Query applyTypeFilter(Query originalQuery) {
        BooleanQuery.Builder excludeBuilder = new BooleanQuery.Builder();
        excludeBuilder.add(originalQuery, BooleanClause.Occur.MUST);
        excludeBuilder.add(typeFilterQuery, BooleanClause.Occur.MUST_NOT);
        return excludeBuilder.build();
    }

    public Query applyCollectionFilter(Query query, String prjName) {
        Map<JsonNode, List<String>> hm = new HashMap<JsonNode, List<String>>();
        List<String> collections = Lists.newArrayList(prjName);
        if (indexManager.getSubCollectionMap().containsKey(prjName)) {
            collections.addAll(indexManager.getSubCollectionMap().get(prjName));
        }
        hm.put(taxonomyManager.PROJECT_FACET, collections);
        return FacetServiceImpl.addFacetDrillDownFilters(taxonomyManager.getFacetsConfig(), query, hm);
    }

    public Pair<Query, EFOExpansionTerms> expandQuery(Query query) throws IOException {
        Map<String, String> queryInfo = analyzerManager.getExpandableFields();
        return efoQueryExpander.expand(queryInfo, query);
    }

    private Query applySelectedFields(ObjectNode selectedFields, Query query, QueryParser queryParser) {
        Iterator<String> fieldNamesIterator = selectedFields.fieldNames();
        String fieldName = "";
        String fieldValue = "";
        BooleanQuery.Builder fieldQueryBuilder = new BooleanQuery.Builder();
        fieldQueryBuilder.add(query, BooleanClause.Occur.MUST);
        while (fieldNamesIterator.hasNext()) {
            try {
                fieldName = fieldNamesIterator.next();
                if (fieldName == null || fieldName.isEmpty() || fieldName.equalsIgnoreCase("query"))
                    continue;
                fieldValue = selectedFields.get(fieldName).textValue();
                if (fieldValue == null || fieldValue.isEmpty())
                    continue;
                Query fieldQuery = queryParser.parse(fieldName + ":" + fieldValue);
                fieldQueryBuilder.add(fieldQuery, BooleanClause.Occur.MUST);
            } catch (Exception ex) {
                logger.error("field {} value {} has problem for lucene queryparser", fieldName, fieldValue, ex);
            }
        }
        return fieldQueryBuilder.build();
    }

}
