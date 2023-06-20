package uk.ac.ebi.biostudies.service.file;

import com.amazonaws.services.s3.model.S3Object;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class FileMetaData {
    private static final Logger LOGGER = LogManager.getLogger(FileMetaData.class.getName());

    private String accession;
    private String fileName;

    private boolean isPublic;

    private String uiRequestedPath;

    private boolean isThumbnail;

    private String collection;

    private String relativePath;
    private S3Object s3Object;
    private Constants.File.StorageMode storageMode;
    private Path path;
    private boolean hasKey;
    private boolean isDirectory;

    private InputStream inputStream;

    public InputStream getInputStream(){
        try {
            if (inputStream != null)
                return inputStream;
            if (s3Object != null) {
                inputStream = s3Object.getObjectContent();
            }
            else if (storageMode == Constants.File.StorageMode.NFS && path != null && Files.exists(path)) {
                inputStream = Files.newInputStream(path);
            }
        }catch (Exception exception){
            LOGGER.error("problem in sending inputstream {}", fileName, exception);
        }
        return inputStream;
    }

    public FileMetaData(String accession){
        this.accession = accession;
    }

    public FileMetaData(String accession, String uiRequestedPath, String fileName, String relativePath, Constants.File.StorageMode storageMode, boolean isPublicStudy, boolean secretKey, String collection){
        this.accession=accession;
        this.uiRequestedPath=uiRequestedPath;
        this.fileName=fileName;
        this.relativePath=relativePath;
        this.storageMode=storageMode;
        this.isPublic=isPublicStudy;
        this.hasKey=secretKey;
        this.collection=collection;
    }

    public FileMetaData(String accession, String uiRequestedPath, String fileName, String relativePath, Constants.File.StorageMode storageMode){
        this.accession=accession;
        this.uiRequestedPath=uiRequestedPath;
        this.relativePath=relativePath;
        this.fileName=fileName;
        this.storageMode=storageMode;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public S3Object getS3Object() {
        return s3Object;
    }

    public void setS3Object(S3Object s3Object) {
        this.s3Object = s3Object;
    }

    public Constants.File.StorageMode getStorageMode() {
        return storageMode;
    }

    public void setStorageMode(Constants.File.StorageMode storageMode) {
        this.storageMode = storageMode;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean getHasKey() {
        return hasKey;
    }

    public void setHasKey(boolean hasKey) {
        this.hasKey = hasKey;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public Long getLastModified() throws IOException{
        if(storageMode!=null && storageMode== Constants.File.StorageMode.FIRE && s3Object!=null)
            return s3Object.getObjectMetadata().getLastModified().getTime();
        if(storageMode!=null && storageMode== Constants.File.StorageMode.NFS && path!=null)
            return Files.getLastModifiedTime(path).toMillis();
        return 0L;
    }

    public Long getFileLength() throws IOException{
        if(storageMode!=null && storageMode== Constants.File.StorageMode.FIRE && s3Object!=null)
            return s3Object.getObjectMetadata().getContentLength();
        if(storageMode!=null && storageMode== Constants.File.StorageMode.NFS && path!=null)
            return Files.size(path);
        return 0L;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getUiRequestedPath() {
        return uiRequestedPath;
    }

    public void setUiRequestedPath(String uiRequestedPath) {
        this.uiRequestedPath = uiRequestedPath;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setInputStream(InputStream mageTabInputStream) throws Exception{
        if(inputStream!=null)
            inputStream.close();
        inputStream = mageTabInputStream;
    }

    public boolean isThumbnail() {
        return isThumbnail;
    }

    public void setThumbnail(boolean thumbnail) {
        isThumbnail = thumbnail;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }
    public void close(){
        try {
            if(s3Object!=null) s3Object.close();
            if(inputStream!=null) inputStream.close();
        }catch (Exception exception){
        LOGGER.error("problem in closing stream", exception);
        }
    }
}
