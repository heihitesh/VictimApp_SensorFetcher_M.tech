package com.itshiteshverma.sensordatafinal.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;
import com.itshiteshverma.sensordatafinal.R;

import org.zeroturnaround.zip.commons.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;


public class RecordSensorData extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    static final long CHART_UPDATE_PERIOD = 10;
    static final String EXTENSION = ".csv";
    static final long MAX_SAMPLES_DISPLAY = 300;
    static final int NAV_ARROWS_OPAQUE = 150;
    static final int NAV_ARROWS_TRANSPARENT = 20;
    static final int SLEEP_DURATION_MILLIS = 2000;
    static final String VALUES_SEPARATOR = ",";

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
    Button bForward, bStop, bBackward, bLeft, bRight;
    String currentLabel = "";
    Long READ_SENSOR_TIME_START;


    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_record_sensor_data);

        currentLabel = "NA";
        toastHelper = new ToastHelper(RecordSensorData.this, getLayoutInflater());
        this.chartViewPager = findViewById(R.id.chartViewPager);
        this.leftNavBtn = findViewById(R.id.leftNavBtn);
        this.rightNavBtn = findViewById(R.id.rightNavBtn);
        this.ivStartPause = findViewById(R.id.ivStart_Pause);
        bForward = findViewById(R.id.bMoveForward);
        bStop = findViewById(R.id.bStop);
        bBackward = findViewById(R.id.bMoveBackWard);
        bLeft = findViewById(R.id.bLeftTurn);
        bRight = findViewById(R.id.bRightTurn);
        bForward.setOnClickListener(this);
        bBackward.setOnClickListener(this);
        bStop.setOnClickListener(this);

        setTouchListner();

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
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListner() {

        bLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //perform your animation when button is touched and held
                    currentLabel = "Left Turn";
                    bForward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    bBackward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    bLeft.setBackgroundColor(getResources().getColor(R.color.green_400));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    //perform your animation when button is released
                    currentLabel = "Move Forward";
                    bLeft.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    bForward.setBackgroundColor(getResources().getColor(R.color.green_400));
                }
                return true;
            }
        });

        bRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //perform your animation when button is touched and held
                    currentLabel = "Right Turn";
                    bForward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    bBackward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    bRight.setBackgroundColor(getResources().getColor(R.color.green_400));
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    //perform your animation when button is released
                    currentLabel = "Move Forward";
                    bRight.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                    bForward.setBackgroundColor(getResources().getColor(R.color.green_400));
                }
                return true;
            }
        });


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
        long j = (long) i2;
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

    public void onBackPressed() {
        RecordSensorData.this.sensorManager.unregisterListener(RecordSensorData.this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Stop current session ?");
        builder.setMessage("The current session will be stopped and saved. Do you want to proceed to Main Page ?");
        builder.setNegativeButton("CANCEL", null);
        builder.setPositiveButton("OK", new OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int i) {
                RecordSensorData.this.sensorManager.unregisterListener(RecordSensorData.this);
                RecordSensorData.this.asyncFileWriter.stop();
                StringBuilder sb = new StringBuilder();
                sb.append("Session stopped. Data file saved in: ");
                sb.append(Utils.APP_STORAGE_DIR);
                Toast.makeText(RecordSensorData.this, sb.toString(), Toast.LENGTH_LONG).show();

                RecordSensorData.this.finish();
            }
        });
        builder.create().show();
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

    private String sensorEventToString(SensorEvent sensorEvent, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.currentTimeMillis() - READ_SENSOR_TIME_START);
        sb.append(str);
        sb.append(Utils.floatsToString(sensorEvent.values, str));
        String sb2 = sb.toString();
        if (!this.currentLabel.isEmpty()) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(sb2);
            sb3.append(str);
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
            case R.id.bMoveBackWard:
                currentLabel = "Move BackWard";
                bStop.setBackgroundColor(getResources().getColor(R.color.orange_300));
                bForward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                bBackward.setBackgroundColor(getResources().getColor(R.color.green_400));
                break;
            case R.id.bMoveForward:
                currentLabel = "Move Forward";
                bStop.setBackgroundColor(getResources().getColor(R.color.orange_300));
                bForward.setBackgroundColor(getResources().getColor(R.color.green_300));
                bBackward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                break;

            case R.id.bStop:
                currentLabel = "Stop";
                bStop.setBackgroundColor(getResources().getColor(R.color.red_400));
                bForward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                bBackward.setBackgroundColor(getResources().getColor(R.color.yellow_200));
                break;

        }
    }
}
