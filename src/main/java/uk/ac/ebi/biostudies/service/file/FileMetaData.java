package uk.ac.ebi.biostudies.service.file;

import com.amazonaws.services.s3.model.S3Object;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.HttpTools;

public class FileMetaData {
    private static final Logger LOGGER = LogManager.getLogger(FileMetaData.class.getName());

    private String accession;
    private String fileName;
    private String uiRequestedPath;
    private String unDecodedRequestedPath;
    private String collection;
    private String relativePath;

    private boolean isPublic;
    private boolean isThumbnail;
    private boolean hasKey;
    private boolean isDirectory;
    private String secKey;

    private Path path;
    private S3Object s3Object;
    private Constants.File.StorageMode storageMode;
    private InputStream inputStream;
    public static String BASE_FTP_FIRE_URL;
    public static String BASE_FTP_NFS_URL;


    public FileMetaData(String accession) {
        this.accession = accession;
    }

    public String getBaseFtpUrl(){
        return storageMode == Constants.File.StorageMode.FIRE ? BASE_FTP_FIRE_URL : BASE_FTP_NFS_URL;
    }

    public FileMetaData(String accession, String uiRequestedPath, String fileName, String relativePath, Constants.File.StorageMode storageMode, boolean isPublicStudy, boolean secretKey, String collection) {
        this.accession = accession;
        this.uiRequestedPath = uiRequestedPath;
        this.fileName = fileName;
        this.relativePath = relativePath;
        this.storageMode = storageMode;
        this.isPublic = isPublicStudy;
        this.hasKey = secretKey;
        this.collection = collection;
    }
    public FileMetaData(String accession, String uiRequestedPath, String unDecodedRequestedPath, String fileName, String relativePath, Constants.File.StorageMode storageMode, boolean isPublicStudy, boolean secretKey, String collection) {
        this.accession = accession;
        this.uiRequestedPath = uiRequestedPath;
        this.unDecodedRequestedPath = unDecodedRequestedPath;
        this.fileName = fileName;
        this.relativePath = relativePath;
        this.storageMode = storageMode;
        this.isPublic = isPublicStudy;
        this.hasKey = secretKey;
        this.collection = collection;
    }

    public FileMetaData(String accession, String uiRequestedPath, String fileName, String relativePath, boolean isPublicStudy, Constants.File.StorageMode storageMode) {
        this.accession = accession;
        this.uiRequestedPath = uiRequestedPath;
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.storageMode = storageMode;
        this.isPublic = isPublicStudy;
    }

    public InputStream getInputStream() {
        try {
            if (inputStream != null) return inputStream;
            if (s3Object != null) {
                inputStream = s3Object.getObjectContent();
            } else if (storageMode == Constants.File.StorageMode.NFS && path != null
            /** && HttpTools.isValidUrl(path) validation will cause performance degradation without a pool**/) {
                String pathStr = path.toString().replace('\\', '/');
                String fullPath = BASE_FTP_NFS_URL.endsWith("/") || pathStr.startsWith("/")
                        ? BASE_FTP_NFS_URL + pathStr
                        : BASE_FTP_NFS_URL + "/" + pathStr;
                inputStream = HttpTools.fetchLargeFileStream(fullPath);
            }
        } catch (Exception exception) {
            LOGGER.error("problem in sending ftp inputstream {}", fileName, exception);
        }
        return inputStream;
    }

    public void setInputStream(InputStream mageTabInputStream) throws Exception {
        if (inputStream != null) inputStream.close();
        inputStream = mageTabInputStream;
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

    public Long getLastModified() throws IOException {
        if (storageMode != null && storageMode == Constants.File.StorageMode.FIRE && s3Object != null)
            return s3Object.getObjectMetadata().getLastModified().getTime();
        if (storageMode != null && storageMode == Constants.File.StorageMode.NFS && path != null)
            return Files.getLastModifiedTime(path).toMillis();
        return 0L;
    }

    public Long getFileLength() throws IOException {
        if (storageMode != null && storageMode == Constants.File.StorageMode.FIRE && s3Object != null)
            return s3Object.getObjectMetadata().getContentLength();
        if (storageMode != null && storageMode == Constants.File.StorageMode.NFS && path != null)
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

    public void close() {
        try {
            if (s3Object != null) s3Object.close();
            if (inputStream != null) inputStream.close();
        } catch (Exception exception) {
            LOGGER.error("problem in closing stream", exception);
        }
    }

    public String getUnDecodedRequestedPath() {
        return unDecodedRequestedPath;
    }

    public String getSecKey() {
        return secKey;
    }

    public void setSecKey(String secKey) {
        this.secKey = secKey;
    }
}
