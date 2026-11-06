package com.smartphonetop.app.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartphonetop.app.R
import com.smartphonetop.app.core.CacheAnalyzer
import com.smartphonetop.app.data.AppEntry
import com.smartphonetop.app.databinding.FragmentAppsBinding
import com.smartphonetop.app.ui.Refreshable
import com.smartphonetop.app.ui.adapters.AppsAdapter
import com.smartphonetop.app.util.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsFragment : Fragment(), Refreshable {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private var apps: List<AppEntry> = emptyList()
    private var filter = "ALL"
    private var query = ""
    private var scanning = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                render()
            }
        })
        binding.filterGroup.setOnCheckedStateChangeListener { _, ids ->
            filter = when (ids.firstOrNull()) {
                R.id.filterUser -> "USER"
                R.id.filterSystem -> "SYSTEM"
                else -> "ALL"
            }
            render()
        }
        scan()
    }

    override fun onResume() {
        super.onResume()
        if (apps.isNotEmpty()) scan()
    }

    override fun refresh() = scan()

    private fun scan() {
        if (scanning) return
        scanning = true
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            apps = withContext(Dispatchers.IO) { CacheAnalyzer(requireContext()).analyzeAllApps() }
            scanning = false
            binding.progress.visibility = View.GONE
            render()
        }
    }

    private fun render() {
        if (_binding == null) return
        val filtered = apps.filter { app ->
            val filterOk = when (filter) {
                "USER" -> !app.isSystem
                "SYSTEM" -> app.isSystem
                else -> true
            }
            filterOk && (query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true))
        }
        binding.count.text = "${apps.size} aplicativos instalados  •  ${apps.count { !it.isSystem }} de usuário"
        binding.list.adapter = AppsAdapter(
            filtered,
            onOpen = { app ->
                val intent = requireContext().packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) runCatching { startActivity(intent) }
                else Toast.makeText(requireContext(), "Este app não tem tela inicial", Toast.LENGTH_SHORT).show()
            },
            onInfo = { app -> startActivity(Permissions.appDetailsIntent(app.packageName)) },
            onUninstall = { app ->
                if (app.isSystem) {
                    Toast.makeText(requireContext(), "Apps do sistema não podem ser desinstalados aqui", Toast.LENGTH_SHORT).show()
                } else {
                    runCatching {
                        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
