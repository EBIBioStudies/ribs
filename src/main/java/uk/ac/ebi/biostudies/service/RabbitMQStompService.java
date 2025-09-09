package uk.ac.ebi.biostudies.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import uk.ac.ebi.biostudies.config.SecurityConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

/** Ehsan */
@Service
@Scope("singleton")
public class RabbitMQStompService {

  private static final Logger logger = LogManager.getLogger(RabbitMQStompService.class);
  @Autowired SecurityConfig securityConfig;
  @Autowired @Lazy PartialUpdater partialUpdater;
  @Autowired private Environment env;
  private StompSession stompSession;

  public boolean isSessionConnected() {
    return stompSession != null && stompSession.isConnected();
  }

  public void stopWebSocket() {
    if (stompSession != null) if (stompSession.isConnected()) stompSession.disconnect();
  }

  public void startWebSocket() {
    if (stompSession == null || !stompSession.isConnected()) init();
  }

  public void init() {
    logger.debug("initiating stomp client service");
    if (!env.getProperty("spring.rabbitmq.stomp.enable", Boolean.class, false)) {
      logger.debug("stomp client is disable");
      return;
    }
    logger.debug("stomp client is enable");
    String url = "ws://%s:%s/ws";
    url = String.format(url, securityConfig.getStompHost(), securityConfig.getStompPort());
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketStompClient stompClient = new WebSocketStompClient(client);
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.afterPropertiesSet();
    stompClient.setTaskScheduler(taskScheduler);
    final StompHeaders stompHeaders = new StompHeaders();
    stompHeaders.add(StompHeaderAccessor.STOMP_LOGIN_HEADER, securityConfig.getStompLoginUser());
    stompHeaders.add(StompHeaderAccessor.STOMP_PASSCODE_HEADER, securityConfig.getStompPassword());
    stompHeaders.add(StompHeaderAccessor.STOMP_ACCEPT_VERSION_HEADER, "1.1,1.2");
    RabbitMQStompSessionHandler sessionHandler = new RabbitMQStompSessionHandler();
    stompClient.connect(url, new WebSocketHttpHeaders(), stompHeaders, sessionHandler);
    logger.debug("Stomp client going to connect");
  }

  private class RabbitMQStompSessionHandler extends StompSessionHandlerAdapter {
    @Override
    public Type getPayloadType(StompHeaders headers) {
      return JsonNode.class;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
      String submissionPartialQueue =
          env.getProperty(
              "partial.submission.rabbitmq.queue",
              String.class,
              "submission-submitted-partials-queue");
      try {
        submissionPartialQueue +=
            new StringBuilder()
                .append("-")
                .append(
                    new BufferedReader(
                            new InputStreamReader(
                                Runtime.getRuntime().exec("hostname").getInputStream()))
                        .readLine()
                        .trim())
                .toString();
      } catch (IOException e) {
        e.printStackTrace();
      }
      stompSession = session;
      logger.debug(
          "Stomp connection: session:{} \t server:{}",
          connectedHeaders.get("session"),
          connectedHeaders.get("server"));
      subscribeToRoutingKey(submissionPartialQueue, "bio.submission.published");
      subscribeToRoutingKey(submissionPartialQueue, "bio.submission.partials");
      logger.debug("Stomp client connected successfully! Queue name {}", submissionPartialQueue);
    }

    private void subscribeToRoutingKey(String submissionPartialQueue, String routingKey) {
      StompHeaders headers = new StompHeaders();
      headers.setDestination("/exchange/biostudies-exchange/" + routingKey);
      headers.set("x-queue-name", submissionPartialQueue);
      headers.set("auto-delete", "false");
      headers.set("durable", "true");
      stompSession.subscribe(headers, this);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
      logger.error("Got a transport exception", exception);
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      try {
        partialUpdater.receivedMessage((JsonNode) payload);
      } catch (Throwable throwable) {
        logger.error("Problem in parsing RabbitMQ message to JsonNode", throwable);
      }
      logger.info("Received update message:", headers.get(StompHeaders.MESSAGE_ID), payload);
    }

    @Override
    public void handleException(
        StompSession session,
        StompCommand command,
        StompHeaders headers,
        byte[] payload,
        Throwable exception) {
      logger.error("Got an exception", exception);
      if (!session.isConnected()) {
        try {
          logger.debug("Sleeping for 10s before trying to connect websocket");
          Thread.sleep(10000);
          startWebSocket();
        } catch (Exception e) {
          logger.error("Problem in reconnecting stomp", e);
        }
      }
    }
  }
}
