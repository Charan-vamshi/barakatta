package com.barakatta.game.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    
    private val colors = intArrayOf(
        Color.parseColor("#FFC107"), // Amber
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#FF5722")  // Deep Orange
    )

    private var animator: ValueAnimator? = null

    class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val color: Int,
        val size: Float,
        var rotation: Float,
        val rotationSpeed: Float,
        val isCircle: Boolean
    )

    fun burst() {
        particles.clear()
        
        val startX = width / 2f
        val startY = height / 2f

        for (i in 0 until 150) {
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val speed = Random.nextDouble(10.0, 60.0)
            
            particles.add(
                Particle(
                    x = startX,
                    y = startY,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat() - 20f, // initial upward burst
                    color = colors.random(),
                    size = Random.nextFloat() * 20f + 15f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 20f - 10f,
                    isCircle = Random.nextBoolean()
                )
            )
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000 // 3 seconds of confetti
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    private fun updateParticles() {
        val gravity = 1.5f
        val drag = 0.98f
        
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            
            p.x += p.vx
            p.y += p.vy
            
            p.vy += gravity
            p.vx *= drag
            p.vy *= drag
            
            p.rotation += p.rotationSpeed
            
            // Remove if fallen off screen
            if (p.y > height + 100) {
                iterator.remove()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for (p in particles) {
            paint.color = p.color
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            
            if (p.isCircle) {
                canvas.drawCircle(0f, 0f, p.size / 2f, paint)
            } else {
                canvas.drawRect(-p.size / 2f, -p.size / 2f, p.size / 2f, p.size / 2f, paint)
            }
            canvas.restore()
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
