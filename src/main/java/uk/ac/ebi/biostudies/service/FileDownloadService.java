package uk.ac.ebi.biostudies.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface FileDownloadService {
  void sendFile(String collection, HttpServletRequest request, HttpServletResponse response)
      throws Exception;
}
