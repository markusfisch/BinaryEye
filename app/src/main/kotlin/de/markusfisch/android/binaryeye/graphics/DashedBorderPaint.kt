package de.markusfisch.android.binaryeye.graphics

import android.content.Context
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import de.markusfisch.android.binaryeye.R

fun Context.getDashedBorderPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
	val dp = resources.displayMetrics.density
	color = ContextCompat.getColor(applicationContext, R.color.crop_bound)
	style = Paint.Style.STROKE
	strokeWidth = dp * 2f
	pathEffect = DashPathEffect(floatArrayOf(10f * dp, 10f * dp), 0f)
}
