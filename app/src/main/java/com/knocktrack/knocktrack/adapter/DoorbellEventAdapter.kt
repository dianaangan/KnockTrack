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
        
        // Format and set time with date
        val timeText = formatEventTime(event)
        val dateText = formatEventDate(event)
        holder.tvEventTime.text = "$timeText - $dateText"
        
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
     * Formats the event time for display as "11:30:05 AM" (with seconds).
     * ESP32 sends format like "07:30:45.123 PM" - we extract "07:30:45 PM"
     */
    private fun formatEventTime(event: DoorbellEvent): String {
        return try {
            // Use the time from the event if available
            if (event.time.isNotEmpty()) {
                // ESP32 format: "07:30:45.123 PM" -> extract "07:30:45 PM"
                val parts = event.time.trim().split(" ")
                if (parts.size >= 2) {
                    val timePart = parts[0] // "07:30:45.123"
                    val amPm = parts[1] // "PM"
                    // Extract HH:MM:SS (before milliseconds)
                    val hourMinuteSec = if (timePart.contains(".")) {
                        timePart.substring(0, timePart.indexOf(".")) // "07:30:45"
                    } else if (timePart.length >= 8) {
                        timePart.substring(0, 8) // "07:30:45"
                    } else {
                        timePart
                    }
                    "$hourMinuteSec $amPm" // "07:30:45 PM"
                } else {
                    event.time
                }
            } else {
                // Fallback to timestamp formatting
                val date = Date(event.timestamp * 1000) // Convert to milliseconds
                val formatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                formatter.format(date)
            }
        } catch (e: Exception) {
            "Unknown time"
        }
    }
    
    /**
     * Formats the event date for display.
     * ESP32 sends format like "2025-11-26" - we format as "Nov 26, 2025"
     */
    private fun formatEventDate(event: DoorbellEvent): String {
        return try {
            // Use the date from the event if available
            if (event.date.isNotEmpty()) {
                // ESP32 format: "2025-11-26" -> format as "Nov 26, 2025"
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(event.date)
                date?.let { outputFormat.format(it) } ?: event.date
            } else {
                // Fallback to timestamp formatting
                val date = Date(event.timestamp * 1000) // Convert to milliseconds
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                formatter.format(date)
            }
        } catch (e: Exception) {
            // If parsing fails, try to return as is or format from timestamp
            try {
                val date = Date(event.timestamp * 1000)
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                formatter.format(date)
            } catch (e2: Exception) {
                "Unknown date"
            }
        }
    }
}
