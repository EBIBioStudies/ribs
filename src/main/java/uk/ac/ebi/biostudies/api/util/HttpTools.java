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

package uk.ac.ebi.biostudies.api.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

public class HttpTools {

    private final static Logger LOGGER = LogManager.getLogger(HttpTools.class.getName());

    public static final String TOKEN_COOKIE = "BioStudiesToken";
    public static final String AUTH_MESSAGE_COOKIE = "BioStudiesMessage";
    public static final String REFERER_HEADER = "Referer";
    public static final Integer MAX_AGE = 31557600;
    public static String PROXY_HOST;
    public static Integer PROXY_PORT;
    private static HttpClient makeConnection(){
        return HttpClient.newBuilder()
//            .executor(Executors.newFixedThreadPool(20)) // ftp over http server has problem with pooled connections and drop them
                .version(HttpClient.Version.HTTP_2)
                .proxy(getProxySelector())  // Apply proxy settings
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    // Method to determine if proxy should be used
    private static ProxySelector getProxySelector() {
        return (PROXY_HOST != null && !PROXY_HOST.isEmpty() && PROXY_PORT != null)
                ? ProxySelector.of(new InetSocketAddress(PROXY_HOST, PROXY_PORT))
                : ProxySelector.getDefault();
    }

    public static InputStream fetchLargeFileStream(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(null, null, url, null))
                .GET()
                .build();

        // Handle the response as an InputStream
        HttpResponse<InputStream> response = makeConnection().send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new Exception("Failed to retrieve file. HTTP response code: " + response.statusCode());
        }

        return response.body(); // Returning InputStream for external consumption
    }

    public static boolean isValidUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // Handle the response as an InputStream
            HttpResponse<InputStream> response = makeConnection().send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream responseBody = response.body()) {
                return response.statusCode() == 200;
            }
            catch (Exception exception){
                LOGGER.error("Problem in closing connection", exception);
                return false;
            }
        }catch (Throwable throwable){
            return false;
        }
    }
    public static boolean isValidUrl(Path path) {
        String encodedPath = "";
        try {
            // Safely encode the path segment
            encodedPath = new URI(null, null, path.toString().replace("\\", "/"), null).getRawPath();

            // Ensure base URL formatting
            String base = FileMetaData.BASE_FTP_NFS_URL;
            if (!base.endsWith("/")) {
                base += "/";
            }

            String fullUrl = base + encodedPath;
            return isValidUrl(fullUrl);
        } catch (Exception e) {
            LOGGER.error("Error encoding URL for path '{}': {}", path, encodedPath, e);
            return false;
        }
    }

    public static void removeTokenCookie(HttpServletResponse response) {
        try {
            setCookie(response, HttpTools.TOKEN_COOKIE, null, 0);
            setCookie(response, HttpTools.AUTH_MESSAGE_COOKIE, null, 0);
        } catch (Exception ex) {
            LOGGER.debug("cannot remove token cookie ", ex);
        }
    }

    public static void setTokenCookie(HttpServletResponse response, String token, Integer maxAge) {
        try {
            setCookie(response, HttpTools.TOKEN_COOKIE, token, maxAge);
            HttpTools.setCookie(response, HttpTools.AUTH_MESSAGE_COOKIE, null, 0);
        } catch (Exception ex) {
            LOGGER.debug("can not set toke cookie", ex);
        }
    }

    public static void setCookie(HttpServletResponse response, String name, String value, Integer maxAge) {
        Cookie cookie = new Cookie(name, null != value ? value : "");
        // if the value is null - delete cookie by expiring it
        cookie.setPath("/");
        if (null == value) {
            cookie.setMaxAge(0);
        } else if (null != maxAge) {
            cookie.setMaxAge(maxAge);
        }
        response.addCookie(cookie);
    }

    public static void sendRedirect(HttpServletResponse response, String returnURL, boolean isSuccessful) throws IOException {
        if (null != returnURL) {
            if (isSuccessful && returnURL.matches("^http[:]//www(dev)?[.]ebi[.]ac[.]uk/.+")) {
                returnURL = returnURL.replaceFirst("^http[:]//", "https://");
            }
            LOGGER.debug("Will redirect to [{}]", returnURL);
            response.sendRedirect(returnURL);
        } else {
            response.setContentType("text/plain; charset=UTF-8");
            // Disable cache no matter what (or we're fucked on IE side)
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Expires", "Fri, 16 May 2008 10:00:00 GMT"); // some date in the past
        }
    }


    public static String getCookie(HttpServletRequest request, String name) {
        CookieMap cookies = new CookieMap(request.getCookies());
        return cookies.containsKey(name) ? cookies.get(name).getValue() : null;
    }

    public static String getFileNameFromPart(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(
                        content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    public static void displayMessage(HttpServletRequest request, HttpServletResponse response, String title, String message) throws ServletException, IOException {
        String forwardedParams = String.format("?title=%s&message=%s",
                URLEncoder.encode(title, "UTF-8"),
                URLEncoder.encode(message, "UTF-8"));
        request.getRequestDispatcher("/servlets/view/display/message/html"+forwardedParams).forward(request, response);
    }

    public static File uploadFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String sourceLocation = System.getProperty("java.io.tmpdir");
        Part filePart = request.getPart("file");
        if (filePart == null) {
            displayMessage(request,response,"Error!", "Could not upload file.");
            return null;
        }
        String fileName = getFileNameFromPart(filePart);
        if ("studies.xml".equalsIgnoreCase(fileName)) {
            displayMessage(request,response,"Error!", fileName+" can't be overwritten.");
            return null;
        }
        File uploadedFile = new File(sourceLocation, fileName);
        try (FileOutputStream out = new FileOutputStream(uploadedFile);
             InputStream fileContent = filePart.getInputStream();
        ) {
            //logger.debug("File {} will be uploaded to {}", fileName, uploadedFile.getAbsolutePath());
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = fileContent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        }
        return uploadedFile;
    }
}
