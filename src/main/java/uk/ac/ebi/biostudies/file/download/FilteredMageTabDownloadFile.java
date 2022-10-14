/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.file.download;
import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilteredMageTabDownloadFile implements IDownloadFile
{
    private InputStream inputStream;
    private File file = null;
    private byte[] filteredFileContents = null;
    private String path;
    private String filename;
    private IDownloadFile unfilteredFile=null;

    public final static String IDF_FILE_NAME_PATTERN = "^.+[.]idf[.]txt$";
    public final static String SDRF_FILE_NAME_PATTERN = "^.+[.]sdrf[.]txt$";

    public FilteredMageTabDownloadFile( File file ) throws IOException
    {
        if (null == file) {
            throw new IllegalArgumentException("File cannot be null");
        }
        this.file = file;
        this.path = file.getPath();
        this.filename = file.getName();

        if (getName().matches(IDF_FILE_NAME_PATTERN)) {
            filteredFileContents = new IdfFilter(file).getFilteredFile();
        } else if (getName().matches(SDRF_FILE_NAME_PATTERN)) {
            filteredFileContents = new SdrfFilter(file).getFilteredFile();
        } else {
            filteredFileContents = null;
        }
    }

    public FilteredMageTabDownloadFile(IDownloadFile downloadFile ) throws IOException
    {
        unfilteredFile = downloadFile;
        this.filename = unfilteredFile.getName();
        this.path = unfilteredFile.getPath();
        if (getName().matches(IDF_FILE_NAME_PATTERN) || getName().matches(SDRF_FILE_NAME_PATTERN)) {
            FilteredMageTabDownloadStream filteredMageTabDownloadStream = new FilteredMageTabDownloadStream(filename, unfilteredFile.getInputStream());
            filteredFileContents = filteredMageTabDownloadStream.getInputStream().readAllBytes();
        } else {
            this.inputStream = unfilteredFile.getInputStream();
        }
    }

    public String getName()
    {
        return this.filename;
    }

    public String getPath()
    {
        return this.path;
    }

    public long getLength()
    {
        return null != filteredFileContents ? filteredFileContents.length :  file!=null ? file.length() : unfilteredFile.getLength();
    }

    public long getLastModified()
    {
        return file!=null ? file.lastModified() : unfilteredFile.getLastModified();
    }

    public boolean canDownload()
    {
        return file!=null ? (file.exists() && file.isFile() && file.canRead()) : true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public boolean isSupported()
    {
        return filteredFileContents !=null;
    }

    public InputStream getInputStream() throws IOException
    {
        if (null != filteredFileContents) {
            return new ByteArrayInputStream(filteredFileContents);
        } else if (unfilteredFile!=null) {
            return unfilteredFile.getInputStream();
        }
        return new FileInputStream(file);
    }

    public void close() throws IOException
    {
    }

    private static final char DEFAULT_COL_DELIMITER = 0x9;
    private static final char DEFAULT_COL_QUOTE_CHAR = '"';
    private static final String DEFAILT_CHARSET = "UTF-8";

    static abstract class MageTabFilter
    {
        public static String processHeader(String header) {
            if (header == null) {
                return "";
            }
            else {
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

        public static String extractType(String header) {
            return header.contains("[") ? header.substring(header.indexOf("[") + 1, header.lastIndexOf("]")) : "";
        }

        public static String extractSubtype(String header) {
            // remove typing first
            String untypedHeader = (header.contains("[")
                    ? header.replace(header.substring(header.indexOf("[") + 1, header.lastIndexOf("]")), "")
                    : header);
            // now check untypedHeader for parentheses
            return untypedHeader.contains("(") ?
                    untypedHeader.substring(untypedHeader.indexOf("(") + 1, untypedHeader.lastIndexOf(")")) : "";
        }
    }

    static class IdfFilter extends MageTabFilter
    {
        private final File file;

        private final static String IDF_FILTER_PATTERN = "^(person.+|pubmedid|publication.+|comment\\[AEAnonymousReview\\])$";

        public IdfFilter( File file )
        {
            this.file = file;
        }

        public byte[] getFilteredFile() throws IOException
        {
            StringBuilder sb = new StringBuilder();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), DEFAILT_CHARSET))) {
                for(String line; (line = br.readLine()) != null; ) {
                    String header = processHeader(line.replaceFirst("^([^\t]*).*$", "$1"));
                    if (!header.matches(IDF_FILTER_PATTERN)) {
                        sb.append(line).append(System.getProperty("line.separator"));
                    }
                }
            }

            return sb.toString().getBytes(DEFAILT_CHARSET);
        }
    }

    static class SdrfFilter extends MageTabFilter
    {
        private final File file;

        private final static String SDRF_FILTER_PATTERN = "^(performer|provider)$";

        public SdrfFilter( File file )
        {
            this.file = file;
        }

        public byte[] getFilteredFile() throws IOException
        {
            StringBuilder sb = new StringBuilder();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), DEFAILT_CHARSET))) {
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
            return sb.toString().getBytes(DEFAILT_CHARSET);
        }

        private void outputLine(StringBuilder sb, String[] line, Set<Integer> columnsToOmit) {
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

}
