package de.markusfisch.android.binaryeye.app

import android.annotation.SuppressLint
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import de.markusfisch.android.binaryeye.R

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
) = fm.beginTransaction().replace(R.id.content_frame, fragment)
