package com.lrscp.FileBackupTool.test;

import java.util.HashMap;

import lrscp.lib.Log;

public class LRUCache {
    private static final String TAG = LRUCache.class.getSimpleName();
    
    HashMap<String, Object> map = new HashMap<String, Object>();

    public LRUCache(int max) {
        // TODO Auto-generated constructor stub
    }

    public int cacheSize() {
        return map.size();
    }

    public void put(String key, Object value) {
        map.put(key, value);
        Log.i(TAG, "path=" + key);
    }

    public Object get(String key) {
        return map.get(key);
    }

}
