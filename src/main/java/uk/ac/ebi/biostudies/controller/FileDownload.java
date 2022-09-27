package uk.ac.ebi.biostudies.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.FileDownloadService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.service.ZipDownloadService;
import uk.ac.ebi.biostudies.service.impl.BatchDownloadScriptBuilder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Created by ehsan on 22/03/2017.
 */
@RestController
public class FileDownload {

    private static final Logger LOGGER = LogManager.getLogger(FileDownload.class.getName());


    static ScriptEngine jsEngine = new ScriptEngineManager().getEngineByName("nashorn");
    @Autowired
    ZipDownloadService zipDownloadService;

    @Autowired
    FileDownloadService fileDownloadService;

    @Autowired
    SearchService searchService;

    @Autowired
    IndexConfig indexConfig;

    @Autowired
    BatchDownloadScriptBuilder batchDownloadScriptBuilder;

    @RequestMapping(value = {"/files/**", "/{collection}/files/**"}, method = RequestMethod.POST)
    public void getFilesInZippedFormat(@PathVariable(required = false) String collection, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String dlType = request.getParameter("type");
        String os = request.getParameter("os");
        if (os == null || os.isEmpty())
            os = "unix";
        String fileExtension = "sh";
        fileExtension = getFileExtension(os);
        Document submissionDoc = getFilePaths(request, response);
        String relativeBaseDir = submissionDoc.get(Constants.Fields.RELATIVE_PATH);
        String accession = submissionDoc.get(Constants.Fields.ACCESSION);
        String storageModeString = submissionDoc.get(Constants.Fields.STORAGE_MODE);
        Constants.File.StorageMode storageMode = Constants.File.StorageMode.valueOf(StringUtils.isEmpty(storageModeString) ? "NFS" : storageModeString);

        if (searchService.isDocumentInCollection(submissionDoc, collection)) {
            throw new SubmissionNotAccessibleException();
        }


        dlType = dlType.replaceFirst("/", "");
        String[] files = request.getParameterMap().get("files");
        if (storageMode == Constants.File.StorageMode.FIRE) {
            files = createFireCompatibleFileNames(files);
        }
        if (dlType.equalsIgnoreCase("zip"))
            zipDownloadService.sendZip(request, response, files, storageMode);
        else if (dlType.equalsIgnoreCase("ftp") || dlType.equalsIgnoreCase("aspera")) {
            response.setContentType("application/txt; charset=UTF-8");
            response.addHeader("Content-Disposition", "attachment; filename=" + accession + "-" + os + "-" + dlType + "." + fileExtension);
            response.addHeader("Cache-Control", "no-cache");
            response.getWriter().print(batchDownloadScriptBuilder.fillTemplate(dlType, Arrays.asList(files), relativeBaseDir, os, storageMode));
            response.getWriter().close();
        }

    }


    @RequestMapping(value = {"/files/**", "/{collection}/files/**"}, method = {RequestMethod.GET, RequestMethod.HEAD})
    public void getSingleFile(@PathVariable(required = false) String collection, HttpServletRequest request, HttpServletResponse response) throws Exception {
        fileDownloadService.sendFile(collection, request, response);
    }


    private Document getFilePaths(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] args = request.getRequestURI().replaceAll(request.getContextPath() + "(/[a-zA-Z])?/files/", "").split("/");
        String key = request.getParameter("key");
        if ("null".equalsIgnoreCase(key)) {
            key = null;
        }

        Document doc = null;
        try {
            doc = searchService.getDocumentByAccession(args[0], key);
        } catch (SubmissionNotAccessibleException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        if (doc == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        String relativePath = doc.get(Constants.Fields.RELATIVE_PATH);

        if (relativePath == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return doc;

    }

    private String getFileExtension(String os) {
        if (os.equalsIgnoreCase("windows"))
            return "bat";
        else return "sh";
    }

    private String[] createFireCompatibleFileNames(String[] inputFileNames) {
        String[] response = Arrays.stream(inputFileNames).map(name -> {
            try {
                return jsEngine.eval(String.format("unescape(encodeURIComponent('%s'))", name)).toString().replace("#", "%23").replace("+", "%2B").replace("=", "%3D").replace("@", "%40").replace("$", "%24");
            } catch (ScriptException e) {
                LOGGER.error(name + " problem in unescapeing", e);
            }
            return "";
        }).toArray(String[]::new);
        return response;
    }
}
