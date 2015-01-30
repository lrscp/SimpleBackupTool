package com.lrscp.FileBackupTool.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class BackupListDialog extends Dialog {

    protected Object result;
    protected Shell shell;
    private Table table;

    /**
     * Create the dialog.
     * 
     * @param parent
     * @param style
     */
    public BackupListDialog(Shell parent, int style) {
        super(parent, style);
        setText("Backup List");
    }

    /**
     * Open the dialog.
     * 
     * @return the result
     */
    public Object open() {
        createContents();
        shell.open();
        shell.layout();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return result;
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        shell = new Shell(getParent(), getStyle());
        shell.setSize(875, 486);
        shell.setText(getText());
        shell.setLayout(new GridLayout(1, false));

        table = new Table(shell, SWT.BORDER | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn tcFile = new TableColumn(table, SWT.NONE);
        tcFile.setWidth(779);
        tcFile.setText("File");

        updateTable();
    }

    private void updateTable() {}
}
