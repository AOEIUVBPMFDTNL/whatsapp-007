package com.whatsapp.android.util;

import lombok.SneakyThrows;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

public class ImageResizerAndCompressor {
    @SneakyThrows
    public static byte[] convertImageToByteArray(byte[] imageData) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
            BufferedImage inputImage = ImageIO.read(bais);
            BufferedImage resizedImage = new BufferedImage(640, 640, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.drawImage(inputImage, 0, 0, 640, 640, null);
            g2d.dispose();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = writers.next();
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(0.75f);
                    }
                    writer.write(null, new IIOImage(resizedImage, null, null), param);
                } finally {
                    writer.dispose();
                }
                return baos.toByteArray();
            }
        }
    }
}
