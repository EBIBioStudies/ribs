package uk.ac.ebi.biostudies.service.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.config.SecurityConfig;

import java.nio.charset.StandardCharsets;

@Service
public class AsperaTokenGenerator {

    @Autowired
    SecurityConfig securityConfig;
    private static final String JSONCONFIG = "{\"transfer_requests\" : [{\"transfer_request\" : {\"paths\":%s,\"destination_root\" : \"/\"}}]}";


    public JsonNode postTokenRequest(ArrayNode requestFiles) throws Exception {
        HttpClientBuilder clientBuilder = HttpClients.custom();
        if(securityConfig.getHttpProxyHost()!=null && !securityConfig.getHttpProxyHost().isEmpty()) {
            clientBuilder.setProxy(new HttpHost(securityConfig.getHttpProxyHost(), securityConfig.getGetHttpProxyPort()));
        }
        CloseableHttpClient httpClient = clientBuilder
                .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                                .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                .build(), NoopHostnameVerifier.INSTANCE
                        )
                ).build();
        JsonNode jsonResult = null;
        try {
            HttpPost httpPost = new HttpPost(securityConfig.getAsperaTokenServer());
            ObjectMapper mapper = new ObjectMapper();
            StringEntity entity = new StringEntity(String.format(JSONCONFIG, requestFiles.toString()), StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            UsernamePasswordCredentials creds
                    = new UsernamePasswordCredentials(securityConfig.getAsperaUser(), securityConfig.getAsperaPass());
            httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String result = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            jsonResult = mapper.readTree(result);
            response.close();
        }finally {
            httpClient.close();
        }
        return jsonResult;

    }
}
