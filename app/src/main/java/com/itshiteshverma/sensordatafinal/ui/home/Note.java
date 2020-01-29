package com.itshiteshverma.sensordatafinal.ui.home;

/**
 * Created by ravi on 20/02/18.
 */

public class Note {
    // Database Version
    public static final int MAIN_DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "values_db";


    public static final String VALUE_ID = "VALUE_ID";
    public static final String LABEL_TAG = "LABEL_TAG";

    public static final String X_VALUE = "X_VALUE";
    public static final String Y_VALUE = "Y_VALUE";
    public static final String Z_VALUE = "Z_VALUE";
    public static final String TIME = "TIME";
    public static final String SAMPLE_NAME = "SAMPLE_NAME";
    public static final String TIME_STAMP = "TIME_STAMP";
    public static final String VALUE_TABLE = "VALUE_TABLE";


    public static final String CREATE_TABLE_ADD_VALUES = "create table " + VALUE_TABLE +
            " (" + VALUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + TIME +
            " INTEGER," + X_VALUE + " FLOAT," + Y_VALUE + " FLOAT," + Z_VALUE + " FLOAT,"
            + LABEL_TAG + " TEXT NOT NULL, " + SAMPLE_NAME + " TEXT )";

    private long timeDiff;
    private float xAxis, yAxis, zAxis;
    private String sampleName;
    private String currentLabel;

    public Note(long timeDiff, float xAxis, float yAxis, float zAxis, String sampleName, String currentLabel) {
        this.timeDiff = timeDiff;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
        this.sampleName = sampleName;
        this.currentLabel = currentLabel;
    }

    public long getTimeDiff() {
        return timeDiff;
    }

    public void setTimeDiff(long timeDiff) {
        this.timeDiff = timeDiff;
    }

    public float getyAxis() {
        return yAxis;
    }

    public void setyAxis(float yAxis) {
        this.yAxis = yAxis;
    }

    public float getzAxis() {
        return zAxis;
    }

    public void setzAxis(float zAxis) {
        this.zAxis = zAxis;
    }

    public float getxAxis() {
        return xAxis;
    }

    public void setxAxis(float xAxis) {
        this.xAxis = xAxis;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getCurrentLabel() {
        return currentLabel;
    }

    public void setCurrentLabel(String currentLabel) {
        this.currentLabel = currentLabel;
    }
}
