package uk.ac.ebi.biostudies.service.file.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class FtpRedirectFilter implements FileChainFilter{
    @Autowired
    private Environment env;
    @Override
    public boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (env.getProperty("fire.s3.ftp.redirect.enable", Boolean.class, true) && fileMetaData.isPublic() && !StudyUtils.isPageTabFile(fileMetaData.getAccession(), fileMetaData.getUiRequestedPath()) ) {
            response.sendRedirect( "https://ftp.ebi.ac.uk/biostudies/"+ fileMetaData.getStorageMode().toString().toLowerCase() +"/" + fileMetaData.getRelativePath() + "/Files/" + fileMetaData.getUiRequestedPath() );
            return true;
        }
        return false;
    }
}
