package com.eric.rockpaperscissor.Subscription

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eric.rockpaperscissor.Common.CipherUtil
import com.eric.rockpaperscissor.Common.Key
import com.eric.rockpaperscissor.Common.PurchaseUtil
import com.eric.rockpaperscissor.R
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.IapClient
import com.huawei.hms.iap.entity.OrderStatusCode
import com.huawei.hms.iap.entity.ProductInfo
import kotlinx.coroutines.*

class SubscriptionActivity : AppCompatActivity(),
    SubscriptionAdapter.OnSubscriptionItemClicked {

    private lateinit var subscriptionRecyclerView: RecyclerView
    private lateinit var subscriptionProductInfo: List<ProductInfo>
    private lateinit var subscribedProducts: ArrayList<String>
    private lateinit var progressDialog: ProgressDialog

    companion object {
        private val parentJob = Job()
        private val coroutineScope = CoroutineScope(Dispatchers.IO + parentJob)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
    }

    override fun onResume() {
        super.onResume()

        //progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading")
        progressDialog.show()

        coroutineScope.launch {

            subscribedProducts = PurchaseUtil.getInstance().getSubscribed(this@SubscriptionActivity)
                ?: throw Exception("subscribedProducts null")
            subscriptionProductInfo =
                PurchaseUtil.getInstance().loadSubscriptionProduct(this@SubscriptionActivity)
                    ?: throw Exception("subscriptionProductInfo null")


        }.invokeOnCompletion {
            runOnUiThread {
                onLoadedAll()
            }
        }
    }


    private fun onLoadedAll() {
        progressDialog.dismiss()
        if (subscriptionProductInfo != null && subscribedProducts != null) {
            subscriptionRecyclerView = findViewById(R.id.subscription_recyclerView)
            subscriptionRecyclerView.adapter =
                SubscriptionAdapter(
                    subscriptionProductInfo,
                    subscribedProducts,
                    this
                )
            subscriptionRecyclerView.layoutManager = LinearLayoutManager(this)
        } else {
            //TODO:error page
        }
    }

    override fun OnSubscriptionItemClicked(productId: String?, isSubscribed: Boolean, url: Uri) {
        var code = 0
        when (productId) {
            "Statistics" -> code = PurchaseUtil.REQ_CODE_SUBSCRIBE_BOTH
            "MyStatistics" -> code = PurchaseUtil.REQ_CODE_SUBSCRIBE_ME
            "OpponentStatistics" -> code = PurchaseUtil.REQ_CODE_SUBSCRIBE_OPPONENT
        }
        if (isSubscribed) {
            //already subscribed, go to unsubscribed page
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(url)
            startActivity(intent)
        } else {
            if (productId != null) {
                coroutineScope.launch {
                    PurchaseUtil.getInstance()
                        .purchase(
                            this@SubscriptionActivity,
                            productId,
                            IapClient.PriceType.IN_APP_SUBSCRIPTION,
                            code
                        )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val purchaseResultInfo = Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data)
        val isSecure = CipherUtil.doCheck(
            purchaseResultInfo.inAppPurchaseData,
            purchaseResultInfo.inAppDataSignature,
            Key.getPublicKey()
        )

        if (resultCode == Activity.RESULT_OK && data != null && isSecure) {
            when (requestCode) {
                PurchaseUtil.REQ_CODE_SUBSCRIBE_ME -> {
                    when (purchaseResultInfo.returnCode) {
                        OrderStatusCode.ORDER_STATE_SUCCESS -> {
                            //TODO: 1) update subscription page UI 2) notify game activity
                            if (!subscribedProducts.contains("MyStatistics")) {
                                subscribedProducts.add("MyStatistics")
                            } else {
                                subscribedProducts.remove("MyStatistics")
                            }
                            subscriptionRecyclerView.adapter?.notifyDataSetChanged()
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra("REQ_CODE", requestCode)
                            )
                        }
                        OrderStatusCode.ORDER_STATE_CANCEL -> {
                            Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                        }
                        OrderStatusCode.ORDER_PRODUCT_OWNED -> {
                            //not applicable for this app?
                        }
                        else -> {
                            Toast.makeText(this, "Payment not successful", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                PurchaseUtil.REQ_CODE_SUBSCRIBE_OPPONENT -> {
                    when (purchaseResultInfo.returnCode) {
                        OrderStatusCode.ORDER_STATE_SUCCESS -> {
                            subscriptionRecyclerView.adapter?.notifyDataSetChanged()
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra("REQ_CODE", requestCode)
                            )
                        }
                        OrderStatusCode.ORDER_STATE_CANCEL -> {
                            Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                        }
                        OrderStatusCode.ORDER_PRODUCT_OWNED -> {
                        }
                        else -> {
                            Toast.makeText(this, "Payment not successful", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                PurchaseUtil.REQ_CODE_SUBSCRIBE_BOTH -> {
                    when (purchaseResultInfo.returnCode) {
                        OrderStatusCode.ORDER_STATE_SUCCESS -> {
                            subscriptionRecyclerView.adapter?.notifyDataSetChanged()
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra("REQ_CODE", requestCode)
                            )
                        }
                        OrderStatusCode.ORDER_STATE_CANCEL -> {
                            Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                        }
                        OrderStatusCode.ORDER_PRODUCT_OWNED -> {
                        }
                        else -> {
                            Toast.makeText(this, "Payment not successful", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                else -> {
                }
            }
        } else {
            //TODO:handle exceptions
        }
    }

    override fun onStop() {
        super.onStop()
        (subscriptionRecyclerView.adapter as SubscriptionAdapter).onStop()
    }
}
