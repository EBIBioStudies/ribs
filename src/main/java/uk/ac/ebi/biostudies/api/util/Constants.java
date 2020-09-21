package uk.ac.ebi.biostudies.api.util;

import org.springframework.http.MediaType;

/**
 * Created by ehsan on 30/03/2017.
 */
public interface Constants {

    String RELEASE_DATE = "release_date";
    String RELEVANCE = "relevance";
    String STUDIES_JSON_FILE = "studies.json";
    String JSON_UNICODE_MEDIA_TYPE = MediaType.APPLICATION_JSON_UTF8_VALUE;
    String STRING_UNICODE_MEDIA_TYPE = MediaType.TEXT_PLAIN_VALUE+";charset=UTF-8";
    String NA = "n/a";
    String PUBLIC = "public";

    int TOP_FACET_COUNT = 10;

    interface SortOrder {
        String ASCENDING = "ascending";
        String DESCENDING = "descending";
    }



    interface IndexEntryAttributes {
        String FIELD_TYPE = "fieldType";
        String NAME = "name";
        String TITLE = "title";
        String RETRIEVED = "retrieved";
        String EXPANDED = "expanded";
        String SORTABLE = "sortable";
        String ANALYZER = "analyzer";
        String MULTIVALUED = "multiValued";
        String FACET_TYPE = "facetType"; // only boolean and text for now
        String JSON_PATH = "jsonPath";
        String DEFAULT_VALUE = "defaultValue";
        String NA_IS_VISIBLE = "naVisible";
        String PRIVATE = "private";
        String PARSER = "parser";
        String JSON_FIELD_KEY = "jsonFieldKey";
        String TO_LOWER_CASE = "toLowerCase";

        interface FieldTypeValues {
            String TOKENIZED_STRING = "tokenized_string";
            String UNTOKENIZED_STRING = "untokenized_string";
            String LONG = "long";
            String FACET = "facet";
        }

    }

    interface Facets {
        String PROJECT = "facet.collection";
        String FILE_TYPE = "facet.file_type";
        String LINK_TYPE = "facet.link_type";
        String RELEASED_YEAR_FACET = "facet.released_year";
        String MODIFICATION_YEAR_FACET="facet.modification_year";

        String DELIMITER = "|";
    }

    interface Fields {
        String ID = "id";
        String ACCESSION = "accession";
        String TITLE = "title";
        String AUTHOR = "author";
        String LINKS = "links";
        String FILES = "files";
        String TYPE = "type";
        String CONTENT = "content";
        String ACCESS = "access";
        String SECRET_KEY = "seckey";
        String ORCID = "orcid";
        String RELEASE_TIME = "rtime";
        String MODIFICATION_TIME="mtime";
        String CREATION_TIME = "ctime";
        String RELEASE_DATE = "release_date";
        String SECTIONS_WITH_FILES = "sections_with_files";
        String LINK_TYPE = "link_type";
        String LINK_VALUE = "link_value";
        String RELATIVE_PATH = "relPath";
        String RELEASE_TIME_FULL = "releaseTime";
        String MODIFICATION_TIME_FULL ="modificationTime";
        String CREATION_TIME_FULL = "creationTime";
    }

    interface File{
//        String JSONPATH = "path";
//        String JSONNAME = "name";
//        String JSONSIZE = "size";
        String FILE_ATTS = "FILEATTS_";
        String PATH = "path";
        String FILENAME = "fileName";
        String SIZE = "Size";
        String SECTION = "Section";
        String NAME = "Name";
        String TYPE = "type";
        String ATTRIBUTES = "attributes";
        String VALUE= "value";
        String FILE = "file";
        String OWNER = "owner";
        String DRAW = "draw";
        String RECORDTOTAL = "recordsTotal";
        String RECORDFILTERED = "recordsFiltered";
        String DATA = "data";
    }

    interface OS{
        String WINDOWS = "windows";
        String UNIX = "unix";
        String MAC = "mac";
        String ANDROID = "android";
        String IPHONE = "iphone";
        String UNKNOWN = "unknown";
    }

}
