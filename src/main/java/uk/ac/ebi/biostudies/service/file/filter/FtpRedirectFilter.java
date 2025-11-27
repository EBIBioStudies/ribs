package uk.ac.ebi.biostudies.service.file.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.config.FireConfig;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

@Component
public class FtpRedirectFilter implements FileChainFilter {
    private final FireConfig fireConfig;
    private final IndexConfig indexConfig;

    public FtpRedirectFilter(FireConfig fireConfig, IndexConfig indexConfig) {
        this.fireConfig = fireConfig;
        this.indexConfig = indexConfig;
    }

    @Override
    public boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        if (fireConfig.isFtpRedirectEnabled() && fileMetaData.isPublic() &&
            !StudyUtils.isPageTabFile(fileMetaData.getAccession(), fileMetaData.getUiRequestedPath())) {

            String url = indexConfig.getFtpOverHttpUrl(fileMetaData.getStorageMode());
            response.sendRedirect(url + "/" + fileMetaData.getRelativePath() + "/Files/" +
                (fileMetaData.getUnDecodedRequestedPath() != null ?
                    fileMetaData.getUnDecodedRequestedPath() : fileMetaData.getUiRequestedPath()));
            return true;
        }
        return false;
    }
}

