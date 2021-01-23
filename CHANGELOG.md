# Change Log

## 1.37.0
* Improve formatting date and time in history listing
* Add haptic and audible feedback for background request errors

## 1.36.0
* Add a setting to automatically return to scanning screen after copying
* Add a button to test the background request
* Show HTTP response for background requests
* Improve SVG export
* Automatically continue with an action after granting a permission
* Update Chinese translation
* Update French translation
* Update Indonesian translation
* Update Turkish translation
* Update Ukrainian translation

## 1.35.0
* Put images with transparent pixels on white/black
* Use cropping limiter when scanning from images too
* Support setting the error correction level when creating QR Codes
* Support setting a custom deviating locale
* Use adaptive icons for shortcut icons
* Add exporting barcodes as plain text
* Add Ukrainian translation
* Add Turkish translation
* Add Georgian translation
* Update Indonesian translation
* Update Russian translation
* Update Italian translation
* Update Simplified Chinese translation

## 1.34.0
* Use Material Design settings with headers
* Add a setting to choose the message type when forwarding scanned content

## 1.33.0
* Add a setting to automatically send the scanned content to a custom URL
* Add a setting to automatically put contents into clipboard
* Add an explicit button to copy WiFi password into clipboard
* Update Indonesian translation

## 1.32.1
* Alternatively remove region of interest by dragging handle to a screen corner
* Restrict region of interest to screen space
* Remove extra action item to create barcode

## 1.32.0
* Add bulk mode
* Remember region of interest permanently
* Export generated barcode as SVG
* Run detection on images in background
* Remove a scan from result view
* Update Indonesian translation

## 1.31.0
* Stop detection while region of interest is modified
* Fix resetting region of interest
* Add Polish translation

## 1.30.1
* Improve usability of cropping limiter
* Draw round corners around region of interest
* Update Indonesian translation

## 1.30.0
* Add a handle to define a region of interest
* Add a setting to show/hide cropping limiter
* Add support for VCALENDAR types
* Add spanish translation
* Add copy to clipboard button to context menu in history listing
* Make history actions work on current listing only
* Keep camera selection over orientation changes

## 1.29.0
* Add a setting to enable reading of vertical 1D barcodes

## 1.28.0
* Share history as CSV and JSON too
* Export history in JSON format
* Update Indonesian translation
* Fix initializing of RenderScript automatically
* Fix highlighting after landscape to landscape rotations

## 1.27.1
* Update Russian translation
* Force compat mode if native RenderScript crashed

## 1.27.0
* Export SQLite database
* Improve order of preferences
* Update Italian translation

## 1.26.0
* Zoom in & out by swiping vertically in camera view
* Add a setting for swipe to zoom feature
* Optimize vector drawables
* Update Indonesian translation

## 1.25.1
* Fix Proguard configuration

## 1.25.0
* Add giving scans individual names in history listing
* Add searching the history
* Add a setting to search harder for barcodes
* Add a setting to control vibration on detection
* Add app shortcuts for launchers that support them
* Remove saving barcodes for intents from other apps
* Show latest possible result point in camera view
* Suggest a WiFi network on Android Q+
* Always put parsed WiFi password into clipboard
* Use native image processing where available
* Ignore consecutive duplicates by default
* Fix saving external files on Android Q+
* Fix order of metadata items

## 1.24.0
* Show barcode metadata
* Add a setting to show/hide metadata
* Include metadata in CSV export
* Update italian translation
* Fix resetting flash button
* Fix CSV export for Android Q

## 1.23.0
* Add a setting to not save consecutive double scans
* Add a setting to show/hide hex dump
* Limit history items to just one line
* Fix window insets for consecutive screens

## 1.22.3
* Fix broken RenderScript built

## 1.22.2
* Fix getting window insets for Android Lollipop and above

## 1.22.1
* Ensure non-printable content is saved as raw data

## 1.22.0
* Highlight detected barcodes in camera view
* Add bing and yandex search engines
* Update Chinese Translation
* Update Italian translation
* Update Indonesian translation

## 1.21.0
* Save and restore history list state
* Improve scanning from large images
* Fix opening image files by "Open with"

## 1.20.1
* Fix window insets below Android Lollipop

## 1.20.0
* Scroll content below tool and system bars
* Preset file name to save with format and content
* Add encode menu item to make encoding easier when keyboard is visible
* Increase font size of barcode contents in history
* Update Hungarian translation
* Update Indonesian translation
* Fix encoding for old Androids
* Fix returning result for ZXing's SCAN intent again
* Fix transfering barcode format from history

## 1.19.0
* Allow scaling a new barcode to half its size
* Remember last selected barcode format

## 1.18.0
* Make open URL default action after decoding a barcode
* Add a setting to open contents without inspection
* Highlight codes for picking in shared/loaded images
* Improve visability of crop border
* Fix returning result for ZXing's SCAN intent

## 1.17.1
* Fix navigating back without choosing an image

## 1.17.0
* Pinch/Zoom shared/loaded images to scan just a section
* Add a load file menu item to load an image file
* Add a save file menu item to the barcode view
* Use discrete style for size seek bar
* Fix bar transparency when zooming/pinching

## 1.16.0
* Improve displaying scanned barcodes
* Show a message if a barcode cannot be shared
* Fix setting system bar transparency when double taping a barcode
* Update Dutch translation
* Update Hungarian translation
* Update Indonesian translation
* Update Italian translation

## 1.15.0
* Additionally export history as CSV file
* Color tool and system bars when a view is scrolled
* Add an intent filter for opening images
* Add Russian translation

## 1.14.0
* Add OTP support
* Update Indonesian translation

## 1.13.1
* Fix crash caused by missing support for Regex on Android < 6

## 1.13.0
* Add missing extras for ZXing result intents

## 1.12.0
* Add preferences dialog and the option to use a custom URL with read contents
* Add Brazilian Portuguese translation
* Add Chinese translation
* Encode text using UTF-8 encoding when creating a barcode

## 1.11.2
* Downgrade ZXing version
* Improve getting system UI metrics

## 1.11.1
* Fix tap to focus on some devices

## 1.11.0
* Add support for VCard and VEvent barcodes contents
* Generated barcodes are now zoomable
* Fix creating PDF417 barcodes

## 1.10.0
* Add support for special barcode contents (WiFi, SMS, phone, E-mail)
* Add Indonesian translation
* Update Hungarian translation
* Make system bars completely transparent

## 1.9.1
* Pick search engine for content that is not an URL

## 1.9.0
* Try Google when opening content as URL fails
* Show generic error message if barcode generation fails

## 1.8.1
* Distribute APK instead of App Bundle

## 1.8.0
* Save binary data into external file
* Fix storage of binary data
* Remove horizontal orientation line from scanner

## 1.7.3
* Fix crashes on Lineage and remove RenderScript fallback
* Improve handling of binary data

## 1.7.2
* Add a fallback for RenderScript
* Add italian translation

## 1.7.1
* Fix support for 64 bit architectures

## 1.7.0
* Pick list separator on sharing history listing
* Update Hungarian translation

## 1.6.0
* Share history listing as plain text
* Add separator line between history items
* Add content description for floating action buttons

## 1.5.1
* Fix generating hex dump

## 1.5.0
* Show a hex dump of decoded contents
* Fix processing text from share menu
* Add French translation

## 1.4.3
* Fix broken history listing

## 1.4.2
* Show barcode type and content length after decoding

## 1.4.1
* Toggle availability of torch mode button according to camera features

## 1.4.0
* Save scanned codes and show a history listing
* Share generated barcodes
* Add scanning from a shared image
* Add menu item to switch between camera front/back camera
* Add dutch and hungarian translation

## 1.3.8
* Qualify for F-Droid

## 1.3.7
* Normalize URL schemes

## 1.3.6
* Fix handling of camera scene modes

## 1.3.5
* Add german translation
* Fix handling of data intents
* Respond to ZXing' SCAN intents

## 1.3.4
* Fix pre-processing for Android 7+
* Fix adaptive icons for Android 8+

## 1.3.3
* Improved pre-processing
* Added adaptive icon for Android 8+
* Decrease default magnification to 10%

## 1.3.2
* Fixed crash when going back very fast

## 1.3.1
* Changed primary color

## 1.3.0
* Support tap to focus
* Save chose zoom level permanently
* Update ZXing to latest version (3.3.2)

## 1.2.0
* Added a camera zoom slider
* Added an unobtrusive wallpaper background
* Replaced lock marks with just a horizontal line
* Put toggling torch mode into Floating Action Button

## 1.1.1
* Brought back inverting every second frame

## 1.1.0
* Create barcodes
* Streamlined video pre-processing
* Decoded content is editable now
* Fixed position of lock marks on tablets
* Open README from menu

## 1.0.1
* Keep proguard from messing with renderscript
