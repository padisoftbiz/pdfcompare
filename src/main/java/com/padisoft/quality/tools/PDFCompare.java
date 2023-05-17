package com.padisoft.quality.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PDFCompare {
    private static String basePath;
    private static String textDivPattern;
    private static int textDifferenceHeadingFontSize;
    private static int textDiffHorizontalPadding;
    private static int textDiffVerticalPadding;
    private static int textDiffLineFontSize;
    private static int textDiffParaPaddingFromHeading;
    private static int textDiffVerticalPaddingForNewLine;
    private static float resultImageWidthScal;
    private static float resultImageHeightScal;
    private static int resultImageHorizontalPadding;
    private static float compareImageResolutionDPI;

    private static float getVerticalAlignment(float actualHeight) {
        return actualHeight < 300 ? actualHeight + 500 : 500;
    }

    public static void generateDifferenceReport(String configFile, String pdf1Path, String pdf2Path, String diffPdfPath) throws IOException {
        loadConfiguration(configFile);

        PDDocument diffPdf = new PDDocument();

        // Compare the PDF files textually using Apache PDFBox
        PDDocument pdf1 = PDDocument.load(new File(pdf1Path));
        PDDocument pdf2 = PDDocument.load(new File(pdf2Path));

        // Code to compare the PDF files textually
        List<String> differences = comparePDFText(pdf1, pdf2, pdf1Path, pdf2Path);

        pdf1.close();
        pdf2.close();

        // Compare the PDF files visually using ImageMagick
        List<BufferedImage> diffImageList = comparePDFImages(pdf1Path, pdf2Path);

        if (diffImageList.size() == 0 && differences.size() == 0) {
            System.out.println("***************** There is no difference found in the files *****************");
            System.exit(0);
        }

        for (int i = 0; i < diffImageList.size(); i++) {
            PDPage page = new PDPage(PDRectangle.LEGAL);
            diffPdf.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(diffPdf, page);

            // Write the visual differences to the PDF
            if (diffImageList.get(i) != null) {
                PDImageXObject image = PDImageXObject.createFromByteArray(diffPdf, bufferedImageToByteArray(diffImageList.get(i)), "diff_image");
                float scale = Math.min(resultImageWidthScal / image.getWidth(), resultImageHeightScal / image.getHeight());
                float width = image.getWidth() * scale;
                float height = image.getHeight() * scale;
                contentStream.drawImage(image, resultImageHorizontalPadding, getVerticalAlignment(height), (width + 150), height);
            }

            printFooter(page, contentStream, "PDF Comparison Report Footer Part");

            contentStream.close();
        }

        if (differences.size() != 0) {
            int paging = differences.size() / 10 == 0 ? 1 : differences.size() / 10;
            int iteration = 0; // Initialize
            int count = 0;
            for (int i = 0; i < paging; i++) {
                PDPage page = new PDPage(PDRectangle.LEGAL);
                diffPdf.addPage(page);
                PDPageContentStream contentStream = new PDPageContentStream(diffPdf, page);

                contentStream.beginText();
                contentStream.setFont(PDType1Font.COURIER_BOLD, textDifferenceHeadingFontSize);
                contentStream.newLineAtOffset(textDiffHorizontalPadding, 950);
                contentStream.showText("File Differences:");
                contentStream.setFont(PDType1Font.COURIER, textDiffLineFontSize);
                contentStream.newLineAtOffset(0, textDiffParaPaddingFromHeading);

                int startIndex = iteration; // Store the current iteration value
                while (count < 12 && iteration < differences.size()) {
                    printLine(contentStream, differences.get(iteration));
                    iteration++;
                    count++;
                }

                if (count == 12) {
                    count = 0;
                    contentStream.endText();
                    printFooter(page, contentStream, "PDF Comparison Report Footer Part");
                    contentStream.close();
                } else if (iteration == differences.size()) {
                    contentStream.endText();
                    printFooter(page, contentStream, "PDF Comparison Report Report Footer Part");
                    contentStream.close();
                } else {
                    // There are more differences to be printed on the next page
                    // Set the iteration and count to continue in the next iteration of the for loop
                    iteration = startIndex;
                    count = 0;
                }
            }
        }

        diffPdf.save(new File(diffPdfPath));
        diffPdf.close();
        System.out.println("Diff PDF generated successfully.");
    }


    private static void loadConfiguration(String configFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        StringBuilder jsonContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonContent.append(line);
        }
        reader.close();

        // Parse the JSON content and set the configuration values
        JSONObject jsonObject = new JSONObject(jsonContent.toString());
        basePath = jsonObject.getString("basePath");
        textDivPattern = jsonObject.getString("textDivPattern");
        textDifferenceHeadingFontSize = jsonObject.getInt("textDifferenceHeadingFontSize");
        textDiffHorizontalPadding = jsonObject.getInt("textDiffHorizontalPadding");
        textDiffVerticalPadding = jsonObject.getInt("textDiffVerticalPadding");
        textDiffLineFontSize = jsonObject.getInt("textDiffLineFontSize");
        textDiffParaPaddingFromHeading = jsonObject.getInt("textDiffParaPaddingFromHeading");
        textDiffVerticalPaddingForNewLine = jsonObject.getInt("textDiffVerticalPaddingForNewLine");
        resultImageWidthScal = (float) jsonObject.getDouble("resultImageWidthScal");
        resultImageHeightScal = (float) jsonObject.getDouble("resultImageHeightScal");
        resultImageHorizontalPadding = jsonObject.getInt("resultImageHorizontalPadding");
        compareImageResolutionDPI = (float) jsonObject.getDouble("compareImageResolutionDPI");
    }


    public static void printLine(PDPageContentStream contentStream, String difference) throws IOException {
        String[] words = difference.split(textDivPattern);
        for (String part : words) {
            limitWordsFromLine(contentStream, part);
        }
        limitWordsFromLine(contentStream, " ");
    }

    private static void limitWordsFromLine(PDPageContentStream contentStream, String line) throws IOException {
        String[] allWords = line.split(" ");
        String oneline = "";
        for (String word : allWords) {
            if (oneline.length() + word.length() > 75) {
                contentStream.showText(oneline.trim());
                contentStream.newLineAtOffset(0, textDiffVerticalPaddingForNewLine);
                oneline = "";
            }
            oneline = oneline + " " + word;
        }
        contentStream.showText(oneline.trim());
        contentStream.newLineAtOffset(0, textDiffVerticalPaddingForNewLine);
    }


    private static void printFooter(PDPage page, PDPageContentStream contentStream, String text) throws IOException {
        PDRectangle mediabox = page.getMediaBox();
        float startX = mediabox.getLowerLeftX() + 20;
        float startY = mediabox.getLowerLeftY() + 25;

        contentStream.beginText();
        contentStream.setFont(PDType1Font.TIMES_ROMAN, 10f);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.newLineAtOffset(startX, startY);
        contentStream.showText("___________________________________________________________________________________________________________________");
        contentStream.newLineAtOffset(startX + 215, startY - 40);
        contentStream.showText(text.trim());
        contentStream.endText();
    }

    private static byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static List<BufferedImage> comparePDFImages(String pdf1Path, String pdf2Path) throws IOException {
        PDDocument pdf1 = PDDocument.load(new File(pdf1Path));
        PDFRenderer renderer1 = new PDFRenderer(pdf1);
        BufferedImage[] images1 = new BufferedImage[pdf1.getNumberOfPages()];
        for (int i = 0; i < images1.length; i++) {
            renderer1.renderImage(i, 1);
            images1[i] = renderer1.renderImageWithDPI(i, compareImageResolutionDPI);
        }

        PDDocument pdf2 = PDDocument.load(new File(pdf2Path));
        PDFRenderer renderer2 = new PDFRenderer(pdf2);
        BufferedImage[] images2 = new BufferedImage[pdf2.getNumberOfPages()];
        for (int i = 0; i < images2.length; i++) {
            images2[i] = renderer2.renderImageWithDPI(i, compareImageResolutionDPI);
        }

        List<BufferedImage> diffImgList = new ArrayList<>();

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
                        int diffRGB = new Color(246, 205, 6).getRGB();
                        int overlapRGBColor = diffRGB & rgb2;
                        image2.setRGB(x, y, overlapRGBColor);
                        hasDifference = true;
                    } else {
                        int transparent = 0x00FFFFFF & rgb1;
                        diff.setRGB(x, y, transparent);
                    }
                }
            }

            if (hasDifference) {
                Graphics2D g = diff.createGraphics();
                g.drawImage(image1, 0, 0, 2550, 3600, null);
                g.drawImage(image2, (image1.getWidth() / 2), 0, 2550, 3600, null);
                g.dispose();
                diffImgList.add(diff);
            }
        }

        pdf1.close();
        pdf2.close();

        return diffImgList;
    }

    private static List<String> comparePDFText(PDDocument pdf1, PDDocument pdf2, String file1, String file2) throws IOException {
        List<String> differences = new ArrayList<>();

        String text1 = new PDFTextStripper().getText(pdf1);
        String text2 = new PDFTextStripper().getText(pdf2);

        String[] lines1 = text1.split("\\r?\\n");
        String[] lines2 = text2.split("\\r?\\n");

        int i = 0, j = 0;
        while (i < lines1.length && j < lines2.length) {
            if (!lines1[i].trim().replace(" ", "").equals(lines2[j].trim().replace(" ", ""))) {
                differences.add("Line " + (i + 1) + ": [" + file1 + "] " + lines1[i] + textDivPattern + "Line " + (j + 1) + ": [" + file2 + "] " + lines2[j]);
            }
            i++;
            j++;
        }
        while (i < lines1.length) {
            differences.add("Line " + (i + 1) + ": [" + file1 + "] " + lines1[i] + textDivPattern);
            i++;
        }
        while (j < lines2.length) {
            differences.add("Line " + (j + 1) + ": [" + file2 + "] " + lines2[j] + textDivPattern);
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

    public static void generatePDFComparison(String constantsFilePath, String fileName1, String fileName2) throws Exception {
        // Load the constant values from a JSON file
        JSONObject constantsJson = loadConstantsFromJson(constantsFilePath);
    
        // Retrieve the values from the JSON object
        String basePath = constantsJson.getString("basePath");
        String textDivPattern = constantsJson.getString("textDivPattern");
        int textDifferenceHeadingFontSize = constantsJson.getInt("textDifferenceHeadingFontSize");
        // ... and so on
    
        // Use the provided file names to construct the PDF paths
        String pdf1Path = basePath + fileName1;
        String pdf2Path = basePath + fileName2;
    
        // Compare the PDF files and generate the difference report
        generateDifferenceReport(constantsFilePath, pdf1Path, pdf2Path, "diff_report.pdf");
    }
    
    public static void main(String[] args) throws Exception {
        // Check if the correct number of arguments is provided
        if (args.length != 2) {
            System.out.println("Please provide two PDF file names as arguments.");
            System.exit(0);
        }
    
        // Get the file names from the arguments
        String fileName1 = args[0];
        String fileName2 = args[1];
    
        // Use the file names to construct the PDF paths
        String pdf1Path = basePath + fileName1;
        String pdf2Path = basePath + fileName2;
    
        // Specify the path to the constants JSON file
        String constantsFilePath = "constants.json";
    
        // Generate the PDF comparison report
        generatePDFComparison(constantsFilePath, fileName1, fileName2);
    }
    
}
