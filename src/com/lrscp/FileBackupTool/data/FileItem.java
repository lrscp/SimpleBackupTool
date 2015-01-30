package com.lrscp.FileBackupTool.data;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.lrscp.FileBackupTool.Util;

public class FileItem {
    public String path;

    public long lastModified;

    public FileItem() {}

    public FileItem(File file) {
        try {
            path = file.getCanonicalPath();
        } catch (Exception e) {}
    }

    /**
     * Get JSON String
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public String toJSONString() {
        JSONObject obj = new JSONObject();
        obj.put("path", path);
        obj.put("lastModified", lastModified + "");
        return obj.toJSONString();
    }

    /**
     * Load data from JSON String
     * 
     * @return
     */
    public void fromJSONString(String json) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jobj = (JSONObject) parser.parse(json);
            path = (String) jobj.get("path");
            lastModified = Long.valueOf((String) jobj.get("lastModified"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object obj) {
        FileItem fi = (FileItem) obj;
        return path.equals(fi.path);
    }

    public File getFile() {
        return new File(path);
    }

    public String getParentPath() {
        File f = new File(path).getParentFile();
        if (f != null) {
            try {
                return f.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isChildOf(File file) {
        if (file.isDirectory()) {
            try {
                return path.startsWith(Util.addSeparator(file.getCanonicalPath()));
            } catch (IOException e) {}
        }
        return false;
    }
}