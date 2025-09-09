package uk.ac.ebi.biostudies.service.file.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

public interface FileChainFilter {
    int KB = 1024;
    boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
