package com.spisoft.sync.utils;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by alexandre on 16/03/17.
 */

public class FileUtils {

    public static void copy(RandomAccessFile inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, len);

        }
        inputStream.close();
        outputStream.close();

    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, len);

        }
        inputStream.close();
        outputStream.close();

    }
    public static String getName(Uri uri){
        if(uri!=null) {
            String name = uri.getLastPathSegment();
            if (name == null || name.isEmpty()) {
                if (uri.toString().lastIndexOf("/") >= 0 && uri.toString().lastIndexOf("/") < (uri.toString().length() - 1))
                    name = uri.toString().substring(uri.toString().lastIndexOf("/") + 1);
                else
                    name = uri.toString();

            }
            if(name!=null&&"content".equals(uri.getScheme())){
                String[] parts = name.split(":");
                name = parts[parts.length-1];

            }
            return name;
        }
        return null;
    }

    public static String getFileNameWithoutExtension(Uri uri){
        String name = getName(uri);
        return stripExtensionFromName(name);
    }

    public static String stripExtensionFromName(String name){
        if (name != null) {
            int dotPos = name.lastIndexOf('.');
            if (dotPos > 0) {
                name = name.substring(0, dotPos);
            }
        }
        return name;
    }


    public static  String getExtension(String filename) {
        if (filename == null)
            return null;
        int dotPos = filename.lastIndexOf('.');
        if (dotPos >= 0 && dotPos < filename.length()) {
            return filename.substring(dotPos + 1).toLowerCase();
        }
        return null;
    }

    public static String md5(String absolutePath) {
        try {
            return getFileChecksum(MessageDigest.getInstance("MD5"), new File(absolutePath));
        } catch (Exception e) {
            throw new IllegalStateException(absolutePath+": no md5 or file not found (exists ?) "+new File(absolutePath).exists());
        }
    }


    public static String readInputStream(InputStream s) {
        BufferedReader br = null;
        InputStreamReader fr = null;
        StringBuilder sb = new StringBuilder();
        try {
            fr = new InputStreamReader(s);
            br = new BufferedReader(fr);

            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null) {
                sb.append(sCurrentLine+"\n");
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (br != null)
                    br.close();

                if (fr != null)
                    fr.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
        return sb.toString();
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }

    public static boolean isAudioFile(String media) {
        media = media.toLowerCase();
        return media.endsWith(".opus") || media.endsWith(".ogg") || media.endsWith(".mp3") || media.endsWith(".flac") || media.endsWith(".wav");
    }
}
