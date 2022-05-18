# Binary Eye

Yet another barcode scanner for Android. As if there weren't [enough][play].

This one is free, without any ads and open source.

Works in portrait and landscape orientation, can read inverted codes,
comes in Material Design and can also generate barcodes.

Binary Eye uses the [ZXing][zxing] ("Zebra Crossing") barcode scanning
library.

## Screenshots

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-scanning.png"
	alt="Screenshot Gallery" width="160"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-scanning-cropped.png"
	alt="Screenshot Gallery" width="160"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-decoded.png"
	alt="Screenshot Theme" width="160"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screencap-compose-barcode.png"
	alt="Screenshot Editor" width="160"/>

## Download

<a href="https://f-droid.org/en/packages/de.markusfisch.android.binaryeye/"><img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80"/></a> <a href="https://play.google.com/store/apps/details?id=de.markusfisch.android.binaryeye"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="80"/></a>

## Supported Barcode Formats

### Read

ZXing can read the following barcode formats:
* [AZTEC][aztec]
* [CODABAR][codabar]
* [CODE 39][code_39]
* [CODE 93][code_93]
* [CODE 128][code_128]
* [DATA MATRIX][data_matrix]
* [EAN 8][ean_8]
* [EAN 13][ean_13]
* [ITF][itf]
* [MAXICODE][maxicode] (only when unrotated and unskewed, see [77][77],
	because of which Binary Eye *cannot* read this barcode)
* [PDF417][pdf417]
* [QR CODE][qr_code]
* [RSS 14][rss]
* [RSS EXPANDED][rss]
* [UPC A][upc_a]
* [UPC E][upc_e]
* [UPC EAN EXTENSION][upc_ean]

### Generate

ZXing can generate the following barcode formats:
* [AZTEC][aztec]
* [CODABAR][codabar]
* [CODE 39][code_39]
* [CODE 128][code_128]
* [DATA MATRIX][data_matrix]
* [EAN 8][ean_8]
* [EAN 13][ean_13]
* [ITF][itf]
* [PDF 417][pdf417]
* [QR CODE][qr_code]
* [UPC A][upc_a]

## Deep Links and Intents

### Deep Links

You can invoke Binary Eye with a web URI intent from anything that can
open URIs. There are two options:

1. [binaryeye://scan](binaryeye://scan)
2. [http(s)://markusfisch.de/BinaryEye](http://markusfisch.de/BinaryEye)

If you want to get the scanned contents, you can add a `ret` query
argument with a (URL encoded) URI template. For example:

[http://markusfisch.de/BinaryEye?ret=http%3A%2F%2Fexample.com%2F%3Fresult%3D{RESULT}](http://markusfisch.de/BinaryEye?ret=http%3A%2F%2Fexample.com%2F%3Fresult%3D{RESULT})

Supported symbols are:

* `RESULT` - scanned content
* `RESULT_BYTES` - raw result as a hex string
* `FORMAT` - barcode format
* `META` - the meta data, if available

### SCAN Intent

You can also use Binary Eye from other apps by using the
`com.google.zxing.client.android.SCAN` Intent with
[startActivityForResult()][start_activity] like this:

```kotlin
startActivityForResult(
	Intent("com.google.zxing.client.android.SCAN"),
	SOME_NUMBER
)
```

And process the result in [onActivityResult()][on_activity_result] of your
`Activity`:

```kotlin
override fun onActivityResult(
	requestCode: Int,
	resultCode: Int,
	data: Intent?
) {
	when (requestCode) {
		SOME_NUMBER -> if (resultCode == RESULT_OK) {
			val result = data.getStringExtra("SCAN_RESULT")
			…
		}
	}
}
```

If you're using AndroidX, this would be the new,
[recommended way][intent_result]:

```kotlin
class YourActivity : Activity() {
	private val resultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			val scan = result.data?.getStringExtra("SCAN_RESULT")
			…
		}
	}

	fun openScanner() {
		resultLauncher.launch(Intent("com.google.zxing.client.android.SCAN"))
	}
}
```

## RenderScript

This app uses [RenderScript][rs] to resize and rotate the camera image.
Unfortunately, RenderScript has some nasty gotchas.

### RenderScript.forceCompat()

It's necessary to call `RenderScript.forceCompat()` on some devices/roms.

`RenderScript.forceCompat()` needs to be run before any other RenderScript
function and unfortunately there is no way to know if invoking `forceCompat()`
is necessary or not.

If `RenderScript.forceCompat()` is necessary, a `RSRuntimeException` will
be thrown and the only option is to restart the app, this time with calling
`forceCompat()` first.

Calling `RenderScript.forceCompat()` means the processing is done in
software so you probably don't want to enable it by default.

### 2D barcodes

If you want to fork this and are only interested in reading 2D barcodes
(like QR or Aztec), you may want to remove the custom rotation kernel
altogether as ZXing can read 2D barcodes in any orientation.

This will make your app a bit simpler and saves you from compiling a
custom RenderScript kernel for each architecture you want to support.

[play]: https://play.google.com/store/search?q=barcode%20scanner&c=apps
[zxing]: https://github.com/zxing/zxing
[kotlin]: http://kotlinlang.org/
[aztec]: https://en.wikipedia.org/wiki/Aztec_Code
[codabar]: https://en.wikipedia.org/wiki/Codabar
[code_39]: https://en.wikipedia.org/wiki/Code_39
[code_93]: https://en.wikipedia.org/wiki/Code_93
[code_128]: https://en.wikipedia.org/wiki/Code_128
[data_matrix]: https://en.wikipedia.org/wiki/Data_Matrix
[ean_8]: https://en.wikipedia.org/wiki/EAN-8
[ean_13]: https://en.wikipedia.org/wiki/International_Article_Number
[itf]: https://en.wikipedia.org/wiki/Interleaved_2_of_5
[maxicode]: https://en.wikipedia.org/wiki/MaxiCode
[pdf417]: https://en.wikipedia.org/wiki/PDF417
[qr_code]: https://en.wikipedia.org/wiki/QR_code
[rss]: https://en.wikipedia.org/wiki/GS1_DataBar
[upc_a]: https://en.wikipedia.org/wiki/Universal_Product_Code
[upc_e]: https://en.wikipedia.org/wiki/Universal_Product_Code#UPC-E
[upc_ean]: https://en.wikipedia.org/wiki/Universal_Product_Code#EAN-13
[77]: https://github.com/markusfisch/BinaryEye/issues/77
[rs]: https://developer.android.com/guide/topics/renderscript/compute
[start_activity]: https://developer.android.com/reference/android/app/Activity#startActivityForResult(android.content.Intent,%20int)
[on_activity_result]: https://developer.android.com/reference/android/app/Activity#onActivityResult(int,%20int,%20android.content.Intent)
[intent_result]: https://developer.android.com/training/basics/intents/result
