package com.eric.rockpaperscissor.Launch

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.eric.rockpaperscissor.R
import com.huawei.hms.ads.AdParam
import com.huawei.hms.ads.AudioFocusType
import com.huawei.hms.ads.splash.SplashView
import com.huawei.hms.ads.splash.SplashView.SplashAdLoadListener

class SplashActivity : AppCompatActivity() {

    private var hasPaused: Boolean = false
    private lateinit var splashView: SplashView

    private val splashAdLoadListener = object : SplashAdLoadListener() {
        override fun onAdFailedToLoad(p0: Int) {
            returnHomePage()
        }

        override fun onAdDismissed() {
            returnHomePage()
        }
    }


    private val timeoutHandler: Handler = Handler(Handler.Callback {
        if (this.hasWindowFocus()) returnHomePage()
        false
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        loadAd()
    }

    private fun loadAd() {
        Log.i("SplashActivity", "Start to load ad")
        val adParam = AdParam.Builder().build()
        splashView = findViewById(R.id.splash_ad_view)
        splashView.setAudioFocusType(AudioFocusType.NOT_GAIN_AUDIO_FOCUS_WHEN_MUTE)
        splashView.load(
            AD_ID,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            adParam,
            splashAdLoadListener
        )
        Log.i("SplashActivity", "End to load ad")
        timeoutHandler.removeMessages(MSG_AD_TIMEOUT)
        timeoutHandler.sendEmptyMessageDelayed(
            MSG_AD_TIMEOUT,
            AD_TIMEOUT
        )
    }


    private fun returnHomePage() {
        Log.i("SplashActivity", "jump hasPaused: " + hasPaused)
        if (!hasPaused) {
            hasPaused = true
            startActivity(Intent(this, LaunchActivity::class.java))
            val mainHandler = Handler()
            mainHandler.postDelayed(Runnable {
                finish()
            }, 1000)
        }
    }

    override fun onStop() {
        timeoutHandler.removeMessages(MSG_AD_TIMEOUT)
        hasPaused = true
        super.onStop()
    }

    override fun onRestart() {
        super.onRestart()
        hasPaused = false
        returnHomePage()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (splashView != null) splashView.destroyView()
    }

    override fun onPause() {
        super.onPause()
        if (splashView != null) splashView.pauseView()
    }

    override fun onResume() {
        super.onResume()
        if (splashView != null) splashView.resumeView()
    }

    companion object {
        const val AD_ID: String = "testq6zq98hecj"
        const val AD_TIMEOUT: Long = 5000L
        const val MSG_AD_TIMEOUT: Int = 1001
    }
}
