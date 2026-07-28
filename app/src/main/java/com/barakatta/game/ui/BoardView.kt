package com.barakatta.game.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.barakatta.game.model.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class BoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var state: GameState? = null
    private var validCoinIds: Set<Int> = emptySet()
    
    // Animation state
    private var currentAnimData: CoinAnimData? = null
    private var animFraction: Float = 0f // 0f to pathCells.size - 1
    
    // Emoji float state
    private var emojiText: String? = null
    private var emojiCell: Cell? = null
    private var emojiYOffset: Float = 0f
    private var emojiAlpha: Int = 0

    var onCoinTap: ((Int) -> Unit)? = null

    fun updateState(newState: GameState?, newValidCoins: Set<Int>) {
        this.state = newState
        this.validCoinIds = newValidCoins
        invalidate()
    }

    fun startCoinAnimation(animData: CoinAnimData?, onComplete: (Boolean, Boolean) -> Unit) {
        if (animData == null) {
            currentAnimData = null
            invalidate()
            return
        }

        currentAnimData = animData
        val totalSteps = (animData.pathCells.size - 1).coerceAtLeast(0)
        
        // 150ms per step feels smooth but fast enough
        val durationMs = (totalSteps * 250L).coerceAtMost(1000L).coerceAtLeast(250L)

        val animator = ValueAnimator.ofFloat(0f, totalSteps.toFloat()).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                animFraction = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animData.isKill || animData.isFinish) {
                        playEmojiAnimation(animData, onComplete)
                    } else {
                        currentAnimData = null
                        onComplete(false, false)
                    }
                }
            })
        }
        animator.start()
    }

    private fun playEmojiAnimation(animData: CoinAnimData, onComplete: (Boolean, Boolean) -> Unit) {
        emojiText = if (animData.isFinish) "👑" else "⚔️"
        emojiCell = animData.pathCells.last()
        
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                emojiYOffset = -(f * cellSize) // floats up by 1 cell height
                emojiAlpha = ((1f - f) * 255).toInt()
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    emojiText = null
                    currentAnimData = null
                    onComplete(animData.isKill, animData.isFinish)
                }
            })
        }
        animator.start()
    }

    // ── Paints & Geometry ─────────────────────────────────────────────────────
    private val boardBgPaint = Paint().apply { color = 0xFF2A1B14.toInt() } // Dark mahogany background
    private val cellBorderPaint = Paint().apply {
        color = 0xFF8D6E63.toInt() // Distinct brown lines
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val safeCellCrossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x668D6E63 // Semi-transparent brown for the cross
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val ringPaints = arrayOf(
        Paint().apply { color = 0xFFFFF8E1.toInt() }, // Outer ring - very light cream
        Paint().apply { color = 0xFFFFECB3.toInt() }, // Middle ring - light warm cream
        Paint().apply { color = 0xFFFFE082.toInt() }, // Inner ring - warm amber
        Paint().apply { color = 0xFFFFD700.toInt() }  // Center - solid gold
    )

    private val homeBorders = mapOf(
        Cell(6, 4) to Paint().apply { color = 0xFFF44336.toInt(); style = Paint.Style.STROKE; strokeWidth = 8f },
        Cell(0, 4) to Paint().apply { color = 0xFF2196F3.toInt(); style = Paint.Style.STROKE; strokeWidth = 8f },
        Cell(3, 7) to Paint().apply { color = 0xFF4CAF50.toInt(); style = Paint.Style.STROKE; strokeWidth = 8f },
        Cell(3, 1) to Paint().apply { color = 0xFFFFC107.toInt(); style = Paint.Style.STROKE; strokeWidth = 8f }
    )

    private val highlightPaint = Paint().apply {
        color = 0x66FFEB3B
        style = Paint.Style.FILL
    }
    
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000
        style = Paint.Style.FILL
    }
    
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private var cellSize: Float = 0f
    private var boardOffset: Float = 0f

    // ── Drawing ───────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val size = min(w, h)
        cellSize = size / 7f
        boardOffset = (h - size) / 2f

        canvas.drawRect(0f, boardOffset, size, boardOffset + size, boardBgPaint)

        // Draw cells
        for (row in 0..6) {
            for (col in 1..7) {
                drawCellBackground(canvas, Cell(row, col))
            }
        }

        val currentState = state ?: return

        // Draw static coins (exclude animating one)
        val coinsByCell = mutableMapOf<Cell, MutableList<Pair<Player, Coin>>>()
        
        var animatingPlayerAndCoin: Pair<Player, Coin>? = null
        
        for (p in currentState.players) {
            for (c in p.coins) {
                if (c.isFinished && !(currentAnimData?.isFinish == true && currentAnimData?.playerId == p.id && currentAnimData?.coinId == c.id)) {
                    continue // Skip finished, unless it's currently finishing animation
                }
                
                if (currentAnimData?.playerId == p.id && currentAnimData?.coinId == c.id) {
                    animatingPlayerAndCoin = p to c
                    continue
                }
                
                val cell = p.path[c.pathIndex]
                coinsByCell.getOrPut(cell) { mutableListOf() }.add(p to c)
            }
        }

        for ((cell, list) in coinsByCell) {
            if (cell == Board.CENTER) continue
            drawCoinsInCell(canvas, cell, list)
        }
        
        // Draw animating coin on top
        animatingPlayerAndCoin?.let { (p, c) ->
            drawAnimatingCoin(canvas, p, c)
        }
        
        // Draw float emoji
        if (emojiText != null && emojiCell != null) {
            val cx = (emojiCell!!.colIndex - 1) * cellSize + cellSize / 2f
            val cy = boardOffset + emojiCell!!.rowIndex * cellSize + cellSize / 2f + emojiYOffset
            emojiPaint.alpha = emojiAlpha
            emojiPaint.textSize = cellSize * 0.6f
            val yOff = (emojiPaint.descent() + emojiPaint.ascent()) / 2
            canvas.drawText(emojiText!!, cx, cy - yOff, emojiPaint)
        }
    }

    private fun drawAnimatingCoin(canvas: Canvas, player: Player, coin: Coin) {
        val anim = currentAnimData ?: return
        val path = anim.pathCells
        if (path.isEmpty()) return
        
        val idx = animFraction.toInt().coerceAtMost(path.size - 1)
        val nextIdx = (idx + 1).coerceAtMost(path.size - 1)
        
        val cell1 = path[idx]
        val cell2 = path[nextIdx]
        
        val fraction = animFraction - idx
        
        val x1 = (cell1.colIndex - 1) * cellSize + cellSize / 2f
        val y1 = boardOffset + cell1.rowIndex * cellSize + cellSize / 2f
        
        val x2 = (cell2.colIndex - 1) * cellSize + cellSize / 2f
        val y2 = boardOffset + cell2.rowIndex * cellSize + cellSize / 2f
        
        val cx = x1 + (x2 - x1) * fraction
        
        // Add a nice bounce arc based on fraction (sin wave)
        val bounceHeight = cellSize * 0.4f
        val cy = (y1 + (y2 - y1) * fraction) - (sin(fraction * Math.PI) * bounceHeight).toFloat()
        
        val radius = cellSize * 0.35f
        drawSingleCoin(canvas, cx, cy, radius, player to coin, isAnimating = true)
    }

    private fun drawCellBackground(canvas: Canvas, cell: Cell) {
        val left = (cell.colIndex - 1) * cellSize
        val top = boardOffset + cell.rowIndex * cellSize
        val right = left + cellSize
        val bottom = top + cellSize

        val ring = Board.getRing(cell)
        canvas.drawRect(left, top, right, bottom, ringPaints[ring])

        homeBorders[cell]?.let { p ->
            val inset = p.strokeWidth / 2f
            canvas.drawRect(left + inset, top + inset, right - inset, bottom - inset, p)
        }

        if (Board.isSafeCell(cell) && cell != Board.CENTER) {
            canvas.drawLine(left, top, right, bottom, safeCellCrossPaint)
            canvas.drawLine(right, top, left, bottom, safeCellCrossPaint)
        }

        canvas.drawRect(left, top, right, bottom, cellBorderPaint)

        if (cell == Board.CENTER) {
            val cx = left + cellSize / 2f
            val cy = top + cellSize / 2f
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF000000.toInt()
                textSize = cellSize * 0.5f
                textAlign = Paint.Align.CENTER
            }
            val yOff = (starPaint.descent() + starPaint.ascent()) / 2
            canvas.drawText("★", cx, cy - yOff, starPaint)
        }
    }

    private fun drawCoinsInCell(canvas: Canvas, cell: Cell, coins: List<Pair<Player, Coin>>) {
        val cx = (cell.colIndex - 1) * cellSize + cellSize / 2f
        val cy = boardOffset + cell.rowIndex * cellSize + cellSize / 2f

        val homeCoins = coins.filter { it.second.pathIndex == 0 }
        if (homeCoins.size == 6 && Board.HOME_CELLS_BY_COUNT.values.any { it.contains(cell) }) {
            drawHomeStack(canvas, cx, cy, homeCoins)
            return
        }

        val baseRadius = cellSize * 0.35f
        when (coins.size) {
            1 -> drawSingleCoin(canvas, cx, cy, baseRadius, coins[0])
            2 -> {
                drawSingleCoin(canvas, cx - baseRadius/2, cy, baseRadius*0.75f, coins[0])
                drawSingleCoin(canvas, cx + baseRadius/2, cy, baseRadius*0.75f, coins[1])
            }
            3 -> {
                drawSingleCoin(canvas, cx, cy - baseRadius/2, baseRadius*0.65f, coins[0])
                drawSingleCoin(canvas, cx - baseRadius/2, cy + baseRadius/2, baseRadius*0.65f, coins[1])
                drawSingleCoin(canvas, cx + baseRadius/2, cy + baseRadius/2, baseRadius*0.65f, coins[2])
            }
            4 -> {
                drawSingleCoin(canvas, cx - baseRadius/2, cy - baseRadius/2, baseRadius*0.65f, coins[0])
                drawSingleCoin(canvas, cx + baseRadius/2, cy - baseRadius/2, baseRadius*0.65f, coins[1])
                drawSingleCoin(canvas, cx - baseRadius/2, cy + baseRadius/2, baseRadius*0.65f, coins[2])
                drawSingleCoin(canvas, cx + baseRadius/2, cy + baseRadius/2, baseRadius*0.65f, coins[3])
            }
            else -> {
                for (i in coins.indices) {
                    val offset = (i - coins.size / 2f) * 10f
                    drawSingleCoin(canvas, cx + offset, cy + offset, baseRadius * 0.7f, coins[i])
                }
            }
        }
    }

    private fun drawHomeStack(canvas: Canvas, cx: Float, cy: Float, coins: List<Pair<Player, Coin>>) {
        val r = cellSize * 0.2f
        val gap = cellSize * 0.08f
        val w = r * 2 + gap
        val h = r * 2 * 3 + gap * 2
        val startX = cx - w/2 + r
        val startY = cy - h/2 + r

        var idx = 0
        for (col in 0..1) {
            for (row in 0..2) {
                if (idx < coins.size) {
                    val x = startX + col * (r * 2 + gap)
                    val y = startY + row * (r * 2 + gap)
                    drawSingleCoin(canvas, x, y, r, coins[idx])
                    idx++
                }
            }
        }
    }

    private fun drawSingleCoin(canvas: Canvas, cx: Float, cy: Float, radius: Float, coinData: Pair<Player, Coin>, isAnimating: Boolean = false) {
        val (player, coin) = coinData
        
        // Shadow is slightly offset for depth
        val shadowOff = if (isAnimating) radius * 0.4f else radius * 0.2f
        canvas.drawCircle(cx + shadowOff, cy + shadowOff, radius, shadowPaint)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val highlightColor = brighten(player.color, 0.4f)
        val darkColor = darken(player.color, 0.3f)
        p.shader = RadialGradient(
            cx - radius*0.3f, cy - radius*0.3f, radius * 1.5f,
            intArrayOf(highlightColor, player.color, darkColor),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, radius, p)

        if (!isAnimating && coin.id in validCoinIds && state?.currentPlayer?.id == player.id) {
            val sel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(cx, cy, radius + 4f, sel)
        }
    }
    
    private fun brighten(color: Int, fraction: Float): Int {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return Color.rgb(
            r + ((255 - r) * fraction).toInt(),
            g + ((255 - g) * fraction).toInt(),
            b + ((255 - b) * fraction).toInt()
        )
    }

    private fun darken(color: Int, fraction: Float): Int {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return Color.rgb(
            (r * (1 - fraction)).toInt(),
            (g * (1 - fraction)).toInt(),
            (b * (1 - fraction)).toInt()
        )
    }

    // ── Input ─────────────────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val currentState = state ?: return true
            if (currentAnimData != null || emojiText != null) return true // Block input while animating

            val w = width.toFloat()
            val h = height.toFloat()
            val size = min(w, h)
            val bx = event.x
            val by = event.y - boardOffset

            if (bx in 0f..size && by in 0f..size) {
                val colIndex = (bx / cellSize).toInt() + 1
                val rowIndex = (by / cellSize).toInt()
                val tappedCell = Cell(rowIndex, colIndex)

                val p = currentState.currentPlayer
                val tappedValid = validCoinIds.firstOrNull { id ->
                    p.coin(id).cell(p.path) == tappedCell
                }

                if (tappedValid != null) {
                    onCoinTap?.invoke(tappedValid)
                }
            }
            return true
        }
        return true
    }
}
