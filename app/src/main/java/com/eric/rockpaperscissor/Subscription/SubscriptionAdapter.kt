package com.eric.rockpaperscissor.Subscription

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eric.rockpaperscissor.BuildConfig
import com.eric.rockpaperscissor.R
import com.huawei.hms.iap.entity.ProductInfo

class SubscriptionAdapter(
    private val productInfoList: List<ProductInfo>,
    private val subsribedProducts: List<String>, activity: Activity
) : RecyclerView.Adapter<SubscriptionViewHolder>() {

    private var activity: Activity? = activity

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val rootView =
            LayoutInflater.from(parent.context).inflate(R.layout.subscription_item, parent, false)
        return SubscriptionViewHolder(
            rootView
        )
    }

    override fun getItemCount(): Int {
        return productInfoList.size
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {

        val isSubscribed = subsribedProducts.contains(productInfoList.get(position).productId)
        val durationString =
            if (productInfoList.get(position).subPeriod.equals("P1W")) "1 week" else ""

        holder.productTextView.text = productInfoList.get(position).productName + " " +
                if (isSubscribed) "subscribed (Tap to unsubscribe)" else ""
        holder.descriptionTextView.text = productInfoList.get(position).productDesc
        holder.durationTextView.text = durationString +
                " for " + productInfoList?.get(position)?.price
        if (isSubscribed) {
            holder.subscribeButton.text = "Unsubscribed"
            holder.subscribeButton.setTextColor(Color.parseColor("#ff0000"))
        } else {
            holder.subscribeButton.text = "Subscribe"
            holder.subscribeButton.setTextColor(Color.parseColor("#000000"))
        }

        holder.subscribeButton.setOnClickListener {
            val productId = productInfoList.get(holder.adapterPosition).productId
            if (activity != null) {
                val urlString = Uri.parse(
                    "pay://com.huawei.hwid.external/subscriptions?" + "package=" + BuildConfig.APPLICATION_ID + "&" + "appid=" + "102122169" + "&" + "sku=" + productInfoList.get(
                        position
                    ).productId
                )
                (activity as OnSubscriptionItemClicked).OnSubscriptionItemClicked(
                    productId,
                    isSubscribed,
                    urlString
                )
            }
        }
    }

    fun onStop() {
        if (activity != null) activity = null
    }

    interface OnSubscriptionItemClicked {
        fun OnSubscriptionItemClicked(productId: String?, isSubscribed: Boolean, url: Uri)
    }
}
