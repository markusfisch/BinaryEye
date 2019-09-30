package de.markusfisch.android.binaryeye.app

import android.annotation.SuppressLint
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.view.View
import de.markusfisch.android.binaryeye.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun setFragment(fm: FragmentManager?, fragment: Fragment) {
	fm?.let { getTransaction(fm, fragment).commit() }
}

fun addFragment(fm: FragmentManager?, fragment: Fragment) {
	fm?.let { getTransaction(fm, fragment).addToBackStack(null).commit() }
}

@SuppressLint("CommitTransaction")
private fun getTransaction(
	fm: FragmentManager,
	fragment: Fragment
): FragmentTransaction {
	return fm.beginTransaction().replace(R.id.content_frame, fragment)
}

suspend inline fun View.useVisibility(whileExecuting: Int = View.VISIBLE, otherwise: Int = View.GONE, crossinline block: suspend () -> Unit) {
	if (visibility == whileExecuting) return
	withContext(Dispatchers.Main) {
		visibility = whileExecuting
	}
	try {
		block()
	} finally {
		withContext(Dispatchers.Main) {
			visibility = otherwise
		}
	}
}
