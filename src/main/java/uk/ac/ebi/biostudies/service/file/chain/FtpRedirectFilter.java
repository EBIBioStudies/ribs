package uk.ac.ebi.biostudies.service.file.chain;

import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FtpRedirectFilter implements FileChainFilter{
    @Override
    public boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (fileMetaData.isPublic() && !StudyUtils.isPageTabFile(fileMetaData.getAccession(), fileMetaData.getUiRequestedPath()) ) {
            response.sendRedirect( "https://ftp.ebi.ac.uk/biostudies/"+ fileMetaData.getStorageMode().toString().toLowerCase() +"/" + fileMetaData.getRelativePath() + "/Files/" + fileMetaData.getUiRequestedPath() );
            return true;
        }
        return false;
    }
}
