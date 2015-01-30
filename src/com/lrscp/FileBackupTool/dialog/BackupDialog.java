package com.lrscp.FileBackupTool.dialog;

import lrscp.lib.swt.SwtUtils;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

public class BackupDialog extends Dialog {

    protected Object result;
    protected Shell shell;
    private Label lblState;
    private Display display;
    private ProgressBar progressBar;

    /**
     * Create the dialog.
     * 
     * @param parent
     * @param style
     */
    public BackupDialog(Shell parent, int style) {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        setText("Backup");
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
        display = getParent().getDisplay();
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
        shell = new Shell(getParent(), SWT.BORDER | SWT.TITLE);
        shell.setSize(632, 152);
        shell.setText(getText());

        progressBar = new ProgressBar(shell, SWT.NONE);
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
        progressBar.setBounds(10, 57, 603, 43);

        lblState = new Label(shell, SWT.NONE);
        lblState.setBounds(10, 21, 603, 20);
        lblState.setText("Calculating...");

        SwtUtils.center(shell, 10);
    }

    public void setState(final String msg) {
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                lblState.setText(msg);
            }
        });
    }

    public void setProgress(final int progress) {
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                progressBar.setSelection(progress);
            }
        });
    }

    public void close() {
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                shell.dispose();
            }
        });
    }
}
