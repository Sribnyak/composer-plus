package com.sribnyak.composer_plus

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.AsyncTask

object Player {
    const val RATE = 22050
    var musicPlaying = false

    @Suppress("DEPRECATION")
    val audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC,
        RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        AudioRecord.getMinBufferSize(
            RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        AudioTrack.MODE_STREAM
    )

    fun init() {
        audioTrack.play()
    }

    private class PlaySoundTask : AsyncTask<ShortArray, Int, Int>() {
        override fun doInBackground(vararg sound: ShortArray): Int {
            audioTrack.write(sound[0], 0, sound[0].size)
            audioTrack.play()
            musicPlaying = false
            return 0
        }
    }

    fun play(notes: Array<Note>) {
        PlaySoundTask().execute(Sound.toSound(notes))
    }

    fun play(note: Note) {
        play(arrayOf(note))
    }
}