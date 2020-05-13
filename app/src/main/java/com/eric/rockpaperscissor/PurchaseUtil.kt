package com.eric.rockpaperscissor

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import com.eric.rockpaperscissor.common.CipherUtil
import com.eric.rockpaperscissor.common.Key
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.IapApiException
import com.huawei.hms.iap.IapClient
import com.huawei.hms.iap.entity.*
import org.json.JSONException
import java.lang.Exception

class PurchaseUtil private constructor() {

    fun loadConsumablesProduct(activity: Activity) {
        val iapClient = Iap.getIapClient(activity)
        val task = iapClient.obtainProductInfo(createConsumablesProductReq())
        task.addOnSuccessListener {
            if (it != null && !it.productInfoList.isEmpty()) {
                (activity as OnLoadedConsumablesInfoListener).onLoadedConsumablesInfo(it.productInfoList)
            }
        }
            .addOnFailureListener {
                Log.e("PurchaseUtil", "loadConsumablesProduct(): fail " + it.localizedMessage)
//                Toast.makeText(activity, "Error. Cannot purchase product", Toast.LENGTH_SHORT).show()
                (activity as OnLoadedConsumablesInfoListener).onLoadedConsumablesInfo(null)
            }
    }

    fun loadSubscriptionProduct(activity: Activity) {
        val iapClient = Iap.getIapClient(activity)
        val task = iapClient.obtainProductInfo(createSubscriptionProductReq())
        task.addOnSuccessListener {
            if (it != null && !it.productInfoList.isEmpty()) {
                (activity as OnLoadedSubscriptionInfoListener).onLoadedSubscriptionInfo(it.productInfoList)
            }
        }
            .addOnFailureListener {
                Log.e("PurchaseUtil", "loadSubscriptionProduct(): fail " + it.localizedMessage)
//                Toast.makeText(activity, "Error. Cannot purchase product", Toast.LENGTH_SHORT).show()
                (activity as OnLoadedSubscriptionInfoListener).onLoadedSubscriptionInfo(null)
            }
    }

    private fun createConsumablesProductReq(): ProductInfoReq {
        val req = ProductInfoReq()
        // In-app product type contains:
        // 0: consumable
        // 1: non-consumable
        // 2: auto-renewable subscription
        req.priceType = IapClient.PriceType.IN_APP_CONSUMABLE
        val productIds = arrayListOf<String>()
        productIds.add("Hearts3")
        productIds.add("Hearts5")
        productIds.add("Hearts10")
        req.productIds = productIds
        return req
    }

    private fun createSubscriptionProductReq(): ProductInfoReq {
        val req = ProductInfoReq()
        // In-app product type contains:
        // 0: consumable
        // 1: non-consumable
        // 2: auto-renewable subscription
        req.priceType = IapClient.PriceType.IN_APP_SUBSCRIPTION
        val productIds = arrayListOf<String>()
        productIds.add("MyStatistics")
        productIds.add("OpponentStatistics")
        productIds.add("Statistics")
        req.productIds = productIds
        return req
    }


    fun purchase(activity: Activity, productId: String, type: Int, requestCode: Int) {
        Log.i("PurchaseUtil", "purchase()")
        val iapClient = Iap.getIapClient(activity)
        val task = iapClient.createPurchaseIntent(createPuchaseIntentReq(productId, type))
        task.addOnSuccessListener {
            if (it == null) {
                Log.e("PurchaseUtil", "purchase(): purchaseintentresult is null")
                return@addOnSuccessListener
            } else {
                val status = it.status
                if (status == null) {
                    Log.e("PurchaseUtil", "purchase(): purchaseintentresult.status is null")
                    return@addOnSuccessListener
                } else {
                    if (status.hasResolution()) {
                        try {
                            status.startResolutionForResult(activity, requestCode)
                        } catch (ex: IntentSender.SendIntentException) {
                            Log.e("PurchaseUtil", ex.localizedMessage)
                        }
                    } else {
                        Log.e("PurchaseUtil", "intent is null")
                    }
                }
            }
        }
            .addOnFailureListener {
                Log.e("PurchaseUtil", it.localizedMessage)
                Toast.makeText(activity, it.localizedMessage, Toast.LENGTH_SHORT).show()
                if (it is IapApiException) {
                    val iapException = it as IapApiException
                    val returnCode = iapException.statusCode
                    Log.e("PurchaseUtil", "createPurchaseIntent, returnCode:" + returnCode)
                } else {
                    //TODO:other exceptions
                }
            }
    }

    private fun createPuchaseIntentReq(productId: String, type: Int): PurchaseIntentReq {
        val req = PurchaseIntentReq()
        req.productId = productId
        req.priceType = type
        req.developerPayload = "test"
        return req
    }

    /**
     * Consume the unconsumed purchase with type 0 after successfully delivering the product, then the Huawei payment server will update the order status and the user can purchase the product again.
     * @param inAppPurchaseData JSON string that contains purchase order details.
     */
    fun consumeOwnedPurchase(context: Context, inAppPurchaseData: String) {
        Log.i("PurchaseUtil", "consumeOwnedPurchase()")
        val iapClient = Iap.getIapClient(context)
        val task = iapClient.consumeOwnedPurchase(createConsumeOwnedPurchaseReq(inAppPurchaseData))
        task.addOnSuccessListener {
            Log.i("PurchaseUtil", "consumeOwnedPurchase success")
            Toast.makeText(
                context,
                "Pay success, and the product has been delivered",
                Toast.LENGTH_SHORT
            ).show()
        }
            .addOnFailureListener {
                Log.e("PurchaseUtil", "consumeOwnedPuschase() failure " + it.localizedMessage)
                Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                if (it is IapApiException) {
                    val iapApiException = it as IapApiException
                    val returnCode = iapApiException.status.statusCode
                    Log.e("PurchaseUtil", "consumeOwnedPurchase fail,returnCode: " + returnCode)
                } else {
                    //other failure reasons
                }
            }
    }

    private fun createConsumeOwnedPurchaseReq(inAppPurchaseData: String): ConsumeOwnedPurchaseReq {
        val req = ConsumeOwnedPurchaseReq()
        try {
            val inAppPurchaseData = InAppPurchaseData(inAppPurchaseData)
            req.purchaseToken = inAppPurchaseData.purchaseToken
        } catch (ex: JSONException) {
            Log.e("PurchaseUtil", "createConsumeOwnedPurchaseReq() " + ex.localizedMessage)
        }
        return req
    }

    fun getUnconsumed(activity: Activity) {
        val ownedPurchaseReq = OwnedPurchasesReq()
        ownedPurchaseReq.priceType = 0
        val task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchaseReq)
        task.addOnSuccessListener {
            if (it != null && it.inAppPurchaseDataList != null) {
                for (i in 0..it.inAppPurchaseDataList.size - 1) {
                    val inAppPurchaseData = it.inAppPurchaseDataList.get(i)
                    val inAppSignature = it.inAppSignature.get(i)
                    val success = CipherUtil.doCheck(
                        inAppPurchaseData,
                        inAppSignature,
                        Key.getPublicKey()
                    )
                    if (success) try {
                        val inAppPurchaseDataBean = InAppPurchaseData(inAppPurchaseData)
                        val purchaseState = inAppPurchaseDataBean.purchaseState
                        if (purchaseState == 0)
                            consumeOwnedPurchase(activity.applicationContext, inAppPurchaseData)
                    } catch (ex: JSONException) {
                        Log.e(
                            "PurchaseUtil",
                            "checkIfPurchasedNeedRedeliver() JSONException" + ex.localizedMessage
                        )
                    } catch (ex: Exception) {
                        Log.e(
                            "PurchaseUtil",
                            "checkIfPurchasedNeedRedeliver() " + ex.localizedMessage
                        )
                    }
                }
            }
        }
            .addOnFailureListener {
                if (it is IapApiException) {
                    val statusCode = (it as IapApiException).status.statusCode
                    Log.e(
                        "PurchaseUtil",
                        "checkIfPurchasedNeedRedeliver fail,statusCode: " + statusCode
                    )
                } else {
                    //other reasons
                }
            }
    }

    fun getSubscribed(activity: Activity) {
        val ownedPurchaseReq = OwnedPurchasesReq()
        ownedPurchaseReq.priceType = 2
        val task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchaseReq)
        task.addOnSuccessListener {
            if (it != null && it.inAppPurchaseDataList != null) {
                val subscribedList = arrayListOf<String>()
                for (i in 0..it.inAppPurchaseDataList.size - 1) {
                    val inAppPurchaseData = it.inAppPurchaseDataList.get(i)
                    val inAppSignature = it.inAppSignature.get(i)
                    val success = CipherUtil.doCheck(
                        inAppPurchaseData,
                        inAppSignature,
                        Key.getPublicKey()
                    )
                    if (success) try {
                        val inAppPurchaseDataBean = InAppPurchaseData(inAppPurchaseData)
                        val purchaseState = inAppPurchaseDataBean.purchaseState
                        Log.i("PurchaseUtil", "purchaseState" + purchaseState)
                        if (purchaseState == InAppPurchaseData.PurchaseState.PURCHASED) {
                            subscribedList.add(inAppPurchaseDataBean.productId)
                        }
                    } catch (ex: JSONException) {
                        Log.e(
                            "PurchaseUtil",
                            "checkIfPurchasedNeedRedeliver() JSONException" + ex.localizedMessage
                        )
                    } catch (ex: Exception) {
                        Log.e(
                            "PurchaseUtil",
                            "checkIfPurchasedNeedRedeliver() " + ex.localizedMessage
                        )
                    }
                }
                //TODO: notify game activity
                (activity as OnLoadedSubscriptionStatusListener).onLoadedSubscriptionStatus(
                    subscribedList
                )
            }
        }
            .addOnFailureListener {
                if (it is IapApiException) {
                    val statusCode = (it as IapApiException).status.statusCode
                    Log.e(
                        "PurchaseUtil",
                        "checkIfPurchasedNeedRedeliver fail,statusCode: " + statusCode
                    )
                } else {
                    //other reasons
                }
            }
    }

    interface OnLoadedConsumablesInfoListener {
        fun onLoadedConsumablesInfo(list: List<ProductInfo>?)
    }

    interface OnLoadedSubscriptionInfoListener {
        fun onLoadedSubscriptionInfo(list: List<ProductInfo>?)
    }

    interface OnLoadedSubscriptionStatusListener {
        fun onLoadedSubscriptionStatus(list: ArrayList<String>)
    }

    companion object {
        const val REQ_CODE_BUY_THREE_HEARTS = 1001
        const val REQ_CODE_BUY_FIVE_HEARTS = 1002
        const val REQ_CODE_BUY_TEN_HEARTS = 1003
        const val REQ_CODE_SUBSCRIBE_ME = 1004
        const val REQ_CODE_SUBSCRIBE_OPPONENT = 1005
        const val REQ_CODE_SUBSCRIBE_BOTH = 1006

        val INSTANCE: PurchaseUtil? = null

        fun getInstance(): PurchaseUtil {
            if (INSTANCE == null) return PurchaseUtil() else return INSTANCE
        }
    }

}