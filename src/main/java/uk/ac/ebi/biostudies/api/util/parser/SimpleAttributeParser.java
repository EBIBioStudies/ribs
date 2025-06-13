package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.ReadContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.ac.ebi.biostudies.api.util.Constants.NA;

public class SimpleAttributeParser extends AbstractParser {
    private final static Logger LOGGER = LogManager.getLogger(SimpleAttributeParser.class.getName());

    private static String jpath = "$.section.attributes[?(@.name=~ /%s/i)].value";
    @Override
    public String parse(Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
        Object result= NA;
        String indexKey = indexEntry.get(Constants.IndexEntryAttributes.NAME).asText();
        try {
            String newJPath;
            if (indexEntry.has(Constants.IndexEntryAttributes.JSON_PATH)) {
                newJPath = indexEntry.get(Constants.IndexEntryAttributes.JSON_PATH).asText();
            } else {
                String title = StringUtils.replace(indexEntry.get(Constants.Fields.TITLE).asText(), "/", "\\/");
                newJPath = String.format(jpath, title);
            }

            List <String> resultData = jsonPathContext.read(newJPath);
            if(indexEntry.has(Constants.Facets.MATCH)) {
                String match = indexEntry.get(Constants.Facets.MATCH).asText();
                resultData = resultData.stream()
                        .map(item -> {
                            Matcher matcher = Pattern.compile(match).matcher(item);
                            return matcher.find() ? matcher.group(1) : "";
                        }).collect(Collectors.toList());
            }
            switch (indexEntry.get(Constants.IndexEntryAttributes.FIELD_TYPE).asText()) {
                case Constants.IndexEntryAttributes.FieldTypeValues.FACET:
                    result =  String.join(Constants.Facets.DELIMITER, resultData);
                    break;
                case Constants.IndexEntryAttributes.FieldTypeValues.LONG:
                    result =  resultData.size()==1 ? Long.parseLong(resultData.get(0).toString()) :  resultData.stream().collect(Collectors.counting());
                    break;
                default:
                    result =  String.join (" ", resultData);
                    break;
            }

        } catch (Exception ex) {
            LOGGER.debug("problem in parsing facet: {} in {}", indexKey, valueMap.toString(), ex);
        }
        valueMap.put(indexKey, result);
        return result.toString();
    }
}
