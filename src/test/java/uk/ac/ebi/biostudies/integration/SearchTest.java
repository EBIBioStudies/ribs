package uk.ac.ebi.biostudies.integration;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.lucene.document.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.biostudies.api.util.StudyUtils;
import uk.ac.ebi.biostudies.auth.UserSecurityService;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.integration.utils.IntegrationTestProperties;
import uk.ac.ebi.biostudies.service.SearchService;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchTest  extends WebDriverTest {

    @LocalServerPort
    int randomPort;
    @Autowired
    IntegrationTestProperties integrationTestProperties;
    @SpyBean
    UserSecurityService userSecurityServiceMock;
    @SpyBean
    private SearchService searchServiceMock;
    @SpyBean
    private IndexConfig indexConfigMock;
    @SpyBean
    private StudyUtils studyUtils;

    @Test
    public void testPageStats() {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector(".result-count")));
        String pages = webDriver.findElement(By.cssSelector(".result-count")).getAttribute("innerText");
        assertTrue(pages.contains("of"));
    }

    // Does not work with <input type="search"...
    //@Test
    public void testAutoComplete() throws Exception {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies/");
        WebDriverWait wait = new WebDriverWait(webDriver, 120);
        wait.until(visibilityOfElementLocated(By.cssSelector(".result-count")));
        WebElement searchBox = webDriver.findElement(By.cssSelector("#query"));
        searchBox.click();
        searchBox.sendKeys("dna");
        wait.until(visibilityOfElementLocated(By.cssSelector(".ac_inner")));
        List<WebElement> we = webDriver.findElements(By.cssSelector(".ac_inner li"));
        if (!we.get(3).getText().startsWith("DNA")) {
            Thread.sleep(60000);
            searchBox.click();
            searchBox.sendKeys("dna");
            we = webDriver.findElements(By.cssSelector(".ac_inner li"));
        }
        assertTrue(we.get(3).getText().startsWith("DNA"));
    }

//    @Test
//    public void testAccessionAscendingSort() throws Exception{
//        driver.get(integProps.getBaseUrl(randomPort) + "/studies?query=cancer");
//        new Select(driver.findElement(By.id("sort-by"))).selectByVisibleText("Relevance");
//        testSort(".browse-study-accession");
//    }

//    @Test
//    public void testAccessionDescendingSort() throws Exception{
//        driver.get(integProps.getBaseUrl(randomPort) + "/studies/search.html?query=cancer");
//        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Accession");
//        driver.findElement(By.cssSelector(".studies-browse-sort-order-left")).click();
//        testSort(".browse-study-accession", true);
//    }

    private void testSort(String cssSelector) {
        testSort(cssSelector, false);
    }

    private void testSort(String cssSelector, boolean isDescending) {
        List<WebElement> list = webDriver.findElements(By.cssSelector(cssSelector));
        String[] values = new String[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = list.get(i).getText().toLowerCase().trim();
        }
        String[] unsortedValues = values.clone();
        if (isDescending) {
            Arrays.sort(values, Collections.reverseOrder());
        } else {
            Arrays.sort(values);
        }
        assertArrayEquals(values, unsortedValues);
    }

//    @Test
//    public void testTitleAscendingSort() throws Exception{
//        driver.get(integProps.getBaseUrl(randomPort) + "/studies/search.html?query=cancer");
//        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Title");
//        testSort(".browse-study-title a");
//    }

//    @Test
//    public void testTitleDescendingSort() throws Exception{
//        driver.get(integProps.getBaseUrl(randomPort) + "/studies/search.html?query=cancer");
//        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Title");
//        driver.findElement(By.cssSelector(".studies-browse-sort-order-left")).click();
//        testSort(".browse-study-title a", true);
//    }

//    @Test
//    public void testAuthorsAscendingSort() throws Exception{
//        driver.get(integProps.getBaseUrl(randomPort) + "/studies/search.html?query=cancer");
//        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Authors");
//        testSort(".browse-study-title + div.search-authors");
//    }

//    @Test
//    public void testAuthorsDescendingSort() throws Exception{
//        driver.get(integProps.getBaseUrl(randomPort) + "/studies/search.html?query=cancer");
//        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Authors");
//        driver.findElement(By.cssSelector(".studies-browse-sort-order-left")).click();
//        testSort(".browse-study-title + div.search-authors", true);
//    }


    @Test
    public void testFilesDescendingSort() throws Exception {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies?query=cancer");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-by")));
        new Select(webDriver.findElement(By.cssSelector("#sort-by"))).selectByVisibleText("Files");
        wait.until(visibilityOfElementLocated(By.cssSelector(".release-files")));
        List<WebElement> list = webDriver.findElements(By.cssSelector(".release-files"));
        Integer[] values = new Integer[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getAttribute("innerText").trim().split(" ")[0]);
        }
        Integer[] unsortedValues = values.clone();
        Arrays.sort(values, Collections.reverseOrder());
        assertArrayEquals(values, unsortedValues);
    }

    @Test(expected = TimeoutException.class)
    public void testFilesAscendingSort() throws Exception {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies?query=cancer");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-by")));
        new Select(webDriver.findElement(By.cssSelector("#sort-by"))).selectByVisibleText("Files");
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-asc")));
        webDriver.findElement(By.cssSelector("#sort-asc")).click();
        WebDriverWait wait2 = new WebDriverWait(webDriver, 20);
        wait2.until(visibilityOfElementLocated(By.cssSelector(".release-files")));
        List<WebElement> list = webDriver.findElements(By.cssSelector(".release-files"));
        Integer[] values = new Integer[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getText().trim().split(" ")[0]);
        }
        Integer[] unsortedValues = values.clone();
        assertArrayEquals(values, unsortedValues);
        throw new TimeoutException("Test finished successfully");
    }

    @Test
    public void testLinksDescendingSort() throws Exception {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies?query=cancer");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-by")));
        new Select(webDriver.findElement(By.cssSelector("#sort-by"))).selectByVisibleText("Links");
        wait.until(visibilityOfElementLocated(By.cssSelector(".release-links")));
        List<WebElement> list = webDriver.findElements(By.cssSelector(".release-links"));
        Integer[] values = new Integer[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getText().trim().split(" ")[0]);
        }
        Integer[] unsortedValues = values.clone();
        Arrays.sort(values, Collections.reverseOrder());
        assertArrayEquals(values, unsortedValues);
    }

    @Test(expected = TimeoutException.class)
    public void testLinksAscendingSort() throws Exception {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies?query=cancer");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-by")));
        new Select(webDriver.findElement(By.cssSelector("#sort-by"))).selectByVisibleText("Links");
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-asc")));
        webDriver.findElement(By.cssSelector("#sort-asc")).click();
        wait.until(visibilityOfElementLocated(By.cssSelector(".release-links")));
        List<WebElement> list = webDriver.findElements(By.cssSelector(".release-links"));
        Integer[] values = new Integer[list.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getText().trim().split(" ")[0]);
        }
        Integer[] unsortedValues = values.clone();
        assertArrayEquals(values, unsortedValues);
        throw new TimeoutException("Test finished successfully");
    }

    @Test
    public void testReleasedDescendingSort() throws Exception {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies?query=cancer");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-by")));
        new Select(webDriver.findElement(By.cssSelector("#sort-by"))).selectByVisibleText("Released");
        wait.until(visibilityOfElementLocated(By.cssSelector(".release-date")));
        List<WebElement> list = webDriver.findElements(By.cssSelector(".release-date"));
        Date[] values = new Date[list.size()];
        SimpleDateFormat formatter1 = new SimpleDateFormat("d MMM yyyy");
        for (int i = 0; i < values.length; i++) {
            String date = list.get(i).getAttribute("innerText").trim();
            if (date.equalsIgnoreCase("today")) {
                values[i] = Calendar.getInstance().getTime();
            } else if (date.equalsIgnoreCase("yesterday")) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                values[i] = cal.getTime();
            } else if (date.equalsIgnoreCase("tomorrow")) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);
                values[i] = cal.getTime();
            } else {
                try {
                    values[i] = formatter1.parse(date);
                } catch (Exception ex) {
                    values[i] = new Date(Long.MAX_VALUE);
                }
            }
        }
        Date[] unsortedValues = values.clone();
        Arrays.sort(values, Collections.reverseOrder());
        for (int i = 0; i < unsortedValues.length; i++) {
            assertTrue(DateUtils.isSameDay(values[i], unsortedValues[i]));
        }
    }


    @Test
    public void testReleasedAscendingSort() {
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-by")));
        new Select(webDriver.findElement(By.cssSelector("#sort-by"))).selectByVisibleText("Released");
        wait.until(visibilityOfElementLocated(By.cssSelector("#sort-asc")));
        webDriver.findElement(By.cssSelector("#sort-asc")).click();
        wait.until(visibilityOfElementLocated(By.cssSelector(".release-date")));
        List<WebElement> list = webDriver.findElements(By.cssSelector(".release-date"));
        Date[] values = new Date[list.size()];
        SimpleDateFormat format = new SimpleDateFormat("d MMM yyyy");

        for (int i = 0; i < values.length; i++) {
            String date = list.get(i).getAttribute("innerText").trim();
            if (date.equalsIgnoreCase("today")) {
                values[i] = new Date();
            } else if (date.equalsIgnoreCase("yesterday")) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                values[i] = cal.getTime();
            } else if (date.equalsIgnoreCase("tomorrow")) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);
                values[i] = cal.getTime();
            } else {
                try {
                    values[i] = format.parse(date);
                } catch (Exception ignored) {
                    values[i] = new Date(Long.MAX_VALUE);
                }

            }
        }
        Date[] unsortedValues = values.clone();
        for (int i = 0; i < unsortedValues.length; i++) {
            assertTrue(DateUtils.isSameDay(values[i], unsortedValues[i]));
        }
    }


    @Test
    public void testPaging() throws Throwable {
        webDriver.get(integrationTestProperties.getBaseUrl(randomPort) + "studies?page=2&pageSize=3");
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(visibilityOfElementLocated(By.cssSelector(".result-count")));
        String pages = webDriver.findElement(By.cssSelector(".result-count")).getAttribute("innerText").trim();
        assertTrue(pages.contains("of"));
        String accession = webDriver.findElement(By.cssSelector(".accession")).getAttribute("innerText").trim();
        Document doc = searchServiceMock.getDocumentByAccession(accession, null);
        assertNotNull(doc);
    }
}
