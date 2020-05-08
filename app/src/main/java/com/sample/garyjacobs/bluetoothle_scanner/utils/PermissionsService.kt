package com.sample.garyjacobs.bluetoothle_scanner.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import com.karumi.dexter.listener.single.BasePermissionListener
import com.sample.garyjacobs.bluetoothle_scanner.R


class PermissionsService(val actvity: Activity) {

    private  var applicationIcon : Drawable

    companion object permissions {
        const val SETTINGS_REQUEST_CODE = 100
    }

    private var packageUri: Uri
    private var handleDenied: Boolean = false
    private var onSuccess: (() -> Unit)? = null

    init {
        packageUri = Uri.parse(String.format("package:%s", actvity.packageName))
        applicationIcon = actvity.resources.getDrawable(R.drawable.ic_launcher_background,null)

    }

    fun handleLocationAndPhonePermission(onSuccess: (() -> Unit)?) = handleMultiplePermissions(true, onSuccess, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun handleCameraAndStoragePermission(onSuccess: (() -> Unit)?) = handleMultiplePermissions(false, onSuccess, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

    fun handleAudioPermission(onSuccess: (() -> Unit)?) = handleSinglePermission(false, onSuccess, Manifest.permission.RECORD_AUDIO)

    fun handleStoragePermission(onSuccess: (() -> Unit)?) = handleSinglePermission(false, onSuccess, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    fun handleMultiplePermissions(handleDenied: Boolean = false, onSuccess: (() -> Unit)?, vararg permissions: String) {
        this.onSuccess = onSuccess
        this.handleDenied = handleDenied

        var permission = Dexter.withActivity(actvity);
        permission.withPermissions(permissions.asList())
            .withListener(mulitPermissionListener)
            .onSameThread()
            .check()
    }

    private fun handleSinglePermission(handleDenied: Boolean = false, onSuccess: (() -> Unit)? = null, permission: String
                                      ) {
        this.onSuccess = onSuccess
        this.handleDenied = handleDenied

        Dexter.withActivity(actvity)
            .withPermission(permission)
            .withListener(singlePermissionListener)
            .onSameThread()
            .check()
    }

    private val mulitPermissionListener = object : BaseMultiplePermissionsListener() {

        override fun onPermissionsChecked(report: MultiplePermissionsReport) {
            if (report.areAllPermissionsGranted()) {
                onSuccess?.let { it.invoke() }
            }
            else if (report.isAnyPermissionPermanentlyDenied()) {
                if (handleDenied) {
                    showPermanentlyDeniedDialog({ _, _ -> actvity.finish() })
                }
                else {
                    showPermanentlyDeniedDialog()
                }
            }
            else if (handleDenied) {
                showSomePermissionsNotGrantedDialog(actvity)
            }
        }

        override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
            showDenialDialogs(permissions[0].name, token)
        }
    }

    private val singlePermissionListener = object : BasePermissionListener() {

        override fun onPermissionGranted(response: PermissionGrantedResponse) {
            onSuccess?.let { it.invoke() }
        }

        override fun onPermissionRationaleShouldBeShown(
            permission: PermissionRequest,
            token: PermissionToken
                                                       ) {
            showDenialDialogs(permission.name, token)
        }

        override fun onPermissionDenied(response: PermissionDeniedResponse) {
            if (handleDenied || response.isPermanentlyDenied) {
                showPermanentlyDeniedDialog()
            }
        }
    }

    private fun showDenialDialogs(
        permissionName: String,
        token: PermissionToken) {
        when (permissionName) {
            Manifest.permission.ACCESS_FINE_LOCATION   -> showDenialDialog(R.string.location_permissions_title, R.string.location_permissions_denial_message, token)
            Manifest.permission.ACCESS_COARSE_LOCATION -> showDenialDialog(R.string.location_permissions_title, R.string.location_permissions_denial_message, token)
         }
    }

    private fun showPermanentlyDeniedDialog(onDenied: ((dialog: DialogInterface, what: Int) -> Unit)? = null) {
        AlertDialog.Builder(actvity)
            .setTitle(R.string.denied_dialog_title)
            .setMessage(R.string.perm_denied_dialog_message)
            .setIcon(applicationIcon)
            .setPositiveButton("App Info",
                { dialog, _ ->
                    kotlin.run {
                        dialog.dismiss()
                        actvity.startActivityForResult(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(packageUri), SETTINGS_REQUEST_CODE)
                    }
                })
            .setNegativeButton("exit", onDenied)
            .show();
    }

    private fun showDenialDialog(
        titleRes: Int,
        messageRes: Int,
        permissionToken: PermissionToken?
                                ) {
        AlertDialog.Builder(actvity)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .setIcon(applicationIcon)
            .setPositiveButton(android.R.string.ok, { dialog, _ ->
                dialog.dismiss()
                permissionToken?.continuePermissionRequest()
            })
            .setNegativeButton(android.R.string.cancel, { dialog, _ ->
                dialog.dismiss()
                permissionToken?.cancelPermissionRequest()
            })
            .setOnDismissListener({ dialog -> dialog.dismiss() })
            .show()
    }

    public fun showSomePermissionsNotGrantedDialog(context: Activity) {
        AlertDialog.Builder(context)
            .setTitle(R.string.denied_dialog_title)
            .setMessage(R.string.some_permissions_denied_dialog_message)
            .setIcon(applicationIcon)
            .setPositiveButton(android.R.string.ok, { dialog, _ ->
                dialog.dismiss()
                context.finish()
            })
            .show()
    }
}