package com.eric.rockpaperscissor

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.emoji.widget.EmojiTextView
import com.eric.rockpaperscissor.common.CipherUtil
import com.eric.rockpaperscissor.common.Key
import com.huawei.hms.iap.Iap
import com.huawei.hms.iap.entity.OrderStatusCode
import com.huawei.hms.iap.entity.ProductInfo
import java.lang.StringBuilder


class GameActivity : AppCompatActivity(), PurchaseUtil.onLoadProductListener {

    private lateinit var rockButton:Button
    private lateinit var paperButton:Button
    private lateinit var scissorButton:Button
    private lateinit var opponentMove:TextView
    private lateinit var playerMove:EmojiTextView
    private lateinit var heartOne:EmojiTextView
    private lateinit var heartTwo:EmojiTextView
    private lateinit var heartThree:EmojiTextView
    private lateinit var scoreText:TextView


    private var playerDecision: Int? = null
    private var numberOfHearts:Int = 3
    private var score:Int = 0
    private var productInfo:List<ProductInfo>? = null
    private lateinit var purchaseUtil: PurchaseUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("GameActivity", "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        init()
        purchaseUtil = PurchaseUtil()
        purchaseUtil.loadProduct(this)
        purchaseUtil.checkIfPurchasedNeedRedeliver(this)
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

        //init views
        rockButton.text = String(Character.toChars(0x1F44A))
        paperButton.text = String(Character.toChars(0x1F590))
        scissorButton.text = String(Character.toChars(0x270C))
        heartOne.text = String(Character.toChars(0x2764))
        heartTwo.text = String(Character.toChars(0x2764))
        heartThree.text = String(Character.toChars(0x2764))
    }

    fun onPlayerMoveClicked(view: View) {
        when (view.id) {
            R.id.player_scissor -> {
                playerMove.text = String(Character.toChars(0x270C))
                playerDecision = 1
                Log.i("", playerDecision!!.toString())
            }
            R.id.player_rock -> {
                playerMove.text = String(Character.toChars(0x1F44A))
                playerDecision = 2
                Log.i("", playerDecision!!.toString())
            }
            R.id.player_paper -> {
                playerMove.text = String(Character.toChars(0x1F590))
                playerDecision = 3
                Log.i("", playerDecision!!.toString())

            }
        }
        Log.i("", playerDecision!!.toString())
        buttonsEnabled(false)
        startMatch()
    }

    private fun buttonsEnabled(enable:Boolean) {
        rockButton.setEnabled(enable)
        paperButton.setEnabled(enable)
        scissorButton.setEnabled(enable)
    }

    private fun startMatch() {
        //logic of opponent making the decision
        val opponentDecision = (1..3).random()
        when (opponentDecision) {
            1 -> opponentMove.text = String(Character.toChars(0x270C)) //scissor
            2 -> opponentMove.text = String(Character.toChars(0x1F44A)) //rock
            3 -> opponentMove.text = String(Character.toChars(0x1F590)) //paper
        }

        //compare player and opponents decision
        val sum = opponentDecision + playerDecision!!
        when (sum) {
            2,6 -> rematch()
            3 -> if(playerDecision!! == 2) win(true) else win(false)
            4 -> {
                if(playerDecision!! == 2) rematch()
                else if (playerDecision!! == 1) win(true) else win(false)
            }
            5 -> {
                if (playerDecision!! == 3) win(true) else win(false)
            }
        }
    }

    //TODO: handle what to do after winning one match
    private fun win(isWon: Boolean) {
        if(!isWon) {
            when(numberOfHearts) {
                3 -> heartThree.text = String(Character.toChars(0x1F90D))
                2 -> heartTwo.text = String(Character.toChars(0x1F90D))
                1 -> heartOne.text = String(Character.toChars(0x1F90D))
            }
            numberOfHearts-=1
        } else {
            score += 1
            scoreText.text = "Score: " + score
        }
        //check if still player still has any heart left
        Log.i("GameActivity", "score " + numberOfHearts )
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
            .setMessage(StringBuilder("You have lost, ")
                .append("buy more " + String(Character.toChars(0x2764)) + " to continue?").append("\n\n")
                .append(if (!productInfo.isNullOrEmpty()) productInfo!![0].productName + " for " + productInfo!![0].price else "").appendln())
//                .append(if (!productInfo.isNullOrEmpty()) productInfo!![1].productName + " for " + productInfo!![1].price else "").appendln()
//                .append(if (!productInfo.isNullOrEmpty()) productInfo!![2].productName + " for " + productInfo!![2].price else ""))
            .setCancelable(false)
            .setPositiveButton("Buy", DialogInterface.OnClickListener{
                dialog, which ->
                dialog.dismiss()
                //ToDO: add a product page
                purchaseUtil.purchase(this, productInfo!![0].productId, productInfo!![0].priceType, PurchaseUtil.REQ_CODE_BUY_THREE_HEARTS)

            })
            .setNegativeButton("No thanks", DialogInterface.OnClickListener{
                dialog, which ->
                dialog.dismiss()
                returnHomePage()
            })
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PurchaseUtil.REQ_CODE_BUY_THREE_HEARTS) {
            if (data == null) {
                Log.e("GameActivity", "onActivityResult(): data is null")
                Toast.makeText(this, "Error: error code", Toast.LENGTH_SHORT).show()
                return
            }
            val purchaseResultInfo = Iap.getIapClient(this).parsePurchaseResultInfoFromIntent(data)
            when (purchaseResultInfo.returnCode) {
                OrderStatusCode.ORDER_STATE_SUCCESS -> {
                    CipherUtil.doCheck(purchaseResultInfo.inAppPurchaseData, purchaseResultInfo.inAppDataSignature, Key.getPublicKey())
                        .also {
                            if (it) { //purchase success
                                Log.i("GameActivity","need to consume owned product")
                                //TODO: what to do after purchase success, e.g. deliver product
                                addHearts(PurchaseUtil.REQ_CODE_BUY_THREE_HEARTS)
                                score += 3
                                buttonsEnabled(true)
                                purchaseUtil.consumeOwnedPurchase(this, purchaseResultInfo.inAppPurchaseData)

                            } else {
                                Log.e("GameActivity", "onActivityResult(): CipherUtil.doCheck return false")
                                returnHomePage()
                                Toast.makeText(this, "Error: error code", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                OrderStatusCode.ORDER_STATE_CANCEL -> {
                    returnHomePage()
                    Toast.makeText(this, "Order cancelled", Toast.LENGTH_SHORT).show()
                }
                OrderStatusCode.ORDER_PRODUCT_OWNED -> {
                    //not applicable for this app?
                }
                else -> {
                    returnHomePage()
                    Toast.makeText(this, "Payment not successful", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addHearts(productType:Int) {
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

    override fun onProductLoaded(list: List<ProductInfo>?) {
        //TODO:
        Log.i("GameActivity","onProductLoaded() " + list.toString())
        if (!list.isNullOrEmpty()) productInfo=list
    }



}
