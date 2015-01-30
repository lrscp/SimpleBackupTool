package com.lrscp.FileBackupTool.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lrscp.lib.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.lrscp.FileBackupTool.Util;

public class DirItem extends FileItem {
    private static final String TAG = DirItem.class.getSimpleName();

    /** Black List that no need to backup **/
    private List<String> mIgnoreList = new ArrayList<String>();

    public DirItem(File file) {
        super(file);
    }

    public DirItem() {
        super();
    }

    /**
     * Get JSON String
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public String toJSONString() {
        JSONObject obj = new JSONObject();
        obj.put("path", path);
        obj.put("lastModified", lastModified + "");
        JSONArray arr = new JSONArray();
        for (String s : mIgnoreList) {
            arr.add(s);
        }
        obj.put("mIgnoreList", arr.toJSONString());
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
            JSONArray jarr = (JSONArray) parser.parse((String) jobj.get("mIgnoreList"));
            for (Object obj : jarr) {
                mIgnoreList.add((String) obj);
            }
        } catch (ParseException e) {
            Log.e(TAG, "save exception: " + e.getLocalizedMessage());
        }
    }

    public static boolean isDirItemJSON(String json) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jobj = (JSONObject) parser.parse(json);
            return jobj.containsKey("mIgnoreList");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void removeFromBlackList(File file) {
        try {
            mIgnoreList.remove(file.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addToBlackList(File file) {
        removeIgnoreItemsInDirectory(file);
        try {
            mIgnoreList.add(file.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeIgnoreItemsInDirectory(File file) {
        Iterator<String> it = mIgnoreList.iterator();
        while (it.hasNext()) {
            String path = it.next();
            if (Util.isFileInDirectory(new File(path), file)) {
                it.remove();
            }
        }
    }

    /**
     * Whether the file is the ignored one or in the ignored directory.
     * 
     * @param file
     * @return
     */
    public boolean isIgnored(File file) {
        try {
            String path = file.getCanonicalPath();
            for (String s : mIgnoreList) {
                File f = new File(s);
                // Equal?
                if (path.equals(s)) {
                    return true;
                }
                // In The Ignored Directory?
                if (f.isDirectory()) {
                    if (path.startsWith(Util.addSeparator(s))) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getIgnoreList() {
        return mIgnoreList;
    }
}