package uk.ac.ebi.biostudies.api.util.parser;

import static uk.ac.ebi.biostudies.api.util.Constants.NA;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.ReadContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;

public class FileTypeParser extends AbstractParser{
    private static final Logger logger = LogManager.getLogger(FileTypeParser.class.getName());

    @Override
    public String parse(Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
        String result=NA;
        try {
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

            result = resultData.stream()
                    .map(jnode -> {
                        Map node = (Map)jnode;
                        if (node==null) return "";
                        String s = "";
                        if (node.containsKey("path") && node.get("path")!=null)  {
                            s =  node.get("path").toString();
                        } else if (node.containsKey("name") && node.get("name")!=null)  {
                            s =  node.get("name").toString();
                        } else if (node.containsKey("fileName") && node.get("fileName")!=null)  {
                            s =  node.get("fileName").toString();
                        }
                        if ( StringUtils.isEmpty(s)) return "";
                        int k = s.lastIndexOf(".");
                        return k >= 0 ? s.substring(s.lastIndexOf(".") + 1) : NA;
                    }).collect(Collectors.joining(Constants.Facets.DELIMITER));
            valueMap.put(indexKey, result);
        }
        catch (Exception ex){
            logger.error("problem in parsing FILE_TYPE in {}", indexEntry.get(Constants.IndexEntryAttributes.NAME), ex);
        }
        return result;
    }
}
