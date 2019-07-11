package edu.tacc.aws_upload;

import android.app.Service;
import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;


public class AWSUploader extends Service {

    private static AmazonS3Client sS3Client;
    private static AWSCredentialsProvider sMobileClient;
    private static TransferUtility transferUtility;
    private static String identityId;
    private Context curContext;
    CognitoCachingCredentialsProvider credentialsProvider;

    final static String INTENT_KEY_NAME = "key";
    final static String INTENT_FILE = "file";
    final static String INTENT_TRANSFER_OPERATION = "transferOperation";
    final static String TRANSFER_OPERATION_UPLOAD = "upload";
    final static String TRANSFER_OPERATION_DOWNLOAD = "download";

    private final static String TAG = AWSUploader.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String key = intent.getStringExtra(INTENT_KEY_NAME);
        final File file = (File) intent.getSerializableExtra(INTENT_FILE);
        final String transferOperation = intent.getStringExtra(INTENT_TRANSFER_OPERATION);
        TransferObserver transferObserver;

        switch (transferOperation) {
            case TRANSFER_OPERATION_DOWNLOAD:
                //Log.d(TAG, "Downloading " + key);
                //transferObserver = transferUtility.download(key, file);
                //transferObserver.setTransferListener(new DownloadListener());
                break;
            case TRANSFER_OPERATION_UPLOAD:
                Log.d(TAG, "Uploading " + key);
                transferObserver = transferUtility.upload("protected/"+identityId+"/"+key, file);
                transferObserver.setTransferListener(new UploadListener());
                break;


        }

        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class UploadListener implements TransferListener {

        private boolean notifyUploadActivityNeeded = true;

        @Override
        public void onStateChanged(int id, TransferState state) {
            if (TransferState.COMPLETED == state) {
                // Handle a completed upload.
                TransferObserver test = transferUtility.getTransferById(id);
                System.out.println("TESTING: " +test.getKey());

                //Log.d("UPLOAD_SUCCESS", id + ":" + path);
            }
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
            int percentDone = (int)percentDonef;

            Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                    + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
        }

        @Override
        public void onError(int id, Exception ex) {
            // Handle errors
        }
    }
}
