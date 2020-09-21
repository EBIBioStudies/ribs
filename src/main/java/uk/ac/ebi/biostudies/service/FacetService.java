package uk.ac.ebi.biostudies.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.search.Query;
import java.util.List;
import java.util.Map;

/**
 * Created by ehsan on 09/03/2017.
 */
public interface FacetService {

    List<FacetResult> getFacetsForQuery(Query query, int limit, Map<String, Map<String, Integer>> selectedFacetFreq, JsonNode selectedFacets);
    JsonNode getDefaultFacetTemplate(String prjName, String queryString, int limit, JsonNode facets);
    Query addFacetDrillDownFilters(Query primaryQuery, Map<JsonNode, List<String>> userSelectedDimValues);
    JsonNode getDimension(String collection, String dimension, String queryString, JsonNode facetAndFields);
    public Query applyFacets(Query query, JsonNode facets);
}
