package uk.ac.ebi.biostudies.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class ExtractedLink {
    private final static String ANSWER = "anns";
    private final static String EXACT = "exact";
    private final static String TYPE = "type";
    private final static String LINK = "uri";
    private final static String TAGS = "tags";

    private String type;
    private String value;
    private String link;

    public static List<ExtractedLink> parseLinks(JsonNode msg){
        List<ExtractedLink> allLinks = new ArrayList<>();
        if(msg==null) return allLinks;
        ArrayNode anns = (ArrayNode)msg.get("anns");
        if(anns==null || anns.size()<1)
            return allLinks;
        for(JsonNode node: anns){
            ExtractedLink lInfo = new ExtractedLink();
            lInfo.setValue(node.get(EXACT).asText(""));
            lInfo.setType(node.get(TYPE).asText(""));
            ArrayNode tagNode = (ArrayNode) node.get(TAGS);
            if(tagNode!=null && tagNode.get(0)!=null && tagNode.get(0).get(LINK)!=null)
                lInfo.setLink(tagNode.get(0).get(LINK).asText(""));
            allLinks.add(lInfo);
        }
        return allLinks;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
