package uk.ac.ebi.biostudies.controller;


import static uk.ac.ebi.biostudies.api.util.Constants.JSON_UNICODE_MEDIA_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biostudies.service.TextMiningLinkUpdater;

/**
 * Temporary url to test text mining msg rcv
 */

@RestController
@RequestMapping(value="/api/v1/extractedlink")
public class ExtractedLink {
    private static final Logger LOGGER = LogManager.getLogger(ExtractedLink.class.getName());

    @Autowired
    TextMiningLinkUpdater textMiningLinkUpdater;
    ObjectMapper mapper = new ObjectMapper();


    @RequestMapping(value = "/update", produces = JSON_UNICODE_MEDIA_TYPE, consumes = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.POST)
    public ResponseEntity<String> mine(@RequestBody String requestBody){
        try {
            textMiningLinkUpdater.receivedMessage(mapper.readTree(requestBody));
        }catch (Exception exception){
            LOGGER.error(exception);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("");
    }
}
