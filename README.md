# Binary Eye

Yet another barcode scanner for Android. As if there weren't [enough][play].

This one is free, without any ads and open source.

Works in portrait and landscape orientation, can read inverted codes,
comes in Material Design and can also generate barcodes.

Binary Eye uses the [ZXing][zxing] ("Zebra Crossing") barcode scanning
library.

## Supported Barcode Formats

### Read

ZXing can _read_ the following barcode formats:
* [AZTEC][aztec]
* [CODABAR][codabar]
* [CODE 39][code_39]
* [CODE 93][code_93]
* [CODE 128][code_128]
* [DATA MATRIX][data_matrix]
* [EAN 8][ean_8]
* [EAN 13][ean_13]
* [ITF][itf]
* [MAXICODE][maxicode]
* [PDF417][pdf417]
* [QR CODE][qr_code]
* [RSS 14][rss]
* [RSS EXPANDED][rss]
* [UPC A][upc_a]
* [UPC E][upc_e]
* [UPC EAN EXTENSION][upc_ean]

### Generate

ZXing can _generate_ the following barcode formats:
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
