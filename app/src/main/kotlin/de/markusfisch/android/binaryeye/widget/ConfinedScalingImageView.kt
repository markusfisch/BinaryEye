package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import de.markusfisch.android.binaryeye.view.colorSystemAndToolBars
import de.markusfisch.android.scalingimageview.widget.ScalingImageView

open class ConfinedScalingImageView : ScalingImageView {
	val insets = Rect()

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle)

	constructor(context: Context, attrs: AttributeSet) :
			this(context, attrs, 0)

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		colorSystemAndToolBars(context, !bounds.contains(mappedRect))
	}

	override fun layoutImage(
		changed: Boolean,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int
	) {
		if (changed) {
			setBounds(
				(left + insets.left).toFloat(),
				(top + insets.top).toFloat(),
				(right - insets.right).toFloat(),
				(bottom - insets.bottom).toFloat()
			)
		}
		centerRemap()
	}
}
