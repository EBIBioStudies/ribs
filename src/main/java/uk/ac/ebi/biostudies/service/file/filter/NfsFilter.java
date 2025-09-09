package uk.ac.ebi.biostudies.service.file.filter;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.service.ZipDownloadService;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

@Service
public class NfsFilter implements FileChainFilter{
    @Autowired
    ZipDownloadService zipDownloadService;
    @Override
    public boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response) throws Exception{
        if (fileMetaData.getStorageMode() == Constants.File.StorageMode.NFS && Files.isDirectory(fileMetaData.getPath(), LinkOption.NOFOLLOW_LINKS)) {
            zipDownloadService.sendZip(request, response, new String[]{fileMetaData.getUiRequestedPath()}, fileMetaData.getStorageMode());
            return true;
        }
        return false;
    }
}
