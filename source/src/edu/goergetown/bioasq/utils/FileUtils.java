package edu.goergetown.bioasq.utils;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by yektaie on 2/26/17.
 */
public class FileUtils {
    public static boolean exists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void createFile(String path) {
        File file = new File(path);
        try {
            file.createNewFile();
        } catch (IOException e) {
//            Logger.error(e, "Unable to create file at \"%s\"", path);
        }
    }

    public static void writeText(String path, String text) {
        File file = new File(path);
        try {
            FileOutputStream fs = new FileOutputStream(file);
            fs.write(text.getBytes("utf8"));
            fs.flush();
            fs.close();
        } catch (Exception e) {
//            Logger.error(e, "Unable to write file at path \"%s\".", path);
        }
    }

    public static void delete(String path) {
        File file = new File(path);
        file.delete();
    }

    public static String[] readAllLines(String path) {
        String[] lines = readTextFile(path).split("\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].replace("\r", "");
        }

        return lines;
    }

    public static String readTextFile(String path) {
        byte[] content = readBinaryFile(path);
        try {
            return new String(content, "UTF-8");
        } catch (Exception e) {
        }

        return null;
    }

    public static byte[] readBinaryFile(String path) {
        byte[] result = null;
        try {
            FileInputStream fs = new FileInputStream(new File(path));
            int length = fs.available();
            byte[] content = new byte[length];
            fs.read(content);

            result = content;

            fs.close();
        } catch (Exception e) {
            // e.printStackTrace();
        }

        return result;
    }

    public static void createDirectory(String path) {
        File file = new File(path);
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
//            Logger.error(e, "Unable to create folder at \"%s\"", path);
        }
    }

    public static ArrayList<String> getDirectories(String path) {
        File f = new File(path);
        ArrayList<String> result = new ArrayList<>();

        for (File file : f.listFiles()) {
            if (file.isDirectory()) {
                result.add(file.getAbsolutePath());
            }
        }

        return result;
    }

    public static ArrayList<String> getFiles(String path) {
        File f = new File(path);
        ArrayList<String> result = new ArrayList<>();

        for (File file : f.listFiles()) {
            if (file.isFile()) {
                result.add(file.getAbsolutePath());
            }
        }

        return result;
    }

    public static void writeBinary(String path, byte[] data) {
        File file = new File(path);
        try {
            FileOutputStream fs = new FileOutputStream(file);
            fs.write(data);
            fs.flush();
            fs.close();
        } catch (Exception e) {
//            Logger.error(e, "Unable to write file at path \"%s\".", path);
        }
    }

    public static byte[] readByteArray(FileInputStream inputFile, int length) throws IOException {
        byte[] data = new byte[length];
        inputFile.read(data);

        return data;
    }

    public static int readInteger(FileInputStream inputFile) throws IOException {
        byte[] data = new byte[4];
        inputFile.read(data);

        BinaryBuffer buffer = new BinaryBuffer(data);
        return buffer.readInt();
    }

    public static int getFileLength(String path) {
        int result = 0;
        try {
            FileInputStream fs = new FileInputStream(new File(path));
            result = fs.available();
            fs.close();
        } catch (Exception e) {
        }

        return result;
    }
}
