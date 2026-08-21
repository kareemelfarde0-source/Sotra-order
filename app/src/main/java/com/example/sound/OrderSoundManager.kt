package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.model.AppSoundSettings
import com.example.model.Order
import com.example.model.RingtoneOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class OrderSoundManager(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var alarmJob: Job? = null
    private var previewJob: Job? = null

    private val _isAlarmPlaying = MutableStateFlow(false)
    val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying.asStateFlow()

    private val _activeAlarmOrder = MutableStateFlow<Order?>(null)
    val activeAlarmOrder: StateFlow<Order?> = _activeAlarmOrder.asStateFlow()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var systemRingtone: Ringtone? = null

    init {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            systemRingtone = RingtoneManager.getRingtone(context, notificationUri)
        } catch (e: Exception) {
            Log.e("OrderSoundManager", "Error initializing system ringtone", e)
        }
    }

    fun startContinuousAlarm(order: Order, settings: AppSoundSettings) {
        if (!settings.soundEnabled) return

        stopAlarm() // Stop previous if any
        _isAlarmPlaying.value = true
        _activeAlarmOrder.value = order

        alarmJob = coroutineScope.launch {
            try {
                while (isActive) {
                    // Trigger vibration pulse
                    if (settings.vibrationEnabled) {
                        triggerVibration()
                    }

                    // Play one sound cycle based on selected ringtone
                    playSoundSequence(settings.selectedRingtone, settings.alarmVolume)

                    // Short interval between loops
                    delay(1200)
                }
            } catch (e: Exception) {
                Log.e("OrderSoundManager", "Alarm loop exception", e)
            } finally {
                _isAlarmPlaying.value = false
            }
        }
    }

    fun previewRingtone(ringtoneOption: RingtoneOption, volume: Float = 0.9f) {
        previewJob?.cancel()
        previewJob = coroutineScope.launch {
            triggerVibration()
            playSoundSequence(ringtoneOption, volume)
        }
    }

    fun stopAlarm() {
        alarmJob?.cancel()
        alarmJob = null
        previewJob?.cancel()
        previewJob = null
        _isAlarmPlaying.value = false
        _activeAlarmOrder.value = null
        try {
            systemRingtone?.stop()
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("OrderSoundManager", "Error stopping audio", e)
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 300, 150, 300)
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
            }
        } catch (e: Exception) {
            Log.e("OrderSoundManager", "Vibrate error", e)
        }
    }

    private suspend fun playSoundSequence(option: RingtoneOption, volume: Float) {
        when (option) {
            RingtoneOption.CASH_REGISTER -> {
                // Play "Ka-Ching / Cash Register" ding-dong sequence: 880Hz -> 1320Hz -> 1760Hz
                playTone(880.0, 120, volume)
                delay(60)
                playTone(1320.0, 150, volume)
                delay(60)
                playTone(1760.0, 350, volume)
            }
            RingtoneOption.URGENT_ALARM -> {
                // Play urgent siren warble
                playTone(950.0, 180, volume)
                delay(40)
                playTone(1250.0, 180, volume)
                delay(40)
                playTone(950.0, 180, volume)
                delay(40)
                playTone(1400.0, 260, volume)
            }
            RingtoneOption.STORE_BELL -> {
                // Play classic double store entry chime (like high frequency brass bell)
                playTone(1046.5, 220, volume) // C6
                delay(90)
                playTone(1318.5, 450, volume) // E6
            }
            RingtoneOption.DIGITAL_CHIME -> {
                // Play cheerful modern electronic melody: G5 -> C6 -> E6 -> G6
                playTone(784.0, 100, volume)
                delay(40)
                playTone(1046.5, 100, volume)
                delay(40)
                playTone(1318.5, 100, volume)
                delay(40)
                playTone(1568.0, 300, volume)
            }
            RingtoneOption.SYSTEM_DEFAULT -> {
                try {
                    systemRingtone?.play()
                    delay(1500)
                } catch (e: Exception) {
                    playTone(1000.0, 400, volume)
                }
            }
        }
    }

    /**
     * Synthesizes a clean sine tone directly in PCM audio.
     * Guarantees zero latency and works completely offline with customized frequencies.
     */
    private fun playTone(freqHz: Double, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            // Apply envelope: smooth attack & decay to eliminate clicks
            val envelope = when {
                i < sampleRate * 0.02 -> i / (sampleRate * 0.02) // 20ms attack
                i > numSamples - (sampleRate * 0.04) -> (numSamples - i) / (sampleRate * 0.04) // 40ms decay
                else -> 1.0
            }
            val sample = sin(2.0 * Math.PI * freqHz * time) * envelope * Short.MAX_VALUE * volume.coerceIn(0.1f, 1.0f)
            generatedSnd[i] = sample.toInt().toShort()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, numSamples)
            audioTrack.play()

            // Release after playback
            coroutineScope.launch {
                delay(durationMs.toLong() + 100)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore track cleanup errors
                }
            }
        } catch (e: Exception) {
            Log.e("OrderSoundManager", "AudioTrack play error", e)
        }
    }
}
