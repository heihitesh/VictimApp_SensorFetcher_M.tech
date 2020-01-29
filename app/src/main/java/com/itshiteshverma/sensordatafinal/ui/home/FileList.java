package com.itshiteshverma.sensordatafinal.ui.home;


import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.itshiteshverma.sensordatafinal.R;

import java.io.File;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class FileList extends Fragment {

    View view;
    static final String PREFIX = "     - ";
    static final String VIZ_FILE_KEY = "filePath";
    ArrayList<String> listFileNames = new ArrayList<>();
    ArrayList<File> listFiles = new ArrayList<>();
    ListView listView;
    LinearLayout llExists;

    public FileList() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_list_file, container, false);
        llExists = view.findViewById(R.id.llExistLayout);
        listView = view.findViewById(R.id.listView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        listFileNames.clear(); //Clearing any Left Data
        listFiles.clear();

        String appFolder_Name = getActivity().getString(R.string.app_name);
        File appFolderLocation = new File(Environment.getExternalStorageDirectory(), appFolder_Name);
        if (!appFolderLocation.exists()) {
            appFolderLocation.mkdirs();
        }

        for (File file : new File(Utils.APP_STORAGE_DIR).listFiles()) {
            this.listFileNames.add(file.getName().toUpperCase());
            this.listFiles.add(file);
            if (file.isDirectory()) {
                for (File file2 : file.listFiles()) {
                    ArrayList<String> arrayList = this.listFileNames;
                    StringBuilder sb = new StringBuilder();
                    sb.append(PREFIX);
                    sb.append(file2.getName());
                    arrayList.add(sb.toString());
                    this.listFiles.add(file2);
                }
            }
        }

        listView.setAdapter(new ArrayAdapter(getActivity(), R.layout.activity_file_list_row, R.id.RowTextView, listFileNames));
        onListItemClick();

    }

    private void onListItemClick() {
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                Intent intent = new Intent(getActivity(), VisualizeActivity.class);
                String filePath = "filePath";
                File file = listFiles.get(i);
                intent.putExtra(filePath, file.getAbsolutePath());
                startActivity(intent);
            }
        });
    }
}
