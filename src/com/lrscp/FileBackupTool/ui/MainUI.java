package com.lrscp.FileBackupTool.ui;

import lrscp.lib.Log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wb.swt.SWTResourceManager;

import com.lrscp.FileBackupTool.data.BackupManager;
import com.lrscp.FileBackupTool.data.DirectoryDetailManager;
import org.eclipse.swt.widgets.Label;

public class MainUI extends Shell {

    /**
     * Launch the application.
     * 
     * @param args
     */
    public static void main(String args[]) {
        try {
            onStart();
            Display display = Display.getDefault();
            MainUI shell = new MainUI(display);
            shell.open();
            shell.layout();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            onExit();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void onStart() {
        Log.startConnectingLogViewer("FileBackupTool");
    }

    private static void onExit() {
        DirectoryDetailManager.getInstance().saveCache();
        BackupManager.getInstance().save();
    }

    private BackupPanel mCurrentPanel;

    protected boolean isViewingFileSystem = true;

    /**
     * Create the shell.
     * 
     * @param display
     */
    public MainUI(Display display) {
        super(display, SWT.SHELL_TRIM);
        setLayout(new GridLayout(1, false));

        Menu menu = new Menu(this, SWT.BAR);
        setMenuBar(menu);

        MenuItem cmFile = new MenuItem(menu, SWT.CASCADE);
        cmFile.setText("File");

        Menu menu_1 = new Menu(cmFile);
        cmFile.setMenu(menu_1);

        MenuItem cmView = new MenuItem(menu, SWT.CASCADE);
        cmView.setText("View");

        Menu menu_3 = new Menu(cmView);
        cmView.setMenu(menu_3);

        MenuItem mntmRefresh = new MenuItem(menu_3, SWT.NONE);
        mntmRefresh.setText("Refresh");
        mntmRefresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRefresh();
            }
        });
        
        MenuItem miGoToRoot = new MenuItem(menu_3, SWT.NONE);
        miGoToRoot.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleGoToRoot();
            }
        });
        miGoToRoot.setText("Go To Root");

        MenuItem menuItem = new MenuItem(menu_3, SWT.SEPARATOR);
        menuItem.setText("Filter");

        MenuItem miViewFileSystem = new MenuItem(menu_3, SWT.RADIO);
        miViewFileSystem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleViewFileSystem();
            }
        });
        miViewFileSystem.setText("View File System");
        miViewFileSystem.setSelection(true);

        MenuItem miViewBackupList = new MenuItem(menu_3, SWT.RADIO);
        miViewBackupList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleViewBackupList();
            }
        });
        miViewBackupList.setText("View Backup List");

        new MenuItem(menu_3, SWT.SEPARATOR);

        MenuItem mntmRefreshDirectorySize = new MenuItem(menu_3, SWT.NONE);
        mntmRefreshDirectorySize.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRefreshSizeView();
            }
        });
        mntmRefreshDirectorySize.setText("Refresh Directory Size");

        MenuItem cmAction = new MenuItem(menu, SWT.CASCADE);
        cmAction.setText("Action");

        Menu menu_2 = new Menu(cmAction);
        cmAction.setMenu(menu_2);

        MenuItem mntmStartBackup = new MenuItem(menu_2, SWT.NONE);
        mntmStartBackup.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleStartBackup();
            }
        });
        mntmStartBackup.setText("Start Backup All");
        
        MenuItem mntmStartBackupCurrent = new MenuItem(menu_2, SWT.NONE);
        mntmStartBackupCurrent.setText("Start Backup Current Directory");
        mntmStartBackupCurrent.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleStartBackupCurrentDirectory();
            }
        });
        
        TabFolder tabFolder = new TabFolder(this, SWT.NONE);
        tabFolder.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        TabItem tbtmNewItem = new TabItem(tabFolder, SWT.NONE);
        tbtmNewItem.setText("NewBackupTask1");
        mCurrentPanel = new BackupPanel(tabFolder, 0);
        tbtmNewItem.setControl(mCurrentPanel);

        createContents();
    }

    protected void handleStartBackupCurrentDirectory() {
        mCurrentPanel.startBackupCurrentDirectory();
    }

    protected void handleRefresh() {
        mCurrentPanel.refresh();
    }

    protected void handleRefreshSizeView() {
        mCurrentPanel.refreshSizeView();
    }

    protected void handleStartBackup() {
        mCurrentPanel.startBackupAll();
    }

    protected void handleViewBackupList() {
        mCurrentPanel.viewFileSystem(false);
    }

    protected void handleViewFileSystem() {
        mCurrentPanel.viewFileSystem(true);
    }

    protected void handleGoToRoot() {
        mCurrentPanel.gotoRoot();
    }

    /**
     * Create contents of the shell.
     */
    protected void createContents() {
        setText("File Backup Tool");
        setSize(764, 462);
        setMaximized(true);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
}
