package uk.ac.ebi.biostudies.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static uk.ac.ebi.biostudies.api.util.Constants.JSON_UNICODE_MEDIA_TYPE;

@RestController
@RequestMapping(value = "/api/v1")
public class ServerStatus {

  private final Logger logger = LogManager.getLogger(ServerStatus.class.getName());

  @RequestMapping(value = "/status", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
  public ResponseEntity<String> status(@RequestParam(value = "path", required = false) String path)
      throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode message = mapper.createObjectNode();
    message.put("status", "up");
    if (path != null && !StringUtils.isEmpty(path)) {
      Files.list(Paths.get(path))
          .forEach(
              p -> {
                try {
                  message.put(p.toString(), Files.size(p));
                } catch (IOException e) {

                }
              });
    }
    return ResponseEntity.ok(mapper.writeValueAsString(message));
  }
}
