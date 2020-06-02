package com.sribnyak.composer_plus

import java.util.*

object Parser {
    const val SUCCESS = 0
    const val TOO_LONG = 1
    const val SYNTAX_ERROR = 2

    data class Result (val newMelody: Melody?, val selection: Int, val code: Int = SYNTAX_ERROR)

    fun cleanText(text: String): String {
        return text.toLowerCase(Locale.ENGLISH)
            .split(' ', '\n', '\t', '\u00A0')
            .filter(String::isNotEmpty)
            .joinToString(" ")
    }

    fun parse(text: String): Result {
        val newMelody = Melody()
        var i = 0
        var end = 0
        while (i < text.length) {
            if (newMelody.isFull())
                return Result(newMelody, i, TOO_LONG)
            while (end < text.length && text[end] != ' ')
                end++
            var v = 0
            while (i < end && text[i] in '0'..'9')
                v = 10 * v + (text[i++].toInt() - '0'.toInt())

            fun Int.isNotPow2(): Boolean {
                var x = this
                while (x != 1) {
                    if (x % 2 == 1)
                        return true
                    x /= 2
                }
                return false
            }

            if (v !in MAX_V..MIN_V || v.isNotPow2() || i >= end)
                return Result(null, i)
            val dot = text[i] == '.'
            if (dot)
                i++
            var pitchClass = String()
            while (i < end && text[i] !in '0'..'9')
                pitchClass += text[i++]
            if (pitchClass !in pitchClassStr)
                return Result(null, i)
            var octave = MIN_OCTAVE
            if (pitchClass != "-") {
                if (i >= end)
                    return Result(null, i)
                octave = 0
                while (i < end && text[i] in '0'..'9')
                    octave = 10 * octave + (text[i++].toInt() - '0'.toInt())
                if (octave !in MIN_OCTAVE..MAX_OCTAVE)
                    return Result(null, i)
            }
            if (i < end)
                return Result(null, i)
            newMelody.add(
                Note(
                    v,
                    pitchClassStr.indexOf(pitchClass),
                    octave,
                    dot
                )
            )
            end = ++i
        }
        return Result(newMelody, i, SUCCESS)
    }
}