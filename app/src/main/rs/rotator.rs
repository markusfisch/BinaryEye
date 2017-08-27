#pragma version(1)
#pragma rs java_package_name(de.markusfisch.android.binaryeye.renderscript)

rs_allocation inImage;
int inWidth;
int inHeight;

uchar4 RS_KERNEL rotate90(uchar4 in, uint32_t x, uint32_t y) {
	const uchar4 *out = rsGetElementAt(inImage, y, inHeight - 1 - x);
	return *out;
}

uchar4 RS_KERNEL rotate180(uchar4 in, uint32_t x, uint32_t y) {
	const uchar4 *out = rsGetElementAt(
			inImage,
			inWidth - 1 - x,
			inHeight - 1 - y);
	return *out;
}

uchar4 RS_KERNEL rotate270(uchar4 in, uint32_t x, uint32_t y) {
	const uchar4 *out = rsGetElementAt(inImage, inWidth - 1 - y, x);
	return *out;
}
