package de.markusfisch.android.binaryeye.net

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.markusfisch.android.binaryeye.database.Scan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ContinuousScanSession : ViewModel() {
	enum class State { READY, APPROVAL, SENDING, FAILED }

	val state = MutableLiveData(State.READY)
	var configured = false
		private set
	var url: String? = null
		private set
	var error: String? = null
		private set
	var lastText: String? = null
		private set
	private var payload: String? = null
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

	@Volatile
	var blocked = false
		private set

	fun configure(url: String?, savedState: Bundle? = null) {
		configured = true
		this.url = url
		lastText = savedState?.getString("lastText")
		payload = savedState?.getString("payload")
		setState(when {
			url == null -> State.READY
			payload != null -> State.FAILED
			savedState?.getBoolean("approved") == true -> State.READY
			else -> State.APPROVAL
		})
	}

	fun approve() {
		setState(State.READY)
	}

	fun submit(scan: Scan) {
		if (blocked) {
			return
		}
		lastText = scan.text
		payload = scan.asJson(mapOf("scanId" to UUID.randomUUID().toString()))
			.toString()
		retry()
	}

	fun retry() {
		val target = url ?: return
		val body = payload ?: return
		if (state.value == State.SENDING) {
			return
		}
		setState(State.SENDING)
		scope.launch {
			val response = withContext(Dispatchers.IO) {
				postScanCallback(target, body)
			}
			if (response.code != null && response.code in 200..299) {
				payload = null
				error = null
				setState(State.READY)
			} else {
				error = listOfNotNull(
					response.code?.toString(), response.body
				).joinToString("\n")
				setState(State.FAILED)
			}
		}
	}

	fun saveState() = Bundle().apply {
		putBoolean("approved", state.value != State.APPROVAL)
		putString("payload", payload)
		putString("lastText", lastText)
	}

	private fun setState(value: State) {
		blocked = value != State.READY
		state.value = value
	}

	override fun onCleared() {
		scope.cancel()
	}
}
