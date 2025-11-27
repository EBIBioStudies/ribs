package uk.ac.ebi.biostudies.service.file.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.config.FireConfig;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

/**
 * File filter that redirects eligible file requests to FTP-over-HTTP endpoints when enabled.
 *
 * <p>This filter is part of the file serving chain and intercepts requests for files that meet
 * specific access criteria. When FTP redirect is enabled via configuration, it redirects to the
 * appropriate FTP-over-HTTP URL constructed from the file's storage mode and relative path,
 * excluding page tab files.
 *
 * <p>Implements constructor injection for dependencies and returns {@code true} when a redirect is
 * performed, signaling the chain to stop further processing.
 */
@Component
public class FtpRedirectFilter implements FileChainFilter {

  private final FireConfig fireConfig;
  private final IndexConfig indexConfig;

  /**
   * Constructs the filter with required configuration dependencies.
   *
   * @param fireConfig configuration for FTP redirect enabling/disabling
   * @param indexConfig configuration for FTP-over-HTTP URL resolution
   */
  public FtpRedirectFilter(FireConfig fireConfig, IndexConfig indexConfig) {
    this.fireConfig = fireConfig;
    this.indexConfig = indexConfig;
  }

  /**
   * Handles file requests and conditionally redirects to FTP-over-HTTP endpoint.
   *
   * <p>Redirect occurs when {@link #shouldHandleFile(FileMetaData)} returns {@code true}.
   *
   * <p>The redirect URL is built as: {@code baseUrl + "/" + relativePath + "/Files/" +
   * requestedPath}
   *
   * @param fileMetaData metadata for the requested file
   * @param request the HTTP servlet request
   * @param response the HTTP servlet response for redirect
   * @return {@code true} if redirect was sent and chain should stop, {@code false} otherwise
   * @throws Exception if redirect construction or sending fails
   */
  @Override
  public boolean handleFile(
      FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    if (shouldHandleFile(fileMetaData)) {

      String url = indexConfig.getFtpOverHttpUrl(fileMetaData.getStorageMode());
      response.sendRedirect(
          url
              + "/"
              + fileMetaData.getRelativePath()
              + "/Files/"
              + (fileMetaData.getUnDecodedRequestedPath() != null
                  ? fileMetaData.getUnDecodedRequestedPath()
                  : fileMetaData.getUiRequestedPath()));
      return true;
    }
    return false;
  }

  /**
   * Determines if this filter should handle and redirect the given file request.
   *
   * <p>Conditions for handling:
   *
   * <ul>
   *   <li>FTP redirect is enabled in {@link FireConfig}
   *   <li>File is public ({@link FileMetaData#isPublic()}) <strong>OR</strong> storage mode is NFS
   *       ({@link Constants.File.StorageMode#NFS})
   *   <li>The file is not a page tab file ({@link StudyUtils#isPageTabFile(String, String)})
   * </ul>
   *
   * @param fileMetaData metadata for the requested file
   * @return {@code true} if FTP redirect conditions are met
   */
  private boolean shouldHandleFile(FileMetaData fileMetaData) {
    return fireConfig.isFtpRedirectEnabled()
        && (fileMetaData.isPublic()
            || Constants.File.StorageMode.NFS.equals(fileMetaData.getStorageMode()))
        && !StudyUtils.isPageTabFile(
            fileMetaData.getAccession(), fileMetaData.getUiRequestedPath());
  }
}
