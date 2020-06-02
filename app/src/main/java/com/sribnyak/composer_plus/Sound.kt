package com.sribnyak.composer_plus

import kotlin.math.*

object Sound {
    var tempo = 120
    private const val fadeIn = 1e-3
    private const val fadeOut = 1e-2
    private val overtones: Array<Double> = arrayOf(1.0, 0.3, 0.1)
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
            return 60.0 / tempo * 3 / 2 * 4 / note.v
        return 60.0 / tempo * 4 / note.v
    }

    private fun waveAt(f: Double, t: Double): Double {
        val phase = 2.0 * PI * f * t
        var x = 0.0
        for (i in overtones.indices)
            x += overtones[i] * sin((i + 1) * phase)
        return x / overtones.sum()
    }

    fun toSound(notes: Array<Note>): ShortArray {
        var arrSize = 0
        for (note in notes)
            arrSize += (getLen(note) * Player.RATE).toInt()
        val sound = ShortArray(arrSize)
        var pos = 0

        for (note in notes) {
            val f = noteFreq4[note.pitchClass] / 2.0.pow(4 - note.octave)
            val volume = exp(equalLoudnessContour(f)) / 110.0

            val noteLen = (getLen(note) * Player.RATE).toInt()
            val fadeInLen = min((fadeIn * Player.RATE).toInt(), noteLen*2/3)
            val fadeOutLen = min((fadeOut * Player.RATE).toInt(), noteLen*2/3)

            val noteSound = Array(noteLen) {
                waveAt(f, it.toDouble() / Player.RATE) * volume
            }
            for (i in 1..fadeInLen)
                noteSound[i - 1] *= i / fadeInLen.toDouble()
            for (i in 1..fadeOutLen)
                noteSound[noteLen - i] *= i / fadeOutLen.toDouble()

            for (i in noteSound.indices)
                sound[pos+i] = (noteSound[i] * 0.7 * Short.MAX_VALUE).toShort()
            pos += noteLen
        }
        return sound
    }
}