package com.sribnyak.composer_plus

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.AsyncTask

object Player {
    const val RATE = 22050

    const val NOT_PLAYING = 0
    const val NOTE_PLAYING = 1
    const val MUSIC_PLAYING = 2

    var state = 0
    var calls = 0

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
            calls--
            if (calls == 0) {
                state = NOT_PLAYING
                btnPlayToPlay()
            }
            return 0
        }
    }

    private fun play(notes: Array<Note>) {
        calls++
        PlaySoundTask().execute(Sound.toSound(notes))
    }

    fun play(melody: Melody) {
        state = MUSIC_PLAYING
        btnPlayToStop()
        play(melody.toArray())
    }

    fun play(note: Note) {
        state = NOTE_PLAYING
        play(arrayOf(note))
    }

    fun stop() {
        audioTrack.pause()
        audioTrack.flush()
    }
}