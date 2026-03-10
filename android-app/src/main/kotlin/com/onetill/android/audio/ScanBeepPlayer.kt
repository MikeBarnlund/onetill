package com.onetill.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.onetill.android.R

object ScanBeepPlayer {
    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var loaded = false

    fun init(context: Context) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, _, status ->
                    loaded = (status == 0)
                }
                soundId = pool.load(context, R.raw.scan_beep, 1)
            }
    }

    fun play() {
        if (loaded) {
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loaded = false
    }
}
