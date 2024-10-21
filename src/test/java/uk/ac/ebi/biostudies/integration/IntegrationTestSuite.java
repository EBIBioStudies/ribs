package uk.ac.ebi.biostudies.integration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.biostudies.integration.rest.FileRestApiTest;
import uk.ac.ebi.biostudies.integration.utils.IntegrationTestProperties;


/**
 * Created by ehsan on 29/06/2017.
 */


@Suite.SuiteClasses({
        IndexTest.class,
//      AuthTest.class,
        DetailTest.class,
        SearchTest.class,
        FileRestApiTest.class
})

@RunWith(Suite.class)
public class IntegrationTestSuite {
    @Autowired
    IntegrationTestProperties integrationTestProperties;
}
