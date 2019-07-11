package edu.tacc.aws_upload;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;


public class UploadActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private LinearLayout ll;
    private static TextView logView;
    private CheckBox[] fileListBox;
    private ArrayList<String> checkedFiles;

    private static final String TAG = "UploadActivity";
    private static AmazonS3Client sS3Client;
    private static AWSCredentialsProvider sMobileClient;
    private static TransferUtility transferUtility;
    private static String identityId;
    private Context curContext;
    CognitoCachingCredentialsProvider credentialsProvider;
    private TransferObserver[] observers;

    private static HashMap<Integer,String> uploadIDs;
    private static int fileCounter;
    private static int numFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ViewGroup layout = (ViewGroup) findViewById(R.id.layout_id);

        ScrollView sv = new ScrollView(this);
        ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        sv.addView(ll);


        layout.addView(sv);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadCheckedFiles();
            }
        });

        init();
    }

    @Override
    public void onResume(){
        super.onResume();



        File dir = this.getFilesDir();
        File[] files = dir.listFiles();
        fileListBox = new CheckBox[files.length];
        checkedFiles = new ArrayList<String>();
        fileCounter = 0;
        numFiles = 0;

        logView = new TextView(this);
        logView.setMovementMethod(new ScrollingMovementMethod());
        logView.setText("LOG\n");

        for(int i = 0; i < files.length; i++){
            CheckBox ch = new CheckBox(this);
            ch.setText(files[i].getName());
            ll.addView(ch);
            ch.setOnCheckedChangeListener(this);

            fileListBox[i] = ch;
        }

        Button delButton = new Button(this);
        delButton.setText("Delete");
        delButton.setOnClickListener(this);
        ll.addView(delButton);
        ll.addView(logView);

    }




    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        int clickedID = buttonView.getId();
        String clickedText = (String) buttonView.getText();

        for(int i = 0; i < fileListBox.length; i++) {
            CheckBox ch = fileListBox[i];
            String compText = (String) ch.getText();
            if(compText == clickedText){
                if(isChecked){
                    checkedFiles.add(compText);
                } else {
                    checkedFiles.remove(compText);
                }
            }
        }

        for(int i = 0; i < checkedFiles.size(); i++){
            System.out.println(checkedFiles.get(i));
        }

    }

    private void uploadCheckedFiles(){
        //Upload
        String filesDir = this.getFilesDir().toString();
        Context context = getApplicationContext();
        observers = new TransferObserver[checkedFiles.size()];
        uploadIDs = new HashMap<Integer, String>();

        numFiles = checkedFiles.size();

        for(int i = 0; i < numFiles; i++) {
            String tempFileName = checkedFiles.get(i);
            String filePath = filesDir + "/" + tempFileName;
            File tempFile = new File(filePath);

            if (tempFile.exists()) {

                TransferObserver observer = transferUtility.upload("protected/"+identityId+"/"+tempFile.getName(), tempFile);
                observers[i] = observer;
                observers[i].setTransferListener(new UploadListener());
                uploadIDs.put(observer.getId(), filePath);
                System.out.println("TEST 1: " + observer.getId() + " " + filePath);

                //background upload
                /*Intent intent = new Intent(context, AWSUploader.class);
                intent.putExtra(AWSUploader.INTENT_KEY_NAME, tempFile.getName());
                intent.putExtra(AWSUploader.INTENT_TRANSFER_OPERATION, AWSUploader.TRANSFER_OPERATION_UPLOAD);
                intent.putExtra(AWSUploader.INTENT_FILE, tempFile);
                context.startService(intent);*/

            } else {
                Log.i("NO_FILE", "File does not exist");
            }
        }

    }



    class UploadListener implements TransferListener {

        // Simply updates the UI list when notified.
        @Override
        public void onError(int id, Exception e) {
            Log.e(TAG, "Error during upload: " + id, e);

            String tempFilePath = uploadIDs.get(id);
            String[] temp = tempFilePath.split("/");
            String fileNameSlim = temp[temp.length-1];
            logView.append("Error uploading " + fileNameSlim +"\n");
            fileCounter++;

        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d(TAG, String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));

        }

        @Override
        public void onStateChanged(int id, TransferState newState) {
            Log.d(TAG, "onStateChanged: " + id + ", " + newState);

            if(TransferState.COMPLETED == newState) {
                String tempFilePath = uploadIDs.get(id);
                Log.d("UPLOAD_COMPLETE", "id: " + id + " file: " + tempFilePath);

                String[] temp = tempFilePath.split("/");
                String fileNameSlim = temp[temp.length-1];
                logView.append("Uploaded " + fileNameSlim +"\n");

                //Transfer completed, lets delete the file in storage
                delete_file(tempFilePath);

                //fileCounter++;
                //if(fileCounter >= numFiles){
                //    reloadActivity();
                //}

            }

        }
    }

    @Override
    public void onClick(View v) {

        String filesDir = this.getFilesDir().toString();
        for(int i = 0; i < checkedFiles.size(); i++) {
            String tempFileName = checkedFiles.get(i);
            String filePath = filesDir + "/" + tempFileName;
            delete_file(filePath);
        }
        reloadActivity();

    }

    public void reloadActivity(){

        finish();
        startActivity(getIntent());
    }

    public static void delete_file(String filePath){
        File tempFile = new File(filePath);
        if(tempFile.exists()){
            Log.d("DEL_FILE","Deleting " + filePath);
            tempFile.delete();
            logView.append("Deleted " + tempFile.getName() +"\n");
        }
    }


    public void init(){
        curContext = getApplicationContext();
        sMobileClient = AWSMobileClient.getInstance();
        AWSConfiguration test = ((AWSMobileClient) sMobileClient).getConfiguration();

        transferUtility =
                TransferUtility.builder()
                        .context(curContext.getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance()))
                        .build();

        // Initialize the Amazon Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:aa35e271-b3af-41b1-a967-788230737854", // Identity pool ID
                Regions.US_EAST_1 // Region
        );
        identityId = credentialsProvider.getIdentityId();
        Log.d("LogTag", "my ID is " + identityId);
    }
}
