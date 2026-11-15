package de.markusfisch.android.binaryeye.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

private const val LOCAL_NETWORK_PERMISSION_TAG = "local_network_permission"

fun FragmentActivity.withLocalNetworkPermission(callback: (Boolean) -> Unit) {
	if (Build.VERSION.SDK_INT < 37 || ContextCompat.checkSelfPermission(
			this,
			Manifest.permission.ACCESS_LOCAL_NETWORK
		) == PackageManager.PERMISSION_GRANTED
	) {
		callback(true)
		return
	}
	val manager = supportFragmentManager
	if (manager.isStateSaved) {
		callback(false)
		return
	}
	val fragment = manager.findFragmentByTag(LOCAL_NETWORK_PERMISSION_TAG)
		as? LocalNetworkPermissionFragment ?: LocalNetworkPermissionFragment().also {
		manager.beginTransaction().add(it, LOCAL_NETWORK_PERMISSION_TAG).commitNow()
	}
	fragment.request(callback)
}

class LocalNetworkPermissionFragment : Fragment() {
	private val callbacks = mutableListOf<(Boolean) -> Unit>()
	private var permissionDenied = false
	private val launcher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted ->
		permissionDenied = !granted
		val pending = callbacks.toList()
		callbacks.clear()
		pending.forEach { it(granted) }
	}

	@RequiresApi(37)
	fun request(callback: (Boolean) -> Unit) {
		if (permissionDenied) {
			callback(false)
			return
		}
		callbacks.add(callback)
		if (callbacks.size == 1) {
			launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
		}
	}
}
