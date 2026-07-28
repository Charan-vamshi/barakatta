package com.barakatta.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.barakatta.game.R
import com.barakatta.game.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Pulse animation on title
        val pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        b.tvTitle.startAnimation(pulse)

        b.btnPlay.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }
}
