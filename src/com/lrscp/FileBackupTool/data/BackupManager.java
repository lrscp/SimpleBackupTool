package com.lrscp.FileBackupTool.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lrscp.lib.Log;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.lrscp.FileBackupTool.Debug;
import com.lrscp.FileBackupTool.Util;
import com.lrscp.FileBackupTool.data.DirectoryDetail.DirectoryDetailListener;

public class BackupManager {
    private static final String TAG = BackupManager.class.getSimpleName();

    private static final String CONFIG = "backup_list";

    private static BackupManager sInstance;

    private List<FileItem> mBackupFiles = new ArrayList<FileItem>();

    private Map<String, Runnable> mCancelRunnable = new HashMap<String, Runnable>();

    private BackupManager() {
        load();
    }

    private void load() {
        try {
            String json = FileUtils.readFileToString(new File(CONFIG), "utf-8");
            JSONParser parser = new JSONParser();
            JSONArray jarr = (JSONArray) parser.parse(json);
            for (Object obj : jarr) {
                if (DirItem.isDirItemJSON((String) obj)) {
                    DirItem item = new DirItem();
                    item.fromJSONString((String) obj);
                    mBackupFiles.add(item);
                } else {
                    FileItem item = new FileItem();
                    item.fromJSONString((String) obj);
                    mBackupFiles.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "load exception: " + e.getLocalizedMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void save() {
        JSONArray arr = new JSONArray();
        for (FileItem fi : mBackupFiles) {
            arr.add(fi.toJSONString());
        }
        try {
            FileUtils.write(new File(CONFIG), arr.toJSONString(), "utf-8");
        } catch (IOException e) {
            Log.e(TAG, "save exception: " + e.getLocalizedMessage());
        }
    }

    public static interface BackupListener {
        void onProgress(BackupDetail detail);

        void onFinish();
    }

    public synchronized static BackupManager getInstance() {
        if (sInstance == null) {
            sInstance = new BackupManager();
        }
        return sInstance;
    }

    /**
     * Whether the file is in the directory in one of the backup directory.
     * 
     * @param file
     * @return
     */
    public boolean isInBackupDirectory(File file) {
        return getBackupItemByChild(file) != null;
    }

    /**
     * Whether the file is in blacklist in one of the backup directory.
     * 
     * @param file
     * @return
     */
    public boolean isIgnored(File file) {
        DirItem item = getBackupItemByChild(file);
        if (item != null) {
            return item.isIgnored(file);
        }
        return false;
    }

    /**
     * Whether any of the child is ignored.
     * 
     * @param file
     * @return
     */
    public boolean isChildInIgnoreList(File file) {
        if (!file.isDirectory()) {
            return false;
        }

        DirItem item = getBackupItemByChild(file);
        if (item != null) {
            for (String path : item.getIgnoreList()) {
                if (Util.isFileInDirectory(new File(path), file)) {
                    return true;
                }
            }
        } else {
            item = getBackupItem(file);
            if (item != null) {
                return item.getIgnoreList().size() > 0;
            }

            List<FileItem> items = getBackupItemsInDirectory(file);
            for (FileItem fi : items) {
                if (fi instanceof DirItem) {
                    DirItem di = (DirItem) fi;
                    if (di.getIgnoreList().size() > 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Whether this file is in the backup list.
     * 
     * @param currentFile
     * @return
     */
    public boolean isInBackupList(File file) {
        return mBackupFiles.contains(new FileItem(file));
    }

    /**
     * Add this file to the backup list.
     * 
     * @param file
     */
    public void addToBackupList(File file) {
        if (file.isDirectory()) {
            Iterator<FileItem> it = mBackupFiles.iterator();
            while (it.hasNext()) {
                FileItem item = it.next();
                if (item.isChildOf(file)) {
                    Log.i(TAG, "remove sub folder backup files " + item.path);
                    it.remove();
                }
            }
            mBackupFiles.add(new DirItem(file));
        } else {
            mBackupFiles.add(new FileItem(file));
        }
    }

    /**
     * Add this file to the backup list.
     * 
     * @param currentFile
     */
    public void addToBackupList(File[] currentFile) {
        for (File file : currentFile) {
            addToBackupList(file);
        }
    }

    /**
     * Remove this file from backup list.
     * 
     * @param file
     */
    public void removeFromBackupList(File file) {
        mBackupFiles.remove(new FileItem(file));
    }

    /**
     * Remove this file from backup list.
     * 
     * @param currentFile
     */
    public void removeFromBackupList(File[] currentFile) {
        for (File file : currentFile) {
            removeFromBackupList(file);
        }
    }

    /**
     * Add to the backup directory's ignore list.
     * 
     * @param file
     */
    public void ignore(File file) {
        DirItem backupItem = getBackupItemByChild(file);
        backupItem.addToBlackList(file);
    }

    /**
     * Add to the backup directory's ignore list.
     * 
     * @param currentFile
     */
    public void ignore(File[] files) {
        for (File file : files) {
            ignore(file);
        }
    }

    /**
     * Get backup item by child file.
     * 
     * @param file
     * @return
     */
    private DirItem getBackupItemByChild(File file) {
        try {
            String path = file.getCanonicalPath();
            for (FileItem fi : mBackupFiles) {
                if (fi.getFile().isDirectory()) {
                    if (path.startsWith(Util.addSeparator(fi.path)) && !path.equals(fi.path)) {
                        return (DirItem) fi;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get backup item by file.
     * 
     * @param f
     * @return
     */
    private DirItem getBackupItem(File f) {
        int index = mBackupFiles.indexOf(new FileItem(f));
        if (index == -1) {
            return null;
        }
        return (DirItem) mBackupFiles.get(index);
    }

    /**
     * Remove from the backup directory's ignore list.
     * 
     * @param file
     */
    public void removeIgnore(File file) {
        DirItem backupItem = getBackupItemByChild(file);
        if (backupItem != null) {
            backupItem.removeFromBlackList(file);
        }
    }

    /**
     * Remove from the backup directory's ignore list.
     * 
     * @param files
     */
    public void removeIgnore(File[] files) {
        for (File f : files) {
            removeIgnore(f);
        }
    }

    /**
     * Whether any child of this file is in backup list.
     * 
     * @param f
     * @return
     */
    public boolean isChildInBackupList(File file) {
        // must be a directory
        if (!file.isDirectory()) {
            return false;
        }

        try {
            String path = Util.addSeparator(file.getCanonicalPath());
            for (FileItem fi : mBackupFiles) {
                if (fi.path.startsWith(path) && !fi.path.equals(path)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Return backup items in the directory.
     * 
     * @param f
     * @return
     */
    private List<FileItem> getBackupItemsInDirectory(File file) {
        List<FileItem> items = new ArrayList<FileItem>();

        // must be a directory
        if (!file.isDirectory()) {
            return items;
        }

        try {
            String path = Util.addSeparator(file.getCanonicalPath());
            for (FileItem fi : mBackupFiles) {
                if (fi.path.startsWith(path) && !fi.path.equals(path)) {
                    items.add(fi);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;
    }

    /**
     * Whether the child of this file is in backup list.
     * 
     * @param currentFile
     * @return
     */
    public boolean isChildInBackupList(File[] files) {
        for (File file : files) {
            if (isChildInBackupList(file)) {
                return true;
            }
        }
        return false;
    }

    private class NotifyThread extends Thread {
        private boolean exit;
        private BackupListener mLsnr;
        private BackupDetail mDetail;

        public NotifyThread(BackupListener lsnr, BackupDetail detail) {
            mLsnr = lsnr;
            mDetail = detail;
        }

        public void exit() {
            exit = true;
        }

        @Override
        public void run() {
            while (!exit) {
                mLsnr.onProgress(mDetail);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class BackupThread extends Thread {
        private BackupListener mLsnr;
        private String mPath;
        private BackupDetail mDetail;
        private NotifyThread mNotifyThread;
        private String mBackupDirectory;

        /**
         * 
         * @param backupDirectory
         *            if empty will backup all else backup the backup list in
         *            current directory.
         * @param backupTo
         * @param lsnr
         */
        public BackupThread(String backupDirectory, String backupTo, BackupListener lsnr) {
            mBackupDirectory = backupDirectory;
            mPath = backupTo;
            mLsnr = lsnr;
        }

        @Override
        public void run() {
            try {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}

                File dest = new File(mPath);

                if (!dest.isDirectory()) {
                    Log.e(TAG, "backup failed, dest is not a directory");
                    return;
                }

                List<FileItem> backupFiles = mBackupFiles;
                if (!mBackupDirectory.isEmpty()) {
                    backupFiles = getBackupItemsInDirectory(new File(mBackupDirectory));
                }

                // Calculate Total Size
                BackupDetail detail = new BackupDetail();
                backupOrCal(backupFiles, mPath, detail, false);

                mDetail = new BackupDetail();
                mDetail.totalSize = detail.size;
                mNotifyThread = new NotifyThread(mLsnr, mDetail);
                mNotifyThread.start();
                backupOrCal(backupFiles, mPath, mDetail, true);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mNotifyThread != null) {
                    mNotifyThread.exit();
                }
                mLsnr.onFinish();
            }
        }
    }

    public static class BackupDetail extends DirectoryDetail {
        public String currentFile = "";

        public long totalSize = 0;
    }

    public void backupTo(String backupDirectory, String backupToPath, BackupListener lsnr) {
        new BackupThread(backupDirectory, backupToPath, lsnr).start();
    }

    public void backupOrCal(List<FileItem> backupFiles, String backupToPath, BackupDetail detail, boolean isBackup) throws IOException {
        for (FileItem item : backupFiles) {
            if (item instanceof DirItem) {
                backupDirectory(backupToPath, item.path, ((DirItem) item), detail, isBackup);
            } else {
                FileUtils.copyFileToDirectory(item.getFile(), new File(buildPath(backupToPath, item.getParentPath())), true);
                detail.fileCount++;
                detail.size += item.getFile().length();
                detail.currentFile = item.path;
            }
        }
    }

    /**
     * 
     * @param backupToPath
     * @param dir
     * @param dirItem
     * @param detail
     * @param isBackup
     * @throws IOException
     */
    private void backupDirectory(String backupToPath, String dir, DirItem dirItem, BackupDetail detail, boolean isBackup) throws IOException {
        File dirFile = new File(dir);
        if (dirFile.isDirectory()) {
            File[] files = dirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isHidden() && !dirItem.isIgnored(file)) {
                        if (file.isDirectory()) {
                            backupDirectory(backupToPath, file.getCanonicalPath(), dirItem, detail, isBackup);
                        } else {
                            if (isBackup) {
                                FileUtils.copyFileToDirectory(file, new File(buildPath(backupToPath, dir)), true);
                            }
                            detail.fileCount++;
                            detail.size += file.length();
                            detail.currentFile = Util.getCanonicalPath(file);
                        }
                    }
                }
            }
        }
    }

    private static String buildPath(String path1, String path2) {
        String result = Util.addSeparator(path1) + path2.replace(":", "");
        return result;
    }

    /**
     * Start get backup detail task.
     * 
     * @param f
     * @param directoryDetailListener
     */
    public synchronized void startGetDetailTask(final File f, final DirectoryDetailListener lsnr) {
        if (Debug.DEBUG_MEMORY) {
            Log.i(TAG, "mCancelRunnable size " + mCancelRunnable.size());
        }

        boolean isInBackupList = isInBackupList(f);
        final String key = Util.getCanonicalPath(f);

        // File?
        if (!f.isDirectory()) {
            lsnr.onGetDetail(new DirectoryDetail(1, f.length()));
            return;
        }

        if (isInBackupList || (isInBackupDirectory(f) && !isIgnored(f))) {
            DirItem item = null;
            if (isInBackupList) {
                // In backup list
                item = getBackupItem(f);
            } else {
                // In backup directory
                item = getBackupItemByChild(f);
            }
            DirectoryDetailManager.getInstance().startGetDetailTaskWithBlackList(f, new DirectoryDetailListener() {
                @Override
                public void onGetDetail(DirectoryDetail detail) {
                    lsnr.onGetDetail(detail);
                    // Remove cancel task when finish
                    mCancelRunnable.remove(key);
                }
            }, item.getIgnoreList());
            mCancelRunnable.put(key, new Runnable() {
                @Override
                public void run() {
                    DirectoryDetailManager.getInstance().stopGetDetailTask(f, true);
                }
            });
        } else if (isChildInBackupList(f)) {
            // Child is in backup list
            final MultiBackupItemDetailGetter getter = new MultiBackupItemDetailGetter();
            getter.backupItems = getBackupItemsInDirectory(f);
            getter.lsnr = new DirectoryDetailListener() {
                @Override
                public void onGetDetail(DirectoryDetail detail) {
                    lsnr.onGetDetail(detail);
                    // Remove cancel task when finish
                    mCancelRunnable.remove(key);
                }
            };
            getter.startGetDetail();
            mCancelRunnable.put(key, new Runnable() {
                @Override
                public void run() {
                    getter.cancelBackup();
                }
            });
        } else {
            return;
        }
    }

    private class MultiBackupItemDetailGetter {
        private List<FileItem> backupItems;
        private ArrayList<DirectoryDetailListener> lsnrs = new ArrayList<DirectoryDetail.DirectoryDetailListener>();;
        private DirectoryDetail result = new DirectoryDetail();
        private DirectoryDetailListener lsnr;

        @SuppressWarnings("unused")
        public void startGetDetail() {
            synchronized (lsnrs) {
                for (FileItem item : backupItems) {
                    lsnrs.add(new DirectoryDetailListener() {
                        @Override
                        public void onGetDetail(DirectoryDetail detail) {
                            synchronized (lsnrs) {
                                result.fileCount += detail.fileCount;
                                result.size += detail.size;
                                lsnrs.remove(this);
                                if (lsnrs.size() == 0) {
                                    lsnr.onGetDetail(result);
                                }
                            }
                        }
                    });
                }
                for (int i = 0; i < backupItems.size(); i++) {
                    FileItem item = backupItems.get(i);
                    DirectoryDetailListener tmpLsnr = lsnrs.get(i);
                    if (item instanceof DirItem) {
                        DirectoryDetailManager.getInstance().startGetDetailTaskWithBlackList(item.getFile(), tmpLsnr,
                                ((DirItem) item).getIgnoreList());
                    } else {
                        tmpLsnr.onGetDetail(new DirectoryDetail(1, item.getFile().length()));
                    }
                }
            }
        }

        public void cancelBackup() {
            for (int i = 0; i < backupItems.size(); i++) {
                FileItem item = backupItems.get(i);
                if (item instanceof DirItem) {
                    DirectoryDetailManager.getInstance().stopGetDetailTask(item.getFile(), true);
                }
            }
            backupItems.clear();
            lsnrs.clear();
            lsnr = null;
        }
    }

    /**
     * Stop get backup detail task.
     * 
     * @param file
     */
    public synchronized void stopGetDetailTask(File file) {
        String key = Util.getCanonicalPath(file);
        if (mCancelRunnable.containsKey(key)) {
            mCancelRunnable.get(key).run();
            mCancelRunnable.remove(key);
        }
    }

    /**
     * Get lost backup drivers.
     * 
     * @return
     */
    public List<String> getLostBackupDrivers() {
        ArrayList<String> lostDrivers = new ArrayList<String>();
        HashSet<File> drivers = new HashSet<File>();
        for (FileItem item : mBackupFiles) {
            drivers.add(Util.getDriverFile(item.getFile()));
        }
        for(File driver : drivers){
            if(!driver.exists()){
                lostDrivers.add(driver.getAbsolutePath());
            }
        }
        return lostDrivers;
    }
}
