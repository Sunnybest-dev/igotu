package com.igotu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.igotu.connections.ConnectionPermissions
import com.igotu.connections.ConnectionsRepository
import com.igotu.connections.IgotuConnection
import com.igotu.databinding.ActivityConnectionsBinding
import com.igotu.databinding.ItemConnectionBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val repository: ConnectionsRepository
) : ViewModel() {

    private val _connections = MutableStateFlow<List<IgotuConnection>>(emptyList())
    val connections: StateFlow<List<IgotuConnection>> = _connections

    private val _inviteUrl = MutableStateFlow<String?>(null)
    val inviteUrl: StateFlow<String?> = _inviteUrl

    fun loadConnections() {
        viewModelScope.launch {
            _connections.value = repository.getConnections().getOrElse { emptyList() }
        }
    }

    fun sendRequest(userId: String) {
        viewModelScope.launch { repository.sendRequest(userId) }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch { repository.acceptRequest(requestId); loadConnections() }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch { repository.rejectRequest(requestId) }
    }

    fun removeConnection(connectionId: String) {
        viewModelScope.launch { repository.removeConnection(connectionId); loadConnections() }
    }

    fun updatePermissions(connectionId: String, permissions: ConnectionPermissions) {
        viewModelScope.launch { repository.updatePermissions(connectionId, permissions) }
    }

    fun blockUser(connectionId: String) {
        viewModelScope.launch { repository.blockUser(connectionId) }
    }

    fun createInvitation() {
        viewModelScope.launch {
            repository.createInvitation()
                .onSuccess { _inviteUrl.value = it.shareUrl }
        }
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class ConnectionsAdapter(
    private val onRemove: (IgotuConnection) -> Unit
) : RecyclerView.Adapter<ConnectionsAdapter.VH>() {

    private var items = listOf<IgotuConnection>()
    fun submit(list: List<IgotuConnection>) { items = list; notifyDataSetChanged() }

    inner class VH(val binding: ItemConnectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemConnectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.binding.tvConnectionAvatar.text = c.connectedUserId.firstOrNull()?.uppercase() ?: "?"
        holder.binding.tvConnectionId.text = c.connectedUserId
        holder.binding.tvStatus.text = c.status.name
        holder.binding.btnRemove.setOnClickListener { onRemove(c) }
    }
}

// ── Activity ──────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class ConnectionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectionsBinding
    private val viewModel: ConnectionsViewModel by viewModels()
    private lateinit var adapter: ConnectionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ConnectionsAdapter(onRemove = { viewModel.removeConnection(it.connectionId) })
        binding.rvConnections.layoutManager = LinearLayoutManager(this)
        binding.rvConnections.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnInvite.setOnClickListener { viewModel.createInvitation() }

        lifecycleScope.launch {
            viewModel.connections.collect { renderConnections(it) }
        }
        lifecycleScope.launch {
            viewModel.inviteUrl.collect { url ->
                url?.let {
                    Toast.makeText(this@ConnectionsActivity, "Invite link: $it", Toast.LENGTH_LONG).show()
                }
            }
        }
        viewModel.loadConnections()
    }

    private fun renderConnections(connections: List<IgotuConnection>) {
        adapter.submit(connections)
        binding.tvEmpty.visibility = if (connections.isEmpty()) View.VISIBLE else View.GONE
    }
}
