package com.go4o.lite.ui

import android.media.AudioManager
import android.media.ToneGenerator

object SoundPlayer {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    fun playPass() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 500)
    }

    fun playFail() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 800)
    }
}
