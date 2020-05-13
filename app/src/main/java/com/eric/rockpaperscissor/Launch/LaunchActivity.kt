package com.eric.rockpaperscissor.Launch

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.eric.rockpaperscissor.Game.GameActivity
import com.eric.rockpaperscissor.R
import com.huawei.hms.ads.AdParam
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.banner.BannerView

class LaunchActivity : AppCompatActivity() {

    private lateinit var startGameButton: Button
    private lateinit var adBanner: BannerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        startGameButton = findViewById(R.id.start_game_button)
        startGameButton.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }

        HwAds.init(this)
        adBanner = findViewById(R.id.hw_banner_view)
        val adParam = AdParam.Builder().build()
        adBanner.loadAd(adParam)
    }


}
