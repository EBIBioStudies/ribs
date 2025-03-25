package uk.ac.ebi.biostudies.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.IndexConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

@Component
@Scope("singleton")
public class ViewCountLoader {
    private static final Logger LOGGER = LogManager.getLogger(ViewCountLoader.class);

    private static final Map<String, Long> ACCESSION_VIEW_COUNT_MAP = new HashMap();
    @Autowired
    IndexConfig indexConfig;

    public static Map<String, Long> getViewCountMap() {
        return ACCESSION_VIEW_COUNT_MAP;
    }

    public static void unloadViewCountMap() {
        ACCESSION_VIEW_COUNT_MAP.clear();
    }

    public void loadViewCountFile() {
        String accession = "";
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(indexConfig.getViewCountInputFile()))) {
            for (String line; (line = bufferedReader.readLine()) != null; ) {
                try{
                    String[] tokens = line.split(",");
                    accession = tokens[0];
                    Long count = Long.valueOf(tokens[1]);
                    ACCESSION_VIEW_COUNT_MAP.put(tokens[0], count);
                }catch (Exception exception){
                   LOGGER.debug("Problem in parsing view stats for accession: {}", accession);
                }
            }
        } catch (Exception exception) {
            LOGGER.error("problem in parsing view count file: {} ", Constants.VIEW_COUNT_CSV, exception);
        }
    }
}
