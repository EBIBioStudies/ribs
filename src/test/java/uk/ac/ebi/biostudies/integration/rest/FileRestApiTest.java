package uk.ac.ebi.biostudies.integration.rest;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.auth.UserSecurityService;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.integration.utils.IntegrationTestProperties;
import uk.ac.ebi.biostudies.service.SearchService;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FileRestApiTest {

    @SpyBean
    private IndexConfig indexConfigMock;

    @SpyBean
    UserSecurityService userSecurityServiceMock;

    @SpyBean
    private SearchService searchServiceMock;

    @Autowired
    IntegrationTestProperties integrationTestProperties;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    SearchService searchServiceImpl;

    @LocalServerPort
    int randomPort;

    static String ACCESSION = "S-EPMC3372839";

    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/api/v1/studies/ACCESSION
     */
    public void getStudyFromRestAPI() throws Exception {
        doReturn(new InputStreamResource(getClass().getClassLoader().getResource(ACCESSION + ".json").openStream()))
                .when(searchServiceMock).getStudyAsStream(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(), true);
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String result = testRestTemplate.getForObject(baseUrl + "api/v1/studies/" + ACCESSION, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJSON = mapper.readTree(result);
        assertNotNull(responseJSON);
        assertEquals(ACCESSION, responseJSON.findValue("accno").asText());

        ReadContext jsonPathContext = JsonPath.parse(result);
        JSONArray authors = jsonPathContext.read("$.section.subsections[?(@.type==\"Author\")].attributes[?(@.name==\"Name\")].value");
        JSONArray organizations = jsonPathContext.read("$.section.subsections[?(@.type==\"Organization\")].attributes[?(@.name==\"Name\")].value");
        assertTrue(authors.size() > 0);
        assertTrue(organizations.size() > 0);
    }

    @Test
    /**
     * Since we can not send relativePath to searchService.getStudyAsStream method by rest
     * we can not test secretKeys directly
     */
    public void getStudyWithSecKeyFromBean() throws Exception {
        String pathToFile = getClass().getClassLoader().getResource(ACCESSION + ".json").getPath().replaceAll("/S-EPMC3372839.json", "");
        if ((pathToFile.charAt(0) == '\\' || pathToFile.charAt(0) == '/') && pathToFile.charAt(2) == ':')
            pathToFile = pathToFile.substring(1);
        doReturn(pathToFile).when(indexConfigMock).getFileRootDir(true);
        InputStreamResource myFileStream = searchServiceImpl.getStudyAsStream(ACCESSION, "", true, Constants.File.StorageMode.NFS, true);
        ReadContext jsonPathContext = JsonPath.parse(myFileStream.getInputStream());
        JSONArray authors = jsonPathContext.read("$.section.subsections[?(@.type==\"Author\")].attributes[?(@.name==\"Name\")].value");
        JSONArray organizations = jsonPathContext.read("$.section.subsections[?(@.type==\"Organization\")].attributes[?(@.name==\"Name\")].value");
        assertEquals(0, authors.size());
        assertEquals(0, organizations.size());
    }

    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/api/v1/studies/ACCESSION/info
     */
    public void getFileInfoRestApiPublicAccess() throws Exception {
        doReturn(new InputStreamResource(getClass().getClassLoader().getResource(ACCESSION + ".json").openStream()))
                .when(searchServiceMock).getStudyAsStream(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(), true);
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String result = testRestTemplate.getForObject(baseUrl + "api/v1/studies/" + ACCESSION + "/info", String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseJSON = mapper.readTree(result);
        assertNotNull(responseJSON);
        assertEquals(1, responseJSON.findValue("files").asInt());
        assertFalse(responseJSON.has("seckey"));
    }

    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/api/v1/studies/ACCESSION/info?key=KEY
     */
    public void getStudyWithSecretKeyFromRestAPI() throws Exception {
        String ACCESSION = "S-BSST658";
        ObjectMapper mapper = new ObjectMapper();
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String result = testRestTemplate.getForObject(baseUrl + "api/v1/studies/" + ACCESSION + "/info?key=test12345", String.class);
        JsonNode responseJSON = mapper.readTree(result);
        assertNotNull(responseJSON);
        assertEquals("test12345", responseJSON.findValue("seckey").asText());
        result = testRestTemplate.getForObject(baseUrl + "api/v1/studies/" + ACCESSION + "/info?key=error", String.class);
        assertTrue(result.contains("errorMessage"));
    }


    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/api/v1/studies/ACCESSION/similar
     */
    public void getSimilarStudiesWithoutSecretKeyRestApi() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String result = testRestTemplate.getForObject(baseUrl + "api/v1/studies/" + ACCESSION + "/similar", String.class);
        JsonNode responseJSON = mapper.readTree(result);
        assertNotNull(responseJSON);
        assertTrue(responseJSON.get("similarStudies").isArray());
        assertEquals(3, responseJSON.get("similarStudies").size());
    }

    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/api/v1/studies/ACCESSION/similar?key=test12345
     */
    public void getSimilarStudiesWithSecretKeyRestApi() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String result = testRestTemplate.getForObject(baseUrl + "api/v1/studies/" + ACCESSION + "/similar?key=test12345", String.class);
        JsonNode responseJSON = mapper.readTree(result);
        assertNotNull(responseJSON);
        assertNull(responseJSON.get("similarStudies"));
    }

    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/api/v1/files/ACCESSION
     */
    public void getFileListForFileDataTableRestApi() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String result = testRestTemplate.getForObject(baseUrl + "api/v1/files/" + ACCESSION, String.class);
        JsonNode responseJSON = mapper.readTree(result);
        assertNotNull(responseJSON);
        assertEquals(1, responseJSON.get("recordsTotal").asInt());
    }


    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/files/ACCESSION/fileName.extension
     * test get download api without secret key
     */
    public void fileGetDownloadRestApi() throws Exception {
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String pathToFile = getClass().getClassLoader().getResource(ACCESSION + ".json").getPath().replaceAll("/S-EPMC3372839.json", "");
        if ((pathToFile.charAt(0) == '\\' || pathToFile.charAt(0) == '/') && pathToFile.charAt(2) == ':')
            pathToFile = pathToFile.substring(1);
        doReturn(pathToFile).when(indexConfigMock).getFileRootDir(true);
        Document privateLuceneDoc = new Document();
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESS, "test1", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESSION, ACCESSION, Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.RELATIVE_PATH, "", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.SECRET_KEY, "test12345", Field.Store.YES));
        doReturn(privateLuceneDoc).when(searchServiceMock).getDocumentByAccession(Mockito.anyString(), Mockito.any());

        String result = testRestTemplate.getForObject(baseUrl + "files/" + ACCESSION + "/" + ACCESSION + ".json", String.class);
        assertNotNull(result);
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(ACCESSION, mapper.readTree(result).findValue("accno").asText());
    }

    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/files/ACCESSION/fileName.json
     * test get download api with secret key for forbidden formats e.g json, xml
     */
    public void fileGetDownloadJsonWithSecretKeyRestApi() throws Exception {
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String pathToFile = getClass().getClassLoader().getResource(ACCESSION + ".json").getPath().replaceAll("/S-EPMC3372839.json", "");
        if ((pathToFile.charAt(0) == '\\' || pathToFile.charAt(0) == '/') && pathToFile.charAt(2) == ':')
            pathToFile = pathToFile.substring(1);
        doReturn(pathToFile).when(indexConfigMock).getFileRootDir(true);
        Document privateLuceneDoc = new Document();
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESS, "test1", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESSION, ACCESSION, Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.RELATIVE_PATH, "", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.SECRET_KEY, "test12345", Field.Store.YES));
        doReturn(privateLuceneDoc).when(searchServiceMock).getDocumentByAccession(Mockito.anyString(), Mockito.any());

        String result = testRestTemplate.getForObject(baseUrl + "files/" + ACCESSION + "/" + ACCESSION + ".json?key=test12345", String.class);
        assertNotNull(result);
        assertTrue(result.contains("forbidden"));
    }

    @Test
    /**
     * https://wwwdev.ebi.ac.uk/biostudies/files/ACCESSION/fileName.properties
     * test get download api with secret key for normal formats
     */
    public void fileGetDownloadNormalWithSecretKeyRestApi() throws Exception {
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String pathToFile = getClass().getClassLoader().getResource("test.properties").getPath().replaceAll("/test.properties", "");
        if ((pathToFile.charAt(0) == '\\' || pathToFile.charAt(0) == '/') && pathToFile.charAt(2) == ':')
            pathToFile = pathToFile.substring(1);
        doReturn(pathToFile).when(indexConfigMock).getFileRootDir(true);
        Document privateLuceneDoc = new Document();
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESS, "test1", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESSION, ACCESSION, Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.RELATIVE_PATH, "", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.SECRET_KEY, "test12345", Field.Store.YES));
        doReturn(privateLuceneDoc).when(searchServiceMock).getDocumentByAccession(Mockito.anyString(), Mockito.any());

        String result = testRestTemplate.getForObject(baseUrl + "files/" + ACCESSION + "/test.properties?key=test12345", String.class);
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    public void zipFileDownloadPostRestApi() throws Exception {
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        String pathToFile = getClass().getClassLoader().getResource("updates/Files/A.txt").getPath().replaceAll("/updates/Files/A.txt", "");
        if ((pathToFile.charAt(0) == '\\' || pathToFile.charAt(0) == '/') && pathToFile.charAt(2) == ':')
            pathToFile = pathToFile.substring(1);
        doReturn(pathToFile).when(indexConfigMock).getFileRootDir(true);
        Document privateLuceneDoc = new Document();
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESS, "test1", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.ACCESSION, ACCESSION, Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.RELATIVE_PATH, "updates", Field.Store.YES));
        privateLuceneDoc.add(new StringField(Constants.Fields.SECRET_KEY, "test12345", Field.Store.YES));
        doReturn(privateLuceneDoc).when(searchServiceMock).getDocumentByAccession(Mockito.anyString(), Mockito.any());
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("type", "zip");
        map.add("files", "A.txt");
        map.add("files", "B.txt");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.ALL));
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        testRestTemplate.getRestTemplate().getMessageConverters().add(new ByteArrayHttpMessageConverter());
        byte[] responseEntity  = testRestTemplate.postForObject(baseUrl + "files/" + ACCESSION + "/zip", request, byte[].class);
        assertTrue(responseEntity.length>0);

    }
}