package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.ReadContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;

public class NodeCountingParser extends AbstractParser {

    private static final Logger logger = LogManager.getLogger(NodeCountingParser.class.getName());

    @Override
    public String parse(Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
        String indexKey = indexEntry.get(Constants.IndexEntryAttributes.NAME).asText();
        String jsonPath = indexEntry.get(Constants.IndexEntryAttributes.JSON_PATH).asText();
        List<Object> resultData = new ArrayList<>();
        for (String jp: jsonPath.split(" OR ")) {
            try {
                Object obj = jsonPathContext.read(jp)  ;
                if (obj instanceof JSONArray) {
                    resultData.addAll((JSONArray)obj);
                } else {
                    resultData.add(obj);
                }
            } catch (JsonPathException ex) {

            }
        }
        long length = resultData!=null ? resultData.size() : 0;
        valueMap.put(indexKey, length);
        return String.valueOf(length);
    }
}
