package uk.ac.ebi.biostudies.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.lucene.index.IndexWriter;
import uk.ac.ebi.biostudies.api.util.DataTableColumnInfo;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface FileIndexService {
    Map<String, Object> indexSubmissionFiles(String accession, String relativePath, JsonNode json, IndexWriter writer, Set<String> attributeColumns, boolean removeFileDocuments, boolean isPublicStudy, String secretKey) throws IOException ;
}
