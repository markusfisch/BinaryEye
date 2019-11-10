package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import de.markusfisch.android.binaryeye.app.setSystemAndToolBarTransparency
import de.markusfisch.android.scalingimageview.widget.ScalingImageView

open class ConfinedScalingImageView : ScalingImageView {
	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle)

	constructor(context: Context, attrs: AttributeSet) :
			this(context, attrs, 0)

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.actionMasked) {
			MotionEvent.ACTION_UP,
			MotionEvent.ACTION_MOVE -> setSystemAndToolBarTransparency(
				context, !inBounds()
			)
		}
		return super.onTouchEvent(event)
	}
}
