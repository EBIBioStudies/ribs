package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.ReadContext;

import java.util.Map;

public abstract class AbstractParser {
  protected JsonNode indexEntry;

  public abstract String parse(
      Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext);

  public void setIndexEntry(JsonNode indexEntry) {
    this.indexEntry = indexEntry;
  }
}
