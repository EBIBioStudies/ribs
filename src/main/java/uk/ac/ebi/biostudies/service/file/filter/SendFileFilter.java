package uk.ac.ebi.biostudies.service.file.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.ebi.biostudies.service.file.FileMetaData;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SendFileFilter implements FileChainFilter{

    private static final Logger logger = LogManager.getLogger(FileChainFilter.class);

    private static final int TRANSFER_BUFFER_SIZE = 64 * KB;

    private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
    @Override
    public boolean handleFile(FileMetaData fileMetaData, HttpServletRequest request, HttpServletResponse response) throws Exception {
        sendRandomAccessFile(fileMetaData, request, response);
        return true;
    }

    private void sendRandomAccessFile(FileMetaData downloadFile, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Prepare some variables. The ETag is an unique identifier of the file
        String fileName = downloadFile.getFileName();
        long length = downloadFile.getFileLength();
        long lastModified = downloadFile.getLastModified();
        String eTag = fileName + "_" + length + "_" + lastModified;


        // Validate request headers for caching ---------------------------------------------------

        // If-None-Match header should contain "*" or ETag. If so, then return 304
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && (ifNoneMatch.contains("*") || matches(ifNoneMatch, eTag))) {
            response.setHeader("ETag", eTag); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        // If-Modified-Since header should be greater than LastModified. If so, then return 304
        // This header is ignored if any If-None-Match header is specified
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        if (ifNoneMatch == null && ifModifiedSince != -1 && ifModifiedSince + 1000 > lastModified) {
            response.setHeader("ETag", eTag); // Required in 304.
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }


        // Validate request headers for resume ----------------------------------------------------

        // If-Match header should contain "*" or ETag. If not, then return 412
        String ifMatch = request.getHeader("If-Match");
        if (ifMatch != null && !ifMatch.contains("*") && !matches(ifMatch, eTag)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        // If-Unmodified-Since header should be greater than LastModified. If not, then return 412
        long ifUnmodifiedSince = request.getDateHeader("If-Unmodified-Since");
        if (ifUnmodifiedSince != -1 && ifUnmodifiedSince + 1000 <= lastModified) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }


        // Validate and process range -------------------------------------------------------------

        // Prepare some variables. The full Range represents the complete file
        Range full = new Range(0, length - 1, length);
        List<Range> ranges = new ArrayList<>();
        // Validate and process Range and If-Range headers
        String range = request.getHeader("Range");
        if (range != null) {

            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            // If-Range header should either match ETag or be greater then LastModified.
            // If not, then return full file
            String ifRange = request.getHeader("If-Range");
            if (ifRange != null && !ifRange.equals(eTag)) {
                try {
                    long ifRangeTime = request.getDateHeader("If-Range"); // Throws IAE if invalid
                    if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
                        ranges.add(full);
                    }
                } catch (IllegalArgumentException ignore) {
                    ranges.add(full);
                }
            }

            // If any valid If-Range header, then process each part of byte range
            if (ranges.isEmpty()) {
                for (String part : range.substring(6).split(",")) {
                    // Assuming a file with length of 100, the following examples returns bytes at:
                    // 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
                    long start = sublong(part, 0, part.indexOf("-"));
                    long end = sublong(part, part.indexOf("-") + 1, part.length());

                    if (start == -1) {
                        start = length - end;
                        end = length - 1;
                    } else if (end == -1 || end > length - 1) {
                        end = length - 1;
                    }

                    // Check if Range is syntactically valid. If not, then return 416
                    if (start > end) {
                        response.setHeader("Content-Range", "bytes */" + length); // Required in 416.
                        response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                        return;
                    }

                    // Add range
                    ranges.add(new Range(start, end, length));
                }
            }
        }


        // Prepare and initialize response --------------------------------------------------------

        // Get content type by file name.
        String contentType = request.getServletContext().getMimeType(fileName);

        // If content type is unknown, then set the default value.
        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
        // To add new content types, add new mime-mapping entry in web.xml.
        if (contentType == null) {
            contentType = "application/octet-stream";
        }


        // Determine content disposition. If content type is supported by the browser or an image
        // then it is set to inline, else attachment which will pop up a 'save as' dialogue.
        String accept = request.getHeader("Accept");
        boolean inline = (accept != null && accepts(accept, contentType));
        String disposition = (inline || contentType.startsWith("image")) ? "inline" : "attachment";

        // Initialize response.
        //response.reset();
        response.setBufferSize(TRANSFER_BUFFER_SIZE);
        response.setHeader("Content-Disposition", disposition + ";filename=\"" + fileName + "\"");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);

        // Send requested file (part(s)) to client ------------------------------------------------

        try (InputStream input = downloadFile.getInputStream();
             ServletOutputStream output = response.getOutputStream()) {

            if (ranges.isEmpty() || ranges.get(0) == full) {

                // Return full file.
                response.setContentType(contentType);
                // according to the spec we shouldn't send this for full download requests
                //response.setHeader("Content-Range", "bytes " + full.start + "-" + full.end + "/" + full.total);
                response.setHeader("Content-Length", String.valueOf(full.length));

                if (request.getMethod().equalsIgnoreCase("HEAD")) {
                    return;
                }

                // Copy full range.
                copy(input, output, full.start, full.length);
                logger.info("Full download of [{}] completed, sent [{}] bytes", fileName, full.length);


            } else if (ranges.size() == 1) {

                // Return single part of file.
                Range r = ranges.get(0);
                response.setContentType(contentType);
                response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
                response.setHeader("Content-Length", String.valueOf(r.length));
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.


                // Copy single part range.
                if (request.getMethod().equalsIgnoreCase("HEAD")) {
                    return;
                }
                copy(input, output, r.start, r.length);
                logger.info("Single range download of [{}] completed, sent [{}] bytes", fileName, r.length);


            } else {
                if (request.getMethod().equalsIgnoreCase("HEAD")) {
                    return;
                }

                // Return multiple parts of file
                response.setContentType("multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.


                // Copy multi part range
                for (Range r : ranges) {
                    // Add multipart boundary and header fields for every range
                    output.println();
                    output.println("--" + MULTIPART_BOUNDARY);
                    output.println("Content-Type: " + contentType);
                    output.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);

                    // Copy single part range of multi part range
                    copy(input, output, r.start, r.length);
                    logger.info("A range of multiple-part download of [{}] completed, sent [{}] bytes", fileName, r.length);
                }

                // End with multipart boundary
                output.println();
                output.println("--" + MULTIPART_BOUNDARY + "--");

            }

            // Finalize task
            output.flush();
        }
    }

    /**
     * Copy the given byte range of the given input to the given output.
     *
     * @param input  The input to copy the given range to the given output for.
     * @param output The output to copy the given range from the given input for.
     * @param start  Start of the byte range.
     * @param length Length of the byte range.
     * @throws IOException If something fails at I/O level.
     */

    private void copy(InputStream input, OutputStream output, long start, long length)
            throws IOException {
        byte[] buffer = new byte[TRANSFER_BUFFER_SIZE];
        int read;

        // Write partial range
        input.skip(start);
        long toRead = length;

        while ((read = input.read(buffer)) > 0) {
            if ((toRead -= read) > 0) {
                output.write(buffer, 0, read);
            } else {
                output.write(buffer, 0, (int) toRead + read);
                break;
            }
        }

    }

    /**
     * Returns true if the given match header matches the given value.
     *
     * @param matchHeader The match header.
     * @param toMatch     The value to be matched.
     * @return True if the given match header matches the given value.
     */
    private static boolean matches(String matchHeader, String toMatch) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, toMatch) > -1
                || Arrays.binarySearch(matchValues, "*") > -1;
    }


    // Inner classes ------------------------------------------------------------------------------

    /**
     * This class represents a byte range.
     */
    protected static class Range {
        long start;
        long end;
        long length;
        long total;

        /**
         * Construct a byte range.
         *
         * @param start Start of the byte range.
         * @param end   End of the byte range.
         * @param total Total length of the byte source.
         */
        public Range(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }

    /**
     * Returns true if the given accept header accepts the given value.
     *
     * @param acceptHeader The accept header.
     * @param toAccept     The value to be accepted.
     * @return True if the given accept header accepts the given value.
     */
    private boolean accepts(String acceptHeader, String toAccept) {
        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
                || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
                || Arrays.binarySearch(acceptValues, "*/*") > -1;
    }

    /**
     * Returns a substring of the given string value from the given begin index to the given end
     * index as a long. If the substring is empty, then -1 will be returned
     *
     * @param value      The string value to return a substring as long for.
     * @param beginIndex The begin index of the substring to be returned as long.
     * @param endIndex   The end index of the substring to be returned as long.
     * @return A substring of the given string value as long or -1 if substring is empty.
     */
    private long sublong(String value, int beginIndex, int endIndex) {
        String substring = value.substring(beginIndex, endIndex);
        return (substring.length() > 0) ? Long.parseLong(substring) : -1;
    }


}
