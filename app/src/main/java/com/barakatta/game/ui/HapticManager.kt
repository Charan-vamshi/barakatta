package com.barakatta.game.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticManager {
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }
    }

    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(10, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(10)
        }
    }

    fun thud() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(40, 200))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(40)
        }
    }

    fun kill() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 50, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 50, 50, 100)
            vibrator?.vibrate(pattern, -1)
        }
    }

    fun win() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 50, 100, 50, 100, 50, 300)
            val amplitudes = intArrayOf(0, 200, 0, 200, 0, 200, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 100, 50, 100, 50, 100, 50, 300)
            vibrator?.vibrate(pattern, -1)
        }
    }
}
