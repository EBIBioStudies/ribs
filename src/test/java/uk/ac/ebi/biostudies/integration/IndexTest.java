package uk.ac.ebi.biostudies.integration;


import org.apache.lucene.document.Document;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biostudies.api.util.Constants;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.auth.UserSecurityService;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.integration.utils.IntegrationTestProperties;
import uk.ac.ebi.biostudies.service.SearchService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;


@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

public class IndexTest extends WebDriverTest {

    @Autowired
    private IntegrationTestProperties integrationTestProperties;

    @Autowired
    private SearchService searchService;

    @SpyBean
    private SearchService searchServiceMock;

    @SpyBean
    private IndexConfig indexConfigMock;

    @SpyBean
    UserSecurityService userSecurityServiceMock;

    @SpyBean
    private StudyUtils studyUtils;

    @LocalServerPort
    int randomPort;


    @Test
    public void test1_clearIndex() throws Throwable{
        webDriver.navigate().to(integrationTestProperties.getBaseUrl(randomPort)+"api/v1/index/clear");
        assertTrue(webDriver.getPageSource().contains("Index empty"));
    }

    @Test
    public void test2_defaultIndex() throws Exception {
        doReturn("src/test/resources/updates/"+ Constants.SUBMISSIONS_JSON).when(indexConfigMock).getStudiesInputFile();
        webDriver.navigate().to(integrationTestProperties.getBaseUrl(randomPort)+"api/v1/index/reload/default");
        Thread.sleep(5000);
        assertTrue(webDriver.getPageSource().contains("default queued for indexing"));
    }

    @Test
    public void test3_updateDocument() throws Throwable{
        doReturn("src/test/resources/updates/"+ Constants.SUBMISSIONS_JSON).when(indexConfigMock).getStudiesInputFile();
        webDriver.navigate().to(integrationTestProperties.getBaseUrl(randomPort)+"api/v1/index/reload/smallJson.json");
        do {
            Thread.sleep(2000);
            webDriver.navigate().to(integrationTestProperties.getBaseUrl(randomPort)+"api/v1/index/status");
        } while (!webDriver.getPageSource().contains("{\"message\":\"UP\"}"));
        Document noDoc = searchService.getDocumentByAccession("S-EPMC3343805", null);
        assertNotNull(noDoc);
    }


    @Test
    public void test4_deleteDocument() throws Throwable{
        Document deletDoc = searchService.getDocumentByAccession("S-EPMC3343805", null);
        assertNotNull(deletDoc);
        webDriver.navigate().to(integrationTestProperties.getBaseUrl(randomPort)+"api/v1/index/delete/S-EPMC3343805");
        do {
            Thread.sleep(2000);
            webDriver.navigate().to(integrationTestProperties.getBaseUrl(randomPort)+"api/v1/index/status");
        } while (!webDriver.getPageSource().contains("{\"message\":\"UP\"}"));
        deletDoc = searchService.getDocumentByAccession("S-EPMC3343805", null);
        assertNull(deletDoc);
    }

}
