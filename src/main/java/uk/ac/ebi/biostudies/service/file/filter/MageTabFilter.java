package uk.ac.ebi.biostudies.service.file.filter;

import au.com.bytecode.opencsv.CSVReader;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

@Deprecated
/**
 * magetab does not contain sensitive data anymore
 */
public class MageTabFilter implements FileChainFilter{
    public final static String IDF_FILE_NAME_PATTERN = "^.+[.]idf[.]txt$";
    public final static String SDRF_FILE_NAME_PATTERN = "^.+[.]sdrf[.]txt$";
    private final static String IDF_FILTER_PATTERN = "^(person.+|pubmedid|publication.+|comment\\[AEAnonymousReview\\])$";
    private final static String SDRF_FILTER_PATTERN = "^(performer|provider)$";


    private static final char DEFAULT_COL_DELIMITER = 0x9;
    private static final char DEFAULT_COL_QUOTE_CHAR = '"';

    private static final String DEFAULT_CHARSET = "UTF-8";

    @Override
    public boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String fileName = fileMetaData.getFileName();
        if(fileName==null || fileName.isEmpty() || !fileMetaData.getHasKey())
            return false;
        byte[] content = {};
        boolean filtered = false;
        if (fileName.matches(IDF_FILE_NAME_PATTERN)) {
            content = getIdfFilteredFile(fileMetaData.getInputStream());
            filtered = true;
        } else if (fileName.matches(SDRF_FILE_NAME_PATTERN)) {
            content = getSdrfFilteredFile(fileMetaData.getInputStream());
            filtered = true;
        }
        if (filtered) {
            IOUtils.copy(new ByteArrayInputStream(content), response.getOutputStream());
        }
        return filtered;
    }

    public static InputStream applyFilter(String fileName, InputStream inputStream) throws IOException {
        if(fileName == null || fileName.isEmpty())
            return inputStream;
        if(fileName.matches(IDF_FILTER_PATTERN))
            return new ByteArrayInputStream(getIdfFilteredFile(inputStream));
        if(fileName.matches(SDRF_FILE_NAME_PATTERN))
            return new ByteArrayInputStream(getSdrfFilteredFile(inputStream));
        return inputStream;
    }

    public static byte[] getIdfFilteredFile(InputStream fileContent) throws IOException
    {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(fileContent, DEFAULT_CHARSET))) {
            for(String line; (line = br.readLine()) != null; ) {
                String header = processHeader(line.replaceFirst("^([^\t]*).*$", "$1"));
                if (!header.matches(IDF_FILTER_PATTERN)) {
                    sb.append(line).append(System.getProperty("line.separator"));
                }
            }
        }

        return sb.toString().getBytes(DEFAULT_CHARSET);
    }

    public static byte[] getSdrfFilteredFile(InputStream fileContent) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(fileContent, DEFAULT_CHARSET))) {
            CSVReader ff = new CSVReader(br, DEFAULT_COL_DELIMITER, DEFAULT_COL_QUOTE_CHAR);
            List<String[]> table = ff.readAll();
            if (null != table && table.size() > 0) {
                Set<Integer> columnsToOmit = new HashSet<>();
                String[] headers = table.get(0);
                for (int col = 0; null != headers && col < headers.length; col++) {
                    String header = processHeader(headers[col]);
                    if (header.matches(SDRF_FILTER_PATTERN)) {
                        columnsToOmit.add(col);
                    } else if (col > 0 && columnsToOmit.contains(col - 1) && header.startsWith("comment[")) {
                        columnsToOmit.add(col);
                    }
                }
                outputLine(sb, headers, columnsToOmit);
                for (int line = 1; line < table.size(); line++) {
                    outputLine(sb, table.get(line), columnsToOmit);
                }
            }
        }
        return sb.toString().getBytes(DEFAULT_CHARSET);
    }

    private static String processHeader(String header) {
        if (header == null) {
            return "";
        } else {
            String main = "";
            String type = "";
            String subtype = "";

            // reduce header to only text, excluding types and subtype
            main = header;

            // remove subtype first
            if (header.contains("(")) {
                // the main part is everything up to ( - there shouldn't be cases of this?
                main = header.substring(0, header.indexOf('('));
                // the qualifier is everything after (
                subtype = "(" + extractSubtype(header) + ")";
            }
            // remove type second
            if (header.contains("[")) {
                // the main part is everything up to [
                main = header.substring(0, header.indexOf('['));
                // the qualifier is everything after [
                type = "[" + extractType(header) + "]";
            }

            StringBuilder processed = new StringBuilder();

            for (int i = 0; i < main.length(); i++) {
                char c = main.charAt(i);
                switch (c) {
                    case ' ':
                    case '\t':
                        continue;
                    default:
                        processed.append(Character.toLowerCase(c));
                }
            }

            // add any [] (type) or () (subtype) qualifiers
            processed.append(type).append(subtype);

            return processed.toString();
        }
    }
    private static String extractType(String header) {
        return header.contains("[") ? header.substring(header.indexOf("[") + 1, header.lastIndexOf("]")) : "";
    }

    private static String extractSubtype(String header) {
        // remove typing first
        String untypedHeader = (header.contains("[")
                ? header.replace(header.substring(header.indexOf("[") + 1, header.lastIndexOf("]")), "")
                : header);
        // now check untypedHeader for parentheses
        return untypedHeader.contains("(") ?
                untypedHeader.substring(untypedHeader.indexOf("(") + 1, untypedHeader.lastIndexOf(")")) : "";
    }

    private static void outputLine(StringBuilder sb, String[] line, Set<Integer> columnsToOmit) {
        boolean shouldAddDelimiter = false;

        for (int col = 0; null != line && col < line.length; col++) {
            if (!columnsToOmit.contains(col)) {
                if (shouldAddDelimiter) {
                    sb.append(DEFAULT_COL_DELIMITER);
                }
                sb.append(line[col]);
                shouldAddDelimiter = true;
            }
        }
        if (null != line) {
            sb.append(System.getProperty("line.separator"));
        }
    }

}
