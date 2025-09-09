/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.biostudies.config.SecurityConfig;

@Service
public class UserSecurityService {
    public static final String X_SESSION_TOKEN = "X-Session-Token";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Cache<Object, Object> userAuthCache;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private SecurityConfig securityConfig;
    private static final int REQUEST_TIMEOUT = 30000;

    public JsonNode sendAuthenticationCheckRequest(String token) throws Exception {
        JsonNode responseJSON = null;
        HttpClientBuilder clientBuilder = HttpClients.custom();
        if (securityConfig.getHttpProxyHost() != null && !securityConfig.getHttpProxyHost().isEmpty()) {
            clientBuilder.setProxy(new HttpHost(securityConfig.getHttpProxyHost(), securityConfig.getHttpProxyPort()));
        }
        CloseableHttpClient httpClient = clientBuilder
                .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                                .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                                .build(), NoopHostnameVerifier.INSTANCE
                        )
                ).build();
        HttpGet httpGet = new HttpGet(securityConfig.getProfileUrl());
        httpGet.setConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                .setConnectTimeout(REQUEST_TIMEOUT)
                .setSocketTimeout(REQUEST_TIMEOUT).build());
        httpGet.setHeader(X_SESSION_TOKEN, token);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            responseJSON = mapper.readTree(EntityUtils.toString(response.getEntity()));
        } catch (Exception exception) {
            logger.error("problem in sending http req to authentication server", exception);
        }
        return responseJSON;
    }

    public JsonNode sendLoginRequest(String username, String password) throws IOException {
        throw new UnsupportedOperationException();
    }


    public UserSecurityService() {
        // TODO: move password cache timeout to config file
        userAuthCache = CacheBuilder.newBuilder()
                .expireAfterAccess(60, TimeUnit.MINUTES)
                .build();
    }

    public User login(String username, String password) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void logout() {
        if (Session.getCurrentUser().token != null)
            userAuthCache.invalidate(Session.getCurrentUser().token);
    }

    User checkAccess(String token) throws Exception {
        if (token == null) return null;

        User user = (User) userAuthCache.getIfPresent(token);
        if (user == null || !token.equals(user.getToken())) {
            user = createUserFromJSONResponse(sendAuthenticationCheckRequest(token));
        }
        if (user == null || user.getAllow() == null) return null;

        return user;
    }

    public User createUserFromJSONResponse(JsonNode responseJSON) throws IOException {
        User user = null;
        if (responseJSON == null || !responseJSON.has("sessid")) {
            return null;
        }
        user = new User();
        user.setFullName(responseJSON.has("fullname") ? responseJSON.get("fullname").textValue() : responseJSON.get("username").textValue());
        user.setLogin(responseJSON.get("username").textValue());
        user.setToken(responseJSON.get("sessid").textValue());
        user.setEmail(responseJSON.get("email").textValue());
        if (responseJSON.has("allow") && responseJSON.get("allow") != null && responseJSON.get("allow").isArray()) {
            String[] allow = mapper.convertValue(responseJSON.get("allow"), String[].class);
            String[] deny = mapper.convertValue(responseJSON.get("deny"), String[].class);
            Set<String> allowedSet = Sets.difference(Sets.newHashSet(allow), Sets.newHashSet(deny));
            user.setAllow(allowedSet.toArray(new String[allowedSet.size()]));
            user.setDeny(deny);
            user.allow = Stream.of(allow).map(item -> item.replaceAll("~", "")).toArray(String[]::new);
            user.deny = Stream.of(deny).map(item -> item.replaceAll("~", "")).toArray(String[]::new);
        }
        user.setSuperUser(responseJSON.get("superuser").asBoolean(false));
        userAuthCache.put(user.getToken(), user);
        return user;
    }

}
