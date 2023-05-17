# pdfcompare

PDFCompare Tool
The PDFCompare tool is a Java-based utility for comparing two PDF files and generating a difference report. It uses Apache PDFBox for textual comparison and ImageMagick for visual comparison.

Prerequisites
Before using the PDFCompare tool, ensure that you have the following prerequisites installed:

Java Development Kit (JDK) version 8 or later
Apache Maven
Installation
Clone the PDFCompare repository from GitHub:

bash
Copy code
git clone https://github.com/your-username/pdfcompare.git
Navigate to the project directory:

bash
Copy code
cd pdfcompare
Build the project using Maven:

bash
Copy code
mvn clean package
Usage
To compare two PDF files and generate a difference report, follow these steps:

Create a JSON configuration file that specifies the required constants. The configuration file should have the following structure:

json
Copy code
{
  "basePath": "/path/to/pdf/files/",
  "textDivPattern": "\\s+",
  "textDifferenceHeadingFontSize": 12,
  "textDiffHorizontalPadding": 20,
  "textDiffVerticalPadding": 10,
  "textDiffLineFontSize": 10,
  "textDiffParaPaddingFromHeading": 10,
  "textDiffVerticalPaddingForNewLine": 10,
  "resultImageWidthScal": 800,
  "resultImageHeightScal": 600,
  "resultImageHorizontalPadding": 50,
  "compareImageResolutionDPI": 300
}
Adjust the values according to your specific requirements.

Execute the following command to compare the PDF files:

bash
Copy code
java -jar target/pdfcompare-1.0.jar /path/to/config.json file1.pdf file2.pdf
Replace /path/to/config.json with the path to your JSON configuration file, and file1.pdf and file2.pdf with the names of the PDF files you want to compare.

After the comparison is complete, a difference report file named diff_report.pdf will be generated in the current directory.

Example
Here's an example of how to use the PDFCompare tool:

Create a JSON configuration file named constants.json with the desired constants.

Run the following command to compare two PDF files named document1.pdf and document2.pdf:

bash
Copy code
java -jar target/pdfcompare-1.0.jar constants.json document1.pdf document2.pdf
The tool will compare the PDF files and generate a difference report named diff_report.pdf.

License
This project is licensed under the MIT License. See the LICENSE file for details.

Contributing
Contributions to the PDFCompare tool are welcome! If you find any issues or want to add new features, feel free to open an issue or submit a pull request.

Acknowledgments
Apache PDFBox: https://pdfbox.apache.org/
ImageMagick: https://imagemagick.org/

Contact
For any questions or inquiries, please contact padisoft.biz@gmail.com.
