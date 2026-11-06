package com.smartphonetop.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartphonetop.app.R
import com.smartphonetop.app.core.MediaScanner
import com.smartphonetop.app.data.MediaEntry
import com.smartphonetop.app.data.MediaKind
import com.smartphonetop.app.data.SmartStore
import com.smartphonetop.app.data.formatBytes
import com.smartphonetop.app.databinding.FragmentFilesBinding
import com.smartphonetop.app.ui.Refreshable
import com.smartphonetop.app.ui.adapters.MediaAdapter
import com.smartphonetop.app.util.Permissions
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FilesFragment : Fragment(), Refreshable {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private var files: List<MediaEntry> = emptyList()
    private var filter = "ALL"
    private var selectionMode = false
    private val selected = mutableSetOf<Uri>()
    private var pendingDelete: List<MediaEntry> = emptyList()
    private var duplicateUris: Set<Uri> = emptySet()
    private var scanning = false

    private val mediaPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        scan()
    }
    private val deleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val entries = pendingDelete
        pendingDelete = emptyList()
        if (result.resultCode == Activity.RESULT_OK && entries.isNotEmpty()) {
            val freed = entries.sumOf { it.sizeBytes }
            SmartStore.recordClean("Mídia", freed, entries.size, entries.take(3).joinToString { it.name })
            selected.clear()
            toast("${entries.size} arquivo(s) excluído(s) • ${formatBytes(freed)}")
            scan()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.grantMedia.setOnClickListener { mediaPermission.launch(Permissions.mediaPermissions()) }
        binding.filterGroup.setOnCheckedStateChangeListener { _, ids ->
            filter = when (ids.firstOrNull()) {
                R.id.filterImages -> "IMAGE"
                R.id.filterVideos -> "VIDEO"
                R.id.filterAudio -> "AUDIO"
                else -> "ALL"
            }
            render()
        }
        binding.selectButton.setOnClickListener { toggleSelectionMode() }
        binding.largeCard.setOnClickListener {
            filter = "LARGE"
            binding.filterAll.isChecked = false
            binding.filterImages.isChecked = false
            binding.filterVideos.isChecked = false
            binding.filterAudio.isChecked = false
            render()
        }
        binding.oldCard.setOnClickListener {
            filter = "OLD"
            binding.filterAll.isChecked = false
            binding.filterImages.isChecked = false
            binding.filterVideos.isChecked = false
            binding.filterAudio.isChecked = false
            render()
        }
        binding.duplicateCard.setOnClickListener {
            filter = "DUP"
            binding.filterAll.isChecked = false
            binding.filterImages.isChecked = false
            binding.filterVideos.isChecked = false
            binding.filterAudio.isChecked = false
            render()
        }
        binding.deleteSelected.setOnClickListener { confirmDeleteSelected() }
        if (Permissions.hasMediaPermission(requireContext())) scan() else refresh()
    }

    override fun onResume() {
        super.onResume()
        if (Permissions.hasMediaPermission(requireContext())) scan() else refresh()
    }

    override fun refresh() = scan()

    private fun scan() {
        if (scanning) return
        if (!Permissions.hasMediaPermission(requireContext())) {
            binding.permissionCard.visibility = View.VISIBLE
            binding.list.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyTitle.text = "Permissão necessária"
            binding.count.text = "Acesso à mídia não concedido"
            binding.selectButton.visibility = View.GONE
            binding.selectionBar.visibility = View.GONE
            return
        }
        scanning = true
        binding.permissionCard.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        lifecycleScope.launch {
            files = MediaScanner.scan(requireContext())
            duplicateUris = MediaScanner.findExactDuplicates(requireContext(), files)
            scanning = false
            render()
        }
    }

    private fun filteredFiles(): List<MediaEntry> {
        val oldThreshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        return when (filter) {
            "LARGE" -> files.filter { it.sizeBytes > 50L * 1024 * 1024 }
            "OLD" -> files.filter {
                it.kind == MediaKind.IMAGE && it.dateAdded * 1000L < oldThreshold &&
                    (it.name.contains("screenshot", true) || it.name.contains("captura", true))
            }
            "DUP" -> files.filter { duplicateUris.contains(it.uri) }
            "ALL" -> files
            else -> files.filter { it.kind.name == filter }
        }
    }

    private fun render() {
        if (_binding == null) return
        val filtered = filteredFiles()
        val large = files.filter { it.sizeBytes > 50L * 1024 * 1024 }
        val old = files.filter {
            it.kind == MediaKind.IMAGE && it.dateAdded * 1000L < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30) &&
                (it.name.contains("screenshot", true) || it.name.contains("captura", true))
        }
        binding.largeCount.text = "${large.size} • ${formatBytes(large.sumOf { it.sizeBytes })}"
        binding.oldCount.text = "${old.size} • ${formatBytes(old.sumOf { it.sizeBytes })}"
        binding.duplicateCount.text = duplicateUris.size.toString()
        binding.count.text = "${filtered.size} arquivo(s)  •  ${formatBytes(filtered.sumOf { it.sizeBytes })}"
        binding.list.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyTitle.text = "Nenhuma mídia encontrada"
        binding.selectButton.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
        binding.selectButton.text = if (selectionMode) "Cancelar" else "Selecionar"
        binding.selectionBar.visibility = if (selectionMode) View.VISIBLE else View.GONE
        binding.selectionCount.text = "${selected.size} selecionado(s) • ${formatBytes(selected.sumOf { uri -> files.firstOrNull { it.uri == uri }?.sizeBytes ?: 0L })}"
        binding.deleteSelected.isEnabled = selected.isNotEmpty()
        binding.deleteSelected.alpha = if (selected.isEmpty()) 0.5f else 1f
        binding.list.adapter = MediaAdapter(
            filtered,
            selectionMode,
            selected,
            onOpen = ::openFile,
            onToggle = ::toggleSelection,
            onDelete = ::confirmDelete
        )
    }

    private fun toggleSelectionMode() {
        selectionMode = !selectionMode
        if (!selectionMode) selected.clear()
        render()
    }

    private fun toggleSelection(entry: MediaEntry) {
        if (!selectionMode) {
            selectionMode = true
        }
        if (selected.contains(entry.uri)) selected.remove(entry.uri) else selected.add(entry.uri)
        render()
    }

    private fun openFile(entry: MediaEntry) {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(entry.uri, entry.mime.ifBlank { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }.onFailure { toast("Nenhum app pode abrir este arquivo") }
    }

    private fun confirmDelete(entry: MediaEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir arquivo?")
            .setMessage("${entry.name}\n${entry.formattedSize}\n\nEssa ação remove a mídia do aparelho.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> deleteEntries(listOf(entry)) }
            .show()
    }

    private fun confirmDeleteSelected() {
        val entries = files.filter { selected.contains(it.uri) }
        if (entries.isEmpty()) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir ${entries.size} arquivo(s)?")
            .setMessage("Total: ${formatBytes(entries.sumOf { it.sizeBytes })}\n\nEssa ação remove a mídia selecionada do aparelho.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> deleteEntries(entries) }
            .show()
    }

    private fun deleteEntries(entries: List<MediaEntry>) {
        if (entries.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingDelete = entries
            runCatching {
                val pending = MediaStore.createDeleteRequest(requireContext().contentResolver, entries.map { it.uri })
                deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            }.onFailure {
                pendingDelete = emptyList()
                toast("O Android não permitiu solicitar a exclusão")
            }
        } else {
            runCatching {
                var freed = 0L
                var count = 0
                entries.forEach { entry ->
                    if (requireContext().contentResolver.delete(entry.uri, null, null) > 0) {
                        freed += entry.sizeBytes
                        count++
                    }
                }
                if (freed > 0) {
                    SmartStore.recordClean("Mídia", freed, count, entries.take(3).joinToString { it.name })
                    selected.clear()
                    toast("$count arquivo(s) excluído(s)")
                    scan()
                } else {
                    toast("Não foi possível excluir")
                }
            }.onFailure { toast("Sem permissão para excluir estes arquivos") }
        }
    }

    private fun toast(text: String) = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
