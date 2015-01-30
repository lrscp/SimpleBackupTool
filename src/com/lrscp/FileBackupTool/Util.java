package com.lrscp.FileBackupTool;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static boolean isEmptyDirectory(String dir) {
        File file = new File(dir);
        return file.isDirectory() && file.listFiles().length == 0;
    }

    public static boolean isDriverPath(String path) {
        return Pattern.matches("\\w:\\\\", path);
    }

    public static boolean isDriverPath(File file) {
        return Pattern.matches("\\w:\\\\", getCanonicalPath(file));
    }

    public static String addSeparator(String path) {
        if (path.endsWith(File.separator)) {
            return path;
        }
        return path + File.separator;
    }

    public static String getCanonicalPath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {}
        return null;
    }

    public static boolean isFileInDirectory(File file, File directory) {
        String pathFile = getCanonicalPath(file);
        String pathDir = addSeparator(getCanonicalPath(directory));
        return pathFile.startsWith(pathDir) && !pathFile.equals(pathDir);
    }

    public static File getDriverFile(File file) {
        String path = getCanonicalPath(file);
        Matcher m = Pattern.compile("(\\w:\\\\).*").matcher(path);
        if (m.find()) {
            return new File(m.group(1));
        }
        return null;
    }
}
