/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.FileDownloadService;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.service.ZipDownloadService;
import uk.ac.ebi.biostudies.service.file.FileMetaData;
import uk.ac.ebi.biostudies.service.file.chain.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FileDownloadServiceImpl implements FileDownloadService {



    private static final Logger logger = LogManager.getLogger(FileDownloadServiceImpl.class);



    @Autowired
    FileService fileService;
    @Autowired
    SearchService searchService;

    @Autowired
    ZipDownloadService zipDownloadService;

    @Autowired
    IndexConfig indexConfig;

    @Autowired
    FireService fireService;






    public void sendFile(String collection, HttpServletRequest request, HttpServletResponse response) throws Exception {

        request.setCharacterEncoding("UTF-8");
//        IDownloadFile downloadFile = null;
        FileMetaData fileMetaData = null;
        try {
            List<String> requestArgs = new ArrayList<>(Arrays.asList(
                    request.getRequestURI().replaceAll(request.getContextPath()
                                    + (StringUtils.isEmpty(collection) ? "" : "/" + collection)
                                    + "/files/", "")
                            .split("/")));

            String accession = requestArgs.remove(0);
            String requestedFilePath = URLDecoder.decode(
                    StringUtils.replace(StringUtils.join(requestArgs, '/'), "..", "")
                    , StandardCharsets.UTF_8.toString());
            String key = request.getParameter("key");
            if ("null".equalsIgnoreCase(key)) {
                key = null;
            }

            if (key != null && StudyUtils.isPageTabFile(accession, requestedFilePath)) {
                throw new SubmissionNotAccessibleException();
            }

            Document document = searchService.getDocumentByAccession(accession, key);

            if (!searchService.isDocumentInCollection(document, collection)) {
                throw new SubmissionNotAccessibleException();
            }
            if (document == null) {
                throw new FileNotFoundException("File does not exist or user does not have the rights to download it.");
            }
            String relativePath = document.get(Constants.Fields.RELATIVE_PATH);
            if (relativePath == null) {
                throw new FileNotFoundException("File does not exist or user does not have the rights to download it.");
            }
            String storageModeString = document.get(Constants.Fields.STORAGE_MODE);
            Constants.File.StorageMode storageMode = Constants.File.StorageMode.valueOf(StringUtils.isEmpty(storageModeString) ? "NFS" : storageModeString);

            fileMetaData = new FileMetaData(accession, requestedFilePath, requestedFilePath, relativePath, storageMode, (" " + document.get(Constants.Fields.ACCESS) + " ").toLowerCase().contains(" public "), (key!=null && key.length()>0), collection);

            try {
                fileMetaData.setThumbnail(false);
                fileService.getDownloadFile(fileMetaData);
            }catch (Exception exception){
                fileMetaData.close();
                logger.error(exception);
            }
            List<FileChainFilter> appliedFilters = new ArrayList<>();
            appliedFilters.add(new FtpRedirectFilter());
            appliedFilters.add(new NfsFilter());
            appliedFilters.add(new MageTabFilter());
            appliedFilters.add(new SendFileFilter());

            for(FileChainFilter fileFilter:appliedFilters){
                if(fileFilter.handleFile(fileMetaData, request, response))
                    break;
            }
            logger.debug("Download of [{}] completed - {}", fileMetaData.getUiRequestedPath(), request.getMethod());

        } finally {
            if (null != fileMetaData) {
                fileMetaData.close();
            }
        }
    }

}
