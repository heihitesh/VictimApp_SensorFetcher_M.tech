package com.itshiteshverma.sensordatafinal.ui.home;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.itshiteshverma.sensordatafinal.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class VisualizeActivity extends AppCompatActivity {

    ArrayList<Float[]> dataset;
    ArrayList<String> labels;
    LineChart lineChart;
    ArrayList<Float> timestamps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualize);
        lineChart = (LineChart) findViewById(R.id.lineChart);
        lineChart.animateX(3000); // animate horizontal and vertical 3000 milliseconds

        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute();
    }


    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

        private String resp;
        ProgressDialog progressDialog;

        @Override
        protected String doInBackground(String... params) {
            publishProgress("Sleeping..."); // Calls onProgressUpdate()
            try {
                Object[] parseFile = VisualizeActivity.this.parseFile(getIntent().getStringExtra("filePath"));
                VisualizeActivity.this.timestamps = (ArrayList) parseFile[0];
                VisualizeActivity.this.dataset = (ArrayList) parseFile[1];
                VisualizeActivity.this.labels = (ArrayList) parseFile[2];
                VisualizeActivity.this.runOnUiThread(new Runnable() {
                    public final void run() {
                        VisualizeActivity.this.plotLines();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }
            return resp;
        }


        @Override
        protected void onPostExecute(String result) {
            // execution of result of Long time consuming operation
            progressDialog.dismiss();
        }


        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(VisualizeActivity.this,
                    "Loading", "Rendering Graph. Please Wait");
        }


        @Override
        protected void onProgressUpdate(String... text) {
//            finalResult.setText(text[0]);

        }
    }


    public void plotLines() {
        if (!this.dataset.isEmpty()) {
            int length = ((Float[]) this.dataset.get(0)).length;
            LineData lineData = new LineData();
            for (int i = 0; i < length; i++) {
                ArrayList arrayList = new ArrayList();
                for (int i2 = 0; i2 < this.dataset.size(); i2++) {
                    arrayList.add(new Entry((float) i2, ((Float[]) this.dataset.get(i2))[i].floatValue()));
                }
                LineDataSet lineDataSet = new LineDataSet(arrayList, Character.toString(Utils.chartLabels.charAt(i)));
                lineDataSet.setColor(Utils.chartColors[i]);
                lineDataSet.setDrawCircles(false);
                lineDataSet.setHighlightEnabled(false);
                lineData.addDataSet(lineDataSet);
            }
            lineData.notifyDataChanged();
            this.lineChart.setData(lineData);
            this.lineChart.getDescription().setTextSize(15.0f);
            this.lineChart.notifyDataSetChanged();
            this.lineChart.invalidate();
        }
    }

    private String getSeparator(String str) {
        return str.split(",").length > str.split(";").length ? "," : ";";
    }

    private Object[] parseFile(String str) throws IOException {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        File file = new File(str);
        if (file.exists()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String readLine = bufferedReader.readLine();
            String separator = getSeparator(readLine);
            while (readLine != null) {
                Object[] parseLine = parseLine(readLine, separator);
                Float[] fArr = (Float[]) parseLine[0];
                String str2 = (String) parseLine[1];
                arrayList.add(fArr[0]);
                arrayList2.add(Arrays.copyOfRange(fArr, 1, fArr.length));
                if (!str2.isEmpty()) {
                    arrayList3.add(str2);
                }
                readLine = bufferedReader.readLine();
            }
        }
        return new Object[]{arrayList, arrayList2, arrayList3};
    }

    private Object[] parseLine(String str, String str2) {
        String[] split;
        String replaceAll = str.replaceAll("\\n", "").replaceAll("\\r", "");
        ArrayList arrayList = new ArrayList();
        String str3 = "";
        for (String str4 : replaceAll.split(str2)) {
            try {
                arrayList.add(Float.valueOf(Float.parseFloat(str4)));
            } catch (NumberFormatException unused) {
                str3 = str4;
            }
        }
        return new Object[]{(Float[]) arrayList.toArray(new Float[arrayList.size()]), str3};
    }
}
