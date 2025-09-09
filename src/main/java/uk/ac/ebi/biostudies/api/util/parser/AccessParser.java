package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.ReadContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minidev.json.JSONArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;

public class AccessParser extends AbstractParser{
    private static final Logger logger = LogManager.getLogger(AccessParser.class.getName());

    @Override
    public String parse(Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
        String indexKey = indexEntry.get(Constants.IndexEntryAttributes.NAME).asText();
        String[] jPaths = {"$.accessTags[?(@ =~ /[^{].*/i)]" , "$.accessTags[?(@.size() > 0)].name", "$.collections..accNo", "$.owner"};
        Set resultData = new HashSet<>();
        for (String jp: jPaths) {
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

        if (submission.has("released") && submission.get("released").asBoolean(false) ) {
           resultData.add(Constants.PUBLIC);
        }
        String result =  String.join (" ", resultData).toLowerCase();
        valueMap.put(indexKey, result );
        return result;
    }
}
