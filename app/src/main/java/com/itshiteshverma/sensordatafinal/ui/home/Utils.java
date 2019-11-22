package com.itshiteshverma.sensordatafinal.ui.home;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.StateListDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.internal.view.SupportMenu;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.ViewCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.itshiteshverma.sensordatafinal.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class Utils {
    static final String APP_STORAGE_DIR;
    static final boolean DISABLE_ADMOB = false;
    static final int LABELS_ACTIVITY_RESULT_CODE = 100;
    static final int SENSORS_ACTIVITY_RESULT_CODE = 200;
    static int[] chartColors = {SupportMenu.CATEGORY_MASK, -16711936, -16776961, ViewCompat.MEASURED_STATE_MASK, -16711681, -7829368, -12303292, -65281, -3355444, InputDeviceCompat.SOURCE_ANY, SupportMenu.CATEGORY_MASK, -16711936, -16776961, ViewCompat.MEASURED_STATE_MASK, -16711681, -7829368, -12303292, -65281, -3355444, InputDeviceCompat.SOURCE_ANY, SupportMenu.CATEGORY_MASK, -16711936, -16776961, ViewCompat.MEASURED_STATE_MASK, -16711681, -7829368, -12303292, -65281, -3355444, InputDeviceCompat.SOURCE_ANY};
    static String chartLabels = "XYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVW";

    static int getSensorDimension(int i) {
        switch (i) {
            case 1:
                return 3;
            case 2:
                return 3;
            case 3:
                return 3;
            case 4:
                return 3;
            case 5:
                return 1;
            case 6:
                return 1;
            case 7:
                return 1;
            case 8:
                return 1;
            case 9:
                return 3;
            case 10:
                return 3;
            case 11:
                return 5;
            case 12:
                return 1;
            case 13:
                return 1;
            case 14:
                return 6;
            case 15:
                return 5;
            case 16:
                return 6;
            case 17:
                return 1;
            case 18:
                return 1;
            case 19:
                return 1;
            case 20:
                return 5;
            case 21:
                return 1;
            default:
                switch (i) {
                    case 28:
                        return 15;
                    case 29:
                        return 1;
                    case 30:
                        return 1;
                    case 31:
                        return 1;
                    default:
                        switch (i) {
                            case 34:
                                return 1;
                            case 35:
                                return 6;
                            default:
                                return 3;
                        }
                }
        }
    }

    Utils() {
    }

    static {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory());
        sb.append(File.separator);
        sb.append("SensorsDataCollector");
        APP_STORAGE_DIR = sb.toString();
    }

    private static boolean shouldShowRationale(Context context, String... strArr) {
        for (String shouldShowRequestPermissionRationale : strArr) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, shouldShowRequestPermissionRationale)) {
                return true;
            }
        }
        return false;
    }


    static StateListDrawable makeSelector(Context context) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.setExitFadeDuration(400);
        stateListDrawable.addState(new int[]{16842919}, context.getResources().getDrawable(R.drawable.label_button_orange));
        stateListDrawable.addState(new int[0], context.getResources().getDrawable(R.drawable.label_button_blue));
        return stateListDrawable;
    }


    static boolean notHavePermissions(Context context, String... strArr) {
        for (String checkSelfPermission : strArr) {
            if (ActivityCompat.checkSelfPermission(context, checkSelfPermission) != 0) {
                return true;
            }
        }
        return false;
    }

    static ArrayList<String> sensorNamesFromTypes(Context context, ArrayList<Integer> arrayList) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        ArrayList<String> arrayList2 = new ArrayList<>();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(sensorManager.getDefaultSensor(((Integer) it.next()).intValue()).getName());
        }
        return arrayList2;
    }


    static ViewGroup getParentView(View view) {
        return (ViewGroup) view.getParent();
    }

    static void removeView(View view) {
        ViewGroup parentView = getParentView(view);
        if (parentView != null) {
            parentView.removeView(view);
        }
    }

    static void replaceView(View view, View view2) {
        ViewGroup parentView = getParentView(view);
        if (parentView != null) {
            int indexOfChild = parentView.indexOfChild(view);
            removeView(view);
            removeView(view2);
            parentView.addView(view2, indexOfChild);
        }
    }

    static void initCurves(LineChart lineChart, int i) {
        LineData lineData = new LineData();
        for (int i2 = 0; i2 < i; i2++) {
            ArrayList arrayList = new ArrayList();
            arrayList.add(new Entry(0.0f, 0.0f));
            LineDataSet lineDataSet = new LineDataSet(arrayList, Character.toString(chartLabels.charAt(i2)));
            lineDataSet.setColor(chartColors[i2]);
            lineDataSet.setDrawCircles(false);
            lineData.addDataSet(lineDataSet);
        }
        lineChart.setData(lineData);
        lineChart.getDescription().setTextSize(15.0f);
        lineChart.invalidate();
    }

    static void updateChart(LineChart lineChart, float[] fArr, long j, long j2) {
        LineData lineData = lineChart.getLineData();
        int min = Math.min(fArr.length, lineData.getDataSetCount());
        for (int i = 0; i < min; i++) {
            LineDataSet lineDataSet = (LineDataSet) lineData.getDataSetByLabel(Character.toString(chartLabels.charAt(i)), true);
            lineDataSet.addEntry(new Entry((float) j, fArr[i]));
            if (j > j2 || j == 1) {
                lineDataSet.removeFirst();
            }
        }
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }

    static void createDirectoryIfNotExist(String str) {
        File file = new File(str);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    static String floatsToString(float[] fArr, String str) {
        StringBuilder sb = new StringBuilder();
        for (float append : fArr) {
            sb.append(append);
            sb.append(str);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    static String doublesToString(double[] dArr, String str) {
        StringBuilder sb = new StringBuilder();
        for (double append : dArr) {
            sb.append(append);
            sb.append(str);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }


    static void nextActivityWithDelay(final Context context, final Class cls, int i) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                Intent intent = new Intent(context, cls);
                ((Activity) context).finish();
                context.startActivity(intent);
            }
        }, (long) i);
    }

    static void finishActivityWithDelay(final Context context, int i) {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                ((Activity) context).finish();
            }
        }, (long) i);
    }

    static String generateNonExistingFileName(String str, String str2) {
        int i = 0;
        do {
            i++;
            StringBuilder sb = new StringBuilder();
            sb.append(str2);
            sb.append(i);
            if (!new File(str, sb.toString()).exists()) {
                break;
            }
        } while (i < 1000);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(str2);
        sb2.append(i);
        return sb2.toString();
    }

    static void disableKeyboardFocus(Activity activity) {
        activity.getWindow().setSoftInputMode(3);
    }

    static List<Sensor> getAvailableSensors(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager != null ? sensorManager.getSensorList(-1) : new ArrayList();
    }

    static void registerSensors(SensorManager sensorManager, SensorEventListener sensorEventListener, ArrayList<Sensor> arrayList, int i) {
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            sensorManager.registerListener(sensorEventListener, (Sensor) it.next(), i);
        }
    }

    static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File deleteRecursive : file.listFiles()) {
                deleteRecursive(deleteRecursive);
            }
        }
        file.delete();
    }
}
