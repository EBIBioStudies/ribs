package uk.ac.ebi.biostudies.api.util;

import com.google.common.base.Charsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;

public class StudyUtils {

    private static final Logger LOGGER = LogManager.getLogger(StudyUtils.class.getName());
    private static ScriptEngine jsEngine = new ScriptEngineManager().getEngineByName("nashorn");
    public static boolean IS_MIGRATED_NFS_DIRECTORY = false;

    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|' || c == '&' || c == '/') {
                sb.append('\\');
            }

            sb.append(c);
        }

        return sb.toString();
    }

    public static String decodeForFireBug(String inputStr) {
        try {
            return jsEngine.eval(String.format("unescape(encodeURIComponent('%s'))", inputStr)).toString()
                    .replace("#", "%23")
                    .replace("+", "%2B")
                    .replace("=", "%3D")
                    .replace("@", "%40")
                    .replace("$", "%24")
                    .toString();
        } catch (ScriptException e) {
            LOGGER.error(inputStr + " problem in unescapeing for Fire encoding bug", e);
        }
        return inputStr;
    }

    public static boolean isPageTabFile(String accession, String requestedFilePath) {
        return requestedFilePath.equalsIgnoreCase(accession + ".json")
                || requestedFilePath.equalsIgnoreCase(accession + ".xml")
                || requestedFilePath.equalsIgnoreCase(accession + ".pagetab.tsv")
                || requestedFilePath.equalsIgnoreCase(accession + ".tsv");
    }

    public static boolean isPublicStudy(Document document){
        if(document==null)
            return false;
        String accessRights = " "+document.get(Constants.Fields.ACCESS)+" ";
        if(!accessRights.toLowerCase().contains(Constants.PUBLIC))
            return false;
        return true;
    }

    public static String modifyRelativePathForPrivateStudies(String secretKey, String relativePath){
        if(!IS_MIGRATED_NFS_DIRECTORY || secretKey==null || secretKey.isEmpty() || secretKey.length()<2 || relativePath.contains(".private"))
            return relativePath;
        return ".private/"+secretKey.substring(0, 2)+"/"+secretKey.substring(2)+"/"+relativePath;
    }

    public static String readPagetabFromNfsCache(String basePath, String relativePath, String accession, boolean isBlindReview) throws Exception {
        String filePath = (basePath.endsWith("/") || relativePath.startsWith("/")) ? basePath+relativePath : (basePath+"/"+relativePath);
        Path pagetabFilePath = Paths.get(filePath, accession+".json");
        if(Files.exists(pagetabFilePath)) {
            LOGGER.debug("Reading pagetab file from NFS cache accession: {} filename: {}", accession, pagetabFilePath.getFileName());
            return  Files.readString(pagetabFilePath, Charsets.UTF_8);
        }
        return null;
    }
}