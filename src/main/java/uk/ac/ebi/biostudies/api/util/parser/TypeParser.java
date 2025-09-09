package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.ReadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * unfortunately there are two titles in json in similar paths therefore it is hard to parse it
 * automatically
 */
public class TypeParser extends AbstractParser {
  private static final Logger LOGGER = LogManager.getLogger(TypeParser.class.getName());

  @Override
  public String parse(
      Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
    String type = "";
    String indexKey = indexEntry.get(Constants.IndexEntryAttributes.NAME).asText();

    try {
      type = submission.get("section").get("type").textValue().toLowerCase();
    } catch (Exception ex1) {
    }
    if (type.equalsIgnoreCase("project")) {
      type = "collection";
    }
    if (type.isEmpty()) LOGGER.error("type is empty in accession: {}", valueMap.toString());
    valueMap.put(indexKey, type);
    return type;
  }
}
