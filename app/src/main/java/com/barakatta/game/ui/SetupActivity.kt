package com.barakatta.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.barakatta.game.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    private lateinit var b: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(b.root)

        updateNameVisibility(2)

        b.rgPlayerCount.setOnCheckedChangeListener { _, checkedId ->
            val count = playerCountFromId(checkedId)
            updateNameVisibility(count)
        }

        b.btnStart.setOnClickListener {
            val count = playerCountFromId(b.rgPlayerCount.checkedRadioButtonId)
            val names = listOf(b.etPlayer1, b.etPlayer2, b.etPlayer3, b.etPlayer4)
                .take(count)
                .mapIndexed { i, et ->
                    et.text.toString().trim().ifEmpty { "Player ${i + 1}" }
                }
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra("PLAYER_COUNT", count)
                putStringArrayListExtra("PLAYER_NAMES", ArrayList(names))
            }
            startActivity(intent)
        }
    }

    private fun playerCountFromId(id: Int) = when (id) {
        b.rb2Players.id -> 2
        b.rb3Players.id -> 3
        else            -> 4
    }

    private fun updateNameVisibility(count: Int) {
        b.etPlayer3.visibility = if (count >= 3) View.VISIBLE else View.GONE
        b.etPlayer4.visibility = if (count >= 4) View.VISIBLE else View.GONE
    }
}
