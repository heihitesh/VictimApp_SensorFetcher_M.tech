package com.itshiteshverma.sensordatafinal.ui.home;

import android.app.Dialog;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.itshiteshverma.sensordatafinal.R;

import org.zeroturnaround.zip.commons.IOUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.itshiteshverma.sensordatafinal.ui.home.DataBase_ImportExportHandler.exportDB;
import static com.itshiteshverma.sensordatafinal.ui.home.MyApplication.NOTIFICATION_CHANNEL_AUTO_BACKUP;


public class RecordSensorData extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    static final long CHART_UPDATE_PERIOD = 10;
    static final String EXTENSION = ".csv";
    static final long MAX_SAMPLES_DISPLAY = 300;
    static final int NAV_ARROWS_OPAQUE = 150;
    static final int NAV_ARROWS_TRANSPARENT = 20;
    static final int SLEEP_DURATION_MILLIS = 2000;
    static final String VALUES_SEPARATOR = ",";
    static final int BUFFER_TIME_MILLS = 1000;

    AsyncFileWriter asyncFileWriter;
    ArrayList<Button> buttons = new ArrayList<>();
    ViewPager chartViewPager;
    SparseArray<LineChart> chartsSpArray = new SparseArray<>();
    SparseArray<File> filesSpArray = new SparseArray<>();
    ImageButton leftNavBtn;
    SparseIntArray nSamplesSpArray = new SparseIntArray();
    ConcurrentLinkedQueue<Object[]> queue = new ConcurrentLinkedQueue<>();
    ImageButton rightNavBtn;
    ArrayList<Sensor> selectedSensors = new ArrayList<>();
    SensorManager sensorManager;
    ToastHelper toastHelper;
    ImageView ivStartPause, ivStop;
    TextView tvHide;
    Boolean START_PAUSE = false;
    Button bForward, bStop;
    String currentLabel = "";
    Long READ_SENSOR_TIME_START;

    TextView CompassDegree, RelativeDegree;
    LinearLayout LeftTurn, RightTurn;
    ImageView compassDial;
    boolean INITIAL_VALUE_SET = false;
    private Compass compass;
    private float currentAzimuth;
    private int INITIAL_VALUE;
    private int MARGIN_FIX_ERROR = 10;
    public static final int REQUEST_CODE_SET_BACKUP_JOB_SCHEDULE = 836; //Any 4 digit Random Value
    public static final String WORK_TAG_AUTO_BACKUP = "SENSOR_DATA";
    private final int notificationID = 1036;
    StorageReference filePath;
    NotificationCompat.Builder notification;
    NotificationManagerCompat notificationManager;
    private DatabaseHelper dbHelper;


    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_record_sensor_data);
        dbHelper = new DatabaseHelper(this);

        currentLabel = "NA";
        toastHelper = new ToastHelper(RecordSensorData.this, getLayoutInflater());
        this.chartViewPager = findViewById(R.id.chartViewPager);
        this.leftNavBtn = findViewById(R.id.leftNavBtn);
        this.rightNavBtn = findViewById(R.id.rightNavBtn);
        this.ivStartPause = findViewById(R.id.ivStart_Pause);
        CompassDegree = findViewById(R.id.tvDegree);
        RelativeDegree = findViewById(R.id.tvRelativeDegree);
        LeftTurn = findViewById(R.id.llLeftTurn);
        RightTurn = findViewById(R.id.llRightTurn);
        compassDial = findViewById(R.id.compass_dial);

        bForward = findViewById(R.id.bMoveForward);
        bStop = findViewById(R.id.bStop);
        bForward.setOnClickListener(this);
        bStop.setOnClickListener(this);

        ivStop = findViewById(R.id.ivStop);
        tvHide = findViewById(R.id.tvHide);
        START_PAUSE = false;

        initSensors();
        initCharts();
        initDataFiles();
        this.asyncFileWriter = new AsyncFileWriter(SLEEP_DURATION_MILLIS, this.queue);
        this.asyncFileWriter.execute();
        onClickStartPauseButton();
        onClickStopBtn();
        onClickHideBtn();
        onClickNavBtns();
        setAdapterChartViewPager();
        onChangeChartViewPager();
        setupCompass();
    }


    public void onSensorChanged(SensorEvent sensorEvent) {
        int type = sensorEvent.sensor.getType();
        File file = this.filesSpArray.get(type);
        int i = this.nSamplesSpArray.get(type, 0);
        LineChart lineChart = this.chartsSpArray.get(type);
        String sensorEventToString = sensorEventToString(sensorEvent, VALUES_SEPARATOR);
        this.queue.add(new Object[]{sensorEventToString, file});
        int i2 = i + 1;
        this.nSamplesSpArray.put(type, i2);
        long j = i2;
        if (j % CHART_UPDATE_PERIOD == 0) {
            Utils.updateChart(lineChart, sensorEvent.values, j, MAX_SAMPLES_DISPLAY);
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        this.sensorManager.unregisterListener(this);
        this.asyncFileWriter.stop();
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(float azimuth) {
                adjustArrow(azimuth);
            }
        };
        compass.setListener(cl);
    }


    private void adjustArrow(float azimuth) {
        Animation animator = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;
        String display = (int) currentAzimuth + "";
        String cardDirect;
        if (currentAzimuth == 0 || currentAzimuth == 360)
            cardDirect = "N";
        else if (currentAzimuth > 0 && currentAzimuth < 90)
            cardDirect = "NE";
        else if (currentAzimuth == 90)
            cardDirect = "E";
        else if (currentAzimuth > 90 && currentAzimuth < 180)
            cardDirect = "SE";
        else if (currentAzimuth == 180)
            cardDirect = "S";
        else if (currentAzimuth > 180 && currentAzimuth < 270)
            cardDirect = "SW";
        else if (currentAzimuth == 270)
            cardDirect = "W";
        else if (currentAzimuth > 270 && currentAzimuth < 360)
            cardDirect = "NW";
        else
            cardDirect = "Unknown";
        CompassDegree.setText(display + "°" + " " + cardDirect);
        if (!INITIAL_VALUE_SET) {
            //Value is not set
            INITIAL_VALUE = (int) currentAzimuth;
            INITIAL_VALUE_SET = true;
        } else {
            //String temp = String.valueOf(getDifference(INITIAL_VALUE, (int) currentAzimuth));
            int temp = getDirectionalDifference(INITIAL_VALUE, (int) currentAzimuth);
            if (temp < 0) {
                //Negative // Possible a Right Turn
                if (temp * -1 >= 90 + MARGIN_FIX_ERROR || temp * -1 >= 90 - MARGIN_FIX_ERROR) {
                    //Definitely a Right Turn
                    RightTurn.setVisibility(View.VISIBLE);
                    currentLabel = "Right Turn";
                    bForward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    //perform your animation when button is released
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(BUFFER_TIME_MILLS);
                                //We will Buffer for some time after resetting the Value
                                INITIAL_VALUE = (int) currentAzimuth;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    RightTurn.setVisibility(View.INVISIBLE);
                    currentLabel = "Move Forward";
                    bForward.setBackgroundColor(getResources().getColor(R.color.green_400));
                }
            } else {
                //Positive // Possible a Left Turn
                if (temp >= 90 + MARGIN_FIX_ERROR || temp >= 90 - MARGIN_FIX_ERROR) {
                    //Definitely a Left Turn
                    LeftTurn.setVisibility(View.VISIBLE);
                    currentLabel = "Left Turn";
                    bForward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(BUFFER_TIME_MILLS);
                                //We will Buffer for some time after resetting the Value
                                INITIAL_VALUE = (int) currentAzimuth;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } else {
                    LeftTurn.setVisibility(View.INVISIBLE);
                    currentLabel = "Move Forward";
                    bForward.setBackgroundColor(getResources().getColor(R.color.green_400));
                }
            }
            RelativeDegree.setText(temp + "° ");
        }

        animator.setDuration(500);
        animator.setRepeatCount(0);
        animator.setFillAfter(true);
        compassDial.startAnimation(animator);
    }

//    private int getDifference(int initial_value, int currentAzimuth) {
//        return Math.min((initial_value - currentAzimuth) < 0 ? (initial_value - currentAzimuth + 360) : (initial_value - currentAzimuth),
//                (currentAzimuth - initial_value) < 0 ? (currentAzimuth - initial_value + 360) : (currentAzimuth - initial_value));
//    }

    //Gives Compass Rotation (clockwise or anti-clockwise)
    private int getDirectionalDifference(int initial_value, int currentAzimuth) {
        return ((((initial_value - currentAzimuth) % 360) + 540) % 360) - 180;
    }

    public void onBackPressed() {

        RecordSensorData.this.sensorManager.unregisterListener(RecordSensorData.this);
        final Dialog dialog = new Dialog(this);

        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.dialog_add_value_dark);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        dialog.setCancelable(true);
        dialog.findViewById(R.id.bt_close).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        Button btnAddPt = dialog.findViewById(R.id.bSave);
        Button btnCancel = dialog.findViewById(R.id.bCancel);
        final CheckBox checkBoxSendData = dialog.findViewById(R.id.checkboxSendData);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnAddPt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecordSensorData.this.sensorManager.unregisterListener(RecordSensorData.this);
                RecordSensorData.this.asyncFileWriter.stop();
                StringBuilder sb = new StringBuilder();
                sb.append("Session stopped. Data file saved in: ");
                sb.append(Utils.APP_STORAGE_DIR);
                Toast.makeText(RecordSensorData.this, sb.toString(), Toast.LENGTH_LONG).show();
                exportDB(RecordSensorData.this, getLayoutInflater());
                if (checkBoxSendData.isChecked()) {
                    //We Can Upload the Data 
                    UploadData();
                }
                dialog.dismiss();
                RecordSensorData.this.finish();

            }
        });

        dialog.show();
    }

    private void UploadData() {
        notificationManager = NotificationManagerCompat.from(getApplicationContext());

        String CurrentDate = "";
        String CurrentTime = "";
        try {
            CurrentDate = new SimpleDateFormat("EEE, d MMM ", Locale.getDefault()).format(new Date());
            CurrentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());

        } catch (Exception e) {

        }
        int PROGRESS_MAX = 100;
        int PROGRESS_CURRENT = 0;

        notification = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_AUTO_BACKUP);
        notification.setSmallIcon(R.drawable.icon_right_arrow);
        notification.setContentTitle("Uploading Sensor Data");
        notification.setContentText("Please Stay Connected To Internet, Date : " + CurrentDate + ", " + CurrentTime);
        notification.setPriority(NotificationCompat.PRIORITY_HIGH);
        notification.setOngoing(true);
        notification.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
        notificationManager.notify(notificationID, notification.build());

        StorageReference mStorageReference = FirebaseStorage.getInstance().getReference();
        String DATABASE_DATA = "DATABASE_DATA";

        filePath = mStorageReference.child("SensorDataCollectionApp").child(DATABASE_DATA).child("Hitesh")
                .child(Note.DATABASE_NAME + ".db");
        try {
            String appFolder_Name = this.getString(R.string.app_name);
            File appFolderLocation = new File(Environment.getExternalStorageDirectory(), appFolder_Name);
            if (!appFolderLocation.exists()) {
                appFolderLocation.mkdirs();
            }
            if (appFolderLocation.canWrite()) {
                String backupDBPath = String.format("%s.db", Note.DATABASE_NAME);
                File currentDB = new File(appFolderLocation, backupDBPath);
                Uri finalFile = Uri.fromFile(currentDB);
                final String finalCurrentDate = CurrentDate;
                final String finalCurrentTime = CurrentTime;
                filePath.putFile(finalFile).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String ImageURIString = uri.toString();
                                SimpleDateFormat s = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
                                String timeStamp = s.format(new Date());
                                notification.setContentTitle("Uploaded Completed Successfully");
                                notification.setOngoing(false); //Can Be Removed From the Notification
                                notification.setContentText("Last Updated On  : " + finalCurrentDate + ", " + finalCurrentTime)
                                        .setProgress(0, 0, false);

                                if (notificationManager != null) {
                                    notificationManager.notify(notificationID, notification.build());
                                }
                            }
                        });
                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                notification.setContentTitle("Back Up Failed");
                                notification.setOngoing(false); //Can Be Removed From the Notification
                                notification.setSmallIcon(R.drawable.ic_error_outline);
                                notification.setContentText("Please Connect To Internet, And Try Again")
                                        .setProgress(0, 0, false);
                                notification.setOngoing(false);

                                if (notificationManager != null) {
                                    notificationManager.notify(notificationID, notification.build());
                                }
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                notification.setProgress(100, (int) progress, false);

                                Notification ni = notification.build();
                                if (notificationManager != null) {
                                    notificationManager.notify(notificationID, ni);
                                }
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
            toastHelper.toastIconError("Data Not Uploaded : " + e.getMessage());
        }


    }

    public void onClickStartPauseButton() {
        ivStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (START_PAUSE) {
                    //On Pause has Been Clicked
                    RecordSensorData.this.sensorManager.unregisterListener(RecordSensorData.this);
                    ivStartPause.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    ivStartPause.setBackgroundColor(getResources().getColor(R.color.green_500));
                    START_PAUSE = false;

                } else {
                    //On Play has Been Clicked
                    ivStop.setVisibility(View.VISIBLE);
                    tvHide.setVisibility(View.VISIBLE);
                    ivStartPause.setImageResource(R.drawable.ic_pause_black_24dp);
                    ivStartPause.setBackgroundColor(getResources().getColor(R.color.blue_500));
                    if (READ_SENSOR_TIME_START == null) {
                        READ_SENSOR_TIME_START = System.currentTimeMillis();
                    }

                    new Handler().postDelayed(new Runnable() {
                        public final void run() {
                            RecordSensorData.this.sensorManager.unregisterListener(RecordSensorData.this);
                        }
                    }, RecordSensorData.this.getIntent().getLongExtra(MainPage.DURATION_KEY, 3600000));
                    Utils.registerSensors(RecordSensorData.this.sensorManager, RecordSensorData.this, RecordSensorData.this.selectedSensors, RecordSensorData.this.getIntent().getIntExtra(MainPage.SENSOR_DELAY_KEY, 1));

                    START_PAUSE = true;
                }
            }
        });


    }

    private void onClickStopBtn() {
        ivStop.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                RecordSensorData.this.onBackPressed();
            }
        });
    }

    private void onClickHideBtn() {
        tvHide.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.addCategory("android.intent.category.HOME");
                RecordSensorData.this.startActivity(intent);
                toastHelper.toastIconInfo("The app is hidden but still running in background.");
            }
        });
    }

    private void onClickNavBtns() {
        this.leftNavBtn.getBackground().setAlpha(20);
        if (getIntent().getIntegerArrayListExtra(MainPage.SENSOR_TYPES_KEY).size() <= 1) {
            this.rightNavBtn.getBackground().setAlpha(20);
        } else {
            this.rightNavBtn.getBackground().setAlpha(NAV_ARROWS_OPAQUE);
        }
        this.leftNavBtn.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                RecordSensorData.this.chartViewPager.arrowScroll(17);
            }
        });
        this.rightNavBtn.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                RecordSensorData.this.chartViewPager.arrowScroll(66);
            }
        });
    }

    private void setAdapterChartViewPager() {
        this.chartViewPager.setAdapter(new PagerAdapter() {
            public int getCount() {
                return RecordSensorData.this.chartsSpArray.size();
            }

            public Object instantiateItem(ViewGroup viewGroup, int i) {
                View inflate = ((LayoutInflater) RecordSensorData.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.charts_page, null);
                Utils.replaceView(inflate.findViewById(R.id.currentChart), RecordSensorData.this.chartsSpArray.get(RecordSensorData.this.chartsSpArray.keyAt(i)));
                viewGroup.addView(inflate, 0);
                return inflate;
            }

            public boolean isViewFromObject(View view, Object obj) {
                return view == obj;
            }

            public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
                viewGroup.removeView((View) obj);
            }
        });
    }

    private void onChangeChartViewPager() {
        this.chartViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int i) {
            }

            public void onPageScrolled(int i, float f, int i2) {
            }

            public void onPageSelected(int i) {
                int currentItem = RecordSensorData.this.chartViewPager.getCurrentItem();
                int count = RecordSensorData.this.chartViewPager.getAdapter().getCount();
                if (count <= 1) {
                    RecordSensorData.this.leftNavBtn.getBackground().setAlpha(20);
                    RecordSensorData.this.rightNavBtn.getBackground().setAlpha(20);
                } else if (currentItem == 0) {
                    RecordSensorData.this.leftNavBtn.getBackground().setAlpha(20);
                    RecordSensorData.this.rightNavBtn.getBackground().setAlpha(RecordSensorData.NAV_ARROWS_OPAQUE);
                } else if (currentItem == count - 1) {
                    RecordSensorData.this.leftNavBtn.getBackground().setAlpha(RecordSensorData.NAV_ARROWS_OPAQUE);
                    RecordSensorData.this.rightNavBtn.getBackground().setAlpha(20);
                } else {
                    RecordSensorData.this.leftNavBtn.getBackground().setAlpha(RecordSensorData.NAV_ARROWS_OPAQUE);
                    RecordSensorData.this.rightNavBtn.getBackground().setAlpha(RecordSensorData.NAV_ARROWS_OPAQUE);
                }
            }
        });
    }

    private void initDataFiles() {
        Iterator it = getIntent().getIntegerArrayListExtra(MainPage.SENSOR_TYPES_KEY).iterator();
        while (it.hasNext()) {
            Integer num = (Integer) it.next();
            String stringExtra = getIntent().getStringExtra(MainPage.FILE_NAME_KEY);
            File file = new File(Utils.APP_STORAGE_DIR, stringExtra);
            if (!file.exists()) {
                file.mkdir();
            }
            StringBuilder sb = new StringBuilder();
            sb.append(this.sensorManager.getDefaultSensor(num.intValue()).getName());
            sb.append(EXTENSION);
            String sb2 = sb.toString();
            StringBuilder sb3 = new StringBuilder();
            sb3.append(Utils.APP_STORAGE_DIR);
            sb3.append(File.separator);
            sb3.append(stringExtra);
            File file2 = new File(sb3.toString(), sb2);
            if (!file2.exists()) {
                try {
                    file2.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.filesSpArray.put(num.intValue(), file2);
        }
    }

    private void initSensors() {
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Iterator it = getIntent().getIntegerArrayListExtra(MainPage.SENSOR_TYPES_KEY).iterator();
        while (it.hasNext()) {
            this.selectedSensors.add(this.sensorManager.getDefaultSensor(((Integer) it.next()).intValue()));
        }
    }

    private void initCharts() {
        Iterator it = getIntent().getIntegerArrayListExtra(MainPage.SENSOR_TYPES_KEY).iterator();
        while (it.hasNext()) {
            int intValue = ((Integer) it.next()).intValue();
            LineChart lineChart = new LineChart(this);
            lineChart.setLayoutParams(new LayoutParams(-1, -1));
            lineChart.getDescription().setText(this.sensorManager.getDefaultSensor(intValue).getName());
            Utils.initCurves(lineChart, Utils.getSensorDimension(intValue));
            this.chartsSpArray.put(intValue, lineChart);
        }
    }

    private String sensorEventToString(SensorEvent sensorEvent, String seprator) {

        long timeDiff = System.currentTimeMillis() - READ_SENSOR_TIME_START;
        float xAxis = sensorEvent.values[0];
        float yAxis = sensorEvent.values[1];
        float zAxis = sensorEvent.values[2];
        String sampleName = getIntent().getStringExtra(MainPage.FILE_NAME_KEY);

        Note note = new Note(timeDiff, xAxis, yAxis, zAxis, sampleName, currentLabel);
        dbHelper.setValue(note);

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();

        StringBuilder sb = new StringBuilder();
        sb.append(timeDiff);
        sb.append(seprator);
        sb.append(Utils.floatsToString(sensorEvent.values, seprator));
        String sb2 = sb.toString();
        if (!this.currentLabel.isEmpty()) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(sb2);
            sb3.append(seprator);
            sb3.append(this.currentLabel);
            sb2 = sb3.toString();
        }
        StringBuilder sb4 = new StringBuilder();
        sb4.append(sb2);
        sb4.append(IOUtils.LINE_SEPARATOR_UNIX);
        return sb4.toString();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bMoveForward:
                currentLabel = "Move Forward";
                bStop.setBackgroundColor(getResources().getColor(R.color.orange_300));
                bForward.setBackgroundColor(getResources().getColor(R.color.green_300));
                break;

            case R.id.bStop:
                currentLabel = "Stop";
                bStop.setBackgroundColor(getResources().getColor(R.color.red_400));
                bForward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                break;

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        compass.stop();
    }
}
