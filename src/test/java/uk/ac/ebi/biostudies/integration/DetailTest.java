package uk.ac.ebi.biostudies.integration;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biostudies.auth.UserSecurityService;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.integration.utils.IntegrationTestProperties;
import uk.ac.ebi.biostudies.service.SearchService;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.doReturn;


@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

public class DetailTest extends WebDriverTest {

    @Autowired
    IntegrationTestProperties integrationTestProperties;
    @LocalServerPort
    int randomPort;
    @SpyBean
    UserSecurityService userSecurityServiceMock;
    @SpyBean
    private SearchService searchServiceMock;
    @SpyBean
    private IndexConfig indexConfigMock;

    //@Test
    public void testFileCount() throws Exception {
        doReturn(new InputStreamResource(getClass().getClassLoader().getResource("S-EPMC3372839").openStream())).when(searchServiceMock)
                .getStudyAsStream(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        webDriver.get(baseUrl + "studies/S-EPMC3372839");
        int expectedFileCount = 1;
        WebDriverWait wait = new WebDriverWait(webDriver, 50);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".dataTables_info")));
        List<WebElement> elements = webDriver.findElements(By.cssSelector(".dataTables_info"));
        int actualFileCount = 0;
        for (WebElement we : elements) {
            if (we.getAttribute("id").contains("file")) {
                Scanner scanner = new Scanner(we.getText());
                int files = 0;
                while (scanner.hasNext()) {
                    if (scanner.hasNextInt()) {
                        files = scanner.nextInt();
                    } else {
                        scanner.next();
                    }
                }
                actualFileCount += files;
            }
        }
        assertEquals(expectedFileCount, actualFileCount);
    }


    //@Test
    public void testLinkCount() throws Throwable {
        doReturn(new InputStreamResource(getClass().getClassLoader().getResource("S-EPMC3372839").openStream())).when(searchServiceMock)
                .getStudyAsStream(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        webDriver.get(baseUrl + "studies/S-EPMC3372839");
        int expectedLinkCount = 1;
        WebDriverWait wait = new WebDriverWait(webDriver, 50);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".dataTables_info")));
        List<WebElement> elements = webDriver.findElements(By.cssSelector(".dataTables_info"));
        int actualLinkCount = 0;

        for (WebElement we : elements) {
            if (!we.getAttribute("id").contains("file")) {
                Scanner scanner = new Scanner(we.getText());
                int links = 0;
                while (scanner.hasNext()) {
                    if (scanner.hasNextInt()) {
                        links = scanner.nextInt();
                    } else {
                        scanner.next();
                    }
                }
                actualLinkCount += links;
            }
        }
        assertEquals(expectedLinkCount, actualLinkCount);
    }

    @Test
    public void testTitle() throws Throwable {
        String accession = "S-EPMC6160819";
        String file = accession + ".json";
        doReturn(new InputStreamResource(getClass().getClassLoader().getResource(file).openStream())).when(searchServiceMock)
                .getStudyAsStream(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        webDriver.get(baseUrl + "studies?query=" + accession);
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".title a")));
        WebElement secondLink = webDriver.findElements(By.cssSelector(".title a")).get(0);
        String expectedTitle = secondLink.getAttribute("text");
        webDriver.navigate().to(secondLink.getAttribute("href"));
        wait = new WebDriverWait(webDriver, 20);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#orcid-title")));
        assertEquals(expectedTitle, webDriver.findElement(By.cssSelector("#orcid-title")).getAttribute("innerText").trim());
    }

    //@ Test // Not working with HtmlUnitDriver
    public void testFileFilter() {
        webDriver.get(integrationTestProperties.getBaseUrl() + "/studies/");
        new Select(webDriver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Files");
        webDriver.findElement(By.cssSelector(".browse-study-title a")).click();
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#file-list_filter input")));
        webDriver.findElement(By.cssSelector("#file-list_filter input")).click();
        webDriver.findElement(By.cssSelector("#file-list_filter input")).sendKeys("pdf");
        assertEquals("Showing 1 to 4 of 4 entries (filtered from 35 total entries)",
                webDriver.findElement(By.cssSelector("#file-list_info")).getText());
    }


    // @Test  // There is a bug in phantomJS that suppose some visible areas as invisible so I disabled this test
    public void testDownloadSelection() {
        String baseUrl = integrationTestProperties.getBaseUrl();
        webDriver.get(baseUrl + "studies/?sortBy=files&sortOrder=descending");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".release-files")));
        String fileCountText = webDriver.findElements(By.cssSelector(".release-files")).get(1).getAttribute("innerText").trim();
        int fileCount = Integer.parseInt(fileCountText.substring(0, fileCountText.indexOf(" ")));
        List<WebElement> studies = webDriver.findElements(By.cssSelector(".title a"));
        String location = studies.get(1).getAttribute("href");
        webDriver.navigate().to(location);
        wait = new WebDriverWait(webDriver, 25);
        webDriver.manage().window().setSize(new Dimension(1280, 1024));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("select-all-files")));
        List<WebElement> elements = webDriver.findElements(By.cssSelector(".file-check-box input"));
        for (WebElement elem : elements)
            elem.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("download-selected-files")));
        assertEquals("Download all " + fileCount, webDriver.findElement(By.id("download-selected-files")).getAttribute("innerText").trim());
    }

    @Test
    public void testMultipleAffiliations() throws IOException {
        String accession = "S-EPMC6160819";
        String file = accession + ".json";
        doReturn(new InputStreamResource(getClass().getClassLoader().getResource(file).openStream()))
                .when(searchServiceMock).getStudyAsStream(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any());
        String baseUrl = integrationTestProperties.getBaseUrl(randomPort);
        webDriver.get(baseUrl + "studies/" + accession);
        WebDriverWait wait = new WebDriverWait(webDriver, 50);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#bs-authors > li:nth-child(1) > span:nth-child(1)")));
        WebElement element = webDriver.findElement(By.cssSelector("#bs-authors > li:nth-child(1) > span:nth-child(1)"));
        String expected = "SanfilippoP12";
        String actual = element.getText().replaceAll("\r|\n| ", "");
        assertEquals(expected, actual);
    }
}
