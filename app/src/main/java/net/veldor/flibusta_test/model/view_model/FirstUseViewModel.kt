package net.veldor.flibusta_test.model.view_model

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.PermissionChecker
import androidx.lifecycle.ViewModel
import net.veldor.flibusta_test.App


class FirstUseViewModel : ViewModel() {
    fun permissionsGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val writeResult =
                App.instance.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val readResult =
                App.instance.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
        }

        val writeResult = PermissionChecker.checkSelfPermission(
            App.instance.applicationContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readResult = PermissionChecker.checkSelfPermission(
            App.instance.applicationContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
    }

}