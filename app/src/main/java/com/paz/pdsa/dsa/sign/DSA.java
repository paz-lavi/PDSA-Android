package com.paz.pdsa.dsa.sign;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.firebase.auth.FirebaseAuth;
import com.paz.logger.EZLog;
import com.paz.pdsa.FileMimeException;
import com.paz.pdsa.dsa.ras.KeyPair;
import com.paz.pdsa.dsa.ras.RSA;
import com.paz.pdsa.utils.Constants;
import com.paz.pdsa.utils.Files;
import com.paz.prefy_lib.Prefy;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Setter
public class DSA {
    private final String TAG = "DSA";
    private Context context;
    private final EZLog ezLog = EZLog.getInstance();


    /**
     * create a signed file. write the signature to file with invisible characters
     *
     * @param fileToSign      the original file path
     * @param encryptedString string with zero width signature
     */

    public String createFileWithSignature(Uri fileToSign, String encryptedString , String outputName) throws IOException {
        ezLog.debug( "createFileWithSignature: uri = " + fileToSign.toString());

        String mime = context.getContentResolver().getType(fileToSign);
        Log.d(TAG + "P", "createFileWithSignature: mime = " + MimeTypeMap.getSingleton().getExtensionFromMimeType(mime));
        String ext = "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        File file = new File(fileToSign.toString());

        String dir = file.getName();
        Log.d(TAG + "P", "createFileWithSignature: getName = " + dir);
        String[] path = URLDecoder.decode(file.getPath(), "UTF-8").split("/");
        Log.d(TAG + "P", "createFileWithSignature: file.getPath()= " + file.getPath());
        Log.d(TAG + "P", "createFileWithSignature: path " + path[path.length - 1]);
        String p = (outputName.isEmpty() ? path[path.length - 1] : outputName).replace(" ", "_");
        if (p.startsWith("primary:"))
            p = p.substring("primary:".length());
        if (!p.endsWith(ext)) {
            p = p + ext;
        }
        String filename = URLDecoder.decode(p, "UTF-8").replace(" ", "_");
        Log.d(TAG + "P", "createFileWithSignature: p " + filename);


        // String outputFilePath = dir + "/sign_" + filename;
        String outputFilePath = "signed_" + filename;

        //File output = new File(context.getExternalCacheDir(), URLEncoder.encode(outputFilePath, "UTF-8"));
        File output = new File(context.getExternalFilesDir(null), outputFilePath);

        String signature = "Singed with P-DSA!" + encryptedString + "! Use our app to validate the signature\n";

        String originalData = Files.readFile(context, fileToSign);
        if (!mime.equalsIgnoreCase(Constants.PDF_MIME)) {
            Files.writeToFile(signature, output.getPath());
            Files.appendStrToFile(output.getAbsolutePath(), originalData);
        } else {
            Files.writePdfFile(context, signature, output.getAbsolutePath(), fileToSign);
        }
        return output.getAbsolutePath();
    }

    /**
     * sing on file. calculate the signature, encrypt it to non visible characters and write it to a new file
     */
    public String signFile(Uri uri , String outputName) throws IOException {
        String mime = context.getContentResolver().getType(uri);
        if (!Arrays.asList(Constants.MIME_TYPES).contains(mime))
            throw new FileMimeException("Not a valid file / Can't open this file path");

        long hash;

        String data = Files.readFile(context, uri);
        hash = new Hash().foldFile(data);
        ezLog.debug( "signFile: hash = " + hash);
        KeyPair keyPair = getUserKeyPair();
        long e = new RSA().encrypt(hash, keyPair.getPrivateKey(), keyPair.getKeyLength());
        ezLog.debug( "signFile: encrypt = " + e);

        String encode = encodeZeroWidth(e, keyPair.getKeyLength(), keyPair.getPublicKey());
        return createFileWithSignature(uri, encode,outputName);


    }




    private KeyPair getUserKeyPair() {
        Prefy prefy = Prefy.getInstance();
        long e = prefy.getLong(Constants.PRIVATE_KEY, -1);
        long d = prefy.getLong(Constants.PUBLIC_KEY, -1);
        long n = prefy.getLong(Constants.KEY_LENGTH, -1);
        return new KeyPair(e, d, n);

    }


    /**
     * encode the signature to a non visible characters
     */
    public String encodeZeroWidth(long signature, long keyLength, long publicKey) {

        String encode = "signature:" + signature + "$keyLength:" + keyLength + "$publicKey:" + publicKey + "$timestamp:" + System.currentTimeMillis() +
                "$user:" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "$";
        Converters converters = new Converters();
        String bin = converters.stringToBin(encode);
        return converters.binToZeroWidth(bin);
    }

    /**
     * validate signed file. read encrypted signature, decode it and validate file
     */
    public ValidateResult validateSignedFile(Uri uri) throws IOException {
        String mime = context.getContentResolver().getType(uri);
        if (!Arrays.asList(Constants.MIME_TYPES).contains(mime))
            throw new FileMimeException("Not a valid file / Can't open this file path");


        String data = Files.readFile(context, uri);
        String[] dataArray = data.split("\n");

        List<String> lines = new ArrayList<>(Arrays.asList(dataArray));
        String encodedSignature = context.getContentResolver().getType(uri).equalsIgnoreCase(Constants.PDF_MIME) ? lines.get(lines.size() - 1) : lines.get(0);
        if (context.getContentResolver().getType(uri).equalsIgnoreCase(Constants.PDF_MIME)) {
            ezLog.debug( "validateSignedFile: last line " + lines.get(lines.size() - 1));
            lines.remove(lines.size() - 1);
            //TODO check here
            String l = lines.get(lines.size() - 1) + "\n";
            lines.set(lines.size() - 1, l);
        } else {
            lines.remove(0);
        }
        String originalDoc = String.join("\n", lines);
        SignatureData signatureData = decodeSignature(encodedSignature);
        long hash = new Hash().foldFile(originalDoc);
        ezLog.debug( "validateSignedFile: hash" + hash);
        return new ValidateResult(new RSA().isValidSignature(hash, signatureData), signatureData);

    }

    /**
     * decode encoded signature
     *
     * @param encoded the encoded signature string
     */
    private SignatureData decodeSignature(String encoded) {
        String bin, plain;
        Converters converters = new Converters();
        String zeroWidth = encoded.split("!")[1];
        ezLog.debug( "decodeSignature: zero = " + zeroWidth);
        bin = converters.zeroWidthToBin(zeroWidth);
        plain = converters.binToString(bin);

        return parseSignature(plain);


    }

    /**
     * parse signature content
     */
    private SignatureData parseSignature(String decoded) {
        ezLog.debug( "parseSignature: decoded = " + decoded);
        SignatureData signatureData = new SignatureData();
        String[] data = decoded.split("\\$");
        signatureData.setSignature(Long.parseLong(data[0].substring("signature:".length())));
        signatureData.setKeyLength(Long.parseLong(data[1].substring("keyLength:".length())));
        signatureData.setPublicKey(Long.parseLong(data[2].substring("publicKey:".length())));
        signatureData.setTimestamp(Long.parseLong(data[3].substring("timestamp:".length())));
        signatureData.setUserId(data[4].substring("user:".length()));

        ezLog.debug( "parseSignature: " + signatureData.toString());
        return signatureData;
    }


}