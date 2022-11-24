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

package uk.ac.ebi.biostudies.file.download;

import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;

public final class FIREDownloadFile implements IDownloadFile {
    private final String path;
    private final S3Object s3Object;
    private final long size;
    private final boolean isDirectory;

    public FIREDownloadFile(String path, S3Object s3Object, long size, boolean isDirectory) {
        this.s3Object = s3Object;
        if (null == path) {
            throw new IllegalArgumentException("File cannot be null");
        }
        this.path = path;
        this.size = size;
        this.isDirectory = isDirectory;
    }

    public String getName() {
        return  path.substring(path.lastIndexOf("/")+1) + (isDirectory ? ".zip" : "");
    }

    public String getPath() {
        return StringUtils.replace(path.toString(), "\\", "/");
    }

    public long getLength() {
        return size;
    }

    public long getLastModified() {
        return 0;
    }

    public boolean canDownload() {
        return true;
    }

    public boolean isDirectory() {
        return isDirectory;
    }


    public InputStream getInputStream() throws IOException {
        return s3Object.getObjectContent();
    }

    public void close() throws IOException {
        if(s3Object!=null)
            s3Object.close();
    }
}
