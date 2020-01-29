package com.itshiteshverma.sensordatafinal.ui.home;


import android.content.Context;
import android.os.Environment;
import android.view.LayoutInflater;

import com.itshiteshverma.sensordatafinal.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class DataBase_ImportExportHandler {

    public static void importDB(Context context, LayoutInflater layoutInflater) {
        ToastHelper toastHelper = new ToastHelper(context, layoutInflater);
        try {
            String appFolder_Name = context.getString(R.string.app_name);
            File appFolderLocation = new File(Environment.getExternalStorageDirectory(), appFolder_Name);
            if (!appFolderLocation.exists()) {
                appFolderLocation.mkdirs();
            }
            if (appFolderLocation.canWrite()) {
                File backupDB = context.getDatabasePath(Note.DATABASE_NAME);
                String backupDBPath = String.format("%s.db", Note.DATABASE_NAME);
                File currentDB = new File(appFolderLocation, backupDBPath);
                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                toastHelper.toastIconInfo("DataBase Exported Success");
            }
        } catch (Exception e) {
            e.printStackTrace();
            toastHelper.toastIconError("DataBase Exported Failure");

        }
    }

    public static void exportDB(Context context, LayoutInflater layoutInflater) {
        ToastHelper toastHelper = new ToastHelper(context, layoutInflater);
        try {
            String appFolder_Name = context.getString(R.string.app_name);
            File appFolderLocation = new File(Environment.getExternalStorageDirectory(), appFolder_Name);
            if (!appFolderLocation.exists()) {
                appFolderLocation.mkdirs();
            }

            //File sd = Environment.getExternalStorageDirectory();
            if (appFolderLocation.canWrite()) {
                String backupDBPath = String.format("%s.db", Note.DATABASE_NAME);
                File currentDB = context.getDatabasePath(Note.DATABASE_NAME);
                File backupDB = new File(appFolderLocation, backupDBPath);

                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                // toastHelper.toastIconInfo("DataBase Exported Success");
            }

//            if (appFolderLocation.canWrite()) {
//                String backupDBPath = String.format("%s.db", Note.LOG_DATABASE);
//                File currentDB = context.getDatabasePath(Note.LOG_DATABASE);
//                File backupDB = new File(appFolderLocation, backupDBPath);
//
//                FileChannel src = new FileInputStream(currentDB).getChannel();
//                FileChannel dst = new FileOutputStream(backupDB).getChannel();
//                dst.transferFrom(src, 0, src.size());
//                src.close();
//                dst.close();
//                toastHelper.toastIconInfo("DataBase Exported Success");
//            }
        } catch (Exception e) {
            toastHelper.toastIconError("DataBase Exported Failure ");
            e.printStackTrace();
        }
    }

    public static void exportExcel(Context context, LayoutInflater layoutInflater) {
        ToastHelper toastHelper = new ToastHelper(context, layoutInflater);
        try {
            String appFolder_Name = context.getString(R.string.app_name);
            File appFolderLocation = new File(Environment.getExternalStorageDirectory(), appFolder_Name);
            if (!appFolderLocation.exists()) {
                appFolderLocation.mkdirs();
            }
            if (appFolderLocation.canWrite()) {
                String backupDBPath = String.format("%s.dbHelper", Note.DATABASE_NAME);
                File currentDB = context.getDatabasePath(Note.DATABASE_NAME);
                File backupDB = new File(appFolderLocation, backupDBPath);

                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                toastHelper.toastIconInfo("DataBase Exported Success");
            }
        } catch (Exception e) {
            toastHelper.toastIconError("DataBase Exported Failure ");
            e.printStackTrace();
        }
    }
}