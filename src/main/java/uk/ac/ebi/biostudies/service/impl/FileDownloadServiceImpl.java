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

import java.io.FileNotFoundException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import uk.ac.ebi.biostudies.service.file.filter.*;

@Service
public class FileDownloadServiceImpl implements FileDownloadService {

    private static final Logger logger = LogManager.getLogger(FileDownloadServiceImpl.class);
    private static List<FileChainFilter> fileChainFilters;

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
    @Autowired
    FtpRedirectFilter ftpRedirectFilter;
    @Autowired
    NfsFilter nfsFilter;


    @PostConstruct
    public void init() {
        fileChainFilters = List.of(ftpRedirectFilter, nfsFilter, new SendFileFilter());
        //        fileChainFilters = List.of(ftpRedirectFilter, nfsFilter, new MageTabFilter(), new SendFileFilter()); nfs and pagetab are deprecated
    }

    public void sendFile(String collection, HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setCharacterEncoding("UTF-8");
        FileMetaData fileMetaData = null;
        try {
            String uriParts = request.getRequestURI().replaceAll(request.getContextPath() + (StringUtils.isEmpty(collection) ? "" : "/" + collection) + "/files/", "");
            List<String> requestArgs = new ArrayList<>(Arrays.asList(uriParts.split("/")));
            String accession = requestArgs.remove(0);
            String unDecodedRequestedPath = StringUtils.replace(StringUtils.join(requestArgs, '/'), "..", "");
            String requestedFilePath = URLDecoder.decode(unDecodedRequestedPath, StandardCharsets.UTF_8);
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
            String docKey = document.get(Constants.Fields.SECRET_KEY); // Just private studies have secret key?  Just NFS studies has .private folder in their path?
            if(!StudyUtils.isPublicStudy(document) && storageMode==Constants.File.StorageMode.NFS)
                relativePath = StudyUtils.modifyRelativePathForPrivateStudies(docKey, relativePath);

            fileMetaData = new FileMetaData(accession, requestedFilePath, unDecodedRequestedPath, requestedFilePath, relativePath,
                    storageMode, (" " + document.get(Constants.Fields.ACCESS) + " ").toLowerCase().contains(" public "
            ), (key != null && !key.isEmpty()), collection);

            try {
                fileMetaData.setThumbnail(false);
                fileService.getDownloadFile(fileMetaData);
                for (FileChainFilter fileFilter : fileChainFilters) {
                    if (fileFilter.handleFile(fileMetaData, request, response)) break;
                }
                logger.debug("Download of [{}] completed - {}", fileMetaData.getUiRequestedPath(), request.getMethod());
            } catch (Exception exception) {
                fileMetaData.close();
                logger.error(exception);
            }
        } finally {
            if (null != fileMetaData) {
                fileMetaData.close();
            }
        }
    }

}
