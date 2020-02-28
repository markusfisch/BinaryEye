package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import de.markusfisch.android.binaryeye.graphics.Candidates

class DetectorView : View {
	private val candidates = Candidates(context)
	private var marks: List<Point>? = null

	constructor(context: Context, attrs: AttributeSet) :
			super(context, attrs)

	constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
			super(context, attrs, defStyleAttr)

	fun mark(points: List<Point>) {
		marks = points
		invalidate()
		postDelayed({
			marks = null
			invalidate()
		}, 500)
	}

	override fun onDraw(canvas: Canvas) {
		canvas.drawColor(0, PorterDuff.Mode.CLEAR)
		marks?.let {
			candidates.draw(canvas, it)
		}
	}
}
