package com.padisoft.quality.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.im4java.process.InputProvider;



public class PDFCompare {

    public static void main(String[] args) throws Exception {
        String pdf1Path = "File1.pdf";
        String pdf2Path = "File2.pdf";
        String diffPdfPath = "diff.pdf";

        // Compare the PDF files textually using Apache PDFBox
        PDDocument pdf1 = PDDocument.load(new File(pdf1Path));
        PDDocument pdf2 = PDDocument.load(new File(pdf2Path));

        // Code to compare the PDF files textually
        List<String> differences = comparePDFText(pdf1, pdf2);

        pdf1.close();
        pdf2.close();

        // Compare the PDF files visually using ImageMagick
        BufferedImage diffImage = comparePDFImages(pdf1Path, pdf2Path);

        // Generate the diff PDF with both textual and visual differences
        PDDocument diffPdf = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        diffPdf.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(diffPdf, page);

        // Write the textual differences to the PDF
        contentStream.beginText();
        contentStream.setFont(PDType1Font.COURIER_BOLD, 12);
        contentStream.newLineAtOffset(50, 700);
        contentStream.showText("Textual Differences:");
        contentStream.setFont(PDType1Font.COURIER, 10);
        contentStream.newLineAtOffset(0, -20);
        for (String difference : differences) {
            contentStream.showText(difference);
            contentStream.newLineAtOffset(0, -15);
        }
        contentStream.endText();

        // Write the visual differences to the PDF
        if (diffImage != null) {
            PDImageXObject image = PDImageXObject.createFromByteArray(diffPdf, bufferedImageToByteArray(diffImage), "diff_image");
            float scale = Math.min(600f / image.getWidth(), 800f / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            contentStream.drawImage(image, 50, 400, width, height);
        }

        contentStream.close();

        diffPdf.save(new File(diffPdfPath));
        diffPdf.close();

        System.out.println("Diff PDF generated successfully.");
    }

    private static byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static BufferedImage comparePDFImages(String pdf1Path, String pdf2Path) throws IOException {
        // Convert PDF files to images using PDFBox
        PDDocument pdf1 = PDDocument.load(new File(pdf1Path));
        PDFRenderer renderer1 = new PDFRenderer(pdf1);
        BufferedImage[] images1 = new BufferedImage[pdf1.getNumberOfPages()];
        for (int i = 0; i < images1.length; i++) {
            images1[i] = renderer1.renderImage(i, 1);
        }
    
        PDDocument pdf2 = PDDocument.load(new File(pdf2Path));
        PDFRenderer renderer2 = new PDFRenderer(pdf2);
        BufferedImage[] images2 = new BufferedImage[pdf2.getNumberOfPages()];
        for (int i = 0; i < images2.length; i++) {
            images2[i] = renderer2.renderImage(i, 1);
        }
    
        // Compare the images using Java BufferedImage
        BufferedImage diffImage = null;
    
        for (int i = 0; i < images1.length; i++) {
            BufferedImage image1 = images1[i];
            BufferedImage image2 = images2[i];
    
            int width1 = image1.getWidth();
            int height1 = image1.getHeight();
            int width2 = image2.getWidth();
            int height2 = image2.getHeight();
    
            if (width1 != width2 || height1 != height2) {
                System.err.println("Images have different dimensions, skipping comparison for page " + (i + 1));
                continue;
            }
    
            BufferedImage diff = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_ARGB);
            boolean hasDifference = false;
    
            for (int y = 0; y < height1; y++) {
                for (int x = 0; x < width1; x++) {
                    int rgb1 = image1.getRGB(x, y);
                    int rgb2 = image2.getRGB(x, y);
    
                    if (rgb1 != rgb2) {
                        int diffRGB = 0xFF0000; // Highlight differences in red
                        diff.setRGB(x, y, diffRGB);
                        hasDifference = true;
                    } else {
                        int transparent = 0x00FFFFFF & rgb1;
                        diff.setRGB(x, y, transparent);
                    }
                }
            }
    
            if (hasDifference) {
                Graphics2D g = diff.createGraphics();
                g.drawImage(image1, 0, 0, null);
                g.drawImage(image2, width1 / 2, 0, null);
                g.dispose();
    
                diffImage = diff;
                break;
            }
        }
    
        pdf1.close();
        pdf2.close();
    
        return diffImage;
    }
        

    private static List<String> comparePDFText(PDDocument pdf1, PDDocument pdf2) throws IOException {
        List<String> differences = new ArrayList<>();
    
        // Get the text content of each PDF file
        String text1 = new PDFTextStripper().getText(pdf1);
        String text2 = new PDFTextStripper().getText(pdf2);
    
        // Split the text content into lines
        String[] lines1 = text1.split("\\r?\\n");
        String[] lines2 = text2.split("\\r?\\n");
    
        // Compare the lines of the two PDF files
        int i = 0, j = 0;
        while (i < lines1.length && j < lines2.length) {
            if (!lines1[i].equals(lines2[j])) {
                // Lines are different, add them to the differences list
                differences.add("Line " + (i+1) + ": " + lines1[i] + " | " + lines2[j]);
            }
            i++;
            j++;
        }
        while (i < lines1.length) {
            // Remaining lines in the first PDF file are different
            differences.add("Line " + (i+1) + ": " + lines1[i] + " | ");
            i++;
        }
        while (j < lines2.length) {
            // Remaining lines in the second PDF file are different
            differences.add("Line " + (j+1) + ": " + " | " + lines2[j]);
            j++;
        }
    
        return differences;
    }

    private static class PipeDataInputProvider implements InputProvider {
        private final InputStream inputStream;

        public PipeDataInputProvider(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void provideInput(OutputStream outputStream) throws IOException {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, len);
                }
            }
        }
    }
}

    
