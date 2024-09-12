package uk.ac.ebi.biostudies.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.file.Thumbnails;
import uk.ac.ebi.biostudies.service.FileDownloadService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by ehsan on 20/03/2017.
 */

@RestController
@RequestMapping(value="/thumbnail")
public class Thumbnail {

    private Logger logger = LogManager.getLogger(Thumbnail.class.getName());


    @Autowired
    Thumbnails thumbnails;
    @Autowired
    SearchService searchService;
    @Autowired
    FileDownloadService fileDownloadService;
    @Autowired
    StudyUtils studyUtils;


    /**
     * TODO UI should pass correct related path to the server, in the previous version It calculated from xml sax transformations but in current version ui has this data in json
     * @param response
     * @param accession
     */
    @RequestMapping(value = "/{accession}/**", method = RequestMethod.GET)
    public void getThumbnail(HttpServletResponse response, HttpServletRequest request, @PathVariable String accession, @RequestParam(value="key", required=false) String key) throws ParseException {
        String name = request.getAttribute( HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE ).toString();
        String prefix = "/thumbnail/"+accession+"/";
        name = name.substring(name.indexOf(prefix)+prefix.length());
        if(accession==null || accession.isEmpty() || name==null || name.isEmpty())
            return;

        try {//Maybe I need to apply some modification to change accession to relative path
            Document document = null;
            try {
                document = searchService.getDocumentByAccession(accession, key);
            } catch (SubmissionNotAccessibleException e) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if(document==null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String relativePath = document.get(Constants.Fields.RELATIVE_PATH);
            String storageModeString = document.get(Constants.Fields.STORAGE_MODE);
            Constants.File.StorageMode storageMode = Constants.File.StorageMode.valueOf(StringUtils.isEmpty(storageModeString) ? "NFS" : storageModeString);
            boolean isPublicStudy = StudyUtils.isPublicStudy(document);
            if(!isPublicStudy && storageMode == Constants.File.StorageMode.NFS) {
                relativePath = studyUtils.modifyRelativePathForPrivateStudies(document.get(Constants.Fields.SECRET_KEY), relativePath);
            }
            try {
                name = URLDecoder.decode(name, StandardCharsets.UTF_8.name());
                if(storageMode == Constants.File.StorageMode.FIRE) {
                    name = StudyUtils.decodeForFireBug(name);
                }
            } catch (Exception exception) {
                logger.error("problem in encoding thumbnail image name {}", name, exception);
            }
            thumbnails.sendThumbnail(response, accession, relativePath, name, storageMode, isPublicStudy);
        } catch (IOException e) {
            logger.error("problem in creating thumbnail ", e);
        }
    }
}
