package uk.ac.ebi.biostudies.api.util.parser;

import static uk.ac.ebi.biostudies.api.util.Constants.IndexEntryAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class ParserManager {

    private Map<String, AbstractParser>  parserPool;
    private final Logger logger = LogManager.getLogger(ParserManager.class.getName());


    public void init(Map<String, JsonNode> fieldMap){
        if(parserPool==null)
            parserPool = new HashMap<>();

        for(String key: fieldMap.keySet()){
            JsonNode indexEntry = fieldMap.get(key);
            String parserClassName = indexEntry.has(IndexEntryAttributes.PARSER) ? indexEntry.get(IndexEntryAttributes.PARSER).asText() : null;
            if(parserClassName==null || parserClassName.isEmpty()) {
                parserClassName = indexEntry.has(IndexEntryAttributes.JSON_PATH)
                        ? "JPathListParser" : "SimpleAttributeParser";
            }

            if( parserClassName.equalsIgnoreCase("null")) {
                continue;
            }

            addParser(parserClassName, indexEntry);
        }
    }

    private void addParser(String parserClassName, JsonNode indexEntry){

        String parserClass =  this.getClass().getPackage().getName() + "." + parserClassName;
        Class<?> clazz;
        try {
            clazz = Class.forName(parserClass);
            AbstractParser parserObj = (AbstractParser) clazz.getDeclaredConstructor().newInstance();
            parserObj.setIndexEntry(indexEntry);
            parserPool.put( indexEntry.get(IndexEntryAttributes.NAME).asText() , parserObj);
        } catch (Exception e) {
            logger.error("cant create Parser with name {}", parserClassName, e);
        }
    }

    public AbstractParser getParser(String field){
        return parserPool.getOrDefault(field, null);
    }
}
