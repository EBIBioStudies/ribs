package uk.ac.ebi.biostudies.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by ehsan on 02/03/2017.
 */


@Configuration
@PropertySource("classpath:efo.properties")
public class EFOConfig implements InitializingBean, DisposableBean {

    @Value("${efo.stopWords}")
    private String stopWords;

    @Value("${efo.indexDirectory}")
    private String indexLocation;

    @Value("${efo.synonyms}")
    private String synonymFilename;

    @Value("${efo.ignoreList}")
    private String ignoreListFilename;

    @Value("${efo.owlFilename}")
    private String owlFilename;

    @Value("${efo.updateUrl}")
    private String url;

    private final String localOwlFilename = "efo.owl";

    private final Set<String> stopWordsSet = new HashSet<>();

    @Override
    public void afterPropertiesSet() {
        String[] words = stopWords.split("\\s*,\\s*");
        stopWordsSet.addAll(Arrays.asList(words));
    }

    public String getUrl(){
        return url;
    }

    public String getIndexLocation(){
        return indexLocation;
    }

    public Set<String> getStopWordsSet(){
        return stopWordsSet;
    }

    public String getSynonymFilename(){
        return synonymFilename;
    }

    public String getIgnoreListFilename(){
        return ignoreListFilename;
    }

    public String getLocalOwlFilename(){
        return localOwlFilename;
    }

    public String getOwlFilename(){
        return owlFilename;
    }


    @Override
    public void destroy() {

    }

}
