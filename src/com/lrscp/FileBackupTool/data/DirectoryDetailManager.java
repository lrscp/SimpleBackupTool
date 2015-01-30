package com.lrscp.FileBackupTool.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import lrscp.lib.Log;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.lrscp.FileBackupTool.Debug;
import com.lrscp.FileBackupTool.Util;
import com.lrscp.FileBackupTool.data.DirectoryDetail.DirectoryDetailListener;
import com.lrscp.FileBackupTool.third.LRUCache;

public class DirectoryDetailManager {
    private static final String TAG = DirectoryDetailManager.class.getSimpleName();

    private static final int CACHE_LEVEL = 3;

    private LRUCache mDetails = new LRUCache(2000);

    private HashMap<String, GetDirectoryDetailTask> mTasks = new HashMap<String, DirectoryDetailManager.GetDirectoryDetailTask>();

    private HashMap<String, GetDirectoryDetailTask> mRunningTasks = new HashMap<String, DirectoryDetailManager.GetDirectoryDetailTask>();

    private static final DirectoryDetail EMPTY_DETAIL = new DirectoryDetail();

    private TaskRunner[] mThreads;

    private static final int THREADS = 1;

    private static DirectoryDetailManager sInstance;

    private DirectoryDetailManager() {
        loadCache();
        mThreads = new TaskRunner[THREADS];
        for (int i = 0; i < THREADS; i++) {
            mThreads[i] = new TaskRunner();
            mThreads[i].start();
        }
    }

    private void loadCache() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject jobj = (JSONObject) parser.parse(FileUtils.readFileToString(new File("cache"), "utf-8"));
            for (Object key : jobj.keySet()) {
                mDetails.put(key, DirectoryDetail.fromJSONString((String) jobj.get(key)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void saveCache() {
        JSONObject jobj = new JSONObject();
        for (Object key : mDetails.getMap().keySet()) {
            DirectoryDetail detail = (DirectoryDetail) mDetails.get(key);
            jobj.put(key, detail.toJSONString());
        }
        try {
            FileUtils.write(new File("cache"), jobj.toJSONString(), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static DirectoryDetailManager getInstance() {
        if (sInstance == null) {
            sInstance = new DirectoryDetailManager();
        }
        return sInstance;
    }

    /**
     * Start get detail task without blacklist.
     * 
     * @param f
     * @param lsnr
     */
    public void startGetDetailTask(File f, DirectoryDetailListener lsnr) {
        startGetDetailTaskWithBlackList(f, lsnr, null);
    }

    /**
     * Start get detail task.
     * 
     * @param f
     * @param lsnr
     * @param blackList
     */
    public synchronized void startGetDetailTaskWithBlackList(File f, DirectoryDetailListener lsnr, List<String> blackList) {
        try {
            if (Debug.DEBUG_MEMORY) {
                Log.d(TAG, "getDetail mDetails count " + mDetails.cacheSize());
                Log.d(TAG, "getDetail mTasks count " + mTasks.size());
                Log.d(TAG, "getDetail mRunningTasks count " + mRunningTasks.size());
            }

            // Start Task
            String path = f.getCanonicalPath();

            // Debug
            if (Debug.DEBUG_DIRECTORY_DETAIL_MANAGER) {
                Log.d(TAG, "startGetDetailTask " + path);
            }

            DirectoryDetail detail = (DirectoryDetail) mDetails.get(path);
            if (detail != null && blackList == null) {
                lsnr.onGetDetail(detail);
            } else {
                mTasks.put(getTaskKey(path, blackList != null), new GetDirectoryDetailTask(f, lsnr, blackList));
                notifyStart();
            }
        } catch (IOException e) {
            Log.e(TAG, "getSize exception: " + e.getLocalizedMessage());
        }
    }

    /**
     * Get task key.
     * 
     * @param path
     * @param hasBlackList
     * @return
     */
    private String getTaskKey(String path, boolean hasBlackList) {
        String hasBlackListKey = hasBlackList ? "Y" : "N";
        return path + hasBlackListKey;
    }

    /**
     * Stop get detail task that have not blacklist.
     * 
     * @param file
     */
    public void stopGetDetailTask(File file) {
        stopGetDetailTask(file, false);
    }

    /**
     * Stop get detail task.
     * 
     * @param file
     * @param hasBlackList
     */
    public synchronized void stopGetDetailTask(File file, boolean hasBlackList) {
        try {
            String path = file.getCanonicalPath();

            // Debug
            if (Debug.DEBUG_DIRECTORY_DETAIL_MANAGER) {
                Log.d(TAG, "stopTask " + path);
            }

            GetDirectoryDetailTask task = mTasks.remove(getTaskKey(path, hasBlackList));
            if (task != null) {
                task.cancel();
            }
            task = mRunningTasks.remove(getTaskKey(path, hasBlackList));
            if (task != null) {
                task.cancel();
            }
        } catch (IOException e) {
            Log.e(TAG, "removeLsnr exception: " + e.getLocalizedMessage());
        }
    }

    private class GetDirectoryDetailTask implements Runnable {
        private File mDirectory;
        private DirectoryDetailListener mLsnr;
        private boolean isCanceled;
        private List<String> mBlackList;

        public GetDirectoryDetailTask(File f, DirectoryDetailListener lsnr, List<String> blackList) {
            mDirectory = f;
            mLsnr = lsnr;
            mBlackList = blackList;
        }

        public String getPath() {
            try {
                return mDirectory.getCanonicalPath();
            } catch (IOException e) {
                Log.e(TAG, "GetDirectoryDetailTask getPath exception: " + e.getLocalizedMessage());
            }
            return "";
        }

        public void cancel() {
            isCanceled = true;
        }

        @Override
        public void run() {
            DirectoryDetail mDetail = getDetail(mDirectory, 0);
            handleBlackList(mDetail);
            if (!isCanceled) {
                mLsnr.onGetDetail(mDetail);
            }
        }

        private void handleBlackList(DirectoryDetail detail) {
            if (mBlackList == null) {
                return;
            }

            for (String path : mBlackList) {
                File f = new File(path);
                if (!Util.isFileInDirectory(f, mDirectory)) {
                    continue;
                }
                if (f.isDirectory()) {
                    DirectoryDetail tmpDetail = getDetail(f, 0);
                    Log.i(TAG, "tmp=" + tmpDetail);
                    detail.fileCount -= tmpDetail.fileCount;
                    detail.size -= tmpDetail.size;
                } else {
                    detail.fileCount -= 1;
                    detail.size -= f.length();
                }
            }
        }

        public DirectoryDetail getDetail(File directory, int recurseLevel) {
            DirectoryDetail detail = new DirectoryDetail();

            // Check Argument
            if (!directory.isDirectory()) {
                throw new IllegalArgumentException("!directory.isDirectory() " + directory.getAbsolutePath());
            }

            // Check hidden
            if (directory.isHidden() && !Util.isDriverPath(directory)) {
                return detail;
            }

            // Check exit
            if (isCanceled) {
                return detail;
            }

            try {
                String path = directory.getCanonicalPath();
                DirectoryDetail d1 = (DirectoryDetail) mDetails.get(path);
                if (d1 != null) {
                    // must return a copy!!
                    return d1.copy();
                }
                if (directory.listFiles() == null) {
                    if (recurseLevel < CACHE_LEVEL) {
                        mDetails.put(path, EMPTY_DETAIL);
                    }
                } else {
                    for (File f : directory.listFiles()) {
                        // Check exit
                        if (isCanceled) {
                            return detail;
                        }

                        if (f.isHidden() && !Util.isDriverPath(f)) {
                            continue;
                        }

                        if (f.isDirectory()) {
                            String tmpPath = f.getCanonicalPath();
                            DirectoryDetail tmpDetail = null;

                            tmpDetail = (DirectoryDetail) mDetails.get(tmpPath);
                            if (tmpDetail == null) {
                                // Get from file system
                                tmpDetail = getDetail(f, recurseLevel + 1);
                            }

                            // Calculate
                            detail.fileCount += tmpDetail.fileCount;
                            detail.size += tmpDetail.size;
                        } else if (f.isFile()) {
                            detail.fileCount++;
                            detail.size += f.length();
                        }
                    }
                    if (recurseLevel < CACHE_LEVEL) {
                        mDetails.put(path, detail.copy());
                    }
                    if (Debug.DEBUG_MEMORY) {
                        Log.d(TAG, "getDetail mDetails count " + mDetails.cacheSize());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "GetDirectoryDetailTask exception: " + e.getLocalizedMessage());
            }

            return detail;
        }
    }

    private void notifyStart() {
        for (int i = 0; i < THREADS; i++) {
            mThreads[i].interrupt();
        }
    }

    public synchronized GetDirectoryDetailTask consumeTask() {
        String key = null;
        GetDirectoryDetailTask task = null;

        Iterator<String> it = mTasks.keySet().iterator();
        if (it.hasNext()) {
            key = it.next();
        }
        if (key != null) {
            if (Debug.DEBUG_DIRECTORY_DETAIL_MANAGER) {
                Log.d(TAG, "consumeTask " + key);
            }
            task = mTasks.remove(key);
            mRunningTasks.put(key, task);
            return task;
        }
        return null;
    }

    public synchronized void finishTask(GetDirectoryDetailTask task) {
        if (Debug.DEBUG_DIRECTORY_DETAIL_MANAGER) {
            Log.d(TAG, "finishTask " + task.getPath());
        }
        mRunningTasks.remove(task.getPath());
    }

    private class TaskRunner extends Thread {
        @Override
        public void run() {
            while (true) {
                GetDirectoryDetailTask task = consumeTask();
                if (task != null) {
                    task.run();
                    finishTask(task);
                } else {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {}
                }
            }
        }
    }

    public void reload(File file) {
        File driverFile = Util.getDriverFile(file);
        ArrayList<String> removed = new ArrayList<String>();
        for (Object path : mDetails.getMap().keySet()) {
            if (Util.isFileInDirectory(new File((String) path), driverFile)) {
                removed.add((String) path);
            }
        }
        for (String path : removed) {
            mDetails.remove(path);
        }
    }

    public void reload() {
        mDetails.cleanupAll();
    }

}
