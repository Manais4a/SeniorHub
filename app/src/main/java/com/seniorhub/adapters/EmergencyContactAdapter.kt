package com.seniorhub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seniorhub.R
import com.seniorhub.models.EmergencyContact

/**
 * EmergencyContactAdapter - RecyclerView Adapter for Emergency Contacts
 *
 * This adapter manages the display and interaction of emergency contacts in the profile screen.
 * Features:
 * - Display contact information with clear, senior-friendly typography
 * - Primary contact highlighting
 * - Edit, delete, and set primary actions
 * - Accessibility support with proper content descriptions
 * - Large touch targets for senior users
 */
class EmergencyContactAdapter(
    private var emergencyContacts: MutableList<EmergencyContact>,
    private val onEditContact: (EmergencyContact) -> Unit,
    private val onDeleteContact: (EmergencyContact) -> Unit,
    private val onSetPrimary: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.EmergencyContactViewHolder>() {

    /**
     * ViewHolder for emergency contact items
     */
    class EmergencyContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvContactName)
        val tvRelationship: TextView = itemView.findViewById(R.id.tvContactRelationship)
        val tvPhone: TextView = itemView.findViewById(R.id.tvContactPhone)
        val tvEmail: TextView = itemView.findViewById(R.id.tvContactEmail)
        val tvAddress: TextView = itemView.findViewById(R.id.tvContactAddress)
        val tvPrimaryBadge: TextView = itemView.findViewById(R.id.tvPrimaryBadge)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditContact)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteContact)
        val btnSetPrimary: ImageButton = itemView.findViewById(R.id.btnSetPrimary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_contact, parent, false)
        return EmergencyContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmergencyContactViewHolder, position: Int) {
        val contact = emergencyContacts[position]
        
        // Set contact information
        holder.tvName.text = contact.name
        holder.tvRelationship.text = contact.relationship.ifBlank { "Emergency Contact" }
        holder.tvPhone.text = contact.phoneNumber
        holder.tvEmail.text = contact.email.ifBlank { "No email provided" }
        holder.tvAddress.text = contact.address.ifBlank { "No address provided" }
        
        // Handle primary contact display
        if (contact.isPrimary) {
            holder.tvPrimaryBadge.visibility = View.VISIBLE
            holder.tvPrimaryBadge.text = "PRIMARY"
            holder.btnSetPrimary.visibility = View.GONE
        } else {
            holder.tvPrimaryBadge.visibility = View.GONE
            holder.btnSetPrimary.visibility = View.VISIBLE
        }
        
        // Set up click listeners
        holder.btnEdit.setOnClickListener {
            onEditContact(contact)
        }
        
        holder.btnDelete.setOnClickListener {
            onDeleteContact(contact)
        }
        
        holder.btnSetPrimary.setOnClickListener {
            onSetPrimary(contact)
        }
        
        // Set accessibility content descriptions
        holder.itemView.contentDescription = buildString {
            append("Emergency contact: ${contact.name}")
            if (contact.relationship.isNotBlank()) {
                append(", ${contact.relationship}")
            }
            append(", Phone: ${contact.phoneNumber}")
            if (contact.isPrimary) {
                append(", Primary contact")
            }
        }
        
        holder.btnEdit.contentDescription = "Edit ${contact.name}"
        holder.btnDelete.contentDescription = "Delete ${contact.name}"
        holder.btnSetPrimary.contentDescription = "Set ${contact.name} as primary contact"
    }

    override fun getItemCount(): Int = emergencyContacts.size

    /**
     * Update the contacts list and notify adapter
     */
    fun updateContacts(newContacts: List<EmergencyContact>) {
        emergencyContacts.clear()
        emergencyContacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    /**
     * Add a new contact
     */
    fun addContact(contact: EmergencyContact) {
        emergencyContacts.add(contact)
        notifyItemInserted(emergencyContacts.size - 1)
    }

    /**
     * Update an existing contact
     */
    fun updateContact(contact: EmergencyContact) {
        val index = emergencyContacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            emergencyContacts[index] = contact
            notifyItemChanged(index)
        }
    }

    /**
     * Remove a contact
     */
    fun removeContact(contact: EmergencyContact) {
        val index = emergencyContacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            emergencyContacts.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}

