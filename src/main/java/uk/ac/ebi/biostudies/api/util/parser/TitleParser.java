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
 * unfortunately there are two titles in json in similar paths therefore it is hard to parse it automatically
 */
public class TitleParser extends AbstractParser  {
    private final static Logger LOGGER = LogManager.getLogger(TitleParser.class.getName());

    @Override
    public String parse(Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
        String title = "";
        String indexKey = indexEntry.get(Constants.IndexEntryAttributes.NAME).asText();

        try {
            title = StreamSupport.stream(submission.get("section").get("attributes").spliterator(), false)
                    .filter(jsonNode -> jsonNode.get("name").textValue().equalsIgnoreCase("Title"))
                    .findFirst().get().get("value").textValue().trim();
        } catch (Exception ex1) {
            //LOGGER.debug( "Title not found. Trying submission title for " + accession);
            try {
                title = StreamSupport.stream(submission.get("attributes").spliterator(), false)
                        .filter(jsonNode -> jsonNode.get("name").textValue().equalsIgnoreCase("Title"))
                        .map(jsonNode -> jsonNode.findValue("value").asText().trim())
                        .collect(Collectors.joining(","));//get().get("value").textValue().trim();
            } catch ( Exception ex2) {
                //LOGGER.error("Title not found for " + submission.toString().substring(0,100));
            }
        }
        if(title.isEmpty())
            LOGGER.error("title is empty in accession: {}", valueMap.toString());
        valueMap.put(indexKey, title);
        return title;

    }
}
