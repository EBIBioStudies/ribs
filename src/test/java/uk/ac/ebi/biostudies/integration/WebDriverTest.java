package uk.ac.ebi.biostudies.integration;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class WebDriverTest {

    public static WebDriver webDriver;

    @BeforeClass
    public static void setup() {
        System.setProperty("webdriver.chrome.driver", new File("chromedriver").getAbsolutePath());
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--start-maximized");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1280,800");
        options.setHeadless(true);
        webDriver = new ChromeDriver(options);
    }

    @AfterClass
    public static void destroy(){
        if (webDriver!=null) {
            webDriver.quit();
        }
    }
}
