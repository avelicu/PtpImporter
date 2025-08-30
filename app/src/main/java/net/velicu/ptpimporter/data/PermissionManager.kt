package net.velicu.ptpimporter.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

class PermissionManager(private val context: Context) {
    
    companion object {
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestPermissions(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            onResult(allGranted)
        }
        
        permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }
    
    fun shouldShowPermissionRationale(activity: FragmentActivity): Boolean {
        return REQUIRED_PERMISSIONS.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }
} 