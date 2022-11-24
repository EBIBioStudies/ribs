package uk.ac.ebi.biostudies.service.impl;

import com.google.common.collect.Collections2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.file.download.FilteredMageTabDownloadFile;
import uk.ac.ebi.biostudies.file.download.FilteredMageTabDownloadStream;
import uk.ac.ebi.biostudies.file.download.IDownloadFile;
import uk.ac.ebi.biostudies.service.SearchService;
import uk.ac.ebi.biostudies.service.SubmissionNotAccessibleException;
import uk.ac.ebi.biostudies.service.ZipDownloadService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service
public class ZipDownloadServiceImpl implements ZipDownloadService {

    private final Logger logger = LogManager.getLogger(ZipDownloadServiceImpl.class.getName());
    @Autowired
    SearchService searchService;
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    FireService fireService;

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

        if (relativePath == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            throw new Exception("File does not exist or user does not have the rights to download it.");
        }

        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", "attachment; filename=" + accession + ".zip");
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

        byte[] buffer = new byte[4 * IDownloadFile.KB];
        String canonicalPath = relativePath + "/Files/";
        InputStream zipInputStream = null;
        for (String fileEntry : files) {
            IDownloadFile fireFile = null;
            try {
                final String fileName = StringUtils.replace(fileEntry, "..", ".");
                fireFile = fireService.getFireFile(accession, relativePath, fileName);
                zipInputStream = fireFile.getInputStream();

                String curFileName = fileName.replaceAll(canonicalPath, "");
                ZipEntry entry = new ZipEntry(curFileName + (fireFile.isDirectory() ? ".zip" : ""));
                zos.putNextEntry(entry);
                if (key != null) {
                    FilteredMageTabDownloadStream filteredMageTabDownloadStream =
                            new FilteredMageTabDownloadStream(curFileName, zipInputStream);
                    if (filteredMageTabDownloadStream.isSupported()) {
                        zipInputStream = filteredMageTabDownloadStream.getInputStream();
                    }
                }
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            } finally {
                zos.closeEntry();
                if (fireFile != null)
                    fireFile.close();
            }
        }
    }

    private void sendNFSZip(String key, String relativePath, String rootFolder, String[] files, ZipOutputStream zos) throws IOException {

        byte[] buffer = new byte[4 * IDownloadFile.KB];
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
                        FilteredMageTabDownloadFile filteredMageTabDownloadFile =
                                new FilteredMageTabDownloadFile(file);
                        if (filteredMageTabDownloadFile.isSupported()) {
                            fin = filteredMageTabDownloadFile.getInputStream();
                        }
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
