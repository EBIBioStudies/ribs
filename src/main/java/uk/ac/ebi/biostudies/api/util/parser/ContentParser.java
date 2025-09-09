package uk.ac.ebi.biostudies.api.util.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.ReadContext;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.ebi.biostudies.api.util.Constants;

public class ContentParser extends AbstractParser {
    @Override
    public String parse(Map<String, Object> valueMap, JsonNode submission, ReadContext jsonPathContext) {
        String accession = valueMap.get(Constants.Fields.ACCESSION).toString();
        StringBuilder content = new StringBuilder( String.join(" ", accession ));
        content.append(" ");
        if (accession.startsWith("S-EPMC")) {// hack to make sure we are indexing PMC accessions in full text
            content.append( accession.substring(3) ).append(" ");
        }
        content.append(String.join(" ", submission.get("section").findValuesAsText("value")));
        content.append(" ");
        content.append(submission.findValues("files").stream()
                .map(jsonNode -> Stream.concat(jsonNode.findValuesAsText("fileName").stream(), jsonNode.findValuesAsText("path").stream())
                        .filter(text -> text != null && !text.equalsIgnoreCase("null"))
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining(" ")));
        content.append(" ");
        content.append(submission.findValues("links").stream()
                .map(jsonNode -> jsonNode.findValuesAsText("url").stream()
                        .filter(text -> text != null && !text.equalsIgnoreCase("null"))
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining(" ")));
        valueMap.put( Constants.Fields.CONTENT, content.toString());
        return "";
    }
}
