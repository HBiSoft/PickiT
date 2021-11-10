package com.hbisoft.pickit;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// This has not been tested extensively
// Feedback is needed from developers
// Some devices do not accept accessing the SD Card UID
// If the device doesn't allow using the UID, we have to replace it with the name of the SD Card.

public class SDUtil {
    private static final String EXTERNAL_STORAGE = System.getenv("EXTERNAL_STORAGE");
    private static final String SECONDARY_STORAGES = System.getenv("SECONDARY_STORAGE");
    private static final String EMULATED_STORAGE_TARGET = System.getenv("EMULATED_STORAGE_TARGET");

    public static String[] getStorageDirectories(Context context) {
        final Set<String> availableDirectoriesSet = new HashSet<>();

        if (!TextUtils.isEmpty(EMULATED_STORAGE_TARGET)) {
            availableDirectoriesSet.add(getEmulatedStorageTarget());
        } else {
            availableDirectoriesSet.addAll(getExternalStorage(context));
        }

        Collections.addAll(availableDirectoriesSet, getAllSecondaryStorages());
        String[] storagesArray = new String[availableDirectoriesSet.size()];
        return availableDirectoriesSet.toArray(storagesArray);
    }

    private static Set<String> getExternalStorage(Context context) {
        final Set<String> availableDirectoriesSet = new HashSet<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            File[] files = getExternalFilesDirs(context);
            for (File file : files) {
                if (file != null) {
                    String applicationSpecificAbsolutePath = file.getAbsolutePath();
                    String rootPath = applicationSpecificAbsolutePath.substring(9, applicationSpecificAbsolutePath.indexOf("Android/data")
                    );

                    rootPath = rootPath.substring(rootPath.indexOf("/storage/") + 1);
                    rootPath = rootPath.substring(0, rootPath.indexOf("/"));

                    if (!rootPath.equals("emulated")) {
                        availableDirectoriesSet.add(rootPath);
                    }
                }
            }
        } else {
            if (TextUtils.isEmpty(EXTERNAL_STORAGE)) {
                availableDirectoriesSet.addAll(getAvailablePhysicalPaths());
            } else {
                availableDirectoriesSet.add(EXTERNAL_STORAGE);
            }
        }
        return availableDirectoriesSet;
    }

    private static String getEmulatedStorageTarget() {
        String rawStorageId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            final String[] folders = path.split(File.separator);
            final String lastSegment = folders[folders.length - 1];
            if (!TextUtils.isEmpty(lastSegment) && TextUtils.isDigitsOnly(lastSegment)) {
                rawStorageId = lastSegment;
            }
        }

        if (TextUtils.isEmpty(rawStorageId)) {
            return EMULATED_STORAGE_TARGET;
        } else {
            return EMULATED_STORAGE_TARGET + File.separator + rawStorageId;
        }
    }

    private static String[] getAllSecondaryStorages() {
        if (!TextUtils.isEmpty(SECONDARY_STORAGES)) {
            if (SECONDARY_STORAGES != null) {
                return SECONDARY_STORAGES.split(File.pathSeparator);
            }
        }
        return new String[0];
    }

    private static List<String> getAvailablePhysicalPaths() {
        List<String> availablePhysicalPaths = new ArrayList<>();
        for (String physicalPath : KNOWN_PHYSICAL_PATHS) {
            File file = new File(physicalPath);
            if (file.exists()) {
                availablePhysicalPaths.add(physicalPath);
            }
        }
        return availablePhysicalPaths;
    }

    private static File[] getExternalFilesDirs(Context context) {
        if (Build.VERSION.SDK_INT >= 19) {
            return context.getExternalFilesDirs(null);
        } else {
            return new File[]{context.getExternalFilesDir(null)};
        }
    }

    @SuppressLint("SdCardPath")
    @SuppressWarnings("SpellCheckingInspection")
    private static final String[] KNOWN_PHYSICAL_PATHS = new String[]{
            "/storage/sdcard0",
            "/storage/sdcard1",
            "/storage/extsdcard",
            "/storage/sdcard0/external_sdcard",
            "/mnt/extsdcard",
            "/mnt/sdcard/external_sd",
            "/mnt/sdcard/ext_sd",
            "/mnt/external_sd",
            "/mnt/media_rw/sdcard1",
            "/removable/microsd",
            "/mnt/emmc",
            "/storage/external_SD",
            "/storage/ext_sd",
            "/storage/removable/sdcard1",
            "/data/sdext",
            "/data/sdext2",
            "/data/sdext3",
            "/data/sdext4",
            "/sdcard1",
            "/sdcard2",
            "/storage/microsd"
    };
}
