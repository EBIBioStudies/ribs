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

import java.awt.image.BufferedImage;
import java.io.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class DOCXThumbnail implements IThumbnail{

    private static final String [] supportedTypes= {"docx"};

    @Override
    public String[] getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public void generateThumbnail(InputStream inputStream, File thumbnailFile) throws IOException{
        String tempPDFFilePath = thumbnailFile.getAbsolutePath() + ".pdf";
        FileOutputStream out = new FileOutputStream(tempPDFFilePath);
        XWPFDocument wordDoc = new XWPFDocument(inputStream);
        PdfConverter.getInstance().convert(wordDoc, out, PdfOptions.create());
        inputStream.close();
        out.close();
        PDFRenderer pdfRenderer = new PDFRenderer(PDDocument.load(  new File(tempPDFFilePath)));
        BufferedImage image = pdfRenderer.renderImageWithDPI (BufferedImage.TYPE_INT_RGB, 96);
        ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
        new File(tempPDFFilePath).delete();
    }


}
