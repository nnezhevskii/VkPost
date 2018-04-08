package com.licht.vkpost.vkpost.utils

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity

public val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 100

fun requestPermission(activity: AppCompatActivity,
                      permission: String,
                      requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
}

fun appHasPermission(context: Context, permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun appHasWriteExternalStoragePermission(context: Context): Boolean {
    return appHasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
}
