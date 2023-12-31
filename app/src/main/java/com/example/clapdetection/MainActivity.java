package com.example.clapdetection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.time.Duration;

public class MainActivity extends AppCompatActivity {
    final int RECORD_AUDIO = 0;
    ImageButton  btn_start;
    Spinner spinDuration;
     int clipDuration = 0;
     LongOperation recordAudioSync = null;
     String[] durationItems = {"5", "10", "15", "20"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initHandlers();
        ArrayAdapter<String> spinDurationAdapter    = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, durationItems);
        spinDurationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinDuration.setAdapter((spinDurationAdapter));
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clipDuration = Integer.parseInt(spinDuration.getSelectedItem().toString());
                if(ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
                } else {
                    recordAudioSync = new LongOperation();
                    recordAudioSync.execute("");
                }
            }
        });
    }

    private void initHandlers () {
        btn_start = findViewById(R.id.start);
        spinDuration = findViewById(R.id.duration);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(recordAudioSync!= null && recordAudioSync.getStatus() != AsyncTask.Status.FINISHED) {
            recordAudioSync.done();
            recordAudioSync.cancel(true);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(recordAudioSync!= null && recordAudioSync.getStatus() != AsyncTask.Status.FINISHED) {
            recordAudioSync.done();
            recordAudioSync.cancel(true);

        }
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        MediaRecorder recorder;
        int clapDetectedNumber;

        @Override
        protected String doInBackground(String... strings) {
            recordAudio();
            return "" + clapDetectedNumber;
        }

        @Override
        protected void onPreExecute() {
            btn_start.setImageResource(R.drawable.mic_off);
            btn_start.setEnabled(false);
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(MainActivity.this, "You Clapped: " + s + " times", Toast.LENGTH_SHORT).show();
        }

        private void recordAudio() {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile("/data/data" + getPackageName() + "/music.3gp");
            int startAmplitude = 0;
            int finishAmplitude;
            int amplitudeThreshold = 18000;
            int counter = 0;
            try{
                recorder.prepare();

                recorder.start();
                startAmplitude = recorder.getMaxAmplitude();

            } catch(IOException e) {
                e.printStackTrace();
            }
            do{
                if(isCancelled()) {
                    break;
                }
                counter++;
                waitSome();
                finishAmplitude = recorder.getMaxAmplitude();
                if(finishAmplitude >= amplitudeThreshold) {
                    clapDetectedNumber ++;
                    Toast.makeText(MainActivity.this, "Clap detected!", Toast.LENGTH_SHORT).show();
                }
            } while(counter < (clipDuration * 4));
            done();
        }

        private void done() {
            if(recorder != null) {
                recorder.stop();
                recorder.release();
            }
        }

        private void waitSome() {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}