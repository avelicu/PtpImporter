# PTP Importer

A comprehensive Android app for copying JPG files from a source directory to a destination directory with advanced features and robust error handling.

## ✨ Features

- **🔐 Smart Permission Management**: 
  - Automatic permission detection and requests
  - Support for both legacy and modern Android storage permissions
  - Clear permission status indicators
  - Permission refresh functionality

- **🔍 Recursive File Discovery**: 
  - Searches through all subdirectories automatically
  - Finds JPG files at any depth level
  - Comprehensive logging for debugging

- **📁 Directory Selection**: 
  - System file picker integration
  - Persistent directory preferences
  - Visual feedback for selected paths

- **📊 Advanced Progress Tracking**: 
  - Real-time progress with file counts
  - Current file name display
  - Estimated time remaining calculation
  - Progress bars and status updates

- **⚠️ Comprehensive Error Handling**: 
  - User-friendly error messages
  - Permission-specific error guidance
  - Detailed error logging
  - Recovery suggestions

- **🎛️ User Experience**: 
  - Cancel operations at any time
  - Skip existing files automatically
  - Modern Material 3 design
  - Responsive and intuitive interface

## 🔧 Technical Details

- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean separation with data models, managers, and UI components
- **File Operations**: Coroutine-based background processing
- **Storage Access**: Storage Access Framework (SAF) with fallback to traditional file operations
- **Permission Handling**: Runtime permission requests with proper error handling
- **State Management**: Compose state and StateFlow for reactive UI updates

## 📱 Requirements

- **Android API**: Level 24+ (Android 7.0+)
- **Permissions**: 
  - `READ_EXTERNAL_STORAGE` (Android < 13)
  - `WRITE_EXTERNAL_STORAGE` (Android < 13)
  - `READ_MEDIA_IMAGES` (Android 13+)
  - `MANAGE_EXTERNAL_STORAGE` (for full access)

## 🚀 Usage Guide

### 1. **Permission Setup**
- Launch the app
- Grant storage permissions when prompted
- Verify permission status in the green confirmation card

### 2. **Directory Selection**
- Select source directory containing JPG files
- Select destination directory for copied files
- Directories are automatically saved for future use

### 3. **File Copying**
- Tap "Start Copying JPG Files"
- Monitor progress with real-time updates
- View estimated time remaining
- Cancel operation anytime if needed

### 4. **Troubleshooting**
- **Permission Issues**: Use the "Grant Permissions" button
- **No Files Found**: Check directory selection and file types
- **Copy Errors**: Review error messages and try again
- **Performance**: App automatically skips existing files

## 🛠️ Building & Development

```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean build

# Run tests
./gradlew test
```

## 🔍 Debugging

The app includes comprehensive logging:
- **FileCopyManager**: Directory scanning and file operations
- **PermissionManager**: Permission status and requests
- **UI Components**: User interactions and state changes

Use `adb logcat` or Android Studio to view detailed logs.

## 🔮 Future Enhancements

- **File Format Support**: PNG, GIF, and other image formats
- **Advanced Filtering**: Date ranges, file sizes, custom patterns
- **Batch Operations**: Multiple source directories
- **File Integrity**: Checksums and verification
- **Cloud Integration**: Google Drive, OneDrive support
- **Scheduling**: Automated backup operations

## 📄 License

This project is open source and available under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

---

**PTP Importer** - Making file management simple and reliable on Android. 