package com.barakatta.game.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

class DiceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        color = Color.WHITE
        style = Paint.Style.FILL 
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        color = 0x44000000
        style = Paint.Style.FILL 
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF222222.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    }

    private val rect = RectF()
    private val innerRect = RectF()
    private val shadowRect = RectF()
    private var currentValue = 1
    
    var playerColor: Int = 0xFF4CAF50.toInt()
        set(value) {
            field = value
            invalidate()
        }

    var isRolling = false
        private set

    fun rollTo(finalValue: Int, onComplete: () -> Unit = {}) {
        if (isRolling) return
        isRolling = true
        
        // Stop any existing animations by resetting scale/rotation
        scaleX = 1f; scaleY = 1f; rotation = 0f; translationX = 0f
        
        // 1. Extremely fast shake (150ms)
        val shake = ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, 15f, -15f, 15f, -15f, 0f)
        shake.duration = 150
        
        // 2. Fast pop (250ms total)
        val scaleUpX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 1.4f)
        val scaleUpY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 1.4f)
        val scaleDownX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1.4f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1.4f, 1f)
        
        val popUp = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            duration = 100
            interpolator = DecelerateInterpolator()
        }
        val popDown = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY)
            duration = 150
            interpolator = OvershootInterpolator(3.0f) // Very heavy snappy bounce
        }
        
        val popSet = AnimatorSet()
        popSet.playSequentially(popUp, popDown)

        // Force a completely different number every single frame for a crazy fast blur
        val valueAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            addUpdateListener {
                currentValue = listOf(1, 2, 3, 4, 5, 6, 12).filter { it != currentValue }.random()
                invalidate()
                HapticManager.tick()
            }
        }
        
        // Combine everything
        val fullSet = AnimatorSet()
        val shakeSet = AnimatorSet()
        shakeSet.playTogether(shake, valueAnim)
        
        fullSet.playSequentially(shakeSet, popSet)
        
        fullSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Lock in final value exactly when the entire animation sequence finishes
                currentValue = finalValue
                isRolling = false
                invalidate()
                HapticManager.thud()
                onComplete()
            }
        })
        
        fullSet.start()
    }
    
    fun setValueInstant(value: Int) {
        currentValue = value
        scaleX = 1f; scaleY = 1f; rotation = 0f; translationX = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val size = w.coerceAtMost(h)
        val cx = w / 2f
        val cy = h / 2f
        
        val corner = size * 0.25f
        rect.set(cx - size/2, cy - size/2, cx + size/2, cy + size/2)
        rect.inset(8f, 8f)
        
        // Shadow
        shadowRect.set(rect)
        shadowRect.offset(0f, 8f)
        canvas.drawRoundRect(shadowRect, corner, corner, shadowPaint)
        
        // Colored border (Player color)
        bgPaint.color = playerColor
        canvas.drawRoundRect(rect, corner, corner, bgPaint)
        
        // White inner box
        val innerPadding = size * 0.12f
        innerRect.set(rect)
        innerRect.inset(innerPadding, innerPadding)
        canvas.drawRoundRect(innerRect, corner * 0.7f, corner * 0.7f, innerPaint)

        // Giant Text Number
        textPaint.textSize = innerRect.height() * 0.75f
        val textOffset = (textPaint.descent() + textPaint.ascent()) / 2
        val displayValue = if (currentValue == 0) "?" else currentValue.toString()
        canvas.drawText(displayValue, cx, cy - textOffset, textPaint)
    }
}
