package uk.ac.ebi.biostudies.service.impl;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.service.FileDownloadService;
import uk.ac.ebi.biostudies.service.FileIndexService;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class FileIndexServiceImpl implements FileIndexService {
    private static final Logger LOGGER = LogManager.getLogger(FileIndexServiceImpl.class.getName());
    private static final int FILE_THREAD_COUNT = 2;
    public static ExecutorService FileListThreadPool = new ThreadPoolExecutor(FILE_THREAD_COUNT, FILE_THREAD_COUNT * 2,
            60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(FILE_THREAD_COUNT * 3), new ThreadPoolExecutor.CallerRunsPolicy());

    @Autowired
    IndexConfig indexConfig;
    @Autowired
    FileService fileService;
    @Autowired
    FileDownloadService fileDownloadService;

    public static void renewFileThreadPool(){
        FileListThreadPool = new ThreadPoolExecutor(FILE_THREAD_COUNT, FILE_THREAD_COUNT * 2,
                60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(FILE_THREAD_COUNT * 3), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private static Document getFileDocument(String accession, long counter, List<String> attributeColumns, JsonNode fNode, JsonNode parent, Set<String> pureTextFileAttKeyValues) throws Throwable {
        Long size;
        String path;
        String name;
        List<JsonNode> attributes;
        String value;
        Document doc = new Document();

        doc.add(new NumericDocValuesField (Constants.File.POSITION, counter));
        doc.add(new StoredField(Constants.File.POSITION, counter));

        if (fNode.get(Constants.File.SIZE.toLowerCase()) != null) {
            // parse format in pagetab and /extended endpoint
            size = Long.valueOf(fNode.get(Constants.File.SIZE.toLowerCase()).asText());
            doc.add(new SortedNumericDocValuesField(Constants.File.SIZE, size));
            doc.add(new StoredField(Constants.File.SIZE, size));
        } else if (fNode.has(Constants.File.FILE_SIZE)) {
            // parse format mongodb
            size = Long.valueOf(fNode.get(Constants.File.FILE_SIZE).asText());
            doc.add(new SortedNumericDocValuesField(Constants.File.SIZE, size));
            doc.add(new StoredField(Constants.File.SIZE, size));
        }

        JsonNode pathNode = fNode.get(Constants.File.PATH);
        path = pathNode == null || pathNode.asText().equalsIgnoreCase("null") ? null : pathNode.asText();
        if (path == null && fNode.has(Constants.File.FILE_PATH)) {
            path = fNode.get(Constants.File.FILE_PATH).asText();
        }
        if (path == null && fNode.has(Constants.File.RELPATH)) {
            path = fNode.get(Constants.File.RELPATH).asText();
        }
        pathNode = fNode.get(Constants.IndexEntryAttributes.NAME);
        name = pathNode == null || pathNode.asText().equalsIgnoreCase("null") ? null : pathNode.asText();
        if (name == null && fNode.has(Constants.File.FILENAME)) name = fNode.get(Constants.File.FILENAME).asText();
        if (path == null && name != null)
            path = name;
        if (path != null && name == null)
            name = path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path;
        if (path != null) {
            doc.add(new StringField(Constants.File.PATH, path, Field.Store.NO));
        }
        doc.add(new StoredField(Constants.File.PATH, path));
        doc.add(new SortedDocValuesField(Constants.File.PATH, new BytesRef(path)));
        if (name != null) {
            doc.add(new StringField(Constants.File.NAME, name.toLowerCase(), Field.Store.NO));
            doc.add(new StringField(Constants.File.NAME, name, Field.Store.NO));
            doc.add(new StoredField(Constants.File.NAME, name));
            doc.add(new SortedDocValuesField(Constants.File.NAME, new BytesRef(name)));
        }
        attributes = fNode.findValues(Constants.File.ATTRIBUTES);

        doc.add(new StringField(Constants.File.TYPE, Constants.File.FILE, Field.Store.YES));
        doc.add(new StringField(Constants.File.IS_DIRECTORY,
                String.valueOf(fNode.has(Constants.File.TYPE) && fNode.get(Constants.File.TYPE).asText("file").equalsIgnoreCase("directory")), Field.Store.YES));
        doc.add(new StringField(Constants.File.OWNER, accession, Field.Store.YES));

        // add section field if file is not global
        if ((parent.has("accno") || parent.has("accNo")) && (!parent.has("type") || !parent.get("type").textValue().toLowerCase().equalsIgnoreCase("study"))) {
            String section = parent.get(parent.has("accno") ? "accno" : "accNo").asText("").replaceAll("/", "").replaceAll(" ", "");
            if (!StringUtils.isEmpty(section)) {
                //to lower case for search should be case insensitive
                doc.add(new StringField(Constants.File.SECTION, section.toLowerCase(), Field.Store.NO));
                doc.add(new StoredField(Constants.File.SECTION, section));
                doc.add(new SortedDocValuesField(Constants.File.SECTION, new BytesRef(section)));
                attributeColumns.add(Constants.File.SECTION);
            }
        }

        if (attributes != null && attributes.size() > 0 && attributes.get(0) != null) {
            for (JsonNode attrib : attributes.get(0)) {
                JsonNode tempAttName = attrib.findValue(Constants.IndexEntryAttributes.NAME);
                JsonNode tempAttValue = attrib.findValue(Constants.File.VALUE);
                if (tempAttName == null || tempAttValue == null)
                    continue;
                name = tempAttName.asText();
                value = tempAttValue.asText();
                if (name != null && value != null && !name.isEmpty() && !value.isEmpty()) {
                    if (doc.getField(name) != null) {
                        //                                        logger.debug("this value is repeated accno: {} firstAppearance value: {}, secondAppearance value: {}", accession, doc.getField(Constants.File.FILE_ATTS + name).stringValue(), name);
                        continue;
                    }
                    if (name.equalsIgnoreCase("type") && accession.toLowerCase().contains("epmc"))
                        continue;
                    doc.add(new StringField(name, value.toLowerCase(), Field.Store.NO));
                    doc.add(new StoredField(name, value));
                    doc.add(new SortedDocValuesField(name, new BytesRef(value)));
                    attributeColumns.add(name);
                    pureTextFileAttKeyValues.add(name);
                    pureTextFileAttKeyValues.add(value);
                }
            }
        }

        return doc;
    }

    public static void removeFileDocuments(IndexWriter writer, String deleteAccession) {
        QueryParser parser = new QueryParser(Constants.File.OWNER, new KeywordAnalyzer());
        try {
            Query query = parser.parse(Constants.File.OWNER + ":" + deleteAccession);
            writer.deleteDocuments(query);
        } catch (Exception e) {
            LOGGER.error("Problem in deleting old files", e);
        }
    }

    public Map<String, Object> indexSubmissionFiles(String accession, String relativePath, JsonNode json, IndexWriter writer, Set<String> attributeColumns, boolean removeFileDocuments) throws IOException {
        Map<String, Object> valueMap = new HashMap<>();
        AtomicLong counter = new AtomicLong();
        List<String> columns = Collections.synchronizedList(new ArrayList<>());
        Set<String> sectionsWithFiles = ConcurrentHashMap.newKeySet();
        Set<String> fileKeyValuePureTextForSearch = ConcurrentHashMap.newKeySet();
        if (removeFileDocuments) {
            removeFileDocuments(writer, accession);
        }

        // find files
        List<JsonNode> filesParents = json.findParents("files").stream().filter(p -> p.get("files").size() > 0).collect(Collectors.toList());
        if (filesParents != null) {
            for (JsonNode parent : filesParents) {
                if (parent == null) continue;
                indexFileList(accession, writer, counter, columns, sectionsWithFiles, parent, parent, fileKeyValuePureTextForSearch);
            }
        }

        //find file lists
        List<JsonNode> subSections = json.findParents("attributes");
        Map<String, JsonNode> parents = new LinkedHashMap<>();
        for (JsonNode subSection : subSections) {
            ArrayNode attributes = (ArrayNode) subSection.get("attributes");
            for (JsonNode attribute : attributes) {
                if (attribute.get("name").textValue().equalsIgnoreCase("file list")) {
                    parents.put(attribute.get("value").textValue(), subSection);
                }
            }
        }

        subSections = json.findParents("fileList");
        for (JsonNode subSection : subSections) {
            JsonNode fileList = subSection.get("fileList");
            if (fileList != null && fileList.has("fileName")) {
                parents.put(fileList.get("fileName").textValue(), subSection);
            }
        }


        parents.forEach((filename, jsonNode) -> {
            if (jsonNode == null) return;
            FileMetaData fileList = new FileMetaData(accession);
            fileList.setUiRequestedPath(filename + (filename.toLowerCase().endsWith(".json") ? "" : ".json"));
            fileList.setRelativePath(relativePath);
            try {
                Constants.File.StorageMode storageMode = Constants.File.StorageMode.valueOf(json.get(Constants.Fields.STORAGE_MODE).asText());
                fileList.setStorageMode(storageMode);
                fileService.getDownloadFile(fileList);
                if (fileList.getInputStream() == null) {
                    fileList.setUiRequestedPath("Files/" + filename + (filename.toLowerCase().endsWith(".json") ? "" : ".json"));
                    fileService.getDownloadFile(fileList);
                }
                indexLibraryFile(accession, writer, counter, columns, sectionsWithFiles, fileKeyValuePureTextForSearch, jsonNode, fileList);

            } catch (Exception e) {
                LOGGER.error("problem in parsing attached files", e);
            }
            finally {
                try {
                    if (fileList != null)
                        fileList.close();
                }catch (Exception exception){
                    LOGGER.debug("problem in closing file", exception);
                }
            }
        });

        //put Section as the first column. Name and size would be prepended later
        if (columns.contains("Section")) {
            columns.remove("Section");
            columns.add(0, "Section");
        }
        attributeColumns.addAll(columns);
        if (sectionsWithFiles.size() != 0) {
            valueMap.put(Constants.Fields.SECTIONS_WITH_FILES, String.join(" ", sectionsWithFiles));
        }
        valueMap.put(Constants.Fields.FILES, counter.longValue());
        valueMap.put(Constants.FILE_ATT_KEY_VALUE, String.join(" ", fileKeyValuePureTextForSearch));
        return valueMap;
    }

    private void indexFileList(String accession, IndexWriter writer, AtomicLong counter, List<String> columns, Set<String> sectionsWithFiles, JsonNode parent, JsonNode nodeWithFiles, Set<String> pureFileAttKeyValues) throws IOException {
        for (JsonNode fNode : nodeWithFiles.get("files")) {
            if (fNode.isArray()) {
                for (JsonNode singleFile : fNode) {
                    indexSingleFile(accession, writer, counter.incrementAndGet(), columns, sectionsWithFiles, parent, singleFile, pureFileAttKeyValues);
                }
            } else if (fNode.has("files") && fNode.get("files").isArray()) {
                for (JsonNode singleFile : fNode.get("files")) {
                    indexSingleFile(accession, writer, counter.incrementAndGet(), columns, sectionsWithFiles, parent, singleFile, pureFileAttKeyValues);
                }
            } else if (fNode.has("extType") && fNode.get("extType").textValue().equalsIgnoreCase("filesTable")) {
                for (JsonNode singleFile : fNode.get("files")) {
                    indexSingleFile(accession, writer, counter.incrementAndGet(), columns, sectionsWithFiles, parent, singleFile, pureFileAttKeyValues);
                }
            } else if (!parent.has("_class") || !parent.get("_class").textValue().endsWith("DocFileList")) {
                indexSingleFile(accession, writer, counter.incrementAndGet(), columns, sectionsWithFiles, parent, fNode, pureFileAttKeyValues);
            }
        }
    }

    private void indexLibraryFile(String accession, IndexWriter writer, AtomicLong counter, List<String> columns, Set<String> sectionsWithFiles, Set<String> fileKeyValuePureTextForSearch, JsonNode parent, FileMetaData libraryFile) throws IOException {
        List<Future> submittedTasks = new ArrayList<>();
        try (InputStreamReader inputStreamReader = new InputStreamReader(libraryFile.getInputStream(), StandardCharsets.UTF_8)) {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(inputStreamReader);
            JsonToken token = parser.nextToken();
            while (token != null && !JsonToken.START_ARRAY.equals(token)) {
                token = parser.nextToken();
            }

            ObjectMapper mapper = new ObjectMapper();
            while (true) {
                token = parser.nextToken();
                if (!JsonToken.START_OBJECT.equals(token)) {
                    break;
                }
                if (token == null) {
                    break;
                }
                JsonNode singleFile = mapper.readTree(parser);
                SingleFileParser parallelParser = new SingleFileParser(accession, writer, counter.incrementAndGet(), columns, sectionsWithFiles, parent, singleFile, fileKeyValuePureTextForSearch);
                submittedTasks.add(FileListThreadPool.submit(parallelParser));
            }

            submittedTasks.stream().forEach(task -> {
                try {
                    task.get(30, TimeUnit.MINUTES);
                } catch (Throwable exception) {
                    LOGGER.error("problem in parsing the attached file of submission, accession id: {} file number: {} ", accession, counter.get(), exception);
                    task.cancel(true);
                }
            });
        }
    }

    private void indexSingleFile(String accession, IndexWriter writer, long position, List<String> columns, Set<String> sectionsWithFiles, JsonNode parent, JsonNode fNode, Set<String> pureFileAttKeyValues) throws IOException {
        String docId = "";
        String fileName = "";
        try {
            Document doc = getFileDocument(accession, position, columns, fNode, parent, pureFileAttKeyValues);
            docId = accession + "-" + position;
            fileName = doc.get(Constants.File.NAME);
            writer.updateDocument(new Term(Constants.Fields.ID, docId), doc);
            if (position % 10000 == 0)
                LOGGER.info("library file parsed: {}", position);
            if (doc.get(Constants.File.SECTION) != null) {
                IndexableField[] sectionFields = doc.getFields(Constants.File.SECTION);
                //To take stored section field from lucene doc instead of indexedField for case sensivity difference in search and UI presentation
                if (sectionFields.length > 0) {
                    for (IndexableField secField : sectionFields) {
                        if (secField.fieldType().stored() && secField.stringValue() != null) {
                            sectionsWithFiles.add(secField.stringValue());
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            LOGGER.error("problem in parsing the attached file of submission, file id: {} file name: {} ", docId, fileName, ex);
        }
    }

    private class SingleFileParser implements Runnable {
        String accession;
        IndexWriter writer;
        long position;
        List<String> columns;
        Set<String> sectionsWithFiles;
        JsonNode parent;
        JsonNode fNode;
        Set<String> pureFileAttKeyValues;

        public SingleFileParser(String accession, IndexWriter writer, long position, List<String> columns, Set<String> sectionsWithFiles, JsonNode parent, JsonNode fNode, Set<String> pureFileAttKeyValues) {
            this.accession = accession;
            this.writer = writer;
            this.position = position;
            this.columns = columns;
            this.sectionsWithFiles = sectionsWithFiles;
            this.parent = parent;
            this.fNode = fNode;
            this.pureFileAttKeyValues = pureFileAttKeyValues;
        }

        @Override
        public void run() {
            try {
                indexSingleFile(accession, writer, position, columns, sectionsWithFiles, parent, fNode, pureFileAttKeyValues);
            } catch (Exception e) {
                LOGGER.error("problem in parsing library file section ", e);
            }
        }
    }

}
