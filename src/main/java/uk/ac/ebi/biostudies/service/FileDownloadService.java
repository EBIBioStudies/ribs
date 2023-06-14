package uk.ac.ebi.biostudies.service;

import uk.ac.ebi.biostudies.service.file.FileMetaData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;

public interface FileDownloadService {
//    void getDownloadFile(FileMetaData fileMetaData) throws FileNotFoundException;
    void sendFile(String collection, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
