package com.hbisoft.pickitexample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements PickiTCallbacks {
    //Permissions
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;

    //Declare PickiT
    PickiT pickiT;

    //Views
    Button button_pick_video, button_pick_image;
    TextView pickitTv, originalTv, originalTitle, pickitTitle;
    String videoImageRef = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        buttonClickEvent();

        //Initialize PickiT
        pickiT = new PickiT(this, this, this);

    }

    //Show Toast
    private void showLongToast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void init() {
        button_pick_video = findViewById(R.id.button_pick_video);
        button_pick_image = findViewById(R.id.button_pick_image);
        pickitTv = findViewById(R.id.pickitTv);
        originalTv = findViewById(R.id.originalTv);
        originalTitle = findViewById(R.id.originalTitle);
        pickitTitle = findViewById(R.id.pickitTitle);
    }

    private void buttonClickEvent() {
        button_pick_video.setOnClickListener(view -> {
            videoImageRef = "video";
            openGallery("video");

            //  Make TextView's invisible
            originalTitle.setVisibility(View.INVISIBLE);
            originalTv.setVisibility(View.INVISIBLE);
            pickitTitle.setVisibility(View.INVISIBLE);
            pickitTv.setVisibility(View.INVISIBLE);
        });
        button_pick_image.setOnClickListener(view -> {
            videoImageRef = "image";
            openGallery("image");

            //  Make TextView's invisible
            originalTitle.setVisibility(View.INVISIBLE);
            originalTv.setVisibility(View.INVISIBLE);
            pickitTitle.setVisibility(View.INVISIBLE);
            pickitTv.setVisibility(View.INVISIBLE);
        });
    }

    private void openGallery(String videoOrImage) {
        //  first check if permissions was granted
        if (checkSelfPermission()) {
            if (videoImageRef.equals("video")) {
                videoImageRef = "";
                Intent intent;
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                } else {
                    intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI);
                }
                //  In this example we will set the type to video
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra("return-data", true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activityResultLauncher.launch(intent);
            }else{
                videoImageRef = "";
                Intent intent;
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                } else {
                    intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                }
                //  In this example we will set the type to video
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra("return-data", true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                activityResultLauncher.launch(intent);
            }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  Permissions was granted, open the gallery
                if (videoImageRef.equals("video")) {
                    openGallery("video");
                }else{
                    openGallery("image");
                }
            }
            //  Permissions was not granted
            else {
                showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        //  Get path from PickiT (The path will be returned in PickiTonCompleteListener)
                        //
                        //  If the selected file is from Dropbox/Google Drive or OnDrive:
                        //  Then it will be "copied" to your app directory (see path example below) and when done the path will be returned in PickiTonCompleteListener
                        //  /storage/emulated/0/Android/data/your.package.name/files/Temp/tempDriveFile.mp4
                        //
                        //  else the path will directly be returned in PickiTonCompleteListener

                        ClipData clipData = Objects.requireNonNull(data).getClipData();
                        if (clipData != null) {
                            int numberOfFilesSelected = clipData.getItemCount();
                            if (numberOfFilesSelected > 1) {
                                pickiT.getMultiplePaths(clipData);
                                StringBuilder allPaths = new StringBuilder("Multiple Files Selected:" + "\n");
                                for(int i = 0; i < clipData.getItemCount(); i++) {
                                    allPaths.append("\n\n").append(clipData.getItemAt(i).getUri());
                                }
                                originalTv.setText(allPaths.toString());
                            }else {
                                pickiT.getPath(clipData.getItemAt(0).getUri(), Build.VERSION.SDK_INT);
                                originalTv.setText(String.valueOf(clipData.getItemAt(0).getUri()));
                            }
                        } else {
                            pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
                            originalTv.setText(String.valueOf(data.getData()));
                        }

                    }
                }

            });

    //  PickiT Listeners
    //
    //  The listeners can be used to display a Dialog when a file is selected from Dropbox/Google Drive or OnDrive.
    //  The listeners are callbacks from an AsyncTask that creates a new File of the original in /storage/emulated/0/Android/data/your.package.name/files/Temp/
    //
    //  PickiTonUriReturned()
    //  When selecting a file from Google Drive, for example, the Uri will be returned before the file is available(if it has not yet been cached/downloaded).
    //  Google Drive will first have to download the file before we have access to it.
    //  This can be used to let the user know that we(the application), are waiting for the file to be returned.
    //
    //  PickiTonStartListener()
    //  This will be call once the file creations starts and will only be called if the selected file is not local
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
    ProgressDialog progressBar;

    @Override
    public void PickiTonUriReturned() {
        progressBar = new ProgressDialog(this);
        progressBar.setMessage("Waiting to receive file...");
        progressBar.setCancelable(false);
        progressBar.show();
    }

    @Override
    public void PickiTonStartListener() {
        if (progressBar.isShowing()) {
            progressBar.cancel();
        }
        final AlertDialog.Builder mPro = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
        @SuppressLint("InflateParams") final View mPView = LayoutInflater.from(this).inflate(R.layout.dailog_layout, null);
        percentText = mPView.findViewById(R.id.percentText);

        percentText.setOnClickListener(view -> {
            pickiT.cancelTask();
            if (mdialog != null && mdialog.isShowing()) {
                mdialog.cancel();
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

    @SuppressLint("SetTextI18n")
    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String reason) {
        if (mdialog != null && mdialog.isShowing()) {
            mdialog.cancel();
        }

        //  Check if it was a Drive/local/unknown provider file and display a Toast
        if (wasDriveFile) {
            showLongToast("Drive file was selected");
        } else if (wasUnknownProvider) {
            showLongToast("File was selected from unknown provider");
        } else {
            showLongToast("Local file was selected");
        }

        //  Chick if it was successful
        if (wasSuccessful) {
            //  Set returned path to TextView
            if (path.contains("/proc/")) {
                pickitTv.setText("Sub-directory inside Downloads was selected." + "\n" + " We will be making use of the /proc/ protocol." + "\n" + " You can use this path as you would normally." + "\n\n" + "PickiT path:" + "\n" + path);
            } else {
                pickitTv.setText(path);
            }

            //  Make TextView's visible
            originalTitle.setVisibility(View.VISIBLE);
            originalTv.setVisibility(View.VISIBLE);
            pickitTitle.setVisibility(View.VISIBLE);
            pickitTv.setVisibility(View.VISIBLE);
        } else {
            showLongToast("Error, please see the log..");
            pickitTv.setVisibility(View.VISIBLE);
            pickitTv.setText(reason);
        }
    }

    @Override
    public void PickiTonMultipleCompleteListener(ArrayList<String> paths, boolean wasSuccessful, String Reason) {
        if (mdialog != null && mdialog.isShowing()) {
            mdialog.cancel();
        }
        StringBuilder allPaths = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            allPaths.append("\n").append(paths.get(i)).append("\n");
        }

        //  Set returned path to TextView
        pickitTv.setText(allPaths.toString());

        //  Make TextView's visible
        originalTitle.setVisibility(View.VISIBLE);
        originalTv.setVisibility(View.VISIBLE);
        pickitTitle.setVisibility(View.VISIBLE);
        pickitTv.setVisibility(View.VISIBLE);
    }


    //
    //  Lifecycle methods
    //

    //  Deleting the temporary file if it exists
    @Override
    public void onBackPressed() {
        pickiT.deleteTemporaryFile(this);
        super.onBackPressed();
    }

    //  Deleting the temporary file if it exists
    //  As we know, this might not even be called if the system kills the application before onDestroy is called
    //  So, it is best to call pickiT.deleteTemporaryFile(); as soon as you are done with the file
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            pickiT.deleteTemporaryFile(this);
        }
    }

}
