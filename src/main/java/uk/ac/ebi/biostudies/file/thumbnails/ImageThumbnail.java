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

package uk.ac.ebi.biostudies.file.thumbnails;

import com.twelvemonkeys.image.ResampleOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class ImageThumbnail implements IThumbnail{

    String[] supportedTypes = {"bmp","jpg","wbmp","jpeg","png","gif","tif","tiff"};
    @Override
    public String[] getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public void generateThumbnail(InputStream inputStream, File thumbnailFile) throws IOException{
        BufferedImage input = ImageIO.read(inputStream);
        float height = (float) input.getHeight(), width = (float) input.getWidth();
        if (width > THUMBNAIL_WIDTH || height>THUMBNAIL_WIDTH) {
            float inverseAspectRatio = height / width;
            BufferedImageOp resampler = new ResampleOp(THUMBNAIL_WIDTH, Math.round(inverseAspectRatio * THUMBNAIL_WIDTH), ResampleOp.FILTER_LANCZOS);
            BufferedImage output = resampler.filter(input, null);
            if (!ImageIO.write(output, "png", thumbnailFile)) {
                throw new IOException("Cannot write thumbnail");
            }
        } else {
            ImageIO.write(input, "png", thumbnailFile);
        }
    }
}
