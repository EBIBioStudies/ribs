package uk.ac.ebi.biostudies.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biostudies.auth.UserSecurityService;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.integration.utils.IntegrationTestProperties;
import uk.ac.ebi.biostudies.integration.utils.TestUtils;
import uk.ac.ebi.biostudies.service.SearchService;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

public class AuthTest extends WebDriverTest {
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


    @LocalServerPort
    int randomPort;


   @Test
    public void testLoginAndLogout() throws Throwable{
       ObjectMapper mapper = new ObjectMapper();
       String responseStr = "{\"sessid\":\"12345\", \"username\":\"%s\", \"email\":\"test@test.com\", \"superuser\":false, \"allow\":[], \"deny\":[]}";
       String formated = String.format(responseStr, integrationTestProperties.getUsername());
       doReturn(mapper.readTree(formated)).when(userSecurityServiceMock).sendLoginRequest(anyString(), anyString());
       doReturn(mapper.readTree(formated)).when(userSecurityServiceMock).sendAuthenticationCheckRequest(anyString());
       TestUtils.login(integrationTestProperties.getBaseUrl(randomPort),integrationTestProperties.getUsername(),"123");
       TestUtils.logout(integrationTestProperties.getBaseUrl(randomPort));
    }

}
