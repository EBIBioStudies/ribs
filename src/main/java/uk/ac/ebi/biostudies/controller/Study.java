package uk.ac.ebi.biostudies.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.PublicRESTMethod;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.auth.Session;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.FilePaginationService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static uk.ac.ebi.biostudies.api.util.Constants.JSON_UNICODE_MEDIA_TYPE;


/**
 * Created by awais on 14/02/2017.
 */
@RestController
@RequestMapping(value = "/api")
public class Study {

    private final Logger logger = LogManager.getLogger(Study.class.getName());
    @Autowired
    SearchService searchService;
    @Autowired
    FilePaginationService paginationService;
    @Autowired
    IndexConfig indexConfig;


    @RequestMapping(value = "/v1/collections/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getCollection(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey, HttpServletResponse httpResponse) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.COLLECTION, "v1", httpResponse);
    }

    @RequestMapping(value = "/v2/collections/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getCollectionV2(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.COLLECTION, "v2", null);
    }

    @RequestMapping(value = "/v2/studies/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getStudyV2(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.STUDY, "v2", null);
    }

    @RequestMapping(value = "/v1/studies/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getStudy(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey, HttpServletResponse httpResponse) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.STUDY, "v1", httpResponse);
    }

    @RequestMapping(value = "/v1/arrays/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getArray(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey, HttpServletResponse httpResponse) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.ARRAY, "v1", httpResponse);
    }

    @RequestMapping(value = "/v2/arrays/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getArrayV2(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.ARRAY, "v2", null);
    }

    @RequestMapping(value = "/v1/compounds/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getCompound(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey, HttpServletResponse httpResponse) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.COMPOUND, "v1", httpResponse);
    }

    @RequestMapping(value = "/v2/compounds/{accession:.+}", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getCompoundV2(@PathVariable("accession") String accession, @RequestParam(value = "key", required = false) String seckey) {
        return prepareResponse(accession, seckey, Constants.SubmissionTypes.COMPOUND, "v2", null);
    }

    @RequestMapping(value = "/v1/studies/{accession:.+}/similar", produces = {JSON_UNICODE_MEDIA_TYPE}, method = RequestMethod.GET)
    public ResponseEntity<String> getSimilarStudies(@PathVariable("accession") String accession,
                                                    @RequestParam(value = "key", required = false) String seckey)
            throws Throwable {
        if ("null".equalsIgnoreCase(seckey)) {
            seckey = null;
        }
        try {
            Document document = searchService.getDocumentByAccession(accession, seckey);
            if (document != null) {
                accession = document.get(Constants.Fields.ACCESSION);
                ResponseEntity result = new ResponseEntity(searchService.getSimilarStudies(accession.replace("..", ""), seckey), HttpStatus.OK);
                return result;
            }
        } catch (SubmissionNotAccessibleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"errorMessage\":\"Study is not accessible\"}");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON).body("{\"similarStudies\":[]}");

    }

    @PublicRESTMethod
    @RequestMapping(value = "/v1/studies/{accession:.+}/info", produces = JSON_UNICODE_MEDIA_TYPE, method = RequestMethod.GET)
    public ResponseEntity<String> getStudyInfo(@PathVariable String accession, @RequestParam(value = "key", required = false) String seckey) {
        if ("null".equalsIgnoreCase(seckey)) {
            seckey = null;
        }
        try {
            return ResponseEntity.status(HttpStatus.OK).body(paginationService.getStudyInfo(accession, seckey).toString());
        } catch (SubmissionNotAccessibleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"errorMessage\":\"Study not accessible\"}");
        }

    }

    private ResponseEntity<String> prepareResponse(String accession, String seckey, String type, String version, HttpServletResponse httpResponse) {
        if ("null".equalsIgnoreCase(seckey)) {
            seckey = null;
        }
        Document document = null;
        try {
            document = searchService.getDocumentByAccessionAndType(accession, seckey, type);
        } catch (SubmissionNotAccessibleException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"errorMessage\":\"Study not accessible\"}");
        }
        if (document == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON).body("{\"errorMessage\":\"Study not found\"}");
        }
        accession = document.get(Constants.Fields.ACCESSION);
        boolean isPublicStudy = StudyUtils.isPublicStudy(document);
        String relativePath = document.get(Constants.Fields.RELATIVE_PATH);
        String storageModeString = document.get(Constants.Fields.STORAGE_MODE);
        storageModeString = storageModeString.isEmpty()? "FIRE":storageModeString;
        Constants.File.StorageMode storageMode = Constants.File.StorageMode.valueOf(StringUtils.isEmpty(storageModeString) ? "NFS" : storageModeString);
        String originalSecretKey = seckey;
        if(Session.getCurrentUser()!=null)
            seckey = document.get(Constants.Fields.SECRET_KEY);
        if((!isPublicStudy || indexConfig.isMigratingNotCompleted()) && storageMode == Constants.File.StorageMode.FIRE){
            return sendFireResponse(accession, relativePath, originalSecretKey, seckey, storageMode, isPublicStudy);
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode studyFtpLink = mapper.createObjectNode();
        if(!isPublicStudy) {
            relativePath = StudyUtils.modifyRelativePathForPrivateStudies(seckey, relativePath);
        }
        String ftpHttpLink = indexConfig.getFtpOverHttpUrl(storageMode) + "/" + relativePath+"/";
        studyFtpLink.put("ftpHttp_link", ftpHttpLink);//+(type.trim().equalsIgnoreCase("study")?"":accession.toUpperCase()+".json"));
        if(version.equalsIgnoreCase("v2")) {
            return new ResponseEntity(studyFtpLink, HttpStatus.OK);
        }else if(httpResponse!=null) {
            try {
                ftpHttpLink+=accession.toUpperCase()+".json";
                httpResponse.sendRedirect(ftpHttpLink);
            }catch (IOException e) {
                logger.debug(e.getMessage());
            }
            return null;
        }
        return new ResponseEntity(studyFtpLink, HttpStatus.OK);
    }

    private ResponseEntity sendFireResponse(String accession, String relativePath, String originalSecretKey, String seckey, Constants.File.StorageMode storageMode,boolean isPublicStudy) {InputStreamResource result;
        try {
            result = searchService.getStudyAsStream(accession.replace("..", ""), relativePath, originalSecretKey != null, storageMode, isPublicStudy, seckey);
        } catch (IOException e) {
            logger.error(e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON).body("{\"errorMessage\":\"Study not found\"}");
        }
        return new ResponseEntity(result, HttpStatus.OK);}
}
