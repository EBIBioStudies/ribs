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

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

public class HttpTools {

    private final static Logger LOGGER = LogManager.getLogger(HttpTools.class.getName());

    public static final String TOKEN_COOKIE = "BioStudiesToken";
    public static final String AUTH_MESSAGE_COOKIE = "BioStudiesMessage";
    public static final String REFERER_HEADER = "Referer";
    public static final Integer MAX_AGE = 31557600;

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
