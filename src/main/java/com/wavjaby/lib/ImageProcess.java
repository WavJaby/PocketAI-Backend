package com.wavjaby.lib;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageProcess {

    public static BufferedImage scaleImageBaseOnHeight(int targetHeight, BufferedImage image) {
        float scale = (float) targetHeight / image.getHeight();
        return scaleImage(scale, image);
    }

    public static BufferedImage scaleImageBaseOnWidth(int targetWidth, BufferedImage image) {
        float scale = (float) targetWidth / image.getWidth();
        return scaleImage(scale, image);
    }

    public static BufferedImage scaleImage(float scale, BufferedImage image) {
        int width = (int) (image.getWidth() * scale);
        int height = (int) (image.getHeight() * scale);
        Image scaledInstance = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        graphics.drawImage(scaledInstance, 0, 0, null);
        graphics.dispose();
        return image;
    }

    public static BufferedImage sharpenImage(float blendFactor, BufferedImage image) {
        // Sharpens image
        final float[] sharpenKernel = {
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
        };
        final float[] identityKernel = {
                0, 0, 0,
                0, 1, 0,
                0, 0, 0
        };

        // Blend the kernels
        float[] blendedKernel = new float[sharpenKernel.length];
        for (int i = 0; i < sharpenKernel.length; i++) {
            blendedKernel[i] = blendFactor * sharpenKernel[i] + (1 - blendFactor) * identityKernel[i];
        }

        Kernel kernel = new Kernel(3, 3, blendedKernel);
        BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        image = op.filter(image, null);
        return image;
    }

    public static Bitmap renderLatexToImage(String msg, String uuid) {
        TeXFormula formula = new TeXFormula(msg);

        TeXIcon icon = formula.new TeXIconBuilder()
                .setStyle(TeXConstants.STYLE_SCRIPT)
                .setSize(100)
                .setFGColor(Color.white)
                .setTrueValues(true)
                .build();
        // Render image
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.black);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        icon.paintIcon(null, g2, 0, 0);
        g2.dispose();

        // ResizeImage
        int targetHeight = 128;
        int targetWidth = (int) (image.getWidth() * ((float) targetHeight / image.getHeight()));
        Image sizedImage = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_FAST);
        image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        g2 = image.createGraphics();
        g2.drawImage(sizedImage, 0, 0, null);
        g2.dispose();

        // Convert to custom 2bit color
        int[] imageData = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        int threshold = (int) (0xFFFFFF * 0.5f);
        int threshold2 = (int) (0xFFFFFF * 0.2f);
        for (int i = 0; i < image.getHeight(); i++) {
            int off = i * image.getWidth();
            for (int j = 0; j < image.getWidth(); j += 2) {
                if (j + 1 < image.getWidth()) {
                    // 2 bit gray
                    int val = ((imageData[off + j] & 0xFFFFFF) + (imageData[off + j + 1] & 0xFFFFFF)) >> 1;
                    if (val > threshold) {
                        imageData[off + j] = imageData[off + j + 1] = 0xFFFFFF;
                    } else if (val > threshold2) {
                        imageData[off + j + 1] = (i % 2 == 0) ? 0xFFFFFF : 0x0;
                        imageData[off + j] = (i % 2 == 0) ? 0x0 : 0xFFFFFF;
                    } else
                        imageData[off + j] = imageData[off + j + 1] = 0x0;
                } else
                    imageData[off + j] = ((imageData[off + j] & 0xFFFFFF) > threshold) ? 0xFFFFFF : 0x0;
            }
        }

        // Convert to bitmap
        int width = image.getWidth();
        int outByteWidth = (int) Math.ceil(width / 8.f);
        byte[] outData = new byte[outByteWidth * image.getHeight()];
        for (int i = 0; i < image.getHeight(); i++) {
            int off = i * width, outOff = i * outByteWidth;
            for (int j = 0; j < width; j += 8) {
                outData[outOff + (j >> 3)] = (byte) ((getImageData(imageData, width, j, off) & 0b10000000) |
                                                     (getImageData(imageData, width, j + 1, off) & 0b01000000) |
                                                     (getImageData(imageData, width, j + 2, off) & 0b00100000) |
                                                     (getImageData(imageData, width, j + 3, off) & 0b00010000) |
                                                     (getImageData(imageData, width, j + 4, off) & 0b00001000) |
                                                     (getImageData(imageData, width, j + 5, off) & 0b00000100) |
                                                     (getImageData(imageData, width, j + 6, off) & 0b00000010) |
                                                     (getImageData(imageData, width, j + 7, off) & 0b00000001)
                );
            }
        }

        // Save debug image
        try {
            BufferedImage debugOut = new BufferedImage(outByteWidth * 8, image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            debugOut.setRGB(0, 0, image.getWidth(), image.getHeight(), imageData, 0, image.getWidth());
            ImageIO.write(debugOut, "bmp", new File("images/" + uuid + ".bmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Bitmap(outByteWidth * 8, targetHeight, outData);
    }

    private static int getImageData(int[] imageData, int imageDataWidth, int x, int offY) {
        return (x < imageDataWidth) ? imageData[offY + x] : 0;
    }

    public static byte[] createJpgDataBytes(float quality, BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Write jpeg to output stream
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(quality);

        jpgWriter.setOutput(new MemoryCacheImageOutputStream(out));
        jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
        jpgWriter.dispose();

        byte[] imageBytes = out.toByteArray();
        out.close();
        return imageBytes;
    }
}
