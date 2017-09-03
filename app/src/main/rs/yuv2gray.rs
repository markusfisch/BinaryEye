#pragma version(1)
#pragma rs java_package_name(de.markusfisch.android.binaryeye.renderscript)
#pragma rs_fp_relaxed

rs_allocation inYUV;
uint32_t inWidth;
uint32_t inHeight;

uchar4 RS_KERNEL yuv2gray(uint32_t x, uint32_t y) {
	uchar Y = rsGetElementAt_uchar(inYUV, x, y);
	uchar4 out;
	out.r = Y;
	out.g = Y;
	out.b = Y;
	out.a = 255;
	return out;
}

uchar4 RS_KERNEL yuv2inverted(uint32_t x, uint32_t y) {
	uchar Y = 255 - rsGetElementAt_uchar(inYUV, x, y);
	uchar4 out;
	out.r = Y;
	out.g = Y;
	out.b = Y;
	out.a = 255;
	return out;
}
