package com.smartphonetop.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartphonetop.app.databinding.SheetMoreBinding

class MoreSheet : BottomSheetDialogFragment() {
    private var _binding: SheetMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.close.setOnClickListener { dismiss() }
        binding.securityCard.setOnClickListener { open(SecurityActivity::class.java) }
        binding.aiCard.setOnClickListener { open(TopAiActivity::class.java) }
        binding.historyCard.setOnClickListener { open(HistoryActivity::class.java) }
        binding.settingsCard.setOnClickListener { open(SettingsActivity::class.java) }
    }

    private fun open(activity: Class<*>) {
        startActivity(Intent(requireContext(), activity))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
