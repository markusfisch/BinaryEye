# Privacy Policy

Binary Eye does not collect any personal information.

If you have any questions or suggestions about my Privacy Policy, do not
hesitate to contact me.

## Permissions

### [ACCESS_FINE_LOCATION][ACCESS_FINE_LOCATION]

Required to add a WiFi network from a barcode (usually a QR Code).

Before Android Q, this feature requires using
[WifiManager.getConfiguredNetworks][getConfiguredNetworks]
which requires `ACCESS_FINE_LOCATION`.

On Android Q an better, this permission is not requested nor required.

### [BLUETOOTH][BLUETOOTH], [BLUETOOTH_ADMIN][BLUETOOTH_ADMIN] and [BLUETOOTH_CONNECT][BLUETOOTH_CONNECT]

Required to optionally forward scans to a Bluetooth device.

[BLUETOOTH][BLUETOOTH] and [BLUETOOTH_ADMIN][BLUETOOTH_ADMIN] are required
before Android S. [BLUETOOTH_CONNECT][BLUETOOTH_CONNECT] from Android S on.

### [CAMERA][CAMERA]

Required to read a barcode from the camera image.

### [INTERNET][INTERNET]

Required to send a scanned code to a custom URL you can specify in settings.

### [VIBRATE][VIBRATE]

Required to vibrate on detection.

### [WRITE_EXTERNAL_STORAGE][WRITE_EXTERNAL_STORAGE]

Required to save generated barcodes as files before Android Q.

### [CHANGE_WIFI_STATE][CHANGE_WIFI_STATE]

Required to add a WiFi network from a barcode (usually a QR Code).

On Android Q and better, this feature requires using
[WifiManager.addNetworkSuggestions][addNetworkSuggestions]
which requires `CHANGE_WIFI_STATE`.

### [ACCESS_WIFI_STATE][ACCESS_FINE_LOCATION]

Required to add a WiFi network from a barcode (usually a QR Code).

Before Android Q, this feature requires using
[WifiManager.getConfiguredNetworks][getConfiguredNetworks]
which requires `ACCESS_WIFI_STATE`.

From Android R, `ACCESS_WIFI_STATE` is required for
[WifiManager.getNetworkSuggestions][getNetworkSuggestions]
which is used to remove individual network suggestions.

[ACCESS_FINE_LOCATION]: https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION
[BLUETOOTH]: https://developer.android.com/reference/android/Manifest.permission#BLUETOOTH
[BLUETOOTH_ADMIN]: https://developer.android.com/reference/android/Manifest.permission#BLUETOOTH_ADMIN
[BLUETOOTH_CONNECT]: https://developer.android.com/reference/android/Manifest.permission#BLUETOOTH_CONNECT
[CAMERA]: https://developer.android.com/reference/android/Manifest.permission#CAMERA
[INTERNET]: https://developer.android.com/reference/android/Manifest.permission#INTERNET
[VIBRATE]: https://developer.android.com/reference/android/Manifest.permission#VIBRATE
[WRITE_EXTERNAL_STORAGE]: https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE
[CHANGE_WIFI_STATE]: https://developer.android.com/reference/android/Manifest.permission#CHANGE_WIFI_STATE
[ACCESS_WIFI_STATE]: https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION
[getConfiguredNetworks]: https://developer.android.com/reference/android/net/wifi/WifiManager#getConfiguredNetworks()
[addNetworkSuggestions]: https://developer.android.com/reference/android/net/wifi/WifiManager#addNetworkSuggestions(java.util.List%3Candroid.net.wifi.WifiNetworkSuggestion%3E)
[getNetworkSuggestions]: https://developer.android.com/reference/android/net/wifi/WifiManager#getNetworkSuggestions()
