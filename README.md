# PickiT 
[![](https://jitpack.io/v/HBiSoft/PickiT.svg)](https://jitpack.io/#HBiSoft/PickiT)
[![Android Arsenal]( https://img.shields.io/badge/Android%20Arsenal-PickiT-green.svg?style=flat )]( https://android-arsenal.com/details/1/7890 )

An Android library that returns real paths from Uri's

![pickiticon](https://user-images.githubusercontent.com/35602540/63160498-37d88780-c01e-11e9-95f7-d6fac239f53b.png)

</br>



Demo screenshot:
---

Download the demo app [here](https://github.com/HBiSoft/PickiT/releases/download/0.1.10/PickiTDemo.apk)

![pickiTDemo](https://user-images.githubusercontent.com/35602540/63206870-1c708980-c0bd-11e9-96dc-374a8a434c0e.png)

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
    implementation 'com.github.HBiSoft:PickiT:0.1.10'
}
```
    
Implementation:
---
    
First, implement PickiT callbacks in the `Activity` that you want to use it, as shown below:

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
        pickiT = new PickiT(this, this);

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
    
If the selected file was from Dropbox,Google Drive, OneDrive or an unknown file provider, it will then be copied/created in</br> 
`Internal Storage - Android - data - your.package.name - files - Temp`

It is your responsibility to delete the file when you are done with it, by calling:

```java
pickiT.deleteTemporaryFile();
```
This can be done in `onBackPressed` and `onDestroy`, as shown below:

```java
@Override
public void onBackPressed() {
    pickiT.deleteTemporaryFile();
    super.onBackPressed();
}

@Override
public void onDestroy() {
    super.onDestroy();
    if (!isChangingConfigurations()) {
        pickiT.deleteTemporaryFile();
    }
}
```

If you do not call `pickiT.deleteTemporaryFile();`, the file will remain in the above mentioned folder and will be overwritten each time you select a new file from Dropbox,Google Drive, OneDrive or an unknown file provider.

    
Callback methods
---

```java
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
