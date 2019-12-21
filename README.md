# Binary Eye

Yet another barcode scanner for Android. As if there weren't [enough][play].

This one is free, without any ads and completely open source.

Works in portrait and landscape orientation, can read inverted codes,
comes in Material Design and can also generate barcodes.

Uses the [ZXing][zxing] ("Zebra Crossing") barcode scanning library that
supports the following barcode formats for reading:
AZTEC, CODABAR, CODE 39, CODE 93, CODE 128, DATA MATRIX, EAN 8, EAN 13, ITF,
MAXICODE, PDF417, QR CODE, RSS 14, RSS EXPANDED, UPC A, UPC E and
UPC EAN EXTENSION.

And those barcodes formats can be generated with ZXing:
AZTEC, CODABAR, CODE 39, CODE 128, DATA MATRIX, EAN 8, EAN 13, ITF, PDF 417,
QR CODE and UPC A.

And it's written in [Kotlin][kotlin] because I wanted to explore that a
bit more.

## Screenshots

<img
	src="https://raw.githubusercontent.com/markusfisch/BinaryEye/gh-pages/screencap-scanning.png"
	alt="Screenshot Scanning" width="160"/>
<img
	src="https://raw.githubusercontent.com/markusfisch/BinaryEye/gh-pages/screencap-decoded.png"
	alt="Screenshot Result" width="160"/>
<img
	src="https://raw.githubusercontent.com/markusfisch/BinaryEye/gh-pages/screencap-compose-barcode.png"
	alt="Screenshot Compose Barcode" width="160"/>

## Download

<a href="https://f-droid.org/en/packages/de.markusfisch.android.binaryeye/"><img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80"/></a> <a href="https://play.google.com/store/apps/details?id=de.markusfisch.android.binaryeye"><img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" height="80"/></a>

[play]: https://play.google.com/store/search?q=barcode%20scanner&c=apps
[zxing]: https://github.com/zxing/zxing
[kotlin]: http://kotlinlang.org/
