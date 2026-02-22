# Change Log

## 1.68.2
* Fix barcode regeneration

## 1.68.1
* Improve UI of search barcode dialog
* Stability improvements
* Update ZXingCpp
* Update Russian translation

## 1.68.0
* Add automated actions
* Improve managing profiles

## 1.67.1
* Fix opening file:// Uris
* Fix auto-return/immediate opening
* Improve Bluetooth stability

## 1.67.0
* Add preference profiles
* Support regex when searching for a barcode
* Improve feedback when searching for a barcode
* Update translations

## 1.66.2
* Remove forcing `?content=` on forwarding URL
* Update Spanish translation

## 1.66.1
* Show elements of invalid Wifi configurations too
* Update translations

## 1.66.0
* Support deep links to encode content via web links
* Parse International Driver Licenses (IDL) in PDF417 barcodes
* Add WiFis from system settings
* Show parsed data items
* Update translations

## 1.65.2
* Sanity check ROI metrics

## 1.65.1
* Improve simple mode
* Remember quiet zone settings
* Reduce height for 1D barcodes
* Add execute argument for binaryeye://encode
* Update Russian translation

## 1.65.0
* Add a welcome screen to choose a mode
* Add a deep link to encode a barcode
* Remember zoom level of generated barcodes
* Remove Human Readable Interpretation (HRI) decoration
* Update Italian translation

## 1.64.2
* Separate crop window settings for image scanning
* Fix initializing dataMask
* Update Russian translation

## 1.64.1
* Handle empty input for recreation
* Fix setting up database

## 1.64.0
* Find barcode by content
* Immediately replace toasts
* Use detected barcode for recreation
* Show data mask of QR Codes
* Fix deep linking
* Replace margin with quiet zone switch when generating barcodes
* Update Russian translation
* Update Portuguese (Brazil) translation

## 1.63.13
* Fix UTF-8 conversion of binary escape sequences when creating barcodes
* Fix scan order for intervals under one second
* Add arabic translation
* Update French translation

## 1.63.12
* Fix magnifying barcode recreation

## 1.63.11
* Update parcel tracking URL
* Update multiple translations
* Only ask for barcode size when exporting/sharing

## 1.63.10
* Use maximum brightness when showing a generated barcode
* Allow file input from share menu
* Update multiple translations

## 1.63.9
* Export/share as JPEG
* Ignore "URL:" before URLs in barcode content
* Fix exporting/sharing multiple history items
* Update multiple translations

## 1.63.8
* Add an option to show a checksum for the decoded content
* Add support for encoding escape sequences for non-printable characters
* Update Italian translation

## 1.63.7
* Update ZXingCpp for Aztec Runes
* Update Italian translation

## 1.63.6
* Add history shortcut
* Return Codabar start/stop codes
* Update French, Russian, Spanish and Ukrainian translations

## 1.63.5
* Add an option to ignore any duplicates
* Fix invalid bluetooth connection
* Update Russian translation
* Update Ukrainian translation

## 1.63.4
* Update Chinese translation
* Improve support for old devices

## 1.63.3
* Fix SCAN Intent

## 1.63.2
* Add DX Film Edge barcode format
* Update Turkish translation
* Update Ukrainian translation

## 1.63.1
* Include scan label in exports/shares
* Update Russian translation

## 1.63.0
* Edit scan label in decoding screen
* Add a button to add generated codes to the history
* Improve scanning from images
* Improve asking for camera permission
* Fix margin for recreated barcodes
* Limit duration of error beep to 1s
* Update Ukrainian translation
* Update Spanish translation

## 1.62.3
* Extend selection with single taps in history listing
* Update Brazilian Portuguese translation

## 1.62.2
* Include rMQR Code for updates

## 1.62.1
* Make rMQR Code selectable

## 1.62.0
* Add support for rMQR (rectangular Micro QR) Code
* Share history as a file
* Only share selected scans from history

## 1.61.2
* Fix quick settings tile for Android 14
* Fix Xiaomi MIUI lockscreen barcode shortcut
* Update French translation
* Update Czech translation
* Update Russian translation

## 1.61.1
* Share scanned contents as a file
* Recreate binary barcodes too
* Add Bulgarian translation and Brave search engine
* Update Russian translation
* Fix general format restriction from preferences

## 1.61.0
* Support encoding binary data
* Add margin parameter for encoding
* Add copying binary data as hex dump
* Fix parsing TYPE argument in VCARDs
* Update Russian translation
* Update Brazilian Portuguese translation
* Update Ukrainian translation
* Update Chinese translation

## 1.60.3
* Fix saving custom language
* Update Brazilian Portuguese translation
* Update Chinese translation
* Update Czech localisation
* Update Russian translation
* Update Italian language

## 1.60.2
* Expand escape sequences in encoding input
* Update Taiwan translation

## 1.60.1
* Support sending scans via bluetooth
* Fix resolving error correction level for recreation
* Improve asset image compression
* Update ZXing C++
* Update Italian translation
* Update Russian translation

## 1.59.0
* Add setting error correction for AZTEC/PDF417
* Fix setting error correction level for QR Codes
* Make error beeps follow beep settings too
* Don't close the app when cameras is unavailable

## 1.58.2
* Fix parsing WiFi network suggestions

## 1.58.1
* Update zh-rCn translation
* Fix monochrome launcher icon

## 1.58.0
* Improve detection of inverted barcodes
* Optionally allow duplicates in bulk scan
* Add Danish translation
* Update Italian translation
* Update Japanese translation
* Update Russian translation
* Fix charset decoding for PDF 417
* Fix monochrome icon

## 1.57.0
* Add optional audio feedback for scanning
* Allow removing individual WiFi network suggestions
* Recognize incomplete and malformed WiFi barcodes
* Fix using shared text for encoding
* Add Korean language
* Update Chinese translation
* Update Português/Brasil translation
* Update Russian translation

## 1.56.3
* Fix parsing URLs in barcodes
* Update French, Portuguese, Turkish and Taiwan translation

## 1.56.2
* Fix generating 1D barcode text output

## 1.56.1
* Allow pinching a barcode to its generated size
* Fix view/frame transformation matrix
* Fix handling of tab characters
* Update Indonesian translation
* Update Russian translation

## 1.56.0
* Add an action item to lock/unlock free rotation
* Support setting fore and background colors when creating barcodes
* Fix creating barcodes with UTF-8 content

## 1.55.0
* Make recreated barcode interactive
* Update to latest ZXing C++
* Update Italian translation

## 1.54.1
* Update Russian translation
* Update Turkish translation
* Update Italian translation
* Fix handling GS1 and ISO15434 content types
* Fix VerifyError on Android 4

## 1.54.0
* Recreate and show barcode from read contents if possible
* Add ebay, Amazon and barcodelookup.com to search engines

## 1.53.1
* Fix proguard configuration

## 1.53.0
* Migrate to ZXing-C++ and add support for Micro QR Codes
* Multi-select scans in history listing
* Remove preset content for Encode shortcut
* Always show QR Code version and remove setting
* Fix PDF-417 UTF-8 text encoding
* Hide sensitive content from clipboard
* Remove setting a custom language on Android 13+
* Condense text output of generated barcodes
* Update Russian translation
* Update Spanish translation
* Add Persian translation

## 1.52.0
* Fix parsing timezone in calendar dates
* Update French translation

## 1.51.1
* Fix extraction of raw bytes for some barcodes

## 1.51.0
* Add support for Deutsche Post Matrixcode stamp
* Allow free image rotation for scanning from images
* Fix setting TO field in MATMSG format
* Update Italian translation
* Update zh-rCN translation
* Update pt-br translation

## 1.50.0
* Add support for MATMSG format
* Add a setting to show the QR Code version
* Add timezone offset in calendar dates
* Recognize colloquial incomplete URLs
* Update ZXing library to latest version
* Update Czech translation
* Update Russian translation
* Update Japanese translation
* Update Hungarian translation

## 1.49.2
* Update design of app icon
* Update pt-br translation

## 1.49.1
* Update Simplified Chinese translation
* Return directly to caller for deep links
* Fix metadata for deep links

## 1.49.0
* Add a setting to quickly enable/disable forwarding
* Stop playing error beep when in silent mode
* Improve enabling/disabling torch mode
* Update to latest ZXing version
* Update German translation
* Update Russian translation

## 1.48.1
* Fix broken detection of binary content

## 1.48.0
* Add a menu item to temporarily scan a certain format only
* Add a monochome launcher icon for Android 13
* Show alpha numeric characters in binary data
* Fix showing binary data label for scans from the history
* Fix handling empty meta strings
* Update Turkish translation
* Update Ukrainian translation
* Update French translation
* Update Polish translation
* Update Russian translation

## 1.47.0
* Add forwarding into external browser
* Fix visibility of zoom slider when switching cameras

## 1.46.3
* Fix cropping still images
* Improve usability of zoom slider
* Update Dutch translation
* Update Chinese translation

## 1.46.2
* Fix saving external files on Android Q and newer

## 1.46.1
* Use cropping limiter settings for image picking too
* Remove extra link on result screen
* Update Turkish translation

## 1.46.0
* Support deep links
* Support "A:" in WIFI codes
* Allow multiple query arguments in custom URL
* Add Wi-Fi info data section
* Add Vietnamese translation
* Update Brazilian Portuguese translation
* Update Hungarian translation
* Update Ukrainian translation
* Update Russian translation

## 1.45.0
* Add a setting to set the delay for bulk mode
* Add support for MECARD contact format
* Remove unused permissions on Android Q

## 1.44.1
* Update Spanish translation
* Update Japanese translation
* Fix in-app language switching for App Bundles

## 1.44.0
* Add a setting to choose barcode formats to scan
* Ask before removing network suggestions
* Update Traditional Chinese Translation
* Update Russian language
* Update Dutch translation

## 1.43.0
* Add a preference item to remove WiFi network suggestions
* Update Traditional Chinese Translation
* Update Russian language

## 1.42.0
* Add a setting to pick a default search engine
* Update Traditional Chinese
* Update Dutch translation

## 1.41.1
* Update Bengali translation
* Update Simplified Chinese

## 1.41.0
* Use cropping limiter by default for new installations
* Truncate all toasts with strings from the outside
* Add Bangla translation
* Add missing Japanese locale
* Update Hungarian translation
* Update Russian app description

## 1.40.0
* Add a setting to show/hide scanned data when scanning continuously
* Only show the first 50 characters when scanning continuously
* Share binary data as hex dump
* Update Russian language

## 1.39.0
* Fix adding contacts and calendar entries on Android 11+
* Add a setting to always start in bulk mode
* Add Czech translation
* Update Spanish translation
* Update Czech translation

## 1.38.0
* Add a quick settings tile to scan from settings
* Make custom background requests time out after 5 seconds
* Add Japanese translation
* Updated French translation

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
