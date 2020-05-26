package com.sribnyak.composer_plus

import kotlin.math.*

object Sound {
    var tempo = 120
    private const val fade = (0.1 * Player.RATE).toInt()
    private val overtones: Array<Double> = arrayOf(1.0, 0.5, 0.25, 0.125)
    private val noteFreq4: Array<Double> = arrayOf(
        4186.0, 4434.9, 4698.6, 4978.0, 5274.0,
        5587.6, 5919.9, 6271.9, 6644.8, 7040.0, 7458.6, 7902.1,
        0.0
    )
    private fun equalLoudnessContour(f: Double): Double {
        return 1.9 * (log10(f) - 3.2).pow(2) + 3.5
    }

    private fun getLen(note: Note): Double {
        if (note.dot)
            return Player.RATE * 60.0 / tempo * 3 / 2 * 4 / note.v
        return Player.RATE * 60.0 / tempo * 4 / note.v
    }

    private fun waveAt(f: Double, volume: Double, t: Double): Short {
        val phase = 2.0 * PI * f * t
        var x = 0.0
        for (i in overtones.indices)
            x += overtones[i] * sin((i + 1) * phase)
        return (x / overtones.sum() * Short.MAX_VALUE * 0.7 * volume).toShort()
    }

    fun toSound(notes: Array<Note>): ShortArray {
        var arrSize = 0
        for (note in notes)
            arrSize += getLen(note).toInt()
        val ans = Array<Short>(arrSize) { 0 }
        var len = 0

        for (note in notes) {
            val f = noteFreq4[note.pitchClass] / 2.0.pow(4 - note.octave)
            val volume = exp(equalLoudnessContour(f)) / 110.0

            for (i in 0 until getLen(note).toInt())
                ans[len++] = waveAt(f, volume, i.toDouble() / Player.RATE)
            for (i in 1 until min(fade, len))
                ans[len - i] = (ans[len - i] * i / fade.toDouble()).toShort()
        }
        return ans.toShortArray()
    }

}