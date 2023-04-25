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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.im4java.process.InputProvider;


/**
 * @author Deepak.Rathod2
 * Purpose: Class is use for comparing two PDF files differences (Textual & visual) then generate difference report PDF.
 */
public class PDFCompare {

    private static String basePath = "D:\\user\\POC\\PDFCompare\\Tested\\Files\\";
    private static String textDivPattern = " <DIFF> ";
    private static int textDifferenceHeadingFontSize = 12;
    private static int textDiffHorizontalPadding = 40;
    private static int textDiffVerticalPadding = 700;
    private static int textDiffLineFontSize = 10;
    private static int textDiffParaPaddingFromHeading = -20;
    private static int textDiffVerticalPaddingForNewLine = -15;
    private static float resultImageWidthScal = 350f;
    private static float resultImageHeightScal = 800f;
    private static int resultImageHorizontalPadding = 50;
    private static float compareImageResolutionDPI = 600f;

    private static float getVerticalAlingment(float actualHeight){
        return actualHeight < 300 ? actualHeight+500:500;
    }

    public static void main(String[] args) throws Exception {

        String fileName1 = "1_01_TW_EN_Baseline.pdf";
        String fileName2 = "1_01_TW_EN_New.pdf";
        String pdf1Path = basePath+fileName1; //3_01_TOTA_ECO_baseline
        String pdf2Path = basePath+fileName2; //3_01_TOTA_ECO_New
        String diffPdfPath = basePath+"diff.pdf";
        PDDocument diffPdf = new PDDocument();

        // Compare the PDF files textually using Apache PDFBox
        PDDocument pdf1 = PDDocument.load(new File(pdf1Path)); //Load PDF File - 1
        PDDocument pdf2 = PDDocument.load(new File(pdf2Path)); //Load PDF File - 2

        // Code to compare the PDF files textually
        List<String> differences = comparePDFText(pdf1, pdf2, fileName1, fileName2); // Compare 2 PDF files text and return the differences
        pdf1.close(); //Close File-1
        pdf2.close(); //Close File-2

        // Compare the PDF files visually using ImageMagick
        List<BufferedImage> diffImageList = comparePDFImages(pdf1Path, pdf2Path);
        if(diffImageList.size()==0 && differences.size()==0){
            System.out.println("***************** There is no difference found in file *****************");
            System.exit(0);
        }

        for(int i=0;i<diffImageList.size();i++){
            PDPage page = new PDPage(PDRectangle.LEGAL); // Defined PDF compare result page type
            diffPdf.addPage(page); //Add [i] page to PDF
            PDPageContentStream contentStream = new PDPageContentStream(diffPdf, page);
            // Write the visual differences to the PDF
            if (diffImageList.get(i) != null) {
                //convert compare image to PNG as result image
                PDImageXObject image = PDImageXObject.createFromByteArray(diffPdf, bufferedImageToByteArray(diffImageList.get(i)), "diff_image");
                float scale = Math.min(resultImageWidthScal / image.getWidth(), resultImageHeightScal / image.getHeight()); // take min value from both
                float width = image.getWidth() * scale; //set Width of result image.
                float height = image.getHeight() * scale; //set Height of result image.
                // draw result image on PDF. X = Horizontal padding, Y=Vertical Padding then set width & height.
                contentStream.drawImage(image, resultImageHorizontalPadding, getVerticalAlingment(height), (width+150), height);

            }
            printFooter(page, contentStream, "PDF Comparison Report Footer Part.");
            //close PDPageContentStream object
            contentStream.close();
        }
        //--------------------- ADDING TEXTUAL DIFFERENCE TO THE NEW PAGE ---------------------------
        if(differences.size()!=0){
            int paging = differences.size()/10==0?1:differences.size()/10;
            int iteration = 0, count = 0;
            for(int i=0;i<paging;i++){
                PDPage page = new PDPage(PDRectangle.LEGAL); // Defined PDF compare result page type
                diffPdf.addPage(page);
                PDPageContentStream contentStream = new PDPageContentStream(diffPdf, page);
                // Write the textual differences to the PDF
                contentStream.beginText(); //Begin some text operations.
                contentStream.setFont(PDType1Font.COURIER_BOLD, textDifferenceHeadingFontSize); //set Fond for heading
                contentStream.newLineAtOffset(textDiffHorizontalPadding, 950); //set x,y for heading
                contentStream.showText("File Differences:"); // Heading text
                contentStream.setFont(PDType1Font.COURIER, textDiffLineFontSize); //set text for difference text
                contentStream.newLineAtOffset(0, textDiffParaPaddingFromHeading); //set padding between heading & text
                for(int index=iteration;index<differences.size();index++){
                    printLine(contentStream, differences.get(index)); // process of printing difference line in PDF [ 93 chars each line ]
                    iteration++;
                    count++;
                    if(count==12){
                        count=0;
                        contentStream.endText(); // End of writing text in PDf
                        printFooter(page, contentStream, "Corcentric Inc. PDF Comparison Report");
                        contentStream.close();
                        break;
                    }
                }
                if(count!=0){
                    contentStream.endText(); // End of writing text in PDf
                    printFooter(page, contentStream, "Corcentric Inc. PDF Comparison Report");
                    contentStream.close();
                }
            }
        }
        //----------------------------------------------------------------------------------------------
        // This object add content to PDF report.
        diffPdf.save(new File(diffPdfPath)); //Generate difference PDF on given location
        diffPdf.close(); //close PDF object.
        System.out.println("Diff PDF generated successfully.");
    }

    public static void printLine(PDPageContentStream  contentStream, String difference) throws IOException {
        String[] words = difference.split(textDivPattern);
        for (String part : words) {
            limitWordsFromLine(contentStream, part);
        }
        limitWordsFromLine(contentStream, " ");
    }
    private static void limitWordsFromLine(PDPageContentStream  contentStream, String line) throws IOException {
        String[] allWords = line.split(" ");
        String oneline="";
        for(String word : allWords){
            if(oneline.length()>75){
                contentStream.showText(oneline.trim());
                contentStream.newLineAtOffset(0, textDiffVerticalPaddingForNewLine);
                oneline="";
            }
            oneline = oneline+" "+word;
        }
        contentStream.showText(oneline.trim());
        contentStream.newLineAtOffset(0, textDiffVerticalPaddingForNewLine);
    }

    private static void printFooter(PDPage page, PDPageContentStream  contentStream, String text) throws IOException {
        PDRectangle mediabox = page.getMediaBox();
        float startX = mediabox.getLowerLeftX() + 20;
        float startY = mediabox.getLowerLeftY() + 25;

        contentStream.beginText();
        contentStream.setFont(PDType1Font.TIMES_ROMAN, 10f);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.newLineAtOffset(startX, startY);
        contentStream.showText("___________________________________________________________________________________________________________________");
        contentStream.newLineAtOffset(startX+215, startY-40);
        contentStream.showText(text.trim());
        contentStream.endText();
    }
    private static byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static List<BufferedImage> comparePDFImages(String pdf1Path, String pdf2Path) throws IOException {
        // Convert PDF files to images using PDFBox - Logic for File-1
        PDDocument pdf1 = PDDocument.load(new File(pdf1Path));
        PDFRenderer renderer1 = new PDFRenderer(pdf1);
        BufferedImage[] images1 = new BufferedImage[pdf1.getNumberOfPages()];
        for (int i = 0; i < images1.length; i++) {
            renderer1.renderImage(i, 1);
            images1[i] = renderer1.renderImageWithDPI(i, compareImageResolutionDPI);
        }

        // Convert PDF files to images using PDFBox - Logic for File-2
        PDDocument pdf2 = PDDocument.load(new File(pdf2Path));
        PDFRenderer renderer2 = new PDFRenderer(pdf2);
        BufferedImage[] images2 = new BufferedImage[pdf2.getNumberOfPages()];
        for (int i = 0; i < images2.length; i++) {
            images2[i] = renderer2.renderImageWithDPI(i, compareImageResolutionDPI);
        }

        // Compare the images using Java BufferedImage
        List<BufferedImage> diffImgList = new ArrayList<>();

        for (int i = 0; i < images1.length; i++) { //Page by page iterations [ page-1 to N ]
            BufferedImage image1 = images1[i]; //storing page into BI object
            BufferedImage image2 = images2[i];

            int width1 = image1.getWidth(); //collecting page width
            int height1 = image1.getHeight(); //collecting page height
            int width2 = image2.getWidth();
            int height2 = image2.getHeight();

            if (width1 != width2 || height1 != height2) { //if both page size if different then it will not compare
                System.err.println("Images have different dimensions, skipping comparison for page " + (i + 1));
                continue;
            }

            BufferedImage diff = new BufferedImage(width1, height1, BufferedImage.TYPE_INT_ARGB);
            boolean hasDifference = false;
            for (int y = 0; y < height1; y++) { //vertically pixel by pixel processing.
                for (int x = 0; x < width1; x++) { //Horizontally pixel by pixel processing.
                    int rgb1 = image1.getRGB(x, y);
                    int rgb2 = image2.getRGB(x, y);

                    if (rgb1 != rgb2) {
                        //int diffRGB = 0x00FFFFFF & rgb1;
                        int diffRGB = new Color(246, 205, 6).getRGB(); // Highlight differences in red
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
                //g.drawImage(image1, -130, 0,2550,3600,null); //Y=Vertical; X=Horizontal
                g.drawImage(image1, 0, 0,2550,3600,null); //Y=Vertical; X=Horizontal
                g.drawImage(image2, (image1.getWidth()/2), 0, 2550,3600, null);
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

        // Get the text content of each PDF file
        String text1 = new PDFTextStripper().getText(pdf1);
        String text2 = new PDFTextStripper().getText(pdf2);

        // Split the text content into lines
        String[] lines1 = text1.split("\\r?\\n");
        String[] lines2 = text2.split("\\r?\\n");

        // Compare the lines of the two PDF files
        int i = 0, j = 0;
        while (i < lines1.length && j < lines2.length) {
            if (!lines1[i].trim().replace(" ","").equals(lines2[j].trim().replace(" ",""))) {
                // Lines are different, add them to the differences list
                differences.add("Line " + (i+1) + ": ["+file1+"] " + lines1[i] + textDivPattern + "Line " + (j+1) + ": ["+file2+"] " +lines2[j]);
            }
            i++;
            j++;
        }
        while (i < lines1.length) {
            // Remaining lines in the first PDF file are different
            differences.add("Line " + (i+1) + ": ["+file1+"] " + lines1[i] + textDivPattern);
            i++;
        }
        while (j < lines2.length) {
            // Remaining lines in the second PDF file are different
            differences.add("Line " + (j+1) + ": ["+file2+"] " + lines2[j] + textDivPattern);
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


