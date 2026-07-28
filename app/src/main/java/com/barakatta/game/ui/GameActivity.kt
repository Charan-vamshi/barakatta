package com.barakatta.game.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.barakatta.game.R
import com.barakatta.game.databinding.ActivityGameBinding
import com.barakatta.game.model.GameState
import com.barakatta.game.model.TurnPhase
import com.barakatta.game.viewmodel.GameViewModel

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playerCount = intent.getIntExtra("PLAYER_COUNT", 2)
        val names = intent.getStringArrayListExtra("PLAYER_NAMES") ?: arrayListOf("P1", "P2")

        viewModel.initGame(playerCount, names)

        binding.boardView.onCoinTap = { coinId ->
            viewModel.onCoinSelected(coinId)
        }

        // Setup dice click listeners
        val dices = arrayOf(binding.dice0, binding.dice1, binding.dice2, binding.dice3)
        for (dice in dices) {
            dice.setOnClickListener {
                if (!dice.isRolling) {
                    viewModel.onRollPressed()
                }
            }
        }

        viewModel.gameState.observe(this) { state ->
            if (state == null) return@observe
            updateUI(state)
        }

        viewModel.validCoinIds.observe(this) { valid ->
            binding.boardView.updateState(viewModel.gameState.value, valid)
        }

        viewModel.animData.observe(this) { anim ->
            binding.boardView.startCoinAnimation(anim) { isKill, isFinish ->
                viewModel.onCoinAnimationComplete(isKill, isFinish)
            }
        }

        viewModel.toastMessage.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.diceRollValue.observe(this) { value ->
            // Find active player's dice and roll it
            val state = viewModel.gameState.value ?: return@observe
            val dices = arrayOf(binding.dice0, binding.dice1, binding.dice2, binding.dice3)
            val activeDice = dices[state.currentPlayer.id]
            activeDice.rollTo(value)
        }
    }

    private fun updateUI(state: GameState) {
        val playerBoxes = arrayOf(binding.playerBox0, binding.playerBox1, binding.playerBox2, binding.playerBox3)
        val tvNames = arrayOf(binding.tvName0, binding.tvName1, binding.tvName2, binding.tvName3)
        val tvScores = arrayOf(binding.tvScore0, binding.tvScore1, binding.tvScore2, binding.tvScore3)
        val dices = arrayOf(binding.dice0, binding.dice1, binding.dice2, binding.dice3)
        val chipContainers = arrayOf(binding.chips0, binding.chips1, binding.chips2, binding.chips3)

        // Reset all
        for (i in 0..3) {
            playerBoxes[i].visibility = View.GONE
            chipContainers[i].removeAllViews()
            dices[i].isEnabled = false
            dices[i].alpha = 0.5f // dim inactive
        }

        binding.tvStatusMessage.text = state.statusMessage

        // Show active players in their corners
        for (p in state.players) {
            val i = p.id
            playerBoxes[i].visibility = View.VISIBLE
            tvNames[i].text = p.name
            tvNames[i].setTextColor(p.color)
            
            var scoreText = "⭐ ${p.coinsFinished}/6"
            if (p.hasKilled) scoreText += "  ⚔️"
            tvScores[i].text = scoreText

            dices[i].playerColor = p.color

            val isActive = (p.id == state.currentPlayer.id)
            if (isActive) {
                dices[i].alpha = 1.0f
                if (state.phase == TurnPhase.ROLLING && viewModel.isAnimating.value != true) {
                    dices[i].isEnabled = true // can tap to roll
                }
                
                // Draw chips for active player
                if (state.phase != TurnPhase.ROLLING) {
                    state.heldRolls.forEachIndexed { index, roll ->
                        val chip = TextView(this).apply {
                            text = roll.toString()
                            textSize = 20f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            gravity = Gravity.CENTER
                            
                            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36f, resources.displayMetrics).toInt()
                            val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
                            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                                setMargins(margin, margin, margin, margin)
                            }
                            
                            setBackgroundResource(R.drawable.roll_chip_bg)
                            
                            if (index == state.selectedRollIndex) {
                                backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
                                setTextColor(Color.WHITE)
                            } else {
                                backgroundTintList = ColorStateList.valueOf(0xFF333333.toInt())
                                setTextColor(Color.parseColor("#FFD700"))
                            }
                            
                            setOnClickListener { viewModel.onRollSelected(index) }
                        }
                        chipContainers[i].addView(chip)
                    }
                    
                    if (viewModel.isAnimating.value != true) {
                        dices[i].setValueInstant(state.lastRoll)
                    }
                }
            }
        }
        // Handle Game Over
        if (state.phase == TurnPhase.GAME_OVER) {
            val winner = state.winner
            if (winner != null && binding.winnerOverlay.visibility == View.GONE) {
                binding.winnerOverlay.visibility = View.VISIBLE
                binding.tvWinnerText.text = "${winner.name} WINS!"
                binding.tvWinnerText.setTextColor(winner.color)
                
                binding.confettiView.burst()
                
                binding.btnPlayAgain.setOnClickListener {
                    binding.winnerOverlay.visibility = View.GONE
                    val playerCount = intent.getIntExtra("PLAYER_COUNT", 2)
                    val names = intent.getStringArrayListExtra("PLAYER_NAMES") ?: arrayListOf("P1", "P2")
                    viewModel.initGame(playerCount, names)
                }
            }
        } else {
            binding.winnerOverlay.visibility = View.GONE
        }
    }
}
