package uk.ac.ebi.biostudies.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.ebi.biostudies.api.util.DataTableColumnInfo;

import java.util.Map;

public interface LinkPaginationService {
    ObjectNode getLinkList(String accession, int start, int pageSize, String search, int draw, boolean metadata, Map<Integer, DataTableColumnInfo> dataTableUiResult, String secretKey) throws SubmissionNotAccessibleException;

}
