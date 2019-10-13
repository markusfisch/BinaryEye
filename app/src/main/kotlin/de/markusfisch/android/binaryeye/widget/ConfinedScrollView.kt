package de.markusfisch.android.binaryeye.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import de.markusfisch.android.binaryeye.app.setSystemAndToolBarTransparency

class ConfinedScrollView : ScrollView {
	private var scrollable = false

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
			// give Android some time to settle down before running this;
			// not putting it on the queue makes it only work sometimes
			post {
				getChildAt(0)?.also { child ->
					scrollable = height < child.height + paddingTop + paddingBottom
					setSystemAndToolBarTransparency(
						context,
						false,
						scrollable
					)
				}
			}
		}
	}

	override fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int) {
		super.onScrollChanged(x, y, oldx, oldy)
		setSystemAndToolBarTransparency(context, y > 0, scrollable)
	}
}
