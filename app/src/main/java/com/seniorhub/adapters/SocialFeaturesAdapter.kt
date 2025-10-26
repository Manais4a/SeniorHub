package com.seniorhub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.seniorhub.models.SocialFeature

/**
 * SocialFeaturesAdapter - RecyclerView adapter for social features
 *
 * Designed for senior citizens with:
 * - Large touch targets
 * - Clear visual hierarchy
 * - Accessible content descriptions
 * - Simple interaction patterns
 */
class SocialFeaturesAdapter(
    private val onFeatureClick: (SocialFeature) -> Unit,
    private val onFeatureToggled: (String, Boolean) -> Unit
) : ListAdapter<SocialFeature, SocialFeaturesAdapter.SocialFeatureViewHolder>(SocialFeatureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocialFeatureViewHolder {
        // Using a simple layout approach instead of view binding for now
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.activity_list_item, parent, false)
        return SocialFeatureViewHolder(view, onFeatureClick, onFeatureToggled)
    }

    override fun onBindViewHolder(holder: SocialFeatureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SocialFeatureViewHolder(
        itemView: View,
        private val onFeatureClick: (SocialFeature) -> Unit,
        private val onFeatureToggled: (String, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(feature: SocialFeature) {
            // Set up the view with feature data
            try {
                // Use standard Android views
                itemView.findViewById<TextView>(android.R.id.text1)?.apply {
                    text = feature.title
                    textSize = 18f // Larger text for seniors
                }

                itemView.findViewById<TextView>(android.R.id.text2)?.apply {
                    text = feature.description
                    textSize = 14f
                }

                // Try to find and set up additional views if available
                itemView.findViewById<ImageView>(android.R.id.icon)?.apply {
                    try {
                        setImageResource(feature.iconResId)
                    } catch (e: Exception) {
                        // Use a default icon resource if available
                        setImageResource(android.R.drawable.ic_dialog_info)
                    }
                }

                // Set up unread count badge if available
                if (feature.hasUnreadMessages()) {
                    itemView.findViewById<TextView>(android.R.id.summary)?.apply {
                        visibility = View.VISIBLE
                        text = if (feature.unreadCount > 9) "9+" else feature.unreadCount.toString()
                        setBackgroundResource(android.R.drawable.presence_online)
                    }
                } else {
                    itemView.findViewById<TextView>(android.R.id.summary)?.visibility = View.GONE
                }

                // Set up participant count
                val participantText = when (feature.getParticipantCount()) {
                    0 -> "No participants"
                    1 -> "1 participant"
                    else -> "${feature.getParticipantCount()} participants"
                }

                // Show feature type and participant info
                itemView.findViewById<TextView>(android.R.id.text1)?.apply {
                    text = "${feature.title} - ${feature.type.getDisplayName()}"
                }

                itemView.findViewById<TextView>(android.R.id.text2)?.apply {
                    text = "${feature.description}\n$participantText"
                }

                // Set up click listener for the entire item
                itemView.setOnClickListener {
                    onFeatureClick(feature)
                }

                // Set up long click for accessibility
                itemView.setOnLongClickListener {
                    // Provide voice feedback or additional info
                    true
                }

                // Set content description for accessibility
                itemView.contentDescription = "${feature.title}: ${feature.description}. " +
                        "${feature.type.getDisplayName()}. $participantText. " +
                        if (feature.hasUnreadMessages()) "${feature.unreadCount} unread messages." else "No new messages."

            } catch (e: Exception) {
                // Handle any view binding errors gracefully
                itemView.findViewById<TextView>(android.R.id.text1)?.text = feature.title
                itemView.findViewById<TextView>(android.R.id.text2)?.text = feature.description
            }
        }
    }

    class SocialFeatureDiffCallback : DiffUtil.ItemCallback<SocialFeature>() {
        override fun areItemsTheSame(oldItem: SocialFeature, newItem: SocialFeature): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SocialFeature, newItem: SocialFeature): Boolean {
            return oldItem == newItem
        }
    }
}