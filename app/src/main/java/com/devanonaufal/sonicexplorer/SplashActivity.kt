package com.devanonaufal.sonicexplorer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            // Mulai MainActivity setelah 5 detik
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 5000) // 5 detik
    }
}
