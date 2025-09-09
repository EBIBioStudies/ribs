package uk.ac.ebi.biostudies.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.service.SearchService;

@Component
public class AppEventListener {

    @Autowired
    SearchService searchService;

    @EventListener(ContextRefreshedEvent.class)
    void contextRefreshedEvent() throws Exception {
        searchService.getFieldStats();
        searchService.getLatestStudies();
    }
}
