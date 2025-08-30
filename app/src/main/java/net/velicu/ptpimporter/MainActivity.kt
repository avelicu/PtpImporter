package net.velicu.ptpimporter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import net.velicu.ptpimporter.ui.PtpImporterApp
import net.velicu.ptpimporter.ui.theme.PtpImporterTheme
import android.content.Intent

class MainActivity : ComponentActivity() {
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    private var permissionCallback: (() -> Unit)? = null
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted, notify UI
            permissionCallback?.invoke()
        } else {
            // Handle permission denied - could show a message
            android.util.Log.w("MainActivity", "Some permissions were denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Extract source directory from intent if available
        val sourceUri = extractSourceUriFromIntent(intent)
        
        setContent {
            PtpImporterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), 
                    color = MaterialTheme.colorScheme.background
                ) {
                    PtpImporterApp(
                        onRequestPermissions = { callback ->
                            permissionCallback = callback
                            requestPermissions()
                        },
                        initialSourceUri = sourceUri
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        
        // Handle new intents (e.g., when app is already running and new MTP device is connected)
        intent?.let { newIntent ->
            val sourceUri = extractSourceUriFromIntent(newIntent)
            if (sourceUri != null) {
                // Update the app with the new source URI
                // This will be handled by the PtpImporterApp
                android.util.Log.d("MainActivity", "New intent received with source URI: $sourceUri")
            }
        }
    }
    
    private fun extractSourceUriFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                // Handle content:// URIs from file managers
                intent.data?.toString()
            }
            Intent.ACTION_PICK -> {
                // Handle image picker intents
                intent.data?.toString()
            }
            Intent.ACTION_GET_CONTENT -> {
                // Handle content selection intents
                intent.data?.toString()
            }
            "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> {
                // Handle USB device attachment
                // For MTP devices, we'll need to prompt user to select the directory
                // since we can't automatically determine the correct path
                android.util.Log.d("MainActivity", "USB device attached - user will need to select directory")
                null
            }
            else -> {
                // Check for any content URI in the intent data
                intent.data?.toString()
            }
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }
} 