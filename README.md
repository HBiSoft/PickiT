# PickiT 
[![](https://jitpack.io/v/HBiSoft/PickiT.svg)](https://jitpack.io/#HBiSoft/PickiT)
[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-PickiT-green.svg?style=flat )]( https://android-arsenal.com/details/1/7890 )

<p align="center">An Android library that returns real paths from Uri's</p>

<p align="center"><img src="https://user-images.githubusercontent.com/35602540/63160498-37d88780-c01e-11e9-95f7-d6fac239f53b.png"></p>

</br>

---

**<p align="center"><b>If you want to thank me for this library, you can do so below:</b></p>**

<p align="center"><a href="https://www.buymeacoffee.com/HBiSoft" target="_blank" ><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: 41px !important;width: 164px !important;box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;-webkit-box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;" ></a></p>


---

<h2 align="center"><b>Demo screenshot:</b></h2>

<p align="center">Download the demo app  <a href="https://github.com/HBiSoft/PickiT/releases/download/2.0.5/PickiTDemo.apk"><nobr>here</nobr></a></p>

<p align="center"><img src="https://user-images.githubusercontent.com/35602540/63206870-1c708980-c0bd-11e9-96dc-374a8a434c0e.png"</p>

</br></br>

<h2 align="center"><b>Take part in the poll</b></h2>

<p align="center">Let other developers know what version you have tested:</p></br></br>

<p align="center"><a href="https://api.gh-polls.com/poll/01EEAANG18KCS6TPFC5YVWKQGW/Android%2011/vote"><img src="https://api.gh-polls.com/poll/01EEAANG18KCS6TPFC5YVWKQGW/Android%2011" alt=""></a></p>
<p align="center"><a href="https://api.gh-polls.com/poll/01EEAANG18KCS6TPFC5YVWKQGW/Android%2010/vote"><img src="https://api.gh-polls.com/poll/01EEAANG18KCS6TPFC5YVWKQGW/Android%2010" alt=""></a></p>
<p align="center"><a href="https://api.gh-polls.com/poll/01EEAANG18KCS6TPFC5YVWKQGW/Android%209%20and%20lower/vote"><img src="https://api.gh-polls.com/poll/01EEAANG18KCS6TPFC5YVWKQGW/Android%209%20and%20lower" alt=""></a></p>

</br>

Add Pickit to your project:
---

Add the following in your root build.gradle at the end of repositories:

```java
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
    
Then, add the dependency, in your app level build.gradle:

```java
dependencies {
    implementation 'com.github.HBiSoft:PickiT:2.0.5'
}
```
    
Implementation:
---
    
First, implement PickiT callbacks, as shown below:

```java
public class MainActivity extends Activity implements PickiTCallbacks {
```

`Alt+Enter` to implement the methods, we will discuss the methods later in the readme.

Implement pickiT in your `onCreate()` method, as shown below:

```java
public class MainActivity extends AppCompatActivity implements PickiTCallbacks {
    //Declare PickiT
    PickiT pickiT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize PickiT
        //context, listener, activity
        pickiT = new PickiT(this, this, this);

    }
}
```
    
You can now select a file as you usually would (have a look at the demo if you don't know how to do this).

Then in `onActivityResult`, you can pass the path to PickiT, as shown below:

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == SELECT_VIDEO_REQUEST) {
        if (resultCode == RESULT_OK) {
            pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);

        }
    }
}
```

Dropbox, Google Drive, OneDrive and files from unknown file providers:
---

You can check if the `Uri` is from Dropbox,Google Drive or OneDrive by calling:
```java
if (!pickiT.wasLocalFileSelected(uri)){
    // Drive file was selected
}
```
You can check if the `Uri` is from an unknown provider by calling:
```java
if (pickiT.isUnknownProvider(uri, Build.VERSION.SDK_INT)){
    // Uri is from unknown provider
}
```

---
    
If the selected file was from Dropbox,Google Drive, OneDrive or an unknown file provider, it will then be copied/created in</br> 
`Internal Storage - Android - data - your.package.name - files - Temp`

It is your responsibility to delete the file when you are done with it, by calling:

```java
pickiT.deleteTemporaryFile(Context);
```
This can be done in `onBackPressed` and `onDestroy`, as shown below:

```java
@Override
public void onBackPressed() {
    pickiT.deleteTemporaryFile(this);
    super.onBackPressed();
}

@Override
public void onDestroy() {
    super.onDestroy();
    if (!isChangingConfigurations()) {
        pickiT.deleteTemporaryFile(this);
    }
}
```

If you do not call `pickiT.deleteTemporaryFile(Context);`, the file will remain in the above mentioned folder and will be overwritten each time you select a new file from Dropbox,Google Drive, OneDrive or an unknown file provider.


Manifest
---
If you are targeting SDK 29> add `android:requestLegacyExternalStorage="true"` in your manifest:
```xml
<manifest ... >
  <application 
    android:requestLegacyExternalStorage="true" 
  </application>
</manifest>
```
The reason for this is, in Android 10 file path access was removed and it returned in Android 11.
<br>If you are targiting SDK 29> and you do not add this, you will get `EACCES (Permission denied)`
    
Callback methods
---

```java
//When selecting a file from Google Drive, for example, the Uri will be returned before the file is available(if it has not yet been cached/downloaded).
//We are unable to see the progress
//Apps like Dropbox will display a dialog inside the picker
//This will only be called when selecting a drive file
@Override
public void PickiTonUriReturned() {
    //Use to let user know that we are waiting for the application to return the file
    //See the demo project to see how I used this.
}

//Called when the file creations starts (similar to onPreExecute)
//This will only be called if the selected file is not local or if the file is from an unknown file provider
@Override
public void PickiTonStartListener() {
    //Can be used to display a ProgressDialog
}

//Returns the progress of the file being created (in percentage)
//This will only be called if the selected file is not local or if the file is from an unknown file provider
@Override
public void PickiTonProgressUpdate(int progress) {
    //Can be used to update the progress of your dialog
}

//If the selected file was a local file then this will be called directly, returning the path as a String.
//String path - returned path
//boolean wasDriveFile - check if it was a drive file
//boolean wasUnknownProvider - check if it was from an unknown file provider
//boolean wasSuccessful - check if it was successful
//String reason - the get the reason why wasSuccessful returned false
@Override
public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String reason) {
    //Dismiss dialog and return the path
}
```
 
 Have a look at the demo project if you have any issues implementing the library.
