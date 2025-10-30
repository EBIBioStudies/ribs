package uk.ac.ebi.biostudies.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biostudies.service.impl.AsperaTokenGenerator;

import static uk.ac.ebi.biostudies.api.util.Constants.STRING_UNICODE_MEDIA_TYPE;

@RestController
@RequestMapping(value = "/api/v1")
public class AsperaToken {
  private static final Logger LOGGER = LogManager.getLogger(AsperaToken.class.getName());

  @Autowired AsperaTokenGenerator tokenGenerator;

  @RequestMapping(value = "/aspera", produces = STRING_UNICODE_MEDIA_TYPE, method = RequestMethod.POST)
  public ResponseEntity generateAsperaToken(@RequestBody String requestFiles) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      ArrayNode reqFileObject = (ArrayNode) mapper.readTree(requestFiles);
      if (reqFileObject == null || reqFileObject.size() < 1)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("invalid request");
      for (JsonNode srcFile : reqFileObject) {
        if (srcFile.get("source").asText() == null)
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("invalid request");
      }
      String token = tokenGenerator.postTokenRequest(reqFileObject).findValue("token").asText();
      LOGGER.debug("Aspera token {} generated successfully for paths: {}", token, requestFiles);
      return ResponseEntity.ok().body(token);
    } catch (Throwable error) {
      LOGGER.error("problem in generating aspera token for this request {}", requestFiles, error);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("invalid request");
    }
  }
}
