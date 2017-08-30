package de.markusfisch.android.binaryeye.fragment

import de.markusfisch.android.binaryeye.app.addFragment
import de.markusfisch.android.binaryeye.R

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast

class CreateBarcodeFragment : Fragment() {
	companion object {
		private val CONTENT = "content"

		fun newInstance(content: String): Fragment {
			val args = Bundle()
			args.putString(CONTENT, content)
			val fragment = CreateBarcodeFragment()
			fragment.setArguments(args)
			return fragment
		}
	}

	override fun onCreateView(
			inflater: LayoutInflater,
			container: ViewGroup?,
			state: Bundle?): View {
		activity.setTitle(R.string.create_barcode)

		val view = inflater.inflate(
				R.layout.fragment_create_barcode,
				container,
				false)

		val formatView = view.findViewById<Spinner>(R.id.format)
		val contentView = view.findViewById<EditText>(R.id.content)

		val args = getArguments()
		args?.let { contentView.setText(args.getString(CONTENT)) }

		view.findViewById<View>(R.id.create).setOnClickListener { v ->
			val format = formatView.getSelectedItem().toString()
			val content = contentView.getText().toString()
			if (content.isEmpty()) {
				Toast.makeText(v.context, R.string.error_no_content,
						Toast.LENGTH_SHORT).show()
			} else {
				addFragment(fragmentManager,
						BarcodeFragment.newInstance(content, format))
			}
		}

		return view
	}
}
