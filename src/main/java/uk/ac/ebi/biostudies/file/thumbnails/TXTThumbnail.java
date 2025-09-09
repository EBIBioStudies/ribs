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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.core.io.ClassPathResource;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

public class TXTThumbnail implements IThumbnail {

  private static final Logger LOGGER = LogManager.getLogger(TXTThumbnail.class.getName());

  private final Color background = Color.WHITE;
  private static Font font;
  private static final String FONT = "NotoMono-Regular.ttf";

  static {
    try {
      font =
          Font.createFont(
                  Font.TRUETYPE_FONT, new ClassPathResource("fonts/" + FONT).getInputStream())
              .deriveFont(12.0f);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static final String[] supportedTypes = {"txt", "csv"};

  @Override
  public String[] getSupportedTypes() {
    return supportedTypes;
  }

  @Override
  public void generateThumbnail(InputStream inputStream, File thumbnailFile) throws IOException {

    byte[] data = new byte[512]; // get only the first 0.5K
    int bytesRead = inputStream.read(data);
    AttributedString text = new AttributedString(new String(data));
    BufferedImage image =
        new BufferedImage(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setColor(background);
    g.fillRect(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
    g.setColor(Color.BLACK);
    text.addAttribute(TextAttribute.FONT, font);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    AttributedCharacterIterator paragraph = text.getIterator();
    int paragraphStart = paragraph.getBeginIndex();
    int paragraphEnd = paragraph.getEndIndex();
    FontRenderContext frc = g.getFontRenderContext();
    LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);
    float breakWidth = THUMBNAIL_WIDTH;
    float drawPosY = 0;
    lineMeasurer.setPosition(paragraphStart);
    while (lineMeasurer.getPosition() < paragraphEnd) {
      TextLayout layout = lineMeasurer.nextLayout(breakWidth);
      float drawPosX = layout.isLeftToRight() ? 0 : breakWidth - layout.getAdvance();
      drawPosY += layout.getAscent();
      layout.draw(g, drawPosX, drawPosY);
      drawPosY += layout.getDescent() + layout.getLeading();
    }

    ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
    inputStream.close();
  }
}
