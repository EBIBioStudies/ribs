package uk.ac.ebi.biostudies.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.lucene.document.Document;
import org.springframework.core.io.InputStreamResource;
import uk.ac.ebi.biostudies.api.util.Constants;

import java.io.IOException;

/**
 * Created by ehsan on 27/02/2017.
 */
public interface SearchService {
    String search(String query, JsonNode selectedFacets, String prjName, int page, int pageSize, String sortBy, String sortOrder);

    String getFieldStats() throws Exception;

    void clearStatsCache();

    InputStreamResource getStudyAsStream(String accession, String relativePath, boolean anonymise, Constants.File.StorageMode storageMode, boolean isPublicStudy, String secretKey) throws IOException;

    InputStreamResource getStudyAsStream(String accession, String relativePath, boolean anonymise, Constants.File.StorageMode storageMode, boolean fillPagetabFromIndex, boolean isPublicStudy, String secretKey) throws IOException;

    ObjectNode getSimilarStudies(String accession, String secretKey) throws Throwable;

    Document getDocumentByAccession(String accession, String secretKey) throws SubmissionNotAccessibleException;

    Document getDocumentByAccessionAndType(String accession, String secretKey, String type) throws SubmissionNotAccessibleException;

    boolean isDocumentPresent(String accession);

    String getLatestStudies() throws Exception;

    boolean isDocumentInCollection(Document submissionDoc, String collection);
}

