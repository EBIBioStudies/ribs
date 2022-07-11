package uk.ac.ebi.biostudies.integration.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import uk.ac.ebi.biostudies.integration.WebDriverTest;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;

/**
 * Created by awais on 18/08/2015.
 */
public class TestUtils extends WebDriverTest {


    public static void login(String url, String username, String password){
        webDriver.navigate().to(url);
        WebDriverWait wait = new WebDriverWait(webDriver, 30);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#studyCount")));
        webDriver.findElement(By.cssSelector("#login-button")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#user-field")));
        webDriver.findElement(By.cssSelector("#user-field")).sendKeys(username);
        webDriver.findElement(By.cssSelector("#pass-field")).sendKeys(password);
        webDriver.findElement(By.cssSelector("input[type='submit'].submit")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#logout-button")));
        String logoutText = webDriver.findElement(By.cssSelector("#logout-button")).getAttribute("innerText").trim();
        assertThat(logoutText, startsWith("Logout "));
    }

    public static void logout(String url){
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        webDriver.navigate().to(url);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#logout-button")));
        webDriver.findElement(By.cssSelector("#logout-button")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#login-button")));
        assertEquals("Login", webDriver.findElement(By.cssSelector("#login-button")).getAttribute("innerText").trim());
    }

    public static void validIndexIsloaded(String baseUrl){
        webDriver.navigate().to(baseUrl);
        WebDriverWait wait = new WebDriverWait(webDriver, 20);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#studyCount")));
        String studyCount = webDriver.findElement(By.id("studyCount")).getText();
        int count = Integer.valueOf(studyCount.replaceAll("[^0-9]*",""));
        assertThat(count, greaterThan(0));
    }


}
