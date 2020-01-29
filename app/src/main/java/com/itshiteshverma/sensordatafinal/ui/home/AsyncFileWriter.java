package com.itshiteshverma.sensordatafinal.ui.home;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

class AsyncFileWriter extends AsyncTask<Void, Void, Void> {
    private boolean keepRunning = true;
    private ConcurrentLinkedQueue<Object[]> queue;
    private int sleepDuration;

    AsyncFileWriter(int i, ConcurrentLinkedQueue<Object[]> concurrentLinkedQueue) {
        this.sleepDuration = i;
        this.queue = concurrentLinkedQueue;
    }

    /* access modifiers changed from: protected */
    public Void doInBackground(Void... voidArr) {
        while (this.keepRunning) {
            try {
                Thread.sleep((long) this.sleepDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (!this.queue.isEmpty()) {
                Object[] objArr = this.queue.remove();
                appendToFile((String) objArr[0], (File) objArr[1]);
            }
        }
        return null;
    }

    private void appendToFile(String str, File file) {
        try {
            //Time,xAxis,yAxis,ZAxis,Current Label
//            String[] arrSplit = str.split(VALUES_SEPARATOR);
//            Note note;
//            note = new Note(Long.parseLong(arrSplit[0]), Float.parseFloat(arrSplit[1]), Float.parseFloat(arrSplit[2])
//                    , Float.parseFloat(arrSplit[3]), stringDataSetFileName, arrSplit[4]);
//
//            dbHelper.setValue(note);
            Log.d("HIT_TAG", str);

            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            bufferedWriter.append(str);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    /* access modifiers changed from: 0000 */
    public void stop() {
        this.keepRunning = false;
    }
}
