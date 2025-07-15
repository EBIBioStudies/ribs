package uk.ac.ebi.biostudies.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.ac.ebi.biostudies.api.util.analyzer.AnalyzerManager;
import uk.ac.ebi.biostudies.api.util.parser.ParserManager;
import uk.ac.ebi.biostudies.config.EFOConfig;
import uk.ac.ebi.biostudies.config.IndexConfig;
import uk.ac.ebi.biostudies.config.IndexManager;
import uk.ac.ebi.biostudies.config.TaxonomyManager;
import uk.ac.ebi.biostudies.efo.index.EFOManager;
import uk.ac.ebi.biostudies.service.IndexManagementService;
import uk.ac.ebi.biostudies.service.RabbitMQStompService;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Scope("singleton")
public class IndexManagementServiceImpl implements IndexManagementService {

    private final static Logger LOGGER = LogManager.getLogger(IndexManagementServiceImpl.class.getName());
    private static final AtomicBoolean closed = new AtomicBoolean(false);


    @Autowired
    IndexManager indexManager;
    @Autowired
    TaxonomyManager taxonomyManager;
    @Autowired
    AnalyzerManager analyzerManager;
    @Autowired
    ParserManager parserManager;
    @Autowired
    IndexConfig indexConfig;
    @Autowired
    RabbitMQStompService rabbitMQStompService;
    @Autowired
    EFOConfig eFOConfig;
    @Autowired
    EFOManager efoManager;

    @Autowired
    private Environment env;

    @Override
    public synchronized boolean isClosed() {
        return closed.get();
    }

    @Scheduled(fixedDelayString = "${schedule.stomp.isalive:300000}", initialDelay = 600000)
    public void webSocketWatchDog() {
        if (!env.getProperty("spring.rabbitmq.stomp.enable", Boolean.class, false) || rabbitMQStompService.isSessionConnected() || isClosed())
            return;
        openWebsocket();
        LOGGER.info("Failed Websocket Connection recovered by watchDog!");
    }

    @Override
    public synchronized void closeWebsocket() {
        if (!env.getProperty("spring.rabbitmq.stomp.enable", Boolean.class, false))
            return;
        rabbitMQStompService.stopWebSocket();
        closed.set(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        LOGGER.info("Application is fully up — starting WebSocket...");
        openWebsocket();
    }

    @Override
    public synchronized void openWebsocket() {
        if (!env.getProperty("spring.rabbitmq.stomp.enable", Boolean.class, false))
            return;
        rabbitMQStompService.startWebSocket();
        closed.set(false);
    }


    @Override
    public void stopAcceptingSubmissionMessagesAndCloseIndices() {
        rabbitMQStompService.stopWebSocket();
        indexManager.closeIndices();
    }


    @Override
    public void openIndicesWritersAndSearchersStartStomp() {
        try {
            indexManager.openIndicesWritersAndSearchers();
            indexManager.openEfoIndex();
        } catch (Throwable error) {
            LOGGER.error(error);
        }

    }

}
