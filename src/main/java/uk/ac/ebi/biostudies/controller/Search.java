package uk.ac.ebi.biostudies.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.PublicRESTMethod;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.service.FacetService;
import uk.ac.ebi.biostudies.service.SearchService;

import java.net.URLDecoder;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.ac.ebi.biostudies.api.util.Constants.JSON_UNICODE_MEDIA_TYPE;

/**
 * Created by awais on 14/02/2017.
 *
 * <p>Rest endpoint for searching Biostudies
 */
@RestController
@RequestMapping(value = "/api/v1")
public class Search {

  private final Logger logger = LogManager.getLogger(Search.class.getName());

  @Autowired SearchService searchService;
  @Autowired FacetService facetService;
  @Autowired IndexManager indexManager;

  @PublicRESTMethod
  @RequestMapping(value = "/search", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
  public String search(
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
      @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
      @RequestParam(value = "sortBy", required = false, defaultValue = "") String sortBy,
      @RequestParam(value = "sortOrder", required = false, defaultValue = "descending")
          String sortOrder,
      @RequestParam MultiValueMap<String, String> params)
      throws Exception {
    String queryString = params.getFirst("query");
    params.remove("query");
    queryString = queryString == null ? "" : queryString;
    ObjectNode selectedFacets = checkSelectedFacetsAndFields(params);
    return searchService.search(
        URLDecoder.decode(queryString, String.valueOf(UTF_8)),
        selectedFacets,
        null,
        page,
        pageSize,
        sortBy,
        sortOrder);
  }

  @PublicRESTMethod
  @RequestMapping(
      value = "/{collection}/search",
      produces = JSON_UNICODE_MEDIA_TYPE,
      method = RequestMethod.GET)
  public String searchCollection(
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
      @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
      @RequestParam(value = "sortBy", required = false, defaultValue = "") String sortBy,
      @RequestParam(value = "sortOrder", required = false, defaultValue = "descending")
          String sortOrder,
      @RequestParam MultiValueMap<String, String> params,
      @PathVariable String collection)
      throws Exception {
    ObjectNode selectedFacets = checkSelectedFacetsAndFields(params);
    String queryString = params.getFirst("query");
    params.remove("query");
    queryString = queryString == null ? "" : queryString;
    return searchService.search(
        URLDecoder.decode(queryString, String.valueOf(UTF_8)),
        selectedFacets,
        collection,
        page,
        pageSize,
        sortBy,
        sortOrder);
  }

  @RequestMapping(
      value = "/{collection}/facets",
      produces = JSON_UNICODE_MEDIA_TYPE,
      method = RequestMethod.GET)
  public String getDefaultFacets(
      @PathVariable String collection,
      @RequestParam(value = "query", required = false, defaultValue = "") String queryString,
      @RequestParam(
              value = "limit",
              required = false,
              defaultValue = "" + Constants.TOP_FACET_COUNT)
          Integer limit,
      @RequestParam MultiValueMap<String, String> params)
      throws Exception {
    ObjectNode selectedFacets = checkSelectedFacetsAndFields(params);
    return facetService
        .getDefaultFacetTemplate(collection, queryString, limit, selectedFacets)
        .toString();
  }

  @RequestMapping(
      value = "/{collection}/facets/{dimension}/",
      produces = JSON_UNICODE_MEDIA_TYPE,
      method = RequestMethod.GET)
  public String getDefaultFacets(
      @PathVariable String collection,
      @PathVariable String dimension,
      @RequestParam(value = "query", required = false, defaultValue = "") String queryString,
      @RequestParam MultiValueMap<String, String> params)
      throws Exception {
    ObjectNode selectedFacets = checkSelectedFacetsAndFields(params);
    return facetService.getDimension(collection, dimension, queryString, selectedFacets).toString();
  }

  private ObjectNode checkSelectedFacetsAndFields(MultiValueMap<String, String> params) {
    Set<String> fieldNames = indexManager.getIndexEntryMap().keySet();
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode allSelected = mapper.createObjectNode();
    ObjectNode selectedFacets = mapper.createObjectNode();
    ObjectNode selectedFields = mapper.createObjectNode();
    if (params != null) {
      for (String key : params.keySet()) {
        if (key.equalsIgnoreCase("pageSize")
            || key.equalsIgnoreCase("page")
            || key.equalsIgnoreCase("sortBy")
            || key.equalsIgnoreCase("sortOrder")
            || key.equalsIgnoreCase("query")
            || key.equalsIgnoreCase("limit")) continue;
        if (!key.toLowerCase().contains("facet.") && fieldNames.contains(key.toLowerCase())) {
          selectedFields.put(key, params.getFirst(key));
        } else {
          String facetKey = StringUtils.remove(key, "[]");
          if (!selectedFacets.has(facetKey)) {
            selectedFacets.set(facetKey, mapper.createArrayNode());
          }
          for (String value : params.get(key)) {
            ((ArrayNode) selectedFacets.get(facetKey)).add(value);
          }
        }
      }
    }
    allSelected.set("facets", selectedFacets);
    allSelected.set("fields", selectedFields);
    return allSelected;
  }

  @PublicRESTMethod
  @RequestMapping(
      value = "/failed-submissions",
      produces = JSON_UNICODE_MEDIA_TYPE,
      method = RequestMethod.GET)
  public String getFailedIndexingSubmissions() {
    return searchService.getFailedIndexingSubmissions();
  }
}
