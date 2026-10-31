package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Studio-grade Real-Time Audio DSP Engine for OpenRadio.
 *
 * Combines:
 * 1. Real-time 16-bit PCM 5-Band Biquad Filter Bank + Sub-Bass Exciter + Soft-Knee Limiter
 *    via Media3 [AudioProcessor] inside ExoPlayer's audio pipeline (works on 100% of devices,
 *    Bluetooth speakers, headphones, and emulators with zero hardware HAL dependency).
 * 2. Hardware [Equalizer], [BassBoost], and [LoudnessEnhancer] bound to PlaybackService's
 *    ExoPlayer audioSessionId when supported by the device DSP.
 * 3. Persistent preferences so the user's audio profile survives app restarts and is shared
 *    between the Equalizer Dialog and Settings Screen.
 */
@OptIn(UnstableApi::class)
object RadioAudioDspEngine {

    private const val PREFS_NAME = "openradio_audio_dsp_prefs"
    private const val KEY_ENABLED = "eq_enabled"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_BASS_BOOST = "eq_bass_boost"
    private const val KEY_LOUDNESS = "eq_loudness"
    private const val KEY_CUSTOM_BANDS = "eq_custom_bands"

    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "12kHz")
    private val bandFrequencies = doubleArrayOf(60.0, 230.0, 910.0, 3600.0, 12000.0)

    /**
     * Distinct, high-contrast acoustic profiles in dB (-10 dB to +10 dB) for each of the 5 bands:
     * [60Hz (Sub/Bass), 230Hz (Warmth/Body), 910Hz (Vocals/Mid), 3.6kHz (Presence/Attack), 12kHz (Air/Brilliance)]
     */
    val presetProfilesDb: Map<String, FloatArray> = linkedMapOf(
        "Normal" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
        "Cumbia / Tropical" to floatArrayOf(6.5f, 3.5f, -1.5f, 4.5f, 6.0f),
        "Rock" to floatArrayOf(7.0f, 3.0f, -3.5f, 4.5f, 7.0f),
        "Pop" to floatArrayOf(-1.5f, 3.5f, 5.5f, 4.0f, 2.5f),
        "Voz / Noticias" to floatArrayOf(-8.5f, -3.5f, 7.5f, 6.5f, -2.0f),
        "Clásica" to floatArrayOf(4.5f, 2.5f, -1.0f, 3.0f, 5.5f),
        "Jazz / Acústico" to floatArrayOf(4.5f, 2.0f, 2.5f, 3.5f, 4.0f),
        "Extra Graves" to floatArrayOf(9.0f, 5.5f, -2.0f, 1.5f, 2.5f)
    )

    val presetsList: List<String> = presetProfilesDb.keys.toList() + "Personalizado"

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _selectedPreset = MutableStateFlow("Normal")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    private val _bandGainsDb = MutableStateFlow(listOf(0f, 0f, 0f, 0f, 0f))
    val bandGainsDb: StateFlow<List<Float>> = _bandGainsDb.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0f)
    val bassBoostStrength: StateFlow<Float> = _bassBoostStrength.asStateFlow()

    private val _loudnessBoost = MutableStateFlow(0f)
    val loudnessBoost: StateFlow<Float> = _loudnessBoost.asStateFlow()

    private var prefs: SharedPreferences? = null
    private var currentAudioSessionId: Int = 0
    private var hwEqualizer: Equalizer? = null
    private var hwBassBoost: BassBoost? = null
    private var hwLoudnessEnhancer: LoudnessEnhancer? = null

    val pcmProcessor: AudioProcessor = BiquadEqAudioProcessor()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        val savedEnabled = sp.getBoolean(KEY_ENABLED, false)
        val savedPreset = sp.getString(KEY_PRESET, "Normal") ?: "Normal"
        val savedBass = sp.getFloat(KEY_BASS_BOOST, 0f).coerceIn(0f, 1f)
        val savedLoudness = sp.getFloat(KEY_LOUDNESS, 0f).coerceIn(0f, 1f)

        val savedBandsStr = sp.getString(KEY_CUSTOM_BANDS, null)
        val initialBands = if (savedPreset == "Personalizado" && !savedBandsStr.isNullOrBlank()) {
            val parsed = savedBandsStr.split(",").mapNotNull { it.toFloatOrNull() }
            if (parsed.size == 5) parsed else listOf(0f, 0f, 0f, 0f, 0f)
        } else {
            presetProfilesDb[savedPreset]?.toList() ?: listOf(0f, 0f, 0f, 0f, 0f)
        }

        _enabled.value = savedEnabled
        _selectedPreset.value = if (savedPreset in presetsList) savedPreset else "Normal"
        _bandGainsDb.value = initialBands
        _bassBoostStrength.value = savedBass
        _loudnessBoost.value = savedLoudness

        updateDspCoefficients()
    }

    fun setEnabled(isEnabled: Boolean) {
        _enabled.value = isEnabled
        prefs?.edit()?.putBoolean(KEY_ENABLED, isEnabled)?.apply()
        updateDspCoefficients()
        applyHardwareEffects()
    }

    fun selectPreset(presetName: String) {
        _selectedPreset.value = presetName
        if (!_enabled.value) {
            _enabled.value = true
            prefs?.edit()?.putBoolean(KEY_ENABLED, true)?.apply()
        }
        val targetBands = presetProfilesDb[presetName]?.toList()
        if (targetBands != null) {
            _bandGainsDb.value = targetBands
        }
        prefs?.edit()?.putString(KEY_PRESET, presetName)?.apply()
        updateDspCoefficients()
        applyHardwareEffects()
    }

    fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0..4) return
        val clamped = gainDb.coerceIn(-10f, 10f)
        val updated = _bandGainsDb.value.toMutableList()
        updated[bandIndex] = clamped
        _bandGainsDb.value = updated
        _selectedPreset.value = "Personalizado"
        if (!_enabled.value) {
            _enabled.value = true
        }
        prefs?.edit()
            ?.putBoolean(KEY_ENABLED, true)
            ?.putString(KEY_PRESET, "Personalizado")
            ?.putString(KEY_CUSTOM_BANDS, updated.joinToString(","))
            ?.apply()
        updateDspCoefficients()
        applyHardwareEffects()
    }

    fun setBassBoost(strength: Float) {
        val clamped = strength.coerceIn(0f, 1f)
        _bassBoostStrength.value = clamped
        if (!_enabled.value && clamped > 0f) {
            _enabled.value = true
            prefs?.edit()?.putBoolean(KEY_ENABLED, true)?.apply()
        }
        prefs?.edit()?.putFloat(KEY_BASS_BOOST, clamped)?.apply()
        updateDspCoefficients()
        applyHardwareEffects()
    }

    fun setLoudnessBoost(strength: Float) {
        val clamped = strength.coerceIn(0f, 1f)
        _loudnessBoost.value = clamped
        if (!_enabled.value && clamped > 0f) {
            _enabled.value = true
            prefs?.edit()?.putBoolean(KEY_ENABLED, true)?.apply()
        }
        prefs?.edit()?.putFloat(KEY_LOUDNESS, clamped)?.apply()
        updateDspCoefficients()
        applyHardwareEffects()
    }

    @Synchronized
    fun bindAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        if (currentAudioSessionId != audioSessionId) {
            releaseHardwareEffects()
            currentAudioSessionId = audioSessionId
        }
        try {
            if (hwEqualizer == null) {
                hwEqualizer = Equalizer(0, audioSessionId)
            }
        } catch (_: Exception) {}
        try {
            if (hwBassBoost == null) {
                hwBassBoost = BassBoost(0, audioSessionId)
            }
        } catch (_: Exception) {}
        try {
            if (hwLoudnessEnhancer == null) {
                hwLoudnessEnhancer = LoudnessEnhancer(audioSessionId)
            }
        } catch (_: Exception) {}
        applyHardwareEffects()
    }

    @Synchronized
    fun release() {
        releaseHardwareEffects()
        currentAudioSessionId = 0
    }

    @Synchronized
    private fun releaseHardwareEffects() {
        try { hwEqualizer?.release() } catch (_: Exception) {}
        try { hwBassBoost?.release() } catch (_: Exception) {}
        try { hwLoudnessEnhancer?.release() } catch (_: Exception) {}
        hwEqualizer = null
        hwBassBoost = null
        hwLoudnessEnhancer = null
    }

    @Synchronized
    private fun applyHardwareEffects() {
        val active = _enabled.value
        val gains = _bandGainsDb.value
        val bass = _bassBoostStrength.value
        val loudness = _loudnessBoost.value

        try {
            hwEqualizer?.let { eq ->
                eq.enabled = active
                if (active) {
                    val numBands = eq.numberOfBands.toInt()
                    val range = eq.bandLevelRange
                    val minMb = range[0].toInt()
                    val maxMb = range[1].toInt()
                    for (b in 0 until numBands) {
                        val mappedIdx = ((b.toFloat() / numBands.coerceAtLeast(1)) * 5).toInt().coerceIn(0, 4)
                        // Apply 40% of gain in millibels on hardware EQ to complement the Biquad PCM processor
                        val gainMb = (gains[mappedIdx] * 40f).toInt()
                        val extraBassMb = if (b == 0) (bass * 300f).toInt() else 0
                        val targetMb = (gainMb + extraBassMb).coerceIn(minMb, maxMb).toShort()
                        eq.setBandLevel(b.toShort(), targetMb)
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            hwBassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.enabled = active && bass > 0.01f
                    if (active) {
                        bb.setStrength((bass * 1000f).toInt().coerceIn(0, 1000).toShort())
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            hwLoudnessEnhancer?.let { le ->
                le.enabled = active && loudness > 0.01f
                if (active) {
                    // Up to +600 mB (+6 dB) hardware loudness boost
                    le.setTargetGain((loudness * 600f).toInt())
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateDspCoefficients() {
        (pcmProcessor as? BiquadEqAudioProcessor)?.rebuildFilters()
    }

    /**
     * Coefficient snapshot for a single 2nd-order Biquad section (Direct Form II Transposed).
     */
    private class BiquadCoeffs(
        val b0: Double,
        val b1: Double,
        val b2: Double,
        val a1: Double,
        val a2: Double,
        val isIdentity: Boolean = false
    )

    private class BiquadFilterBankSnapshot(
        val active: Boolean,
        val stages: Array<BiquadCoeffs>,
        val preampLinearGain: Double,
        val hasAnyActiveStage: Boolean
    )

    /**
     * Real-time 5-band Biquad PCM AudioProcessor with Soft-Knee Mastering Limiter.
     */
    private class BiquadEqAudioProcessor : AudioProcessor {
        private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
        private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET
        private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
        private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
        private var inputEnded = false

        // State per channel (up to 2 channels) and 5 filter stages: z1[ch][stage], z2[ch][stage]
        private var z1 = Array(2) { DoubleArray(5) }
        private var z2 = Array(2) { DoubleArray(5) }

        @Volatile
        private var currentSnapshot: BiquadFilterBankSnapshot = BiquadFilterBankSnapshot(
            active = false,
            stages = emptyArray(),
            preampLinearGain = 1.0,
            hasAnyActiveStage = false
        )

        override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
            if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
                this.inputAudioFormat = AudioFormat.NOT_SET
                this.outputAudioFormat = AudioFormat.NOT_SET
                return AudioFormat.NOT_SET
            }
            this.inputAudioFormat = inputAudioFormat
            this.outputAudioFormat = inputAudioFormat
            val channels = inputAudioFormat.channelCount.coerceIn(1, 8)
            z1 = Array(channels) { DoubleArray(5) }
            z2 = Array(channels) { DoubleArray(5) }
            rebuildFilters()
            return outputAudioFormat
        }

        override fun isActive(): Boolean {
            return outputAudioFormat != AudioFormat.NOT_SET
        }

        fun rebuildFilters() {
            val sampleRate = inputAudioFormat.sampleRate.takeIf { it > 0 }?.toDouble() ?: 44100.0
            val isEqEnabled = _enabled.value
            val gains = _bandGainsDb.value
            val bassStrength = _bassBoostStrength.value
            val loudnessStrength = _loudnessBoost.value

            if (!isEqEnabled) {
                currentSnapshot = BiquadFilterBankSnapshot(
                    active = false,
                    stages = emptyArray(),
                    preampLinearGain = 1.0,
                    hasAnyActiveStage = false
                )
                return
            }

            // Combine Band 0 (60Hz) with dedicated Bass Boost slider (+0 to +9.5 dB extra sub-bass)
            // and Band 1 (230Hz) with warm punch (+0 to +3.5 dB)
            val effectiveGains = DoubleArray(5) { idx ->
                val base = gains.getOrElse(idx) { 0f }.toDouble()
                when (idx) {
                    0 -> (base + bassStrength * 9.5).coerceIn(-12.0, 14.0)
                    1 -> (base + bassStrength * 3.5).coerceIn(-12.0, 12.0)
                    3 -> (base + loudnessStrength * 3.0).coerceIn(-12.0, 12.0)
                    else -> base.coerceIn(-12.0, 12.0)
                }
            }

            val stages = Array(5) { stageIdx ->
                val freq = bandFrequencies[stageIdx].coerceAtMost(sampleRate * 0.45)
                val db = effectiveGains[stageIdx]
                when (stageIdx) {
                    0 -> createLowShelfCoeffs(sampleRate, freq, db, q = 0.707)
                    4 -> createHighShelfCoeffs(sampleRate, freq, db, q = 0.707)
                    else -> createPeakingCoeffs(sampleRate, freq, db, q = 1.15)
                }
            }

            // Smart auto-headroom + loudness compensation so boosted bands sound punchy without clipping
            val maxPositiveBoostDb = effectiveGains.maxOrNull()?.coerceAtLeast(0.0) ?: 0.0
            val headroomCutDb = if (maxPositiveBoostDb > 3.5) -(maxPositiveBoostDb - 3.5) * 0.35 else 0.0
            val loudnessGainDb = loudnessStrength * 4.5
            val totalPreampDb = (headroomCutDb + loudnessGainDb).coerceIn(-6.0, 6.0)
            val preampLinear = 10.0.pow(totalPreampDb / 20.0)
            val hasActive = stages.any { !it.isIdentity } || kotlin.math.abs(preampLinear - 1.0) > 0.01

            currentSnapshot = BiquadFilterBankSnapshot(
                active = true,
                stages = stages,
                preampLinearGain = preampLinear,
                hasAnyActiveStage = hasActive
            )
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val remaining = inputBuffer.remaining()
            if (remaining == 0) return

            if (buffer.capacity() < remaining) {
                buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
            } else {
                buffer.clear()
            }

            val snap = currentSnapshot
            val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)

            if (!snap.active || !snap.hasAnyActiveStage || snap.stages.isEmpty()) {
                buffer.put(inputBuffer)
                buffer.flip()
                outputBuffer = buffer
                return
            }

            inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val stages = snap.stages
            val preamp = snap.preampLinearGain
            val numSamples = remaining / 2
            var ch = 0

            for (i in 0 until numSamples) {
                val rawShort = inputBuffer.short
                var x = (rawShort / 32768.0) * preamp

                val chIdx = if (ch < z1.size) ch else 0
                val chZ1 = z1[chIdx]
                val chZ2 = z2[chIdx]

                // Process through active Biquad filter stages (Direct Form II Transposed)
                for (s in 0 until stages.size) {
                    val c = stages[s]
                    if (c.isIdentity) continue
                    val y = c.b0 * x + chZ1[s]
                    chZ1[s] = c.b1 * x - c.a1 * y + chZ2[s]
                    chZ2[s] = c.b2 * x - c.a2 * y
                    x = y
                }

                // Mastering soft-knee limiter above +/- 0.85 to prevent digital harshness/clipping
                val limited = softClip(x)
                val outInt = (limited * 32767.0).toInt().coerceIn(-32768, 32767)
                buffer.putShort(outInt.toShort())

                ch++
                if (ch >= channelCount) ch = 0
            }

            buffer.flip()
            outputBuffer = buffer
        }

        private fun softClip(sample: Double): Double {
            val threshold = 0.85
            return when {
                sample > threshold -> {
                    val excess = sample - threshold
                    threshold + (1.0 - threshold) * (excess / (excess + (1.0 - threshold)))
                }
                sample < -threshold -> {
                    val excess = -sample - threshold
                    -(threshold + (1.0 - threshold) * (excess / (excess + (1.0 - threshold))))
                }
                else -> sample
            }
        }

        override fun queueEndOfStream() {
            inputEnded = true
        }

        override fun getOutput(): ByteBuffer {
            val out = outputBuffer
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            return out
        }

        override fun isEnded(): Boolean {
            return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
        }

        override fun flush() {
            outputBuffer = AudioProcessor.EMPTY_BUFFER
            inputEnded = false
            for (c in z1.indices) {
                z1[c].fill(0.0)
                z2[c].fill(0.0)
            }
        }

        override fun reset() {
            flush()
            buffer = AudioProcessor.EMPTY_BUFFER
            inputAudioFormat = AudioFormat.NOT_SET
            outputAudioFormat = AudioFormat.NOT_SET
        }

        private fun createPeakingCoeffs(fs: Double, f0: Double, dbGain: Double, q: Double): BiquadCoeffs {
            if (kotlin.math.abs(dbGain) < 0.05) return BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0, isIdentity = true)
            val a = 10.0.pow(dbGain / 40.0)
            val w0 = 2.0 * PI * f0 / fs
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)

            val b0 = 1.0 + alpha * a
            val b1 = -2.0 * cosW0
            val b2 = 1.0 - alpha * a
            val a0 = 1.0 + alpha / a
            val a1 = -2.0 * cosW0
            val a2 = 1.0 - alpha / a

            return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
        }

        private fun createLowShelfCoeffs(fs: Double, f0: Double, dbGain: Double, q: Double): BiquadCoeffs {
            if (kotlin.math.abs(dbGain) < 0.05) return BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0, isIdentity = true)
            val a = 10.0.pow(dbGain / 40.0)
            val w0 = 2.0 * PI * f0 / fs
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

            val b0 = a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)
            val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
            val b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)
            val a0 = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
            val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
            val a2 = (a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha

            return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
        }

        private fun createHighShelfCoeffs(fs: Double, f0: Double, dbGain: Double, q: Double): BiquadCoeffs {
            if (kotlin.math.abs(dbGain) < 0.05) return BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0, isIdentity = true)
            val a = 10.0.pow(dbGain / 40.0)
            val w0 = 2.0 * PI * f0 / fs
            val cosW0 = cos(w0)
            val alpha = sin(w0) / (2.0 * q)
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

            val b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)
            val b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
            val b2 = a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha)
            val a0 = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
            val a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0)
            val a2 = (a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha

            return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
        }
    }
}
