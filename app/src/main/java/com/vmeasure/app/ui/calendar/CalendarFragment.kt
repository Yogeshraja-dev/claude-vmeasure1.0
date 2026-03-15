package com.vmeasure.app.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class CalendarFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val tv = TextView(requireContext()).apply {
            text = "Calendar\n(Coming Soon)"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
        }
        return tv
    }
}