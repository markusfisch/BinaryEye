#pragma version(1)
#pragma rs java_package_name(de.markusfisch.android.binaryeye.rs)
#pragma rs_fp_relaxed

rs_allocation inImage;
int inWidth;
int inHeight;

uchar RS_KERNEL rotate90(const uchar in, uint32_t x, uint32_t y) {
	const uchar *out = rsGetElementAt(inImage, y, inHeight - 1 - x);
	return *out;
}

uchar RS_KERNEL rotate180(const uchar in, uint32_t x, uint32_t y) {
	const uchar *out = rsGetElementAt(
			inImage,
			inWidth - 1 - x,
			inHeight - 1 - y);
	return *out;
}

uchar RS_KERNEL rotate270(const uchar in, uint32_t x, uint32_t y) {
	const uchar *out = rsGetElementAt(inImage, inWidth - 1 - y, x);
	return *out;
}
