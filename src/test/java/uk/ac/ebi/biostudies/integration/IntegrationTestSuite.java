package uk.ac.ebi.biostudies.integration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.biostudies.integration.rest.FileRestApiTest;
import uk.ac.ebi.biostudies.integration.utils.IntegrationTestProperties;
import java.io.File;


/**
 * Created by ehsan on 29/06/2017.
 */


@Suite.SuiteClasses({
        IndexTest.class,
        DetailTest.class,
        AuthTest.class,
        SearchTest.class,
        FileRestApiTest.class
})

@RunWith(Suite.class)
public class IntegrationTestSuite {

    public static WebDriver webDriver;

    @Autowired
    IntegrationTestProperties integrationTestProperties;

    @BeforeClass
    public static void setup() {
        System.setProperty("webdriver.chrome.driver", new File("chromedriver").getAbsolutePath());
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setHeadless(true);
        webDriver = new ChromeDriver(options);
    }

    @AfterClass
    public static void destroy(){
        if (webDriver!=null)
            webDriver.close();
    }

}
