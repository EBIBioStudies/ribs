package uk.ac.ebi.biostudies.service;

import uk.ac.ebi.biostudies.api.util.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ZipDownloadService {

  void sendZip(
      HttpServletRequest request,
      HttpServletResponse response,
      String[] files,
      Constants.File.StorageMode storageMode)
      throws Exception;
}
