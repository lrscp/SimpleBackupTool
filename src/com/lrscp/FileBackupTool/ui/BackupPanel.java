package com.lrscp.FileBackupTool.ui;

import images.ImageResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import lrscp.lib.Log;
import lrscp.lib.Preference;
import lrscp.lib.oscmd.OsCmd;
import lrscp.lib.swt.SwtUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.lrscp.FileBackupTool.Const;
import com.lrscp.FileBackupTool.Util;
import com.lrscp.FileBackupTool.data.BackupManager;
import com.lrscp.FileBackupTool.data.BackupManager.BackupDetail;
import com.lrscp.FileBackupTool.data.BackupManager.BackupListener;
import com.lrscp.FileBackupTool.data.DirectoryDetail;
import com.lrscp.FileBackupTool.data.DirectoryDetail.DirectoryDetailListener;
import com.lrscp.FileBackupTool.data.DirectoryDetailManager;
import com.lrscp.FileBackupTool.dialog.BackupDialog;

public class BackupPanel extends Composite {
    protected static final String TAG = BackupPanel.class.getSimpleName();

    // Colors
    private static final int COLOR_IN_BACKUP_LIST = 0x00ff0000;
    private static final int COLOR_CHILD_IN_BACKUP_LIST = 0xff00aa00;
    private static final int COLOR_NOT_IN_BACKUP_LIST = 0xff000000;

    private static final int SORT_BACKUP_SIZE = 1;
    private static final int SORT_SIZE = 2;
    private static final int SORT_NONE = 3;

    private BackupManager mBackupManager = BackupManager.getInstance();

    private Table table;

    private String mPath = "";

    private File[] mFiles;

    private Label lblState;

    private boolean isFileSystemView = true;

    private HashMap<File, DirectoryDetail> mDirectoryDetailCache = new HashMap<File, DirectoryDetail>();

    private HashMap<File, DirectoryDetail> mBackupDetailCache = new HashMap<File, DirectoryDetail>();

    private boolean mHasPendingRefresh;

    private TableColumn tblclmnSize;

    private TableColumn tblclmnBackupSize;

    private int mCurrentSort = SORT_NONE;

    /**
     * Create the composite.
     * 
     * @param parent
     * @param style
     */
    public BackupPanel(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(1, false));

        lblState = new Label(this, SWT.NONE);
        lblState.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblState.setText("State:");
        updateStateText();

        table = new Table(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (table.getSelectionCount() > 0) {
                    File f = (File) table.getSelection()[0].getData();
                    if (f.isDirectory()) {
                        try {
                            changePath(f.getCanonicalPath());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Log.i(TAG, "table selection " + table.getSelectionCount());
                if (table.getSelectionCount() > 0) {
                    File[] files = new File[table.getSelectionCount()];
                    for (int i = 0; i < files.length; i++) {
                        files[i] = (File) table.getSelection()[i].getData();
                    }
                    table.setMenu(createPopMenu(files));
                } else {
                    table.getMenu().dispose();
                }
            }
        });

        TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
        tblclmnNewColumn.setWidth(397);
        tblclmnNewColumn.setText("File Name");

        TableColumn tblclmnLastModified = new TableColumn(table, SWT.NONE);
        tblclmnLastModified.setWidth(156);
        tblclmnLastModified.setText("Last Modified");

        tblclmnBackupSize = new TableColumn(table, SWT.NONE);
        tblclmnBackupSize.setWidth(100);
        tblclmnBackupSize.setText("Backup Size");
        tblclmnBackupSize.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mCurrentSort == SORT_BACKUP_SIZE) {
                    setSort(SORT_NONE);
                } else {
                    setSort(SORT_BACKUP_SIZE);
                }
            }
        });

        TableColumn tblclmnBackupCount = new TableColumn(table, SWT.NONE);
        tblclmnBackupCount.setWidth(113);
        tblclmnBackupCount.setText("Backup Count");

        tblclmnSize = new TableColumn(table, SWT.NONE);
        tblclmnSize.setWidth(86);
        tblclmnSize.setText("Size");
        tblclmnSize.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mCurrentSort == SORT_SIZE) {
                    setSort(SORT_NONE);
                } else {
                    setSort(SORT_SIZE);
                }
            }
        });

        TableColumn tblclmnFileCount = new TableColumn(table, SWT.NONE);
        tblclmnFileCount.setWidth(100);
        tblclmnFileCount.setText("File Count");

        TableColumn tblclmnHasIgnoreItems = new TableColumn(table, SWT.NONE);
        tblclmnHasIgnoreItems.setWidth(132);
        tblclmnHasIgnoreItems.setText("Backup Percent");

        refreshTable(true);
    }

    protected void setSort(int sort) {
        mCurrentSort = sort;
        switch (sort) {
            case SORT_NONE:
                table.setSortColumn(null);
                break;
            case SORT_SIZE:
                table.setSortColumn(tblclmnSize);
                table.setSortDirection(SWT.DOWN);
                break;
            case SORT_BACKUP_SIZE:
                table.setSortColumn(tblclmnBackupSize);
                table.setSortDirection(SWT.DOWN);
                break;
        }
        refreshTable(false);
    }

    protected void onPathChanged() {
        Log.i(TAG, "onPathChanged to " + mPath);
        for (TableItem ti : table.getItems()) {
            File file = (File) ti.getData();
            DirectoryDetailManager.getInstance().stopGetDetailTask(file);
            mBackupManager.stopGetDetailTask(file);
        }
        clearDetailCache();
    }

    private Menu createPopMenu(final File[] currentFile) {
        Menu menu = new Menu(getShell(), SWT.POP_UP);

        boolean isInBackupDirectory = mBackupManager.isInBackupDirectory(currentFile[0]);

        if (isInBackupDirectory) {
            // In the directory that is marked as backup
            boolean isInBlackList = mBackupManager.isIgnored(currentFile[0]);

            // Current selection are all ignored?
            for (int i = 1; i < currentFile.length; i++) {
                if (isInBlackList != mBackupManager.isIgnored(currentFile[i])) {
                    createWarnSelectionDiffDialog();
                    return menu;
                }
            }

            if (isInBlackList) {
                MenuItem menuItem = new MenuItem(menu, 0);
                menuItem.setText("Add To Backup List");
                menuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleRemoveFromIgnoreList(currentFile);
                    }
                });
            } else {
                MenuItem menuItem = new MenuItem(menu, 0);
                menuItem.setText("Ignore Backup");
                menuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleIgnoreBackup(currentFile);
                    }
                });
            }
        } else {
            // In the directory that is not marked as backup
            boolean isInBackupList = mBackupManager.isInBackupList(currentFile[0]);

            // Are all the selections in the backup list?
            for (int i = 1; i < currentFile.length; i++) {
                if (isInBackupList != mBackupManager.isInBackupList(currentFile[i])) {
                    createWarnSelectionDiffDialog();
                    return menu;
                }
            }

            if (isInBackupList) {
                boolean isDirectory = currentFile[0].isDirectory();
                boolean mutiSelection = currentFile.length > 1;

                if (isDirectory || mutiSelection) {
                    MenuItem menuItem = new MenuItem(menu, 0);
                    menuItem.setText("Cancel Backup Directory");
                    menuItem.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            handleCancelBackupDirectory(currentFile);
                        }
                    });
                } else {
                    MenuItem menuItem = new MenuItem(menu, 0);
                    menuItem.setText("Cancel Backup");
                    menuItem.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            handleCancelBackupFile(currentFile);
                        }
                    });
                }
            } else {
                MenuItem menuItem = new MenuItem(menu, 0);
                menuItem.setText("Add To Backup List");
                menuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        handleAddToBackupList(currentFile);
                    }
                });
            }
        }

        new MenuItem(menu, SWT.SEPARATOR);

        MenuItem menuItem = new MenuItem(menu, 0);
        menuItem.setText("Open File Explorer");
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openWindowsFileExplorer(currentFile[0].getAbsolutePath(), true);
            }
        });
        return menu;
    }

    void openWindowsFileExplorer(String path, boolean select) {
        if (select) {
            OsCmd.exec("Explorer /select," + path);
        } else {
            OsCmd.exec("Explorer " + path);
        }
    }

    private void createWarnSelectionDiffDialog() {
        SwtUtils.message(getShell(), "Waring", "No menu to show when files state not the same!!");
    }

    protected void handleAddToBackupList(File[] currentFile) {
        if (mBackupManager.isChildInBackupList(currentFile)) {
            if (SwtUtils.messageOkCancel(getShell(), "Ask", "Some directory already have backup items, do you really want to add to backup items?")) {
                mBackupManager.addToBackupList(currentFile);;
            }
        } else {
            mBackupManager.addToBackupList(currentFile);
        }
        refreshTable(true);
    }

    protected void handleCancelBackupFile(File[] currentFile) {
        mBackupManager.removeFromBackupList(currentFile);
        removeFromBackupDetailCache(currentFile);
        refreshTable(false);
    }

    private void removeFromBackupDetailCache(File[] currentFile) {
        for (File f : currentFile) {
            mBackupDetailCache.remove(f);
        }
    }

    protected void handleCancelBackupDirectory(File[] currentFile) {
        boolean ok = SwtUtils.messageOkCancel(getShell(), "Ask", //
                "Are you sure to cancel backup mutiple files?\n");
        if (ok) {
            mBackupManager.removeFromBackupList(currentFile);
            removeFromBackupDetailCache(currentFile);
            refreshTable(false);
        }
    }

    protected void handleIgnoreBackup(File[] currentFile) {
        mBackupManager.ignore(currentFile);
        removeFromBackupDetailCache(currentFile);
        refreshTable(false);
    }

    protected void handleRemoveFromIgnoreList(File[] currentFile) {
        mBackupManager.removeIgnore(currentFile);
        refreshTable(true);
    }

    protected void updateStateText() {
        lblState.setText(mPath);
    }

    @SuppressWarnings("serial")
    private class RootFile extends File {

        public RootFile(String pathname) {
            super(pathname);
        }

        @Override
        public String getCanonicalPath() throws IOException {
            return "";
        }

        @Override
        public boolean isDirectory() {
            return true;
        }
    }

    private void refreshTable(boolean startLoadSizeTask) {
        Log.i(TAG, "refreshTable startLoadSizeTask=" + startLoadSizeTask);

        clearTable();

        TableItem tiHeader = null;

        // indexes
        int backupSizeIndex = 0;
        int backupFileCountIndex = 0;
        int sizeIndex = 0;
        int fileCountIndex = 0;
        int backupPercentIndex = 0;

        // statistic
        long totalBackupSize = 0;
        int totalBackupCount = 0;
        long totalSize = 0;
        int totalFileCount = 0;

        if (mPath.isEmpty()) {
            mFiles = File.listRoots();
        } else {
            mFiles = new File(mPath).listFiles();

            // add header
            File parent = new File(mPath).getParentFile();
            tiHeader = new TableItem(table, 0);
            tiHeader.setText(0, "..");
            tiHeader.setImage(0, ImageResource.getDirIcon());
            if (parent != null) {
                tiHeader.setData(parent);
            } else {
                tiHeader.setData(new RootFile(""));
            }
        }

        if (mFiles != null) {
            List<File> sortedFiles = sort(mFiles);
            for (File f : sortedFiles) {
                // Don't show hidden files
                if (f.isHidden() && !Util.isDriverPath(f.getAbsolutePath())) {
                    // Log.i(TAG, "skip hidden " + f.getAbsolutePath());
                    continue;
                }

                // Check is backup list view mode
                if (!isFileSystemView) {
                    if (!(mBackupManager.isInBackupList(f) || (mBackupManager.isInBackupDirectory(f) && !mBackupManager.isIgnored(f)) || mBackupManager
                            .isChildInBackupList(f))) {
                        continue;
                    }
                }

                final TableItem ti = new TableItem(table, 0);
                String[] columns = new String[table.getColumnCount()];
                int i = 0;
                boolean needShowBackupSize = true;
                final File tmpFile = f;
                long backupSize = 0;
                long size = 0;

                if (mBackupManager.isInBackupList(f) || (mBackupManager.isInBackupDirectory(f) && !mBackupManager.isIgnored(f))) {
                    ti.setForeground(SwtUtils.getColor(COLOR_IN_BACKUP_LIST));
                } else if (mBackupManager.isChildInBackupList(f)) {
                    ti.setForeground(SwtUtils.getColor(COLOR_CHILD_IN_BACKUP_LIST));
                } else {
                    ti.setForeground(SwtUtils.getColor(COLOR_NOT_IN_BACKUP_LIST));
                    needShowBackupSize = false;
                }

                // Bind Data
                ti.setData(f);

                // File Name
                if (mPath.isEmpty()) {
                    columns[i++] = f.getAbsolutePath();
                } else {
                    columns[i++] = f.getName();
                }

                // last modified
                columns[i++] = getLastModifiedForDisplay(f);

                backupSizeIndex = i++;
                backupFileCountIndex = i++;
                if (needShowBackupSize) {
                    // Backup size and count
                    if (startLoadSizeTask) {
                        mBackupManager.startGetDetailTask(f, new DirectoryDetailListener() {
                            @Override
                            public void onGetDetail(final DirectoryDetail detail) {
                                Log.i(TAG, "backup onGetDetail file=" + tmpFile + " detail=" + detail);
                                mBackupDetailCache.put(tmpFile, detail);
                                enqueueRefreshTable();
                            }
                        });
                    }
                    if (mBackupDetailCache.containsKey(f)) {
                        DirectoryDetail detail = mBackupDetailCache.get(f);
                        setText(ti, backupSizeIndex, getDisplaySize(detail.size));
                        setText(ti, backupFileCountIndex, getDisplayFileCount(detail.fileCount));
                        totalBackupCount += detail.fileCount;
                        totalBackupSize += detail.size;
                        backupSize = detail.size;
                    }
                }

                sizeIndex = i++;
                fileCountIndex = i++;
                // Size
                if (f.isFile()) {
                    // File Size
                    columns[sizeIndex] = getDisplaySize(f.length());

                    // File Count
                    columns[fileCountIndex] = "1";

                    mDirectoryDetailCache.put(tmpFile, new DirectoryDetail(1, f.length()));

                    size = f.length();
                } else {
                    // Directory Size
                    if (startLoadSizeTask) {
                        DirectoryDetailManager.getInstance().startGetDetailTask(f, new DirectoryDetailListener() {
                            @Override
                            public void onGetDetail(final DirectoryDetail detail) {
                                Log.i(TAG, "directory onGetDetail " + tmpFile + " detail=" + detail);
                                mDirectoryDetailCache.put(tmpFile, detail);
                                enqueueRefreshTable();
                            }
                        });
                    }
                    if (mDirectoryDetailCache.containsKey(f)) {
                        DirectoryDetail detail = mDirectoryDetailCache.get(f);
                        setText(ti, sizeIndex, getDisplaySize(detail.size));
                        setText(ti, fileCountIndex, getDisplayFileCount(detail.fileCount));
                        totalFileCount += detail.fileCount;
                        totalSize += detail.size;
                        size = detail.size;
                    }
                }

                backupPercentIndex = i++;
                // Backup percent
                if (needShowBackupSize) {
                    columns[backupPercentIndex] = getBackupPercentString(backupSize, size);
                }

                ti.setText(columns);

                // Image
                if (f.isFile()) {
                    ti.setImage(0, ImageResource.getFileIcon());
                } else {
                    ti.setImage(0, ImageResource.getDirIcon());
                }
            }
        }

        if (tiHeader != null) {
            setText(tiHeader, backupSizeIndex, getDisplaySize(totalBackupSize));
            setText(tiHeader, backupFileCountIndex, getDisplayFileCount(totalBackupCount));
            setText(tiHeader, sizeIndex, getDisplaySize(totalSize));
            setText(tiHeader, fileCountIndex, getDisplayFileCount(totalFileCount));
            setText(tiHeader, backupPercentIndex, getBackupPercentString(totalBackupSize, totalSize));
        }
    }

    private String getBackupPercentString(long backupSize, long size) {
        if (size == 0 || backupSize == 0) {
            return "";
        }
        return (backupSize * 100 / size) + "%";
    }

    private synchronized void enqueueRefreshTable() {
        if (!mHasPendingRefresh) {
            mHasPendingRefresh = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {}
                    getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            refreshTable(false);
                        }
                    });
                    synchronized (BackupPanel.this) {
                        mHasPendingRefresh = false;
                    }
                }
            }).start();
        }
    }

    private List<File> sort(File[] files) {
        ArrayList<File> list = new ArrayList<File>();
        for (File f : files) {
            list.add(f);
        }
        if (mCurrentSort != SORT_NONE) {
            Collections.sort(list, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    HashMap<File, DirectoryDetail> cache = null;
                    if (mCurrentSort == SORT_SIZE) {
                        cache = mDirectoryDetailCache;
                    } else if (mCurrentSort == SORT_BACKUP_SIZE) {
                        cache = mBackupDetailCache;
                    }

                    DirectoryDetail d1 = (DirectoryDetail) cache.get(o1);
                    DirectoryDetail d2 = (DirectoryDetail) cache.get(o2);

                    // if (mCurrentSort == SORT_SIZE) {
                    // if (!o1.isDirectory()) {
                    // d1 = new DirectoryDetail(1, o1.length());
                    // }
                    //
                    // if (!o2.isDirectory()) {
                    // d2 = new DirectoryDetail(1, o2.length());
                    // }
                    // }

                    if (d1 != null && d2 != null) {
                        if (d2.size > d1.size) {
                            return 1;
                        } else if (d2.size < d1.size) {
                            return -1;
                        } else {
                            return 0;
                        }
                    } else if (d1 != null) {
                        return -1;
                    } else if (d2 != null) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }
        return list;
    }

    protected String getDisplayFileCount(int fileCount) {
        return fileCount + "";
    }

    protected void setText(final TableItem ti, final int index, final String string) {
        if (Thread.currentThread() == Display.getDefault().getThread()) {
            ti.setText(index, string);
        } else {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    ti.setText(index, string);
                }
            });
        }
    }

    private void clearTable() {
        table.removeAll();
    }

    private String getDisplaySize(long length) {
        if (length < 1024) {
            // B
            return String.format("%d B", length);
        } else if (length < 1024L * 1024) {
            // KB
            return String.format("%.1f KB", length / 1024.0);
        } else if (length < 1024L * 1024 * 1024) {
            // MB
            return String.format("%.1f MB", length * 1.0 / 1024 / 1024);
        } else if (length < 1024L * 1024 * 1024 * 1024) {
            // GB
            return String.format("%.1f GB", length * 1.0 / 1024 / 1024 / 1024);
        }
        return length + "";
    }

    private String getLastModifiedForDisplay(File f) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(f.lastModified());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);
        return String.format("%d-%02d-%02d %02d:%02d", year, month, day, hour, minute);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    public void gotoRoot() {
        changePath("");
    }

    private void changePath(String path) {
        mPath = path;
        onPathChanged();
        updateStateText();
        refreshTable(true);
    }

    public void startBackupAll() {
        startBackup("");
    }

    public void startBackupCurrentDirectory() {
        startBackup(mPath);
    }

    private void startBackup(String backupDirectory) {
        List<String> lostBackupDrivers = mBackupManager.getLostBackupDrivers();
        if (backupDirectory.isEmpty() && !lostBackupDrivers.isEmpty()) {
            if (!SwtUtils.messageOkCancel(getShell(), "Warning",
                    "Some backup driver is not exist, do you still want to backup?\n" + Arrays.toString(lostBackupDrivers.toArray()))) {
                return;
            }
        }
        DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
        dd.setText("Choose A Directory To Backup To");
        dd.setFilterPath(Preference.getString(Const.PREF_BACKUP_TO, "/"));
        String backupTo = dd.open();
        if (backupTo != null) {
            Preference.setString(Const.PREF_BACKUP_TO, backupTo);
            if (!Util.isEmptyDirectory(backupTo)) {
                if (!SwtUtils.messageOkCancel(getShell(), "Prompt", "Directory is not empty, do you want to overwrite it?")) {
                    return;
                }
            }
            final BackupDialog bd = new BackupDialog(getShell(), 0);
            BackupManager.getInstance().backupTo(backupDirectory, backupTo, new BackupListener() {
                @Override
                public void onProgress(BackupDetail detail) {
                    Log.i(TAG, "onProgress size=" + detail.size + " total=" + detail.totalSize);
                    bd.setState(getDisplaySize(detail.size) + " / " + getDisplaySize(detail.totalSize) + "  " + detail.currentFile);
                    bd.setProgress((int) (detail.size * 100L / detail.totalSize));
                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "onFinish");
                    bd.close();
                }
            });
            bd.open();
        }
    }

    public void viewFileSystem(boolean b) {
        isFileSystemView = b;
        refreshTable(false);
    }

    public boolean isFileSystemView() {
        return isFileSystemView;
    }

    public void refreshSizeView() {
        if (mPath.equals("")) {
            DirectoryDetailManager.getInstance().reload();
        } else {
            DirectoryDetailManager.getInstance().reload(new File(mPath));
        }
        clearDetailCache();
        refreshTable(true);
    }

    private void clearDetailCache() {
        mBackupDetailCache.clear();
        mDirectoryDetailCache.clear();
    }

    public void refresh() {
        refreshTable(false);
    }
}
