package uk.ac.ebi.biostudies.service.impl;

import com.google.common.collect.Collections2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.service.ZipDownloadService;
import uk.ac.ebi.biostudies.service.file.FileMetaData;
import uk.ac.ebi.biostudies.service.file.filter.MageTabFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static uk.ac.ebi.biostudies.service.file.filter.FileChainFilter.KB;


@Service
public class ZipDownloadServiceImpl implements ZipDownloadService {

    private final Logger logger = LogManager.getLogger(ZipDownloadServiceImpl.class.getName());
    @Autowired
    SearchService searchService;
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    FileService fileService;


    @Override
    public void sendZip(HttpServletRequest request, HttpServletResponse response, String[] files, Constants.File.StorageMode storageMode) throws Exception {

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
        }
        if (doc == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String accession = doc.get(Constants.Fields.ACCESSION);
        String relativePath = doc.get(Constants.Fields.RELATIVE_PATH);
        String docKey = doc.get(Constants.Fields.SECRET_KEY);
        boolean isPublicStudy = StudyUtils.isPublicStudy(doc);
        if (relativePath == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            throw new Exception("File does not exist or user does not have the rights to download it.");
        }
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", "attachment; filename=" + accession + ".zip");
        if(!isPublicStudy && storageMode == Constants.File.StorageMode.NFS) {
            relativePath = StudyUtils.modifyRelativePathForPrivateStudies(docKey, relativePath);
        }
        String rootFolder = indexConfig.getFileRootDir();

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            if (storageMode == Constants.File.StorageMode.FIRE)
                sendFireZip(accession, key, relativePath, rootFolder, files, zos);
            else
                sendNFSZip(key, relativePath, rootFolder, files, zos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFireZip(String accession, String key, String relativePath, String rootFolder, String[] files, ZipOutputStream zos) throws Exception {

        byte[] buffer = new byte[64 * KB];
        String canonicalPath = relativePath + "/Files/";
        InputStream zipInputStream = null;
        for (String fileEntry : files) {
            final String fileName = StringUtils.replace(fileEntry, "..", ".");
            FileMetaData fireFile = new FileMetaData(accession, fileName, fileName, relativePath, true, Constants.File.StorageMode.FIRE);
            try {
                fileService.getDownloadFile(fireFile);
                zipInputStream = fireFile.getInputStream();

                String curFileName = fileName.replaceAll(canonicalPath, "");
                ZipEntry entry = new ZipEntry(curFileName + (fireFile.isDirectory() ? ".zip" : ""));
                zos.putNextEntry(entry);
                if (key != null) {
                    zipInputStream = MageTabFilter.applyFilter(curFileName, zipInputStream);
                }
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            } finally {
                try{
                    zos.closeEntry();
                }catch (Exception exception){
                    logger.debug("cant close zip stream. Download cancelled by user", exception);
                }
                if (fireFile != null)
                    fireFile.close();
            }
        }
    }

    private void sendNFSZip(String key, String relativePath, String rootFolder, String[] files, ZipOutputStream zos) throws IOException {

        byte[] buffer = new byte[64 * KB];
        Stack<String> fileStack = new Stack<>();
        fileStack.addAll(Arrays.asList(files));
        String canonicalPath = rootFolder + "/" + relativePath + "/Files/";
        String envIndependentCanonicalPath = new File(canonicalPath).getCanonicalPath();
        var streamedFiles = new HashSet<String>();
        while (!fileStack.empty()) {
            final String filename = StringUtils.replace(fileStack.pop(), "..", ".");
            File file = new File(canonicalPath, filename);
            if (!file.getCanonicalPath().startsWith(envIndependentCanonicalPath)) break;
            if (!file.exists()) {
                logger.debug("{} not found ", file.getAbsolutePath());
                file = new File(canonicalPath + "u/", filename);
                logger.debug("Trying ", file.getAbsolutePath());
            }
            if (file.exists() && !streamedFiles.contains(file.getAbsolutePath())) {
                if (file.isDirectory()) {
                    ZipEntry entry = new ZipEntry(filename + "/");
                    zos.putNextEntry(entry);
                    fileStack.addAll(Collections2.transform(Arrays.asList(file.list()),
                            f -> filename + "/" + f));
                } else {
                    ZipEntry entry = new ZipEntry(filename);
                    zos.putNextEntry(entry);
                    InputStream fin = new FileInputStream(file);
                    if (key != null) {
                        fin = MageTabFilter.applyFilter(filename, fin);
                    }
                    int length;
                    while ((length = fin.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    fin.close();
                }
                zos.closeEntry();
                streamedFiles.add(file.getAbsolutePath());
            }

        }
    }
}
