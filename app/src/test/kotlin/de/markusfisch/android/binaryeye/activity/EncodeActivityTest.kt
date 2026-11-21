package de.markusfisch.android.binaryeye.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.View
import de.markusfisch.android.binaryeye.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.fakes.RoboMenuItem
import java.io.File

@RunWith(RobolectricTestRunner::class)
class EncodeActivityTest {
	@Test
	fun keepsImportedBytesAfterRecreation() {
		Robolectric.buildActivity(EncodeActivity::class.java).setup().use { controller ->
			val activity = controller.get()
			val bytes = byteArrayOf(1, 0, -1)
			val file = File(activity.cacheDir, "import.bin").apply { writeBytes(bytes) }
			val main = shadowOf(Looper.getMainLooper())
			val shadow = shadowOf(activity)
			activity.onOptionsItemSelected(RoboMenuItem(R.id.pick_file))
			val request = shadow.nextStartedActivityForResult.intent
			shadow.receiveResult(request, Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))

			val content = activity.findViewById<View>(R.id.content)
			val deadline = System.nanoTime() + 3_000_000_000L
			while (content.isEnabled && System.nanoTime() < deadline) {
				main.idle()
				Thread.sleep(10)
			}
			assertFalse(content.isEnabled)

			controller.recreate()
			val recreated = controller.get()
			assertFalse(recreated.findViewById<View>(R.id.content).isEnabled)
			recreated.findViewById<View>(R.id.encode).performClick()
			main.idle()
			assertArrayEquals(
				bytes,
				shadowOf(recreated).nextStartedActivity.getByteArrayExtra("content_raw")
			)
		}
	}
}
