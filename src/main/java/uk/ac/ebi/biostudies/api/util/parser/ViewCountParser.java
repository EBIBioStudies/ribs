package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.ReadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.service.impl.ViewCountLoader;

import java.util.Map;

public class ViewCountParser extends AbstractParser {
  private static final Logger LOGGER = LogManager.getLogger(ViewCountParser.class.getName());

  @Override
  public String parse(
      Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
    String indexKey = indexEntry.get(Constants.IndexEntryAttributes.NAME).asText();
    String accession = valueMap.get(Constants.Fields.ACCESSION).toString();
    Long freq = ViewCountLoader.getViewCountMap().get(accession);
    if (freq == null) freq = 0L;
    valueMap.put(indexKey, freq);
    return String.valueOf(freq);
  }
}
