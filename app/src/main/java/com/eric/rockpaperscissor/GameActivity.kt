package com.eric.rockpaperscissor

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.emoji.widget.EmojiTextView
import com.eric.rockpaperscissor.common.CipherUtil
import com.eric.rockpaperscissor.common.Key
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.entity.OrderStatusCode
import com.huawei.hms.iap.entity.ProductInfo
import java.lang.StringBuilder


class GameActivity : AppCompatActivity(), PurchaseUtil.OnLoadedConsumablesInfoListener,
    PurchaseUtil.OnLoadedSubscriptionStatusListener {

    private lateinit var rockButton: Button
    private lateinit var paperButton: Button
    private lateinit var scissorButton: Button
    private lateinit var opponentMove: TextView
    private lateinit var playerMove: EmojiTextView
    private lateinit var heartOne: EmojiTextView
    private lateinit var heartTwo: EmojiTextView
    private lateinit var heartThree: EmojiTextView
    private lateinit var scoreText: TextView
    private lateinit var shop: EmojiTextView
    private lateinit var statisticsImage: ImageView

    private var playerDecision: Int? = null
    private var playerScissorCount: Int = 0
    private var playerPaperCount: Int = 0
    private var playerRockCount: Int = 0
    private var opponentScissorCount: Int = 0
    private var opponentPaperCount: Int = 0
    private var opponentRockCount: Int = 0
    private var numberOfHearts: Int = 3
    private var score: Int = 0
    private lateinit var consumablesProductInfo: List<ProductInfo>
    private var playerSubscriptionEnabled: Boolean = false
    private var opponentSubscriptionEnabled: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("GameActivity", "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        init()
        PurchaseUtil.getInstance().loadConsumablesProduct(this)
        PurchaseUtil.getInstance().getUnconsumed(this)
        PurchaseUtil.getInstance().getSubscribed(this)
    }

    private fun init() {
        //findViews
        rockButton = findViewById(R.id.player_rock)
        paperButton = findViewById(R.id.player_paper)
        scissorButton = findViewById(R.id.player_scissor)
        opponentMove = findViewById(R.id.opponent_move)
        playerMove = findViewById(R.id.player_move)
        heartOne = findViewById(R.id.heart_one)
        heartTwo = findViewById(R.id.heart_two)
        heartThree = findViewById(R.id.heart_three)
        scoreText = findViewById(R.id.score_text)
        shop = findViewById(R.id.shop)
        statisticsImage = findViewById(R.id.statistics_image)

        //init views
        rockButton.text = String(Character.toChars(0x1F44A))
        paperButton.text = String(Character.toChars(0x1F590))
        scissorButton.text = String(Character.toChars(0x270C))
        heartOne.text = String(Character.toChars(0x2764))
        heartTwo.text = String(Character.toChars(0x2764))
        heartThree.text = String(Character.toChars(0x2764))
        shop.text = String(Character.toChars(0x1F3AA))
    }

    fun onPlayerMoveClicked(view: View) {
        when (view.id) {
            R.id.player_scissor -> {
                playerScissorCount += 1
                playerMove.text = String(Character.toChars(0x270C))
                playerDecision = 1
                Log.i("", playerDecision!!.toString())
            }
            R.id.player_rock -> {
                playerRockCount += 1
                playerMove.text = String(Character.toChars(0x1F44A))
                playerDecision = 2
                Log.i("", playerDecision!!.toString())
            }
            R.id.player_paper -> {
                playerPaperCount += 1
                playerMove.text = String(Character.toChars(0x1F590))
                playerDecision = 3
                Log.i("", playerDecision!!.toString())

            }
        }
        Log.i("", playerDecision!!.toString())
        buttonsEnabled(false)
        startMatch()
    }

    fun onShopClicked(view: View) {
        when (view.id) {
            R.id.shop -> {
                startActivityForResult(
                    Intent(this, SubscriptionActivity::class.java),
                    TO_SUBSCRIPTION_PAGE
                )
            }
        }
    }

    private fun buttonsEnabled(enable: Boolean) {
        rockButton.setEnabled(enable)
        paperButton.setEnabled(enable)
        scissorButton.setEnabled(enable)
    }

    private fun startMatch() {
        //logic of opponent making the decision
        val opponentDecision = (1..3).random()
        when (opponentDecision) {
            1 -> {
                opponentScissorCount += 1
                opponentMove.text = String(Character.toChars(0x270C)) //scissor
            }
            2 -> {
                opponentRockCount += 1
                opponentMove.text = String(Character.toChars(0x1F44A)) //rock
            }
            3 -> {
                opponentPaperCount += 1
                opponentMove.text = String(Character.toChars(0x1F590)) //paper
            }
        }

        //compare player and opponents decision
        val sum = opponentDecision + playerDecision!!
        when (sum) {
            2, 6 -> rematch()
            3 -> if (playerDecision!! == 2) win(true) else win(false)
            4 -> {
                if (playerDecision!! == 2) rematch()
                else if (playerDecision!! == 1) win(true) else win(false)
            }
            5 -> {
                if (playerDecision!! == 3) win(true) else win(false)
            }
        }
    }

    //TODO: handle what to do after winning one match
    private fun win(isWon: Boolean) {
        if (!isWon) {
            when (numberOfHearts) {
                3 -> heartThree.text = String(Character.toChars(0x1F90D))
                2 -> heartTwo.text = String(Character.toChars(0x1F90D))
                1 -> heartOne.text = String(Character.toChars(0x1F90D))
            }
            numberOfHearts -= 1
        } else {
            score += 1
            scoreText.text = "Score: " + score
        }
        //check if still player still has any heart left
        Log.i("GameActivity", "score " + numberOfHearts)
        if (numberOfHearts <= 0) gameOver() else rematch()

    }

    private fun rematch() {
        Log.i("GameActivity", "rematch()")
        buttonsEnabled(true)
    }

    private fun gameOver() {
        Log.i("GameActivity", "gameOver()")
        val dialog = AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(
                StringBuilder("You have lost, ")
                    .append("buy more " + String(Character.toChars(0x2764)) + " to continue?").append(
                        "\n\n"
                    )
                    .append(consumablesProductInfo[0].productName + " for " + consumablesProductInfo[0].price).appendln()
            )
//                .append(productInfo[1].productName + " for " + productInfo[1].price).appendln()
//                .append(productInfo[2].productName + " for " + productInfo[2].price))
            .setCancelable(false)
            .setPositiveButton("Buy", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                //ToDO: add a product page
                PurchaseUtil.getInstance().purchase(
                    this,
                    consumablesProductInfo[0].productId,
                    consumablesProductInfo[0].priceType,
                    PurchaseUtil.REQ_CODE_BUY_THREE_HEARTS
                )

            })
            .setNegativeButton("No thanks", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
                returnHomePage()
            })
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                PurchaseUtil.REQ_CODE_BUY_THREE_HEARTS -> {
                    val purchaseResultInfo =
                        Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data)
                    val isSecure = CipherUtil.doCheck(
                        purchaseResultInfo.inAppPurchaseData,
                        purchaseResultInfo.inAppDataSignature,
                        Key.getPublicKey()
                    )
                    if (!isSecure) {
                        Log.e("GameActivity", "not secure")
                        Toast.makeText(
                            this,
                            "Please contact seller. code:xxxxd",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    when (purchaseResultInfo.returnCode) {
                        OrderStatusCode.ORDER_STATE_SUCCESS -> {
                            addHearts(PurchaseUtil.REQ_CODE_BUY_THREE_HEARTS)
                            buttonsEnabled(true)
                            PurchaseUtil.getInstance().consumeOwnedPurchase(
                                this,
                                purchaseResultInfo.inAppPurchaseData
                            )
                            Log.i(
                                "GameActivity",
                                "data: " + purchaseResultInfo.inAppPurchaseData.toString()
                            )
                        }
                        OrderStatusCode.ORDER_STATE_CANCEL -> {
                            returnHomePage()
                            Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                        }
                        OrderStatusCode.ORDER_PRODUCT_OWNED -> {
                        }
                        else -> {
                            returnHomePage()
                            Toast.makeText(this, "Payment not successful", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                TO_SUBSCRIPTION_PAGE -> {
                    val reqCode = data.getIntExtra("REQ_CODE", -1)
                    when (reqCode) {
                        PurchaseUtil.REQ_CODE_SUBSCRIBE_ME -> playerSubscriptionEnabled =
                            !playerSubscriptionEnabled
                        PurchaseUtil.REQ_CODE_SUBSCRIBE_OPPONENT -> opponentSubscriptionEnabled =
                            !opponentSubscriptionEnabled
                        PurchaseUtil.REQ_CODE_SUBSCRIBE_BOTH -> {
                            playerSubscriptionEnabled = !playerSubscriptionEnabled
                            opponentSubscriptionEnabled = !opponentSubscriptionEnabled
                        }
                    }
                }
            }
        } else {
            //TODO: handle exception
            when (requestCode) {
                TO_SUBSCRIPTION_PAGE -> {
                    // from subscription page come back, need to check subscription status again
                    PurchaseUtil.getInstance().getSubscribed(this)
                }
                else -> {
                    returnHomePage()
                }
            }
        }
    }

    private fun addHearts(productType: Int) {
        if (productType == PurchaseUtil.REQ_CODE_BUY_THREE_HEARTS) {
            numberOfHearts += 3
            heartOne.text = String(Character.toChars(0x2764))
            heartTwo.text = String(Character.toChars(0x2764))
            heartThree.text = String(Character.toChars(0x2764))
        }
    }

    private fun returnHomePage() {
        startActivity(Intent(this, LaunchActivity::class.java))
        finish()
    }

    override fun onLoadedConsumablesInfo(list: List<ProductInfo>?) {
        //TODO:
        Log.i("GameActivity", "onConsumablesLoaded() " + list.toString())
        if (list != null) {
            consumablesProductInfo = list
        } else {
            //TODO:error page
        }
    }

    fun onStatisticsClicked(view: View) {
        var playerMessageString = ""
        var opponentMessageString = ""
        val promptMessage =
            "Please subscribe to \"Statistics\" in " + shop.text + " for this function."

        if (playerSubscriptionEnabled) {
            //player's stat
            val playerTotalCount: Float =
                (playerPaperCount * 1f + playerScissorCount * 1f + playerRockCount * 1f)
            playerMessageString = if (playerTotalCount != 0f) {
                val playerScissorPercentage = (playerScissorCount / playerTotalCount) * 100
                val playerRockPercentage = (playerRockCount / playerTotalCount) * 100
                val playerPaperPercentage = (playerPaperCount / playerTotalCount) * 100
                val playerScissorMessage =
                    String(Character.toChars(0x270C)) + ":" + playerScissorPercentage.toString().split(
                        "."
                    )[0] + "%"
                val playerRockMessage =
                    String(Character.toChars(0x1F44A)) + ":" + playerRockPercentage.toString().split(
                        "."
                    )[0] + "%"
                val playerPaperMessage =
                    String(Character.toChars(0x1F590)) + ":" + playerPaperPercentage.toString().split(
                        "."
                    )[0] + "%"
                "You: " + playerScissorMessage + playerRockMessage + playerPaperMessage
            } else {
                "You have no statistics yet."
            }
        } else {
            //should not happened
            val playerScissorMessage = String(Character.toChars(0x270C)) + ":" + "***"
            val playerRockMessage = String(Character.toChars(0x1F44A)) + ":" + "***"
            val playerPaperMessage = String(Character.toChars(0x1F590)) + ":" + "***"
            playerMessageString =
                "You: " + playerScissorMessage + playerRockMessage + playerPaperMessage
        }

        if (opponentSubscriptionEnabled) {
            //opponent's stat
            val opponentTotalCount: Float =
                (opponentPaperCount * 1f + opponentScissorCount * 1f + opponentRockCount * 1f)
            opponentMessageString = if (opponentTotalCount != 0f) {
                val opponentScissorPercentage = (opponentScissorCount / opponentTotalCount) * 100
                val opponentRockPercentage = (opponentRockCount / opponentTotalCount) * 100
                val opponentPaperPercentage = (opponentPaperCount / opponentTotalCount) * 100
                val opponentScissorMessage =
                    String(Character.toChars(0x270C)) + ":" + opponentScissorPercentage.toString().split(
                        "."
                    )[0] + "%"
                val opponentRockMessage =
                    String(Character.toChars(0x1F44A)) + ":" + opponentRockPercentage.toString().split(
                        "."
                    )[0] + "%"
                val opponentPaperMessage =
                    String(Character.toChars(0x1F590)) + ":" + opponentPaperPercentage.toString().split(
                        "."
                    )[0] + "%"
                "Computer: " + opponentScissorMessage + opponentRockMessage + opponentPaperMessage
            } else {
                "The opponent has no statistics yet."
            }
        } else {
            //should not happened
            val opponentScissorMessage = String(Character.toChars(0x270C)) + ":" + "***"
            val opponentRockMessage = String(Character.toChars(0x1F44A)) + ":" + "***"
            val opponentPaperMessage = String(Character.toChars(0x1F590)) + ":" + "***"
            opponentMessageString =
                "Computer: " + opponentScissorMessage + opponentRockMessage + opponentPaperMessage
        }

        var messageString = ""
        if (!playerSubscriptionEnabled && !opponentSubscriptionEnabled) {
            messageString = promptMessage
        } else {
            messageString = playerMessageString + "\n" + opponentMessageString
        }

        createStatisticsDialog("Statistics", messageString, R.drawable.ic_stats).show()
    }

    private fun createStatisticsDialog(
        title: String,
        messageString: String,
        icon: Int
    ): AlertDialog.Builder {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setIcon(icon)
            .setMessage(messageString)
            .setCancelable(true)
        return dialog
    }

    override fun onLoadedSubscriptionStatus(list: ArrayList<String>) {

        playerSubscriptionEnabled = false
        opponentSubscriptionEnabled = false

        for (i in 0..list.size - 1) {
            when (list.get(i)) {
                "MyStatistics" -> playerSubscriptionEnabled = true
                "OpponentStatistics" -> opponentSubscriptionEnabled = true
                //disabled...will it be effective?
                "Statistics" -> {
                    playerSubscriptionEnabled = true; opponentSubscriptionEnabled = true
                }
            }
        }
    }

    companion object {
        const val TO_SUBSCRIPTION_PAGE = 1
    }

}
