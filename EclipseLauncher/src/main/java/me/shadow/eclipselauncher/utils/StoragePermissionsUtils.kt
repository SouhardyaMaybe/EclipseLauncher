package me.shadow.eclipselauncher.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.ui.dialog.TipDialog

class StoragePermissionsUtils {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS: Int = 0
        @JvmStatic
        private var hasStoragePermission: Boolean = false

        /**
         * Check the storage permission and return whether it is granted
         */
        @JvmStatic
        fun checkPermissions(context: Context) {
            hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                checkPermissionsForAndroid11AndAbove()
            } else {
                hasStoragePermissions(context)
            }
        }

        /**
         * Get the previously checked storage permission
         */
        @JvmStatic
        fun checkPermissions() = hasStoragePermission

        /**
         * Check the storage permission; if it is not granted, show a dialog requesting it from the user
         */
        @JvmStatic
        fun checkPermissions(
            activity: Activity,
            title: Int = R.string.generic_warning,
            message: String = getDefaultPermissionMessage(activity),
            permissionGranted: PermissionGranted?
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                handlePermissionsForAndroid11AndAbove(activity, title, message, permissionGranted)
            } else {
                handlePermissionsForAndroid10AndBelow(activity, title, message, permissionGranted)
            }
        }

        /**
         * Storage permission check for Android 10 and below
         */
        fun hasStoragePermissions(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        @RequiresApi(api = Build.VERSION_CODES.R)
        private fun checkPermissionsForAndroid11AndAbove() = Environment.isExternalStorageManager()

        @RequiresApi(api = Build.VERSION_CODES.R)
        private fun handlePermissionsForAndroid11AndAbove(activity: Activity, title: Int, message: String, permissionGranted: PermissionGranted?) {
            if (!checkPermissionsForAndroid11AndAbove()) {
                showPermissionRequestDialog(activity, title, message, object : RequestPermissions {
                    override fun onRequest() {
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.setData(Uri.parse("package:" + activity.packageName))
                        activity.startActivityForResult(intent, REQUEST_CODE_PERMISSIONS)
                    }

                    override fun onCancel() {
                        permissionGranted?.cancelled()
                    }
                })
            } else {
                permissionGranted?.granted()
            }
        }

        private fun handlePermissionsForAndroid10AndBelow(activity: Activity, title: Int, message: String, permissionGranted: PermissionGranted?) {
            if (!hasStoragePermissions(activity)) {
                showPermissionRequestDialog(activity, title, message, object : RequestPermissions {
                    override fun onRequest() {
                        ActivityCompat.requestPermissions(
                            activity, arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ), REQUEST_CODE_PERMISSIONS
                        )
                    }

                    override fun onCancel() {
                        permissionGranted?.cancelled()
                    }
                })
            } else {
                permissionGranted?.granted()
            }
        }

        private fun showPermissionRequestDialog(
            context: Context,
            title: Int,
            message: String,
            requestPermissions: RequestPermissions
        ) {
            TipDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setConfirmClickListener { requestPermissions.onRequest() }
                .setCancelClickListener { requestPermissions.onCancel() }
                .setCancelable(false)
                .showDialog()
        }

        private fun getDefaultPermissionMessage(context: Context) =
            InfoCenter.replaceName(context, R.string.permissions_manage_external_storage)
    }

    private interface RequestPermissions {
        fun onRequest()
        fun onCancel()
    }

    interface PermissionGranted {
        fun granted()
        fun cancelled()
    }
}