package uk.ac.ebi.biostudies.service.file.chain;

import uk.ac.ebi.biostudies.service.file.FileMetaData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FileChainFilter {
    static int KB = 1024;
    boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
