package com.lrscp.FileBackupTool.data;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DirectoryDetail {
    // Directory Size
    public long size = 0;

    // Directory files count
    public int fileCount = 0;

    public static interface DirectoryDetailListener {
        void onGetDetail(DirectoryDetail detail);
    }

    // Last cache time
    // public long lastCacheTime = 0;

    public DirectoryDetail() {}

    public DirectoryDetail(int count, long length) {
        fileCount = count;
        size = length;
    }

    @SuppressWarnings("unchecked")
    public String toJSONString() {
        JSONObject jobj = new JSONObject();
        jobj.put("size", size + "");
        jobj.put("fileCount", fileCount + "");
        // jobj.put("lastCacheTime", lastCacheTime + "");
        return jobj.toJSONString();
    }

    public static DirectoryDetail fromJSONString(String json) {
        DirectoryDetail detail = new DirectoryDetail();
        JSONParser parser = new JSONParser();
        JSONObject jobj;
        try {
            jobj = (JSONObject) parser.parse(json);
            detail.fileCount = Integer.valueOf((String) jobj.get("fileCount"));
            detail.size = Long.valueOf((String) jobj.get("size"));
            // detail.lastCacheTime = Long.valueOf((String)
            // jobj.get("lastCacheTime"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return detail;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" fileCount=" + fileCount);
        sb.append(" size=" + size);
        return sb.toString();
    }

    public DirectoryDetail copy() {
        return new DirectoryDetail(fileCount, size);
    }
    
}