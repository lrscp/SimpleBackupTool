package com.lrscp.FileBackupTool.test;

import java.io.File;
import java.io.IOException;

import com.lrscp.FileBackupTool.Util;

import lrscp.lib.Log;

public class Test {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Log.i(Util.getDriverFile(new File("E:\\")));
    }

}
