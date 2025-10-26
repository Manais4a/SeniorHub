package com.seniorhub.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.seniorhub.R
import com.seniorhub.models.EmergencyService

/**
 * Adapter for displaying emergency services in a RecyclerView
 * 
 * Features:
 * - Call functionality with phone number extraction
 * - Map integration with Google Maps
 * - Website functionality
 * - Accessibility support
 * - Service type badges
 */
class EmergencyServicesAdapter(
    private val services: MutableList<EmergencyService>,
    private val onServiceClick: ((EmergencyService) -> Unit)? = null
) : RecyclerView.Adapter<EmergencyServicesAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvServiceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val tvServicePhone: TextView = itemView.findViewById(R.id.tvServicePhone)
        val btnCall: MaterialButton = itemView.findViewById(R.id.btnCall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        
        // Set basic information
        holder.tvServiceName.text = service.name
        holder.tvServicePhone.text = "(${service.getFormattedPhoneNumber()})"

        // Set up action button
        setupCallButton(holder, service)

        // Set up card click listener
        holder.itemView.setOnClickListener {
            onServiceClick?.invoke(service)
        }
    }

    private fun setupCallButton(holder: ServiceViewHolder, service: EmergencyService) {
        val phoneNumber = service.getFormattedPhoneNumber()
        
        if (service.hasPhoneNumber()) {
            holder.btnCall.visibility = View.VISIBLE
            holder.btnCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                if (intent.resolveActivity(holder.itemView.context.packageManager) != null) {
                    holder.itemView.context.startActivity(intent)
                } else {
                    Toast.makeText(holder.itemView.context, "Unable to make call", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.btnCall.visibility = View.GONE
        }
    }

    // Map and Website buttons removed per new design

    override fun getItemCount(): Int = services.size

    /**
     * Update the services list
     */
    fun updateServices(newServices: List<EmergencyService>) {
        android.util.Log.d("EmergencyServicesAdapter", "updateServices called with ${newServices.size} services")
        services.clear()
        services.addAll(newServices)
        android.util.Log.d("EmergencyServicesAdapter", "Adapter now has ${services.size} services")
        notifyDataSetChanged()
    }

    /**
     * Add a single service
     */
    fun addService(service: EmergencyService) {
        services.add(service)
        notifyItemInserted(services.size - 1)
    }

    /**
     * Remove a service
     */
    fun removeService(serviceId: String) {
        val index = services.indexOfFirst { it.id == serviceId }
        if (index != -1) {
            services.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    /**
     * Get service by ID
     */
    fun getServiceById(serviceId: String): EmergencyService? {
        return services.find { it.id == serviceId }
    }
}

