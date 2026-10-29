package com.igotu.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.igotu.contacts.EmergencyContact
import com.igotu.contacts.EmergencyContactsRepository
import com.igotu.databinding.ActivityEmergencyContactsBinding
import com.igotu.databinding.DialogAddContactBinding
import com.igotu.databinding.ItemEmergencyContactBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class EmergencyContactsViewModel @Inject constructor(
    private val repository: EmergencyContactsRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init { load() }

    fun load() {
        viewModelScope.launch {
            repository.getContacts()
                .onSuccess { _contacts.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun add(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.addContact(contact)
                .onSuccess { load() }
                .onFailure { _error.value = it.message }
        }
    }

    fun update(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.updateContact(contact)
                .onSuccess { load() }
                .onFailure { _error.value = it.message }
        }
    }

    fun delete(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
                .onSuccess { load() }
                .onFailure { _error.value = it.message }
        }
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class EmergencyContactsAdapter(
    private val onEdit: (EmergencyContact) -> Unit,
    private val onDelete: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactsAdapter.VH>() {

    private var items = listOf<EmergencyContact>()

    fun submit(list: List<EmergencyContact>) { items = list; notifyDataSetChanged() }

    inner class VH(val binding: ItemEmergencyContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemEmergencyContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.binding.tvAvatar.text = c.name.firstOrNull()?.uppercase() ?: "?"
        holder.binding.tvContactName.text = c.name
        holder.binding.tvContactDetail.text = listOfNotNull(c.relationship, c.phone ?: c.email)
            .joinToString(" · ")
        holder.binding.chipSms.visibility      = if (c.notifySms)      View.VISIBLE else View.GONE
        holder.binding.chipWhatsapp.visibility = if (c.notifyWhatsapp) View.VISIBLE else View.GONE
        holder.binding.chipEmail.visibility    = if (c.notifyEmail)    View.VISIBLE else View.GONE
        holder.binding.btnEdit.setOnClickListener { onEdit(c) }
        holder.binding.btnDelete.setOnClickListener { onDelete(c) }
    }
}

// ── Activity ──────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private val viewModel: EmergencyContactsViewModel by viewModels()
    private lateinit var adapter: EmergencyContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = EmergencyContactsAdapter(
            onEdit = { showContactDialog(it) },
            onDelete = { confirmDelete(it) }
        )
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.fabAddContact.setOnClickListener { showContactDialog(null) }

        lifecycleScope.launch {
            viewModel.contacts.collect { contacts ->
                adapter.submit(contacts)
                binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.error.collect { err ->
                err?.let { Toast.makeText(this@EmergencyContactsActivity, it, Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun showContactDialog(existing: EmergencyContact?) {
        val sheet = BottomSheetDialog(this)
        val db = DialogAddContactBinding.inflate(layoutInflater)
        sheet.setContentView(db.root)

        db.cbWhatsapp.setOnCheckedChangeListener { _, checked ->
            db.tvWhatsappNote.visibility = if (checked) View.VISIBLE else View.GONE
        }

        db.tvDialogTitle.text = if (existing == null) "Add Emergency Contact" else "Edit Contact"
        existing?.let {
            db.etName.setText(it.name)
            db.etRelationship.setText(it.relationship)
            db.etPhone.setText(it.phone)
            db.etEmail.setText(it.email)
            db.cbSms.isChecked = it.notifySms
            db.cbWhatsapp.isChecked = it.notifyWhatsapp
            db.cbEmail.isChecked = it.notifyEmail
            db.tvWhatsappNote.visibility = if (it.notifyWhatsapp) View.VISIBLE else View.GONE
        }

        db.btnSaveContact.setOnClickListener {
            val name = db.etName.text?.toString().orEmpty().trim()
            val phone = db.etPhone.text?.toString().orEmpty().trim().ifEmpty { null }
            val email = db.etEmail.text?.toString().orEmpty().trim().ifEmpty { null }

            if (name.isEmpty()) { db.tilName.error = "Required"; return@setOnClickListener }
            if (phone == null && email == null) {
                db.tilPhone.error = "Phone or email required"
                return@setOnClickListener
            }
            // WhatsApp requires a phone number
            if (db.cbWhatsapp.isChecked && phone == null) {
                db.tilPhone.error = "Phone number required for WhatsApp"
                return@setOnClickListener
            }
            db.tilName.error = null
            db.tilPhone.error = null

            val contact = EmergencyContact(
                id = existing?.id ?: "",
                name = name,
                phone = phone,
                email = email,
                relationship = db.etRelationship.text?.toString().orEmpty().trim().ifEmpty { null },
                notifySms = db.cbSms.isChecked,
                notifyWhatsapp = db.cbWhatsapp.isChecked,
                notifyEmail = db.cbEmail.isChecked,
                priority = existing?.priority ?: 1
            )
            if (existing == null) viewModel.add(contact) else viewModel.update(contact)
            sheet.dismiss()
        }
        sheet.show()
    }

    private fun confirmDelete(contact: EmergencyContact) {
        AlertDialog.Builder(this)
            .setTitle("Remove contact?")
            .setMessage("${contact.name} will no longer receive SOS alerts.")
            .setPositiveButton("Remove") { _, _ -> viewModel.delete(contact.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
