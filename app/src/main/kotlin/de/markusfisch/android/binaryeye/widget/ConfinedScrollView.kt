package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import de.markusfisch.android.binaryeye.app.setSystemAndToolBarTransparency

class ConfinedScrollView : ScrollView {
	private var scrollsAlways = false

	constructor(context: Context, attrs: AttributeSet, defStyle: Int) :
			super(context, attrs, defStyle)

	constructor(context: Context, attrs: AttributeSet) :
			this(context, attrs, 0)

	override fun onLayout(
		changed: Boolean,
		left: Int,
		top: Int,
		right: Int,
		bottom: Int
	) {
		super.onLayout(changed, left, top, right, bottom)
		if (changed) {
			getChildAt(0)?.also { child ->
				scrollsAlways = height < child.height + paddingTop + paddingBottom
				post({
					setSystemAndToolBarTransparency(context, scrollsAlways)
				})
			}
		}
	}

	override fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int) {
		super.onScrollChanged(x, y, oldx, oldy)
		setSystemAndToolBarTransparency(context, scrollsAlways || y > 0)
	}
}
