package uk.ac.ebi.biostudies.service.impl;

import com.amazonaws.services.s3.model.S3Object;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.HttpTools;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {

    @Autowired
    IndexConfig indexConfig;
    @Autowired
    FireService fireService;
    private static final Logger logger = LogManager.getLogger(FileService.class);


    public void getDownloadFile(FileMetaData fileMetaData) throws FileNotFoundException{

        //Serve pagetab or filelist from nfs cache and return
        if(indexConfig.getPageTabHasNFSBackup() && fileMetaData.getUiRequestedPath()!=null && fileMetaData.getUiRequestedPath().endsWith(".json")){
            Path path = Paths.get(indexConfig.getNfsCachePath(), fileMetaData.getRelativePath(), "Files", fileMetaData.getUiRequestedPath());
            if(Files.exists(path)){
                try {
                    logger.debug("Accessing library filelist {} from NFS cache, accession: {}", path.getFileName(), fileMetaData.getAccession());
                    fileMetaData.setPath(path);
                    fileMetaData.setInputStream(Files.newInputStream(path));
                    return;
                }catch (Exception e){
                    fileMetaData.setPath(null);
                    logger.debug("Problem in creating inputStream from NFS cache", e);
                }
            }
        }
        if(fileMetaData.getStorageMode()== Constants.File.StorageMode.FIRE)
            resolveFirePath(fileMetaData);
        else if(fileMetaData.getStorageMode()== Constants.File.StorageMode.NFS)
            resolveNfsPath(fileMetaData);
        else {
            logger.error("no storage mode for file: {} {}", fileMetaData.getStorageMode(), fileMetaData.getUiRequestedPath());
            throw new FileNotFoundException(fileMetaData.getUiRequestedPath());
        }
    }

    private void resolveNfsPath(FileMetaData fileMetaData) throws FileNotFoundException {

        if(fileMetaData.getSecKey()!=null){//This is a private study pagetab that there is not in cache too and http server is our last hope
            //I used secretKey as a flag for modifying url the better choice is adding a separated flag for it
            fileMetaData.setRelativePath(StudyUtils.modifyRelativePathForPrivateStudies(fileMetaData.getSecKey(), fileMetaData.getRelativePath()));
        }

        Path downloadFile = Paths.get( fileMetaData.getRelativePath() + (fileMetaData.isThumbnail() ? "/Thumbnails/" : (fileMetaData.getUiRequestedPath().startsWith("Files/")?"/":"/Files/")) + fileMetaData.getUiRequestedPath()+ (fileMetaData.isThumbnail() ? ".thumbnail.png" : ""));

        //Hack start: override relative path if file is not found
//        if (!Files.exists(downloadFile, LinkOption.NOFOLLOW_LINKS) && fileMetaData.getUiRequestedPath().endsWith(".tsv")) {
//            if (fileMetaData.getUiRequestedPath().endsWith(".pagetab.tsv"))
//                fileMetaData.setUiRequestedPath(fileMetaData.getUiRequestedPath().replaceAll(".pagetab.tsv", ".tsv"));
//            else
//                fileMetaData.setUiRequestedPath(fileMetaData.getUiRequestedPath().replaceAll(".tsv", ".pagetab.tsv"));
//
//            downloadFile = Paths.get(indexConfig.getFileRootDir(fileMetaData.isPublic()), fileMetaData.getRelativePath() + "/Files/" + fileMetaData.getUiRequestedPath());
//        }
//        if (!Files.exists(downloadFile, LinkOption.NOFOLLOW_LINKS)) {
//            logger.debug("{} not found ", downloadFile.toFile().getAbsolutePath());
//            downloadFile = Paths.get(indexConfig.getFileRootDir(fileMetaData.isPublic()), fileMetaData.getRelativePath() + "/Files/u/" + fileMetaData.getUiRequestedPath());
//            logger.debug("Trying {}", downloadFile.toFile().getAbsolutePath());
//        }
//        if (!Files.exists(downloadFile, LinkOption.NOFOLLOW_LINKS)) {
//            downloadFile = Paths.get(indexConfig.getFileRootDir(fileMetaData.isPublic()), fileMetaData.getUiRequestedPath() + "/Files/u/" + fileMetaData.getRelativePath() + "/" + fileMetaData.getUiRequestedPath());
//            logger.debug("Trying {}", downloadFile.toFile().getAbsolutePath());
//        }
//        if (!Files.exists(downloadFile, LinkOption.NOFOLLOW_LINKS)) { // for file list
//            logger.debug("{} not found ", downloadFile.toFile().getAbsolutePath());
//            downloadFile = Paths.get(indexConfig.getFileRootDir(fileMetaData.isPublic()), fileMetaData.getRelativePath() + "/" + fileMetaData.getUiRequestedPath());
//            logger.debug("Trying file list file {}", downloadFile.toFile().getAbsolutePath());
//        }
        //Hack end
        if (!HttpTools.isValidUrl(downloadFile)) {
            logger.error("Could not find on FTP server {}", downloadFile);
            throw new FileNotFoundException();
        }
        fileMetaData.setPath(downloadFile);
    }

    private void resolveFirePath(FileMetaData fileMetaData) throws FileNotFoundException{
        S3Object s3Object = null;
        String path = fileMetaData.getRelativePath() + (fileMetaData.isThumbnail() ? "/Thumbnails/" : (fileMetaData.getUiRequestedPath().startsWith("Files/")?"/":"/Files/"))
                + fileMetaData.getUiRequestedPath() + (fileMetaData.isThumbnail() ? ".thumbnail.png" : "");
        if (fileMetaData.getUiRequestedPath().equals(fileMetaData.getAccession() + ".json") || fileMetaData.getUiRequestedPath().equals(fileMetaData.getAccession() + ".xml") || fileMetaData.getUiRequestedPath().equals(fileMetaData.getAccession() + ".tsv")) {
            path = fileMetaData.getRelativePath() + "/" + fileMetaData.getUiRequestedPath();
        }
        try {
            logger.debug("accessing s3DownloadClient {}", path);
            s3Object = fireService.getFireObjectByPath(path);
        } catch (Exception ex1) {
            try {
                if (fileMetaData.getUiRequestedPath().equals(fileMetaData.getAccession() + ".tsv")) {
                    logger.debug("{} not found. Trying old .pagetab.tsv file.", path);
                    path = fileMetaData.getRelativePath() + "/" + fileMetaData.getAccession() + ".pagetab.tsv";
                } else {
                    logger.debug("{} not found and might be a folder. Trying zipped archive.", path);
                    path = path + ".zip";
                }
                // For folders
                logger.debug("accessing s3DownloadClient {}", path);
                s3Object = fireService.getFireObjectByPath(path);
            } catch (Exception ex4) {
                try {
                    if(s3Object!=null)
                        s3Object.close();
                }catch (Exception exception){
                    logger.debug("can not close fire pool http connection");
                }
                throw new FileNotFoundException(ex4.getMessage());
            }
        }
        fileMetaData.setS3Object(s3Object);
        fileMetaData.setPath(Path.of(path));
    }
}
