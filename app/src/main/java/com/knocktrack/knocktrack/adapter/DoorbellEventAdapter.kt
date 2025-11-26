package com.knocktrack.knocktrack.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.knocktrack.knocktrack.R
import com.knocktrack.knocktrack.model.DoorbellEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView adapter for displaying doorbell events in the history screen.
 */
class DoorbellEventAdapter(
    private var events: List<DoorbellEvent> = emptyList(),
    private var onDeleteClick: ((DoorbellEvent) -> Unit)? = null
) : RecyclerView.Adapter<DoorbellEventAdapter.DoorbellEventViewHolder>() {

    class DoorbellEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventText: TextView = itemView.findViewById(R.id.tvEventText)
        val tvEventTime: TextView = itemView.findViewById(R.id.tvEventTime)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoorbellEventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doorbell_event, parent, false)
        return DoorbellEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoorbellEventViewHolder, position: Int) {
        val event = events[position]
        
        // Set event text
        holder.tvEventText.text = "🔔 Doorbell Pressed"
        
        // Format and set time
        val timeText = formatEventTime(event)
        holder.tvEventTime.text = timeText
        
        // Set delete button click listener
        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(event)
        }
    }

    override fun getItemCount(): Int = events.size

    /**
     * Updates the events list and notifies the adapter.
     */
    fun updateEvents(newEvents: List<DoorbellEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    /**
     * Formats the event time for display.
     */
    private fun formatEventTime(event: DoorbellEvent): String {
        return try {
            // Use the time from the event if available
            if (event.time.isNotEmpty()) {
                event.time
            } else {
                // Fallback to timestamp formatting
                val date = Date(event.timestamp)
                val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                formatter.format(date)
            }
        } catch (e: Exception) {
            "Unknown time"
        }
    }
}
