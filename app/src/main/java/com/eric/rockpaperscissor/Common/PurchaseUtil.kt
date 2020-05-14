package com.eric.rockpaperscissor.Common

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.IapApiException
import com.huawei.hms.iap.IapClient
import com.huawei.hms.iap.entity.*
import org.json.JSONException
import java.lang.Exception
import kotlin.math.truncate

class PurchaseUtil private constructor() {

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

    suspend fun loadConsumablesProduct(activity: Activity) : List<ProductInfo>? {
        val iapClient = Iap.getIapClient(activity)
        val productInfoList: List<ProductInfo>
        try { productInfoList =  iapClient.obtainProductInfo(createConsumablesProductReq()).await().productInfoList }
        catch (e : Exception) { return null }
        return productInfoList
    }

    suspend fun loadSubscriptionProduct(activity: Activity) : List<ProductInfo>? {
        val iapClient = Iap.getIapClient(activity)
        val productInfoList : List<ProductInfo>
        try { productInfoList =  iapClient.obtainProductInfo(createSubscriptionProductReq()).await().productInfoList }
        catch (e : Exception) {return null}
        return productInfoList
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

    suspend fun purchase(activity: Activity, productId: String, type: Int, requestCode: Int) {
        val iapClient = Iap.getIapClient(activity)
        try {
            val purchaseIntentResult = iapClient.createPurchaseIntent(createPuchaseIntentReq(productId, type)).await()
            try {
                if (purchaseIntentResult.status != null && purchaseIntentResult.status.hasResolution()) {
                    purchaseIntentResult.status.startResolutionForResult(activity, requestCode)
                }
            } catch (e : IntentSender.SendIntentException) {Log.e("PurchaseUtil", e.localizedMessage) }
        }
        catch ( iap : IapApiException) {
            Log.e("PurchaseUtil", iap.localizedMessage)
        }
        catch (e : Exception) {
            Log.e("PurchaseUtil", e.localizedMessage)
            Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
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
    suspend fun consumeOwnedPurchase(context: Context, inAppPurchaseData: String) {
        val iapClient = Iap.getIapClient(context)
        try {
            val consumeOwnedPurchaseResult = iapClient.consumeOwnedPurchase(createConsumeOwnedPurchaseReq(inAppPurchaseData)).await()
            Toast.makeText(
                context,
                "Pay success, and the product has been delivered",
                Toast.LENGTH_SHORT
            ).show()
        }
        catch ( iap : IapApiException) {
            Log.e("PurchaseUtil", iap.localizedMessage)
        }
        catch (e : Exception) {
            Log.e("PurchaseUtil", e.localizedMessage)
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

    suspend fun getUnconsumed(activity: Activity) {
        val ownedPurchaseReq = OwnedPurchasesReq()
        ownedPurchaseReq.priceType = 0
        try {
            val ownedPurchaseResult = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchaseReq).await()
            if (ownedPurchaseResult != null && ownedPurchaseResult.inAppPurchaseDataList != null) {
                for (i in 0..ownedPurchaseResult.inAppPurchaseDataList.size - 1) {
                    val inAppPurchaseData = ownedPurchaseResult.inAppPurchaseDataList.get(i)
                    val inAppSignature = ownedPurchaseResult.inAppSignature.get(i)
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
        catch ( iap : IapApiException) {
            val statusCode = iap.status.statusCode
            Log.e("PurchaseUtil", "checkIfPurchasedNeedRedeliver fail,statusCode: " + statusCode)
        }
        catch (e : Exception) {
            Log.e("PurchaseUtil", e.localizedMessage)
        }
    }

    suspend fun getSubscribed(activity: Activity) : ArrayList<String>? {
        val ownedPurchaseReq = OwnedPurchasesReq()
        ownedPurchaseReq.priceType = 2
        try {
            val ownedPurchaseResult = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchaseReq).await()
            if (ownedPurchaseResult != null && ownedPurchaseResult.inAppPurchaseDataList != null) {
                val subscribedList = arrayListOf<String>()
                for (i in 0..ownedPurchaseResult.inAppPurchaseDataList.size - 1) {
                    val inAppPurchaseData = ownedPurchaseResult.inAppPurchaseDataList.get(i)
                    val inAppSignature = ownedPurchaseResult.inAppSignature.get(i)
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
                return subscribedList
            } else {return null}
        }
        catch ( iap : IapApiException) {
            val statusCode = iap.status.statusCode
            Log.e("PurchaseUtil", "checkIfPurchasedNeedRedeliver fail,statusCode: " + statusCode)
            return null
        }
        catch (e : Exception) {
            Log.e("PurchaseUtil", e.localizedMessage)
            return null
        }
    }
}