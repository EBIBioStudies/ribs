package uk.ac.ebi.biostudies.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.schedule.jobs.UpdateOntologyJob;
import uk.ac.ebi.biostudies.service.IndexManagementService;
import uk.ac.ebi.biostudies.service.IndexService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.impl.IndexServiceImpl;

import static uk.ac.ebi.biostudies.api.util.Constants.JSON_UNICODE_MEDIA_TYPE;
import static uk.ac.ebi.biostudies.api.util.Constants.STRING_UNICODE_MEDIA_TYPE;


@SuppressWarnings("Duplicates")
@RestController
@RequestMapping(value = "/api/v1")
public class Index {

    @Autowired
    IndexService indexService;
    @Autowired
    IndexManager indexManager;
    @Autowired
    IndexManagementService indexManagementService;
    @Autowired
    SearchService searchService;
    @Autowired
    UpdateOntologyJob updateOntologyJob;
    @Autowired
    IndexConfig indexConfig;
    private Logger logger = LogManager.getLogger(Index.class.getName());

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @RequestMapping(value = "/index/reload/{filename:.*}", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public ResponseEntity<String> indexAll(@PathVariable("filename") String filename) throws Exception {
        if (indexManagementService.isClosed())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Indexer does not accept new submissions");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode message = mapper.createObjectNode();
        try {
            indexService.getIndexFileQueue().put(filename);
            message.put("message", filename + " queued for indexing");
            logger.debug("Adding {} to indexing queue at position {}", filename, indexService.getIndexFileQueue().size());
            return ResponseEntity.ok(mapper.writeValueAsString(message));
        } catch (Exception e) {
            logger.error(e);
            message.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapper.writeValueAsString(message));
        }

    }

    @RequestMapping(value = "/index/clear", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public ResponseEntity<String> clearIndex() throws Exception {
        if (indexManagementService.isClosed())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Indexer does not accept new submissions");
        indexService.clearIndex(true);
        return new ResponseEntity<String>("{\"message\":\"Index empty\"}", HttpStatus.OK);
    }

    @RequestMapping(value = "/index/delete/{accession}", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public ResponseEntity<String> deleteDoc(@PathVariable(required = false) String accession) throws Exception {
        if (indexManagementService.isClosed())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Indexer does not accept new submissions");
        indexService.deleteDoc(accession);
        return new ResponseEntity<String>("{\"message\":\"" + accession + " deleted\"}", HttpStatus.OK);
    }

    @RequestMapping(value = "/index/close", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public ResponseEntity<String> shutDown() throws Exception {
        indexManagementService.closeWebsocket();
        return new ResponseEntity<String>("{\"message\":\"Closing indexer\"}", HttpStatus.OK);
    }

    @RequestMapping(value = "/index/open", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public ResponseEntity<String> start() throws Exception {
        indexManagementService.openWebsocket();
        return new ResponseEntity<String>("{\"message\":\"Indexer open\"}", HttpStatus.OK);
    }

    @RequestMapping(value = "/index/status", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public ResponseEntity<String> getStatus() throws Exception {
        if (!indexManagementService.isClosed())
            return new ResponseEntity<String>("{\"message\":\"UP\"}", HttpStatus.OK);
        else if (indexService.getIndexFileQueue().size() == 0 && IndexServiceImpl.ActiveExecutorService.get() == 0) {
            return new ResponseEntity<String>("{\"message\":\"DOWN\"}", HttpStatus.OK);
        } else {
            return new ResponseEntity<String>("{\"message\":\"CLOSING\"}", HttpStatus.OK);
        }
    }

    /**
     * updating and building EFO ontology index manually
     *
     * @return
     */
    @RequestMapping(value = "/index/efo/build", produces = STRING_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public String buildEFOIndex() {
        try {
            updateOntologyJob.doExecute();
        } catch (Exception ex) {
            logger.error(ex);
            return String.format("Error: %s", ex.getMessage());
        }
        return "Updating and building EFO Ontology";
    }

    @RequestMapping(value = "/index/backup", method = RequestMethod.GET, produces = STRING_UNICODE_MEDIA_TYPE)
    String backUp() {
        try {
            indexManager.takeIndexSnapShotForBackUp();
            indexManager.copyBackupToRemote();
        } catch (Exception ex) {
            logger.error(ex);
            return String.format("Error: %s", ex.getMessage());
        }
        return "Back up copied successfully!";
    }

    @RequestMapping(value = "/index/closeindex", method = RequestMethod.GET, produces = STRING_UNICODE_MEDIA_TYPE)
    String closeIndex() {
        searchService.clearStatsCache();
        indexManagementService.stopAcceptingSubmissionMessagesAndCloseIndices();
        return "index closed successfully";
    }

    @RequestMapping(value = "/index/openindex", method = RequestMethod.GET, produces = STRING_UNICODE_MEDIA_TYPE)
    String openIndex() {
        searchService.clearStatsCache();
        indexManagementService.openIndicesWritersAndSearchersStartStomp();
        return "all indices opened successfully";
    }

    @RequestMapping(value = "/index/loadbackup", method = RequestMethod.GET, produces = STRING_UNICODE_MEDIA_TYPE)
    String loadBackup() {
        searchService.clearStatsCache();
        indexManagementService.stopAcceptingSubmissionMessagesAndCloseIndices();
        if (indexManager.copyBackupToLocal()) {
            indexManagementService.openIndicesWritersAndSearchersStartStomp();
            indexManagementService.openEfoIndexAndLoadOntology();
            return "Backup loaded successfully!";
        } else {
            return "problem in loading backup!!!";
        }
    }

    @RequestMapping(value = "/index/synctime", method = RequestMethod.GET, produces = STRING_UNICODE_MEDIA_TYPE)
    String getBackupSyncTime() {
        return indexManager.getBackUpSynchTime();
    }

}
