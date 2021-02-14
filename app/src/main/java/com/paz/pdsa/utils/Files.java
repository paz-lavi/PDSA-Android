package com.paz.pdsa.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.paz.logger.EZLog;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class Files {
    private final static String TAG = "DSA";
    private static final EZLog ezLog = EZLog.getInstance();

    public static String readFile(Context context, Uri uri) throws IOException {
        InputStream stream;
        stream = context.getContentResolver().openInputStream(uri);
        String mime = context.getContentResolver().getType(uri);
        if (!mime.equalsIgnoreCase(Constants.PDF_MIME)) {

            return new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
        } else {
            PDDocument document = PDDocument.load(stream);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String str = pdfStripper.getText(document);
            document.close();
            ezLog.debug( "readFile: file:\n" + str + "\n\n.");
            return str;
        }
    }

    public static void writePdfFile(Context context, String data, String filePath, Uri originalFile) throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(originalFile);
        File signed = new File(filePath);
        PDDocument org = PDDocument.load(stream);
        PDDocument sig = new PDDocument();
        for (PDPage page : org.getDocumentCatalog().getPages()) {
            sig.addPage(page);
        }

        PDPage page = new PDPage();
        sig.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(sig, page);
        contentStream.beginText();
        contentStream.setFont(PDType0Font.load(sig, context.getAssets().open("com/tom_roush/pdfbox/resources/ttf/LiberationSans-Regular.ttf")), 12);
        contentStream.newLineAtOffset(25, 700);
        contentStream.setLeading(14.5f);
        contentStream.showText(data.replace("\n", ""));


        contentStream.endText();
        contentStream.close();
        sig.save(filePath);
        org.close();
        sig.close();
    }

    public static void writeToFile(String data, String filePath) throws IOException {


        BufferedWriter out = new BufferedWriter(
                new FileWriter(filePath, false));
        out.write(data);
        out.close();

    }

    public static void appendStrToFile(String filePath, String str) throws IOException {


        // Open given file in append mode.
        BufferedWriter out = new BufferedWriter(
                new FileWriter(filePath, true));
        out.write(str);
        out.close();

    }

    public static boolean copyFile(Context context, Uri source, Uri dest, String mime) {
        if (!mime.equals(Constants.PDF_MIME )) {
            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                outputStream = context.getContentResolver().openOutputStream(dest);
                inputStream = context.getContentResolver().openInputStream(source);
                String str = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                        .collect(Collectors.joining("\n"));
                outputStream.write(str.getBytes());
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                return true;

            } catch (IOException e) {
                ezLog.logException(e.getMessage() , e);
                return false;
            }
        } else {

            InputStream stream;
            OutputStream outStream;
            try {
                stream = context.getContentResolver().openInputStream(source);
                outStream = context.getContentResolver().openOutputStream(dest);

                PDDocument org = PDDocument.load(stream);
                PDDocument out = new PDDocument();
                for (PDPage page : org.getDocumentCatalog().getPages()) {
                    out.addPage(page);
                }

                out.save(outStream);
                org.close();
                out.close();
                return true;
            } catch (IOException e) {
                ezLog.logException(e.getMessage() , e);
                return false;
            }
        }
    }

}
