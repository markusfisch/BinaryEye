#pragma version(1)
#pragma rs java_package_name(de.markusfisch.android.binaryeye.rs)
#pragma rs reduce(analyze) initializer(analyzeInit) accumulator(analyzeAccumulator) combiner(analyzeCombiner) outconverter(analyzeConverter)

const static float3 relativeLuminance = {0.2126f, 0.7152f, 0.0722f};

typedef struct {
	int transparent;
	int visible;
	int bright;
} Report;

static void analyzeInit(Report *report) {
	report->transparent = 0;
	report->visible = 0;
	report->bright = 0;
}

static void analyzeAccumulator(Report *accum, uchar4 val) {
	if (val.a < 255) {
		accum->transparent = 1;
	}
	if (val.a > 0) {
		++accum->visible;
		float4 f4 = rsUnpackColor8888(val);
		float3 result = dot(f4.rgb, relativeLuminance);
		accum->bright += step(0.5f, result.r + result.g + result.b);
	}
}

static void analyzeCombiner(Report *accum, const Report *val) {
	accum->transparent += val->transparent;
	accum->visible += val->visible;
	accum->bright += val->bright;
}

static void analyzeConverter(int2 *result, const Report *report) {
	result->x = report->transparent;
	result->y = report->bright > 0 && report->visible / report->bright < 2;
}
