package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import de.markusfisch.android.binaryeye.app.setSystemAndToolBarTransparency
import de.markusfisch.android.scalingimageview.widget.ScalingImageView

open class ConfinedScalingImageView : ScalingImageView {
	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle)

	constructor(context: Context, attrs: AttributeSet) :
			this(context, attrs, 0)

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		setSystemAndToolBarTransparency(context, !bounds.contains(mappedRect))
	}
}
