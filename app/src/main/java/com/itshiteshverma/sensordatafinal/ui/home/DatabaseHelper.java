package com.itshiteshverma.sensordatafinal.ui.home;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {

    Context mContext;

    public DatabaseHelper(Context context) {
        super(context, Note.DATABASE_NAME, null, Note.MAIN_DATABASE_VERSION);
        mContext = context;
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        // create notes table
        db.execSQL(Note.CREATE_TABLE_ADD_VALUES);
        // dbHelper.execSQL(Note.CREATE_MONTHLY_ROOM_TABLE_VIEW);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    public void setValue(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        // if the data is new and not present // else update it above
        //Inserting New Data
        values.put(Note.TIME, note.getTimeDiff());
        values.put(Note.X_VALUE, note.getxAxis());
        values.put(Note.Y_VALUE, note.getyAxis());
        values.put(Note.Z_VALUE, note.getzAxis());
        values.put(Note.LABEL_TAG, note.getCurrentLabel());
        values.put(Note.SAMPLE_NAME, note.getSampleName());

        db.insert(Note.VALUE_TABLE, null, values);
        db.close();
        return;
    }

    public void clearValueTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE " + Note.VALUE_TABLE);
        db.execSQL(Note.CREATE_TABLE_ADD_VALUES);
        db.close();
    }
}
