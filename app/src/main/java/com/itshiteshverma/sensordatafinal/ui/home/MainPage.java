package com.itshiteshverma.sensordatafinal.ui.home;


import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itshiteshverma.sensordatafinal.R;
import com.shawnlin.numberpicker.NumberPicker;

import org.zeroturnaround.zip.commons.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.itshiteshverma.sensordatafinal.MainActivity.toastHelper;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainPage extends Fragment {

    View view;
    FloatingActionButton fabAddSensor;
    LinearLayout listOfSensorsView;
    TextView displaySensor;
    Button bStart;
    EditText etFileName;

    public static final String DURATION_KEY = "duration";
    public static final String FILE_NAME_KEY = "fileName";
    public static final String LABELS_KEY = "labels";
    public static final String SENSOR_DELAY_KEY = "sensorDelay";
    public static final String SENSOR_TYPES_KEY = "sensorTypes";
    protected long durationMillis;
    protected String fileName;
    ArrayList<String> selectedLabels = new ArrayList<>();
    ArrayList<Integer> selectedSensorTypes = new ArrayList<>();
    protected int sensorDelay;
    RadioGroup radioGroupFrequency;
    LinearLayout llMainLayout;
    NumberPicker hourNumberPicker, minutesNumberPicker;

    public MainPage() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_main_details, container, false);
        fabAddSensor = view.findViewById(R.id.selectSensorsBtn);
        displaySensor = view.findViewById(R.id.displaySensorsTextView);
        bStart = view.findViewById(R.id.saveBtn);
        etFileName = view.findViewById(R.id.dbNameEditText);
        radioGroupFrequency = view.findViewById(R.id.radioGroupFrequency);
        llMainLayout = view.findViewById(R.id.llMainLayout);
        hourNumberPicker = view.findViewById(R.id.hour_picker);
        minutesNumberPicker = view.findViewById(R.id.minute_picker);

        initilize();
        return view;
    }

    private void initilize() {

        fabAddSensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(getActivity());
                dialog.requestWindowFeature(1);
                dialog.setContentView(R.layout.dialog_about_dark);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                dialog.setCancelable(true);

                listOfSensorsView = dialog.findViewById(R.id.listOfSensorsView);
                Button okBtn = dialog.findViewById(R.id.okSensorsSelectionBtn);

                final List availableSensors = Utils.getAvailableSensors(getActivity());
                final ArrayList<CheckBox> arrayList = createSensorsCheckBoxes(availableSensors);

                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ArrayList<Integer> arrayList2 = new ArrayList<>();
                        for (int i = 0; i < arrayList.size(); i++) {
                            if (arrayList.get(i).isChecked()) {
                                arrayList2.add(Integer.valueOf(((Sensor) availableSensors.get(i)).getType()));
                            }
                        }
                        if (arrayList2.isEmpty()) {
                            toastHelper.toastIconError("Select At Least One Sensor");
                            return;
                        }
                        selectedSensorTypes = arrayList2;
                        displaySensor.setText(TextUtils.join(IOUtils.LINE_SEPARATOR_UNIX, Utils.sensorNamesFromTypes(getActivity(), arrayList2)));
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });


        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedFrequency = radioGroupFrequency.getCheckedRadioButtonId();
                RadioButton radioFrequncy = view.findViewById(selectedFrequency);
                getSensorDelay(radioFrequncy.getText().toString().trim());

                if ((MainPage.this.setFileName() & MainPage.this.setDuration()) && MainPage.this.checkSelectedSensorTypes()) {
                    Intent intent = new Intent(getActivity(), RecordSensorData.class);
                    intent.putExtra(FILE_NAME_KEY, MainPage.this.fileName);
                    intent.putExtra(DURATION_KEY, MainPage.this.durationMillis);
                    intent.putExtra(SENSOR_DELAY_KEY, MainPage.this.sensorDelay);
                    intent.putExtra(LABELS_KEY, MainPage.this.selectedLabels);
                    intent.putExtra(SENSOR_TYPES_KEY, MainPage.this.selectedSensorTypes);
                    startActivity(intent);
                } else {
                    toastHelper.toastIconError("Add a Sensor.");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        etFileName.setText(Utils.generateNonExistingFileName(Utils.APP_STORAGE_DIR, "dataset"));
        displaySensor.setText("No Sensor has been Selected");
        selectedSensorTypes.clear();
    }

    /* access modifiers changed from: protected */
    public ArrayList<CheckBox> createSensorsCheckBoxes(List<Sensor> list) {
        ArrayList<CheckBox> arrayList = new ArrayList<>();
        for (Sensor sensor : list) {
            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setText(sensor.getName());
            arrayList.add(checkBox);
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            this.listOfSensorsView.addView((CheckBox) it.next());
        }
        return arrayList;
    }

    /* access modifiers changed from: protected */
    public boolean setFileName() {
        this.fileName = etFileName.getText().toString();
        if (this.fileName.isEmpty()) {
            this.etFileName.setError("File name is required.");
            return false;
        } else if (!new File(Utils.APP_STORAGE_DIR, this.fileName).exists()) {
            return true;
        } else {
            this.etFileName.setError("File name already exists, choose another name or delete the existing file.");
            return false;
        }
    }

    /* access modifiers changed from: protected */
    public boolean setDuration() {
//        this.durationMillis = (long) (Float.parseFloat(obj) * 60.0f * 60.0f * 1000.0f);
        float tempHours = hourNumberPicker.getValue() * 60.0f * 60.0f * 1000.0f;
        float tempMinutes = minutesNumberPicker.getValue() * 60.0f * 1000.0f;
        this.durationMillis = (long) (tempHours + tempMinutes);
        if (this.durationMillis > 0) {
            return true;
        }
        toastHelper.toastIconError("Duration should be higher than 0 hours.");
        return false;
    }

    private boolean checkSelectedSensorTypes() {
        if (!this.selectedSensorTypes.isEmpty()) {
            return true;
        }
        toastHelper.toastIconError("At least one sensor should be Added");
        return false;
    }

    public void getSensorDelay(String delay) {
        if (delay.equals("Very Fast")) {
            MainPage.this.sensorDelay = 0;
            return;
        }
        if (delay.equals("Fast")) {
            MainPage.this.sensorDelay = 1;
            return;
        }
        if (delay.equals("Medium")) {
            MainPage.this.sensorDelay = 2;
            return;
        }
        if (delay.equals("Slow")) {
            MainPage.this.sensorDelay = 3;
            return;
        } else {
            MainPage.this.sensorDelay = 1; //Default Case
            return;
        }
    }

}
