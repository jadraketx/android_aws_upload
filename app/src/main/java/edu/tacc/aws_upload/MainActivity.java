package edu.tacc.aws_upload;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;


import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;

import static java.lang.String.valueOf;


public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private TextView textView;
    //Sensor manager
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private static Double pointCounter;

    //File stuff
    private FileOutputStream tempOutFile;
    private String tempFileName = "";
    private OutputStreamWriter outputStreamWriter;
    private int fileCounter = 0;
    private EditText notes;

    //Graph
    GraphView graph;
    LineGraphSeries<DataPoint> series;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.tv_steps);
        Button start = findViewById(R.id.btn_start);
        Button stop = findViewById(R.id.btn_stop);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //graph test
        graph = (GraphView) findViewById(R.id.graph);
        series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 0)
        });
        graph.addSeries(series);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);

        //graph.getViewport().setYAxisBoundsManual(true);
        //graph.getViewport().setMinY(-4);
        //graph.getViewport().setMaxY(4);


    }

    public void onClickUpload(View view) {

        Intent intent = new Intent(this, UploadActivity.class);
        startActivity(intent);
        String tempOut = "Hello world. Nice to see you";

        /*
        //Upload
        String filesDir = this.getFilesDir().toString();
        String filePath = filesDir + "/config.txt";
        File tempFile = new File(filePath);

        if(tempFile.exists()){
            //continue upload
            Context context = getApplicationContext();
            Intent intent = new Intent(context, AWSUploader.class);
            intent.putExtra(AWSUploader.INTENT_KEY_NAME, tempFile.getName());
            intent.putExtra(AWSUploader.INTENT_TRANSFER_OPERATION, AWSUploader.TRANSFER_OPERATION_UPLOAD);
            intent.putExtra(AWSUploader.INTENT_FILE, tempFile);
            context.startService(intent);
        } else {
            Log.i("NO_FILE","File does not exist");
        }*/

    }



    @Override
    protected void onResume(){
        super.onResume();
        //getApplicationContext().startService(new Intent(getApplicationContext(), TransferService.class));
        pointCounter = 1d;


    }


    //For the accelerometer
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;

        }
        pointCounter += 1d;
        System.out.print("Timestamp");
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        // Task task = ActivityRecognition.getClient(context).requestActivityTransitionUpdates(request, pendingIntent);
        Long t1 = System.currentTimeMillis();
        Timestamp timestamp = new Timestamp(t1);
        System.out.print(timestamp.getTime());
        StringBuilder total = new StringBuilder();
        total.append(timestamp).append(",");
        total.append(t1).append(",");
        total.append(x).append(",");
        total.append(y).append(",");
        total.append(z).append("\n");
        String temp = valueOf(total);
//        System.out.print(total.toString());
//        System.out.print(textView.getId());
        textView.setText(total.toString());
        try {
            //out.append(String.valueOf(total));
            outputStreamWriter.write(temp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //add to plot
        series.appendData(
                new DataPoint(pointCounter, y), true, 40
        );


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //For the buttons
    @Override
    public void onClick(View view) {
        Button btn_start = (Button) findViewById(R.id.btn_start);
        Button btn_stop = (Button) findViewById(R.id.btn_stop);
        RadioButton running= (RadioButton) findViewById(R.id.btn_running);
        RadioButton walking = (RadioButton) findViewById(R.id.btn_walking);
        RadioButton sitting = (RadioButton) findViewById(R.id.btn_sitting);
        switch (view.getId()) {
            case R.id.btn_start:
                //EditText notes = (EditText) findViewById(R.id.notes);
                //String strValue = notes.getText().toString();
                pointCounter = 1d;

                EditText notes = (EditText) findViewById(R.id.notes);
                String strValue = notes.getText().toString();

                btn_start.setEnabled(false);
                running.setClickable(false);
                walking.setClickable(false);
                sitting.setClickable(false);
                try {
                    tempOutFile = getApplicationContext().openFileOutput(tempFileName, Context.MODE_PRIVATE);
                    outputStreamWriter = new OutputStreamWriter(tempOutFile);

                    String noteString = "NOTES: ";
                    if(strValue != "") {noteString += strValue;}
                    outputStreamWriter.write(noteString + "\n");

                    outputStreamWriter.write("Timestamp,Epoch,X,Y,Z \n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
                break;
            case R.id.btn_stop:
                sensorManager.unregisterListener(this);
                btn_start.setEnabled(true);
                running.setClickable(true);
                walking.setClickable(true);
                sitting.setClickable(true);
                try {
                    outputStreamWriter.close();
                    fileCounter++;
                } catch (IOException e) {
                    e.printStackTrace();
                }

                series.resetData(new DataPoint[]{
                            new DataPoint(0, 0)
                        });
                pointCounter = 1d;

                break;
        }
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        Long currentTime = System.currentTimeMillis();
        switch(view.getId()) {
            case R.id.btn_running:
                if (checked)
                    System.out.println("RUNNING CHECKED");
                tempFileName = "running_" + Integer.toString(fileCounter) + "_" + Long.toString(currentTime) +".txt";

                break;
            case R.id.btn_walking:
                if (checked)
                    System.out.println("WALKING CHECKED");
                tempFileName = "walking_" + Integer.toString(fileCounter)+ "_" + Long.toString(currentTime) + ".txt";
                break;

            case R.id.btn_sitting:
                if (checked)
                    System.out.println("SITTING CHECKED");
                tempFileName = "sitting_" + Integer.toString(fileCounter) + "_" + Long.toString(currentTime) + ".txt";
                break;



        }
    }
}

    /*private void writeToFile(String data, Context context) {
        for(int i = 0; i < 10; i++) {
            data = data + " " + Integer.toString(i);
            String fileName = "config_" + Integer.toString(i) + ".txt";

            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE));
                outputStreamWriter.write(data);
                outputStreamWriter.close();
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
    }*/