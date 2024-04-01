import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

public class HackedCampus {

    // Define a list to store downloaded image filenames
    private static List<String> downloadedImageFilenames = new ArrayList<>();

    public static void main(String[] args) {

        System.out.println("HackedCampus Book Downloader CLI | v.1.0 | Written by Jan Roszczynialski | 2023");
        System.out.println("******************************************************************************************************************");
        System.out.println("USER'S MANUAL: While browsing Hackampus, go to the page containing a book (slideshow) that you are interested in.");
        System.out.println("The embedded content viewer should appear.");
        System.out.println("Do right click on a currently displayed page (slide) and choose \"Copy Image Link\" command from the context menu.");
        System.out.println("Once you have made sure that the page\'s (slide\'s) URL is copied to a clipboard, please paste it here and hit Enter.");
        System.out.println("From now on, the program will determine a range of pages (slides) that make up the whole book (slideshow).");
        System.out.println("Once it is done, the program will start to combine and convert contents of the respective URLs into a PDF file.");
        System.out.println("The whole procedure may take a while, so please be patient :)");
        System.out.println("Once it is finished, your PDF file will show up under default Downloads directory (output.pdf).");
        System.out.println("Enjoy! ;)");
        System.out.println("******************************************************************************************************************");

        String fullUrl = getInput("Enter the full URL: ");
        String baseUrl = truncateBaseUrl(fullUrl);
        int startPage = 1;
        int endPage = 999999;  // Adjust the end page as needed

        String outputFilePath = getDefaultDownloadsFolderPath() + "actionable_urls.txt";

        int consecutive404Count = 0; // Track consecutive 404 responses
        try {
            FileWriter writer = new FileWriter(outputFilePath);

            for (int pageNumber = startPage; pageNumber <= endPage; pageNumber++) {
                String urlToCheck = baseUrl + formatPageNumber(pageNumber) + ".png";

                int responseCode = checkPageExistence(urlToCheck);

                if (responseCode != 404) {
                    writer.write(urlToCheck + "\n");
                    System.out.println("Fetched URL: " + urlToCheck);
                    consecutive404Count = 0; // Reset consecutive count if not 404
                } else {
                    consecutive404Count++; // Increment count if 404
                    System.out.println("URL: " + urlToCheck + " returned a 404 response. Skipping...");
                    if (consecutive404Count >= 3) {
                        System.out.println("Three consecutive 404 responses encountered. Closing the list of actionable URLs.");
                        break; // Terminate if three consecutive 404 responses
                    }
                }
            }

            writer.close();
            System.out.println("Actionable URLs written to " + outputFilePath);

            List<String> urls = readUrlsFromTxtFile(outputFilePath);

            for (String url : urls) {
                try {
                    byte[] imageData = downloadImage(url);
                    if (imageData != null) {
                        // Generate a unique filename based on the current timestamp
                        String fileName = "image_" + getCurrentTimestamp() + ".png";
                        String filePath = getDefaultDownloadsFolderPath() + fileName;
                        saveImageToFile(imageData, filePath);
                        downloadedImageFilenames.add(fileName);
                        System.out.println("Image downloaded successfully: " + url);
                    }
                } catch (IOException e) {
                    System.out.println("Error downloading image: " + e.getMessage());
                }
            }

            // Create the PDF document and add images to it
            String outputPdfPath = getDefaultDownloadsFolderPath() + "output.pdf";
            createPdfFromImages(downloadedImageFilenames, outputPdfPath);
            System.out.println("PDF created successfully: " + outputPdfPath);

            // Delete downloaded images and the text file with actionable URLs
            deleteDownloadedImages();
            deleteTextFile(outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readUrlsFromTxtFile(String filePath) {
        List<String> urls = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;

            while ((line = reader.readLine()) != null) {
                urls.add(line);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return urls;
    }

    public static void saveImageToFile(byte[] imageData, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageData);
        }
    }

    public static void createPdfFromImages(List<String> imageFilenames, String outputPdfPath) throws IOException {
        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputPdfPath));
        Document document = new Document(pdfDocument);

        int totalImages = imageFilenames.size();
        int completedImages = 0;

        for (String fileName : imageFilenames) {
            Image image = new Image(ImageDataFactory.create(getDefaultDownloadsFolderPath() + fileName));
            // Determine image dimensions
            float width = image.getImageWidth();
            float height = image.getImageHeight();

            // Set page size based on the image dimensions and remove margins
            PageSize pageSize = new PageSize(width, height);
            pdfDocument.addNewPage(pageSize);

            document.setMargins(0, 0, 0, 0); // Remove margins
            document.add(image);

            completedImages++;
            System.out.println("Page " + completedImages + "/" + totalImages + " successfully processed.");
        }

        document.close();
        pdfDocument.close();
    }

    public static byte[] downloadImage(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                byte[] imageData = out.toByteArray();
                return imageData;
            }
        } else {
            return null;
        }
    }

    public static String truncateBaseUrl(String fullUrl) {
        int lastIndex = fullUrl.lastIndexOf('-');  // Find the last hyphen
        return fullUrl.substring(0, lastIndex + 1);  // Include the hyphen
    }

    public static String formatPageNumber(int pageNumber) {
        return String.format("%06d", pageNumber);
    }

    public static int checkPageExistence(String urlToCheck) {
        try {
            URL url = new URL(urlToCheck);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            return connection.getResponseCode();
        } catch (IOException e) {
            return -1;
        }
    }

    public static String getDefaultDownloadsFolderPath() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + "Downloads" + File.separator;
    }

    public static String getInput(String message) {
        System.out.print(message);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void deleteDownloadedImages() {
        for (String fileName : downloadedImageFilenames) {
            // Construct the local path to the downloaded image
            String localImagePath = getDefaultDownloadsFolderPath() + fileName;

            // Create a File object for the downloaded image
            File imageFile = new File(localImagePath);

            // Check if the image file exists and delete it
            if (imageFile.exists()) {
                if (imageFile.delete()) {
                    System.out.println("Downloaded image deleted: " + imageFile.getName());
                } else {
                    System.out.println("Error deleting downloaded image: " + imageFile.getName());
                }
            }
        }
    }

    public static void deleteTextFile(String filePath) {
        // Delete the text file with actionable URLs
        File fileToDelete = new File(filePath);
        if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
                System.out.println("Text file with actionable URLs deleted successfully.");
            } else {
                System.out.println("Error deleting text file with actionable URLs.");
            }
        }
    }

    public static String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        return dateFormat.format(new Date());
    }
}
