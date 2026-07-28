package com.barakatta.game.viewmodel

import androidx.lifecycle.*
import com.barakatta.game.engine.GameEngine
import com.barakatta.game.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private var engine: GameEngine? = null

    private val _gameState     = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _validCoinIds  = MutableLiveData<Set<Int>>(emptySet())
    val validCoinIds: LiveData<Set<Int>> = _validCoinIds

    private val _diceRollValue = MutableLiveData<Int>()
    val diceRollValue: LiveData<Int> = _diceRollValue

    private val _toastMessage  = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _isAnimating   = MutableLiveData(false)
    val isAnimating: LiveData<Boolean> = _isAnimating

    private val _animData = MutableLiveData<CoinAnimData?>(null)
    val animData: LiveData<CoinAnimData?> = _animData

    fun initGame(playerCount: Int, playerNames: List<String>) {
        if (engine != null) return
        engine = GameEngine(playerCount, playerNames)
        push()
    }

    fun onRollPressed() {
        val eng = engine ?: return
        if (eng.state.phase != TurnPhase.ROLLING) return
        if (_isAnimating.value == true) return

        viewModelScope.launch {
            _isAnimating.value = true
            var last: Int
            do {
                last = eng.performRoll()
                _diceRollValue.value = last
                delay(1100L)
            } while (eng.shouldContinueChain(last))

            eng.finishRolling()
            
            // UX: pre-calculate valid coins for auto-selected roll
            updateValidCoinsForSelection()

            if (!eng.hasAnyValidMoves()) {
                _toastMessage.value = "${eng.state.currentPlayer.name}: No valid moves — skipped"
                eng.forceEndTurn()
            }

            _isAnimating.value = false
            push()
        }
    }

    fun onRollSelected(index: Int) {
        val eng = engine ?: return
        if (index !in eng.state.heldRolls.indices) return
        if (_isAnimating.value == true) return

        eng.selectRoll(index)
        updateValidCoinsForSelection()
        
        if (_validCoinIds.value.isNullOrEmpty()) {
            _toastMessage.value = "No coin can move ${eng.state.heldRolls[index]} — discarded"
            eng.state.heldRolls.removeAt(index)
            eng.state.selectedRollIndex = -1
            eng.autoSelectFirstRoll()
            updateValidCoinsForSelection()
            eng.endTurnIfEmpty()
        }
        push()
    }

    private fun updateValidCoinsForSelection() {
        val eng = engine ?: return
        _validCoinIds.value = eng.getValidCoinsForSelectedRoll().toSet()
    }

    fun onCoinSelected(coinId: Int) {
        val eng = engine ?: return
        if (_isAnimating.value == true) return
        if (eng.state.selectedRollIndex !in eng.state.heldRolls.indices) {
            _toastMessage.value = "Tap a roll chip first"
            return
        }
        if (coinId !in eng.getValidCoinsForSelectedRoll()) {
            _toastMessage.value = "That coin can't use this roll"
            return
        }

        val player  = eng.state.currentPlayer
        val coin    = player.coin(coinId)
        val startIdx = coin.pathIndex
        val roll    = eng.state.heldRolls[eng.state.selectedRollIndex]
        val endIdx  = (startIdx + roll).coerceAtMost(48)

        // Apply move in engine
        val result = eng.applySelectedRoll(coinId)
        
        // Auto select next roll if we still have rolls
        if (eng.state.phase == TurnPhase.SELECTING) {
            eng.autoSelectFirstRoll()
        }
        
        _validCoinIds.value = emptySet()
        _isAnimating.value = true

        val pathCells = player.path.subList(startIdx, endIdx + 1)
        
        // Push animation data to BoardView
        _animData.value = CoinAnimData(player.id, coinId, pathCells, result.killed, result.finished)
        
        // The rest of the logic continues in onCoinAnimationComplete() 
        // which BoardView will call when the animation (and emojis) finish.
        push()
    }

    fun onCoinAnimationComplete(isKill: Boolean, isFinish: Boolean) {
        val eng = engine ?: return
        _animData.value = null
        
        viewModelScope.launch {
            if (isFinish) {
                _toastMessage.value = "⭐ Coin reached the center!"
            } else if (isKill) {
                // The killed coin is returned to pathIndex 0 by the engine instantly,
                // so visually it just pops back home after the kill emoji.
                _toastMessage.value = "💀 Opponent's coin sent home!"
            }

            // Both kill AND finish grant a bonus roll chain now
            if (isKill || isFinish) {
                if (eng.state.phase != TurnPhase.GAME_OVER) {
                    var kLast: Int
                    do {
                        kLast = eng.appendKillBonus()
                        _diceRollValue.value = kLast
                        delay(1100L)
                    } while (eng.shouldContinueChain(kLast))
                    eng.finishKillBonus()
                }
            }

            if (eng.state.phase == TurnPhase.GAME_OVER) {
                _isAnimating.value = false
                push()
                return@launch
            }

            if (eng.state.phase == TurnPhase.SELECTING && !eng.hasAnyValidMoves()) {
                _toastMessage.value = "No more valid moves"
                eng.forceEndTurn()
            }

            // Restore valid coin highlights for the next auto-selected roll
            updateValidCoinsForSelection()

            _isAnimating.value = false
            push()
        }
    }

    private fun push() { _gameState.value = engine?.state }
}
