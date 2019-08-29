package com.hbisoft.pickitexample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

public class MainActivity extends AppCompatActivity implements PickiTCallbacks {
    //Permissions
    private static final int SELECT_VIDEO_REQUEST = 777;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;

    //Declare PickiT
    PickiT pickiT;

    //Views
    Button button_pick;
    TextView pickitTv, originalTv, originalTitle, pickitTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        buttonClickEvent();

        //Initialize PickiT
        pickiT = new PickiT(this, this);

    }

    //Show Toast
    private void showLongToast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void init() {
        button_pick = findViewById(R.id.button_pick);
        pickitTv = findViewById(R.id.pickitTv);
        originalTv = findViewById(R.id.originalTv);
        originalTitle = findViewById(R.id.originalTitle);
        pickitTitle = findViewById(R.id.pickitTitle);
    }

    private void buttonClickEvent() {
        button_pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();

                //  Make TextView's invisible
                originalTitle.setVisibility(View.INVISIBLE);
                originalTv.setVisibility(View.INVISIBLE);
                pickitTitle.setVisibility(View.INVISIBLE);
                pickitTv.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void openGallery() {
        //  first check if permissions was granted
        if (checkSelfPermission()) {
            Intent intent;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            } else {
                intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI);
            }
            //  In this example we will set the type to video
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.putExtra("return-data", true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, SELECT_VIDEO_REQUEST);
        }
    }

    //  Check if permissions was granted
    private boolean checkSelfPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, MainActivity.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    //  Handle permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  Permissions was granted, open the gallery
                openGallery();
            }
            //  Permissions was not granted
            else {
                showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_VIDEO_REQUEST) {
            if (resultCode == RESULT_OK) {

                //  Get path from PickiT (The path will be returned in PickiTonCompleteListener)
                //
                //  If the selected file is from Dropbox/Google Drive or OnDrive:
                //  Then it will be "copied" to your app directory (see path example below) and when done the path will be returned in PickiTonCompleteListener
                //  /storage/emulated/0/Android/data/your.package.name/files/Temp/tempDriveFile.mp4
                //
                //  else the path will directly be returned in PickiTonCompleteListener
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);

                originalTv.setText(String.valueOf(data.getData()));

            }
        }
    }

    //
    //  PickiT Listeners
    //
    //  The listeners can be used to display a Dialog when a file is selected from Dropbox/Google Drive or OnDrive.
    //  The listeners are callbacks from an AsyncTask that creates a new File of the original in /storage/emulated/0/Android/data/your.package.name/files/Temp/
    //
    //  PickiTonStartListener()
    //  This will be call once the file creations starts (onPreExecute) and will only be called if the selected file is not local
    //
    //  PickiTonProgressUpdate(int progress)
    //  This will return the progress of the file creation (in percentage) and will only be called if the selected file is not local
    //
    //  PickiTonCompleteListener(String path, boolean wasDriveFile)
    //  If the selected file was from Dropbox/Google Drive or OnDrive, then this will be called after the file was created.
    //  If the selected file was a local file then this will be called directly, returning the path as a String
    //  Additionally, a boolean will be returned letting you know if the file selected was from Dropbox/Google Drive or OnDrive.

    ProgressBar mProgressBar;
    TextView percentText;
    private AlertDialog mdialog;

    @Override
    public void PickiTonStartListener() {
        final AlertDialog.Builder mPro = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
        @SuppressLint("InflateParams") final View mPView = LayoutInflater.from(this).inflate(R.layout.dailog_layout, null);
        percentText = mPView.findViewById(R.id.percentText);

        percentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickiT.cancelTask();
                if (mdialog != null && mdialog.isShowing()) {
                    mdialog.cancel();
                }
            }
        });

        mProgressBar = mPView.findViewById(R.id.mProgressBar);
        mProgressBar.setMax(100);
        mPro.setView(mPView);
        mdialog = mPro.create();
        mdialog.show();

    }

    @Override
    public void PickiTonProgressUpdate(int progress) {
        String progressPlusPercent = progress + "%";
        percentText.setText(progressPlusPercent);
        mProgressBar.setProgress(progress);
    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String reason) {

        if (mdialog != null && mdialog.isShowing()) {
            mdialog.cancel();
        }

        //  Check if it was a Drive/local/unknown provider file and display a Toast
        if (wasDriveFile){
            showLongToast("Drive file was selected");
        }else if (wasUnknownProvider){
            showLongToast("File was selected from unknown provider");
        }else {
            showLongToast("Local file was selected");
        }

        //  Chick if it was successful
        if (wasSuccessful) {
            //  Set returned path to TextView
            pickitTv.setText(path);

            //  Make TextView's visible
            originalTitle.setVisibility(View.VISIBLE);
            originalTv.setVisibility(View.VISIBLE);
            pickitTitle.setVisibility(View.VISIBLE);
            pickitTv.setVisibility(View.VISIBLE);
        }else {
            showLongToast("Error, please see the log..");
            pickitTv.setVisibility(View.VISIBLE);
            pickitTv.setText(reason);
        }
    }


    //
    //  Lifecycle methods
    //

    //  Deleting the temporary file if it exists
    @Override
    public void onBackPressed() {
        pickiT.deleteTemporaryFile();
        super.onBackPressed();
    }

    //  Deleting the temporary file if it exists
    //  As we know, this might not even be called if the system kills the application before onDestroy is called
    //  So, it is best to call pickiT.deleteTemporaryFile(); as soon as you are done with the file
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            pickiT.deleteTemporaryFile();
        }
    }

}
