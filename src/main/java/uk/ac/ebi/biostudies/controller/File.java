package uk.ac.ebi.biostudies.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biostudies.api.util.DataTableColumnInfo;
import uk.ac.ebi.biostudies.service.FilePaginationService;
import uk.ac.ebi.biostudies.service.ExtractedLinkPaginationService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;

import java.util.Map;

import static uk.ac.ebi.biostudies.api.util.Constants.JSON_UNICODE_MEDIA_TYPE;

@RestController
@RequestMapping(value="/api/v1")
public class File {
    private Logger logger = LogManager.getLogger(File.class.getName());

    @Autowired
    FilePaginationService paginationService;
    @Autowired
    ExtractedLinkPaginationService linkPaginationService;


    @RequestMapping(value = "/files/{accession:.+}", produces = JSON_UNICODE_MEDIA_TYPE, method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<String> search(@PathVariable(value="accession") String accession,
                                             @RequestParam(value="start", required=false, defaultValue = "0") Integer start,
                                             @RequestParam(value="length", required=false, defaultValue = "5") Integer pageSize,
                                             @RequestParam(value="search[value]", required=false, defaultValue = "") String search,
                                             @RequestParam(value="draw", required=false, defaultValue = "1") Integer draw,
                                             @RequestParam(value="metadata", required=false, defaultValue = "true") boolean metadata,
                                             @RequestParam MultiValueMap<String,String> order,
                                             @RequestParam(value="key", required=false) String seckey
    ) throws Exception
    {
        if ("null".equalsIgnoreCase(seckey)) {
            seckey = null;
        }
        Map<Integer, DataTableColumnInfo> parseResult = DataTableColumnInfo.ParseDataTableRequest(order);
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(paginationService.getFileList(accession, start, pageSize, search, draw, metadata, parseResult, seckey).toString());
        } catch (SubmissionNotAccessibleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"errorMessage\":\"Study not accessible\"}");
        }
    }

    @RequestMapping(value = "/{accession:.+}/extractedlinks", produces = JSON_UNICODE_MEDIA_TYPE, method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<String> searchLink(@PathVariable(value="accession") String accession,
                                         @RequestParam(value="start", required=false, defaultValue = "0") Integer start,
                                         @RequestParam(value="length", required=false, defaultValue = "5") Integer pageSize,
                                         @RequestParam(value="search[value]", required=false, defaultValue = "") String search,
                                         @RequestParam(value="draw", required=false, defaultValue = "1") Integer draw,
                                         @RequestParam(value="metadata", required=false, defaultValue = "true") boolean metadata,
                                         @RequestParam MultiValueMap<String,String> order,
                                         @RequestParam(value="key", required=false) String seckey
    ) throws Exception
    {
        if ("null".equalsIgnoreCase(seckey)) {
            seckey = null;
        }
        Map<Integer, DataTableColumnInfo> parseResult = DataTableColumnInfo.ParseDataTableRequest(order);
        try {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(linkPaginationService.getExtractedLinkList(accession, start, pageSize, search, draw, metadata, parseResult, seckey).toString());
        } catch (SubmissionNotAccessibleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"errorMessage\":\"Study not accessible\"}");
        }
    }

}
