package com.eric.rockpaperscissor.Subscription

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SubscriptionLoadedReceiver(onLoadedAllListener: OnLoadedAllListener) : BroadcastReceiver() {

    private var called = 0
    private var onLoadedAllListener: OnLoadedAllListener? = onLoadedAllListener

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent?.action.equals("SubscriptionLoaded")) {
            called += 1
            if (called >= 2) {
                onLoadedAllListener?.onLoadedAll()
                called = 0
            }
        }
    }

    fun onStop() {
        onLoadedAllListener = null
    }

    interface OnLoadedAllListener {
        fun onLoadedAll()
    }
}