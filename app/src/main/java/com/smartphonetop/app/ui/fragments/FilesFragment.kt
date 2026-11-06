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

class FilesFragment : Fragment(), Refreshable {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private var files: List<MediaEntry> = emptyList()
    private var filter = "ALL"
    private var pendingDelete: MediaEntry? = null
    private var scanning = false

    private val mediaPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        scan()
    }
    private val deleteLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val entry = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && entry != null) {
            SmartStore.recordClean("Mídia", entry.sizeBytes, 1, "Exclusão de ${entry.name}")
            toast("${entry.name} excluído")
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
        if (Permissions.hasMediaPermission(requireContext())) scan()
        else refresh()
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
            return
        }
        scanning = true
        binding.permissionCard.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        lifecycleScope.launch {
            files = MediaScanner.scan(requireContext())
            scanning = false
            render()
        }
    }

    private fun render() {
        if (_binding == null) return
        val filtered = files.filter {
            filter == "ALL" || it.kind.name == filter
        }
        binding.count.text = "${filtered.size} arquivo(s)  •  ${formatBytes(filtered.sumOf { it.sizeBytes })}"
        binding.list.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyTitle.text = "Nenhuma mídia encontrada"
        binding.list.adapter = MediaAdapter(filtered, onOpen = ::openFile, onDelete = ::confirmDelete)
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
            .setPositiveButton("Excluir") { _, _ -> delete(entry) }
            .show()
    }

    private fun delete(entry: MediaEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingDelete = entry
            runCatching {
                val pending = MediaStore.createDeleteRequest(requireContext().contentResolver, listOf(entry.uri))
                deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            }.onFailure {
                pendingDelete = null
                toast("O Android não permitiu solicitar a exclusão")
            }
        } else {
            runCatching {
                val deleted = requireContext().contentResolver.delete(entry.uri, null, null)
                if (deleted > 0) {
                    SmartStore.recordClean("Mídia", entry.sizeBytes, 1, "Exclusão de ${entry.name}")
                    toast("Arquivo excluído")
                    scan()
                } else {
                    toast("Não foi possível excluir")
                }
            }.onFailure { toast("Sem permissão para excluir este arquivo") }
        }
    }

    private fun toast(text: String) = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
