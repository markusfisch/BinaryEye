# Optional Tapirscan development fallback

Ordinary builds continue to use ZXing, support API 21 and retain their ABIs.
For a local comparison build, build Tapirscan from a pinned source revision:

```sh
# In the Tapirscan checkout, with its pinned Rust toolchain and an Android NDK:
python3 scripts/build_android.py --ndk "$ANDROID_NDK_HOME"
# In BinaryEye:
./gradlew assembleDebug -PtapirscanAar=/absolute/path/to/tapirscan/build/android/tapirscan-android.aar
```

This optional build targets API 24+ and arm64-v8a/x86_64. No AAR is downloaded or
committed. Record the Tapirscan commit and AAR SHA-256 alongside results; the new
Telepen, MicroPDF417 and rMQR readers require a development revision containing
them, and must not be attributed to the published 1.5.0 release.

ZXing runs first, retaining its format support, metadata and all barcode
creation. Tapirscan runs only on unsuccessful camera frames and only for mapped,
selected formats. It receives a copy of the cropped, rotated Y plane. Closing
the activity disposes the reusable native scanner on the analyzer executor.
Native load or scan failure disables this development fallback for the activity.

This is a development comparison, not a complete replacement. The fallback
requires rotation enabled, plain text, and ignored EAN supplements. It currently
excludes GS1, structured append and non-text payloads; richer metadata mapping,
explicit format-family expansion, 32-bit support and API 21 compatibility remain
library or adapter work. Missing metadata (EC level, version, mask, symbology
identifier) is left empty rather than inferred. Device camera testing is required
before considering default enablement.

Validated development source: `980a415b271d58738c6475bba26e56d351f3115c` in
[Tapirscan](https://github.com/kleinicke/tapirscan). Textual linear readers may
omit payload bytes; the adapter reconstructs bytes only for printable ASCII.
Binary-capable formats require original payload bytes matching their UTF-8 text.
