package com.eric.rockpaperscissor

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var productTextView: TextView
    var descriptionTextView: TextView
    var durationTextView: TextView
    var subscribeButton: Button

    init {
        itemView.apply {
            productTextView = findViewById(R.id.product_textview)
            descriptionTextView = findViewById(R.id.description_textview)
            durationTextView = findViewById(R.id.duration_textview)
            subscribeButton = findViewById(R.id.subscribe_button)
        }
    }
}