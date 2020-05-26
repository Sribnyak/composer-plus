package com.sribnyak.composer_plus

object Parser {
    data class Result (val newMelody: Melody?, val selection: Int)
    fun parse(text: String): Result {
        val newMelody = Melody()
        var i = 0
        var end = 0
        while (i < text.length) {
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
        return Result(newMelody, i)
    }
}