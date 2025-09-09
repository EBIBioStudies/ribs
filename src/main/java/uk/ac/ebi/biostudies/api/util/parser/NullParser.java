package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.ReadContext;
import uk.ac.ebi.biostudies.api.util.Constants;

import java.util.List;
import java.util.Map;

public class NullParser extends AbstractParser {
  @Override
  public String parse(
      Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
    return null;
  }
}
