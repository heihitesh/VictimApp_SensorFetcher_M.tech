package com.itshiteshverma.sensordatafinal.ui.home;

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
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;
import com.itshiteshverma.sensordatafinal.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.zeroturnaround.zip.commons.IOUtils;


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


    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_record_sensor_data);

        toastHelper = new ToastHelper(RecordSensorData.this, getLayoutInflater());
        this.chartViewPager = (ViewPager) findViewById(R.id.chartViewPager);
        this.leftNavBtn = (ImageButton) findViewById(R.id.leftNavBtn);
        this.rightNavBtn = (ImageButton) findViewById(R.id.rightNavBtn);
        this.ivStartPause = findViewById(R.id.ivStart_Pause);
        bForward = findViewById(R.id.bMoveForward);
        bStop = findViewById(R.id.bStop);
        bBackward = findViewById(R.id.bMoveBackWard);
        bLeft = findViewById(R.id.bLeftTurn);
        bRight = findViewById(R.id.bRightTurn);


        ivStop = findViewById(R.id.ivStop);
        tvHide = findViewById(R.id.tvHide);
        START_PAUSE = false;

        initSensors();
        initCharts();
        initDataFiles();
        this.asyncFileWriter = new AsyncFileWriter(SLEEP_DURATION_MILLIS, this.queue);
        this.asyncFileWriter.execute(new Void[0]);
        onClickStartPauseButton();
        onClickStopBtn();
        onClickHideBtn();
        onClickNavBtns();
        setAdapterChartViewPager();
        onChangeChartViewPager();
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        int type = sensorEvent.sensor.getType();
        File file = (File) this.filesSpArray.get(type);
        int i = this.nSamplesSpArray.get(type, 0);
        LineChart lineChart = (LineChart) this.chartsSpArray.get(type);
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
        builder.setTitle((CharSequence) "Stop current session ?");
        builder.setMessage((CharSequence) "The current session will be stopped and saved. Do you want to proceed to Main Page ?");
        builder.setNegativeButton("CANCEL", (OnClickListener) null);
        builder.setPositiveButton("OK", (OnClickListener) new OnClickListener() {
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
                Utils.replaceView((LineChart) inflate.findViewById(R.id.currentChart), (View) RecordSensorData.this.chartsSpArray.get(RecordSensorData.this.chartsSpArray.keyAt(i)));
                viewGroup.addView(inflate, 0);
                return inflate;
            }

            public boolean isViewFromObject(View view, Object obj) {
                return view == ((View) obj);
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
//
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
//    private void initLabelButtonsList() {
//        int i;
//        int i2;
////        TableLayout tableLayout = (TableLayout) findViewById(R.id.tabLayoutLabels);
////        ArrayList stringArrayListExtra = getIntent().getStringArrayListExtra(MainPage.LABELS_KEY);
//        if (!stringArrayListExtra.isEmpty()) {
//            this.currentLabel = (String) stringArrayListExtra.get(0);
//            i2 = (int) Math.ceil(Math.sqrt((double) stringArrayListExtra.size()));
//            i = ((stringArrayListExtra.size() + i2) - 1) / i2;
//        } else {
//            TableRow tableRow = new TableRow(this);
//            Button newLabelButton = newLabelButton("No labels selected", false, R.color.green_100, 12.0f);
//            newLabelButton.setBackgroundResource(R.drawable.contained_button_bg);
//            tableRow.addView(newLabelButton);
//            tableLayout.addView(tableRow);
//            i2 = 1;
//            i = 0;
//        }
//        for (int i3 = 0; i3 < i2; i3++) {
//            tableLayout.setColumnShrinkable(i3, true);
//            tableLayout.setColumnStretchable(i3, true);
//        }
//        for (int i4 = 0; i4 < i; i4++) {
//            TableRow tableRow2 = new TableRow(this);
//            for (int i5 = 0; i5 < i2; i5++) {
//                int i6 = (i4 * i2) + i5;
//                if (i6 <= stringArrayListExtra.size() - 1) {
//                    Button newLabelButton2 = newLabelButton((String) stringArrayListExtra.get(i6), true, R.color.quantum_white_100, 15.0f);
//                    this.buttons.add(newLabelButton2);
//                    tableRow2.addView(newLabelButton2);
//                }
//            }
//            tableLayout.addView(tableRow2);
//        }
//    }

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
//    private void refreshLabelButtons() {
//        ArrayList stringArrayListExtra = getIntent().getStringArrayListExtra(MainPage.LABELS_KEY);
//        for (int i = 0; i < stringArrayListExtra.size(); i++) {
//            String str = (String) stringArrayListExtra.get(i);
//            Button button = (Button) this.buttons.get(i);
//            button.setBackground(Utils.makeSelector(this));
//            if (str.equals(this.currentLabel)) {
//                button.setBackground(getResources().getDrawable(R.drawable.label_button_red));
//            }
//        }
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
//    private Button newLabelButton(String str, boolean z, int i, float f) {
//        Button button = new Button(this);
//        button.setText(str);
//        button.setTextColor(getResources().getColor(i));
//        button.setTextSize(f);
//        button.setEnabled(z);
//        if (!z) {
//            return button;
//        }
//        button.setBackground(Utils.makeSelector(this));
//        if (str.equals(this.currentLabel)) {
//            button.setBackground(getResources().getDrawable(R.drawable.label_button_red));
//        }
//        button.setOnClickListener(new View.OnClickListener() {
//            public final void onClick(View view) {
//                Toast.makeText(RecordSensorData.this, "Press for 1 second to select", Toast.LENGTH_SHORT).show();
//            }
//        });
//        button.setOnLongClickListener(new OnLongClickListener() {
//            public final boolean onLongClick(View view) {
//                return RecordSensorData.lambda$newLabelButton$10(RecordSensorData.this, view);
//            }
//        });
//        return button;
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
//    public static /* synthetic */ boolean lambda$newLabelButton$10(RecordSensorData RecordSensorData, View view) {
//        RecordSensorData.currentLabel = ((Button) view).getText().toString();
//        RecordSensorData.refreshLabelButtons();
//        return true;
//    }

    private String sensorEventToString(SensorEvent sensorEvent, String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.currentTimeMillis());
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
                bBackward.setBackgroundColor(getResources().getColor(R.color.green_400));
                break;
            // Do something
        }
    }
}
