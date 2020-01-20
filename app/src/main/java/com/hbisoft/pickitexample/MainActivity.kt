package com.hbisoft.pickitexample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallback
import com.hbisoft.pickit.PickiTProvider
import com.hbisoft.pickit.PickiTStatus

class MainActivity : AppCompatActivity() {
    //Declare PickiT
    var pickiT = PickiT()
    //Views
    lateinit var pickButton: Button
    lateinit var pickitTv: TextView
    lateinit var originalTv: TextView
    lateinit var originalTitle: TextView
    lateinit var pickitTitle: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        buttonClickEvent()
    }

    //Show Toast
    private fun showLongToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    private fun init() {
        pickButton = findViewById(R.id.button_pick)
        pickitTv = findViewById(R.id.pickitTv)
        originalTv = findViewById(R.id.originalTv)
        originalTitle = findViewById(R.id.originalTitle)
        pickitTitle = findViewById(R.id.pickitTitle)
    }

    private fun buttonClickEvent() {
        pickButton.setOnClickListener {
            openGallery()
            //  Make TextView's invisible
            originalTitle.visibility = View.INVISIBLE
            originalTv.visibility = View.INVISIBLE
            pickitTitle.visibility = View.INVISIBLE
            pickitTv.visibility = View.INVISIBLE
        }
    }

    private fun openGallery() { //  first check if permissions was granted
        if (checkSelfPermission()) {
            val intent = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            } else {
                Intent(Intent.ACTION_PICK, MediaStore.Video.Media.INTERNAL_CONTENT_URI)
            }
            //  In this example we will set the type to video
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            intent.putExtra("return-data", true)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, SELECT_VIDEO_REQUEST)
        }
    }

    //  Check if permissions was granted
    private fun checkSelfPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
            )
            return false
        }
        return true
    }

    //  Handle permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { //  Permissions was granted, open the gallery
                openGallery()
            } else {
                showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_VIDEO_REQUEST) {
            if (resultCode == Activity.RESULT_OK) { //  Get path from PickiT (The path will be returned in onCompleteListener)
//
//  If the selected file is from Dropbox/Google Drive or OnDrive:
//  Then it will be "copied" to your app directory (see path example below) and when done the path will be returned in onCompleteListener
//  /storage/emulated/0/Android/data/your.package.name/files/Temp/tempDriveFile.mp4
//
//  else the path will directly be returned in onCompleteListener
                pickiT.getPath(this, 0, data?.data, Build.VERSION.SDK_INT, object : PickiTCallback {
                    override fun onStartListener(request: Int) {
                        val builder = AlertDialog.Builder(
                            ContextThemeWrapper(
                                this@MainActivity,
                                R.style.myDialog
                            )
                        )
                        @SuppressLint("InflateParams") val mPView =
                            LayoutInflater.from(this@MainActivity).inflate(R.layout.dailog_layout, null)
                        percentText = mPView.findViewById(R.id.percentText)
                        percentText.setOnClickListener {
                            pickiT.cancelTask(this@MainActivity)
                            if (mdialog?.isShowing == true) {
                                mdialog?.cancel()
                            }
                        }
                        mProgressBar = mPView.findViewById(R.id.mProgressBar)
                        mProgressBar.max = 100
                        builder.setView(mPView)
                        mdialog = builder.create()
                        mdialog?.show()
                    }

                    override fun onProgressUpdate(request: Int, progress: Int?) {
                        val progressPlusPercent = progress.toString() + "%"
                        percentText.text = progressPlusPercent
                        mProgressBar.progress = progress ?: 0
                    }

                    override fun onCompleteListener(
                        request: Int,
                        path: String?,
                        provider: PickiTProvider,
                        status: PickiTStatus,
                        reason: String?
                    ) {
                        if (mdialog?.isShowing == true) {
                            mdialog?.cancel()
                        }
                        //  Check if it was a Drive/local/unknown provider file and display a Toast
                        when {
                            provider === PickiTProvider.drive -> {
                                showLongToast("Drive file was selected")
                            }
                            provider === PickiTProvider.unknown -> {
                                showLongToast("File was selected from unknown provider")
                            }
                            else -> {
                                showLongToast("Local file was selected")
                            }
                        }
                        //  Chick if it was successful
                        if (status === PickiTStatus.success) { //  Set returned path to TextView
                            pickitTv.text = path
                            //  Make TextView's visible
                            originalTitle.visibility = View.VISIBLE
                            originalTv.visibility = View.VISIBLE
                            pickitTitle.visibility = View.VISIBLE
                            pickitTv.visibility = View.VISIBLE
                        } else {
                            showLongToast("Error, please see the log..")
                            pickitTv.visibility = View.VISIBLE
                            pickitTv.text = reason
                        }
                    }
                })
                originalTv.text = data?.data?.toString()
            }
        }
    }

    //
//  PickiT Listeners
//
//  The listeners can be used to display a Dialog when a file is selected from Dropbox/Google Drive or OnDrive.
//  The listeners are callbacks from an AsyncTask that creates a new File of the original in /storage/emulated/0/Android/data/your.package.name/files/Temp/
//
//  onStartListener()
//  This will be call once the file creations starts (onPreExecute) and will only be called if the selected file is not local
//
//  onProgressUpdate(int progress)
//  This will return the progress of the file creation (in percentage) and will only be called if the selected file is not local
//
//  onCompleteListener(String path, boolean wasDriveFile)
//  If the selected file was from Dropbox/Google Drive or OnDrive, then this will be called after the file was created.
//  If the selected file was a local file then this will be called directly, returning the path as a String
//  Additionally, a boolean will be returned letting you know if the file selected was from Dropbox/Google Drive or OnDrive.
    lateinit var mProgressBar: ProgressBar
    lateinit var percentText: TextView
    private var mdialog: AlertDialog? = null
    //
//  Lifecycle methods
//
//  Deleting the temporary file if it exists
    override fun onBackPressed() {
        pickiT.deleteTemporaryFile(this)
        super.onBackPressed()
    }

    //  Deleting the temporary file if it exists
//  As we know, this might not even be called if the system kills the application before onDestroy is called
//  So, it is best to call pickiT.deleteTemporaryFile(); as soon as you are done with the file
    public override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            pickiT.deleteTemporaryFile(this)
        }
    }

    companion object {
        //Permissions
        private const val SELECT_VIDEO_REQUEST = 777
        private const val PERMISSION_REQ_ID_RECORD_AUDIO = 22
        private const val PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1
    }
}