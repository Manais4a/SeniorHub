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
import com.seniorhub.models.SocialService

/**
 * Adapter for displaying social services in a RecyclerView
 * 
 * Features:
 * - Call functionality with phone number extraction
 * - Map integration with Google Maps
 * - Email functionality
 * - Accessibility support
 * - Service type badges
 */
class SocialServicesAdapter(
    private val services: MutableList<SocialService>,
    private val onServiceClick: ((SocialService) -> Unit)? = null
) : RecyclerView.Adapter<SocialServicesAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvServiceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val tvServiceType: TextView = itemView.findViewById(R.id.tvServiceType)
        val tvServiceAddress: TextView = itemView.findViewById(R.id.tvServiceAddress)
        val tvServicePhone: TextView = itemView.findViewById(R.id.tvServicePhone)
        val tvServiceEmail: TextView = itemView.findViewById(R.id.tvServiceEmail)
        val tvServicesOffered: TextView = itemView.findViewById(R.id.tvServicesOffered)
        val btnCall: MaterialButton = itemView.findViewById(R.id.btnCall)
        val btnMap: MaterialButton = itemView.findViewById(R.id.btnMap)
        val btnWebsite: MaterialButton = itemView.findViewById(R.id.btnWebsite)
        val headerContainer: View = itemView.findViewById(R.id.headerContainer)
        val detailsContainer: View = itemView.findViewById(R.id.detailsContainer)
        val ivExpand: View = itemView.findViewById(R.id.ivExpand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_social_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        
        // Set basic information
        holder.tvServiceName.text = service.name
        holder.tvServiceType.text = service.serviceType
        holder.tvServiceAddress.text = service.address
        holder.tvServicePhone.text = "Tel: ${service.phoneNumber ?: "N/A"}"
        holder.tvServiceEmail.text = "Email: ${service.email ?: "N/A"}"
        holder.tvServicesOffered.text = service.servicesOffered

        // Collapsed by default
        holder.detailsContainer.visibility = View.GONE
        holder.ivExpand.rotation = 0f

        // Set up expand/collapse functionality
        val toggle: (View) -> Unit = {
            val isVisible = holder.detailsContainer.visibility == View.VISIBLE
            holder.detailsContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
            holder.ivExpand.animate().rotation(if (isVisible) 0f else 180f).setDuration(150).start()
        }
        
        // Only the header should be clickable for expand/collapse
        holder.headerContainer.setOnClickListener(toggle)

        // Set up action buttons
        setupCallButton(holder, service)
        setupMapButton(holder, service)
        setupWebsiteButton(holder, service)

        // Set up card click listener for service details
        holder.itemView.setOnLongClickListener {
            onServiceClick?.invoke(service)
            true
        }
    }

    private fun setupCallButton(holder: ServiceViewHolder, service: SocialService) {
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
                    Toast.makeText(holder.itemView.context, R.string.error_making_call, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.btnCall.visibility = View.GONE
        }
    }

    private fun setupMapButton(holder: ServiceViewHolder, service: SocialService) {
        if (service.hasAddress()) {
            holder.btnMap.visibility = View.VISIBLE
            holder.btnMap.setOnClickListener {
                val address = service.address
                val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                
                if (mapIntent.resolveActivity(holder.itemView.context.packageManager) != null) {
                    holder.itemView.context.startActivity(mapIntent)
                } else {
                    // Fallback to web browser
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=" + Uri.encode(address)))
                    if (webIntent.resolveActivity(holder.itemView.context.packageManager) != null) {
                        holder.itemView.context.startActivity(webIntent)
                    } else {
                        Toast.makeText(holder.itemView.context, R.string.error_opening_map, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            holder.btnMap.visibility = View.GONE
        }
    }

    private fun setupWebsiteButton(holder: ServiceViewHolder, service: SocialService) {
        if (service.website.isNotBlank()) {
            holder.btnWebsite.visibility = View.VISIBLE
            holder.btnWebsite.setOnClickListener {
                try {
                    val websiteUrl = if (service.website.startsWith("http://") || service.website.startsWith("https://")) {
                        service.website
                    } else {
                        "https://${service.website}"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                    if (intent.resolveActivity(holder.itemView.context.packageManager) != null) {
                        holder.itemView.context.startActivity(intent)
                    } else {
                        Toast.makeText(holder.itemView.context, "No browser app available", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Error opening website", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.btnWebsite.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = services.size

    /**
     * Update the services list
     */
    fun updateServices(newServices: List<SocialService>) {
        android.util.Log.d("SocialServicesAdapter", "updateServices called with ${newServices.size} services")
        services.clear()
        services.addAll(newServices)
        android.util.Log.d("SocialServicesAdapter", "Adapter now has ${services.size} services")
        notifyDataSetChanged()
    }

    /**
     * Add a single service
     */
    fun addService(service: SocialService) {
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
    fun getServiceById(serviceId: String): SocialService? {
        return services.find { it.id == serviceId }
    }
}

