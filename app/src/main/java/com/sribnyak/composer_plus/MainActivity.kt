package com.sribnyak.composer_plus

import android.annotation.SuppressLint
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.text.InputFilter
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import java.util.*
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

const val RATE = 22050
const val MIN_V = 32
const val MAX_V = 1
const val MIN_OCTAVE = 0 // change pitches and transposing to change this
const val MAX_OCTAVE = 3
const val DEFAULT_TEXT_SIZE = 18f
const val DEFAULT_PADDING_SIZE = 16
val pitchClassStr: Array<String> = arrayOf(
    "c", "#c", "d", "#d", "e",
    "f", "#f", "g", "#g", "a", "#a", "b",
    "-"
)
val noteFreq4: Array<Double> = arrayOf(
    4186.0, 4434.9, 4698.6, 4978.0, 5274.0,
    5587.6, 5919.9, 6271.9, 6644.8, 7040.0, 7458.6, 7902.1,
    0.0
)
val noteKeysId: Array<Int> = arrayOf(
    R.id.keyC, R.id.keyCis, R.id.keyD, R.id.keyEs, R.id.keyE,
    R.id.keyF, R.id.keyFis, R.id.keyG, R.id.keyGis, R.id.keyA, R.id.keyB, R.id.keyH,
    R.id.btnRest
)

const val fade = (0.1 * RATE).toInt()

var tempo = 120
var currentOctave = 1
var currentV = 4
var melody = Melody()
var musicPlaying = false

@Suppress("DEPRECATION")
val audioTrack = AudioTrack(
    AudioManager.STREAM_MUSIC,
    RATE,
    AudioFormat.CHANNEL_OUT_MONO,
    AudioFormat.ENCODING_PCM_16BIT,
    AudioRecord.getMinBufferSize(
        RATE, AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ),
    AudioTrack.MODE_STREAM
)
lateinit var editText: EditText

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        val scrollView: ScrollView = findViewById(R.id.scrollView)
        val btnTempo: Button = findViewById(R.id.btnTempo)
        btnTempo.text = getString(R.string.btnTempo, tempo)
        audioTrack.play()

        for (pitchClass in 0..12) {
            findViewById<Button>(noteKeysId[pitchClass]).setOnClickListener {
                val note = Note(currentV, pitchClass, currentOctave)
                if (melody.add(note)) {
                    val text = editText.text.toString()
                    if (text.isNotEmpty()) {
                        editText.setText("$text $note")
                        scrollView.fullScroll(View.FOCUS_DOWN)
                    } else {
                        editText.setText(note.toString())
                    }
                    if (!musicPlaying)
                        PlaySoundTask().execute(toSound(arrayOf(note)))
                } else {
                    Toast.makeText(
                        applicationContext,
                        R.string.maxLen, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
            val text = editText.text.toString()
            if (text.isNotEmpty()) {
                editText.setText(text.dropLastWhile { it != ' ' }.dropLast(1))
                melody.pop()
            }
        }
        findViewById<Button>(R.id.btnBackspace).setOnLongClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.confirm)
            val txt = TextView(this)
            txt.setTextColor(editText.currentTextColor)
            txt.setText(R.string.clearAllText)
            txt.textSize = DEFAULT_TEXT_SIZE
            txt.setPadding(dp2px(DEFAULT_PADDING_SIZE, resources.displayMetrics.density))
            txt.movementMethod = ScrollingMovementMethod()
            builder.setView(txt)
            builder.setPositiveButton(R.string.yes) { _, _ ->
                editText.setText("")
                melody = Melody()
            }
            builder.setNegativeButton(R.string.cancel, null)
            builder.show()
            return@setOnLongClickListener true
        }
        findViewById<Button>(R.id.btnPlus).setOnClickListener {
            if (currentV != MAX_V)
                currentV /= 2
        }
        findViewById<Button>(R.id.btnMinus).setOnClickListener {
            if (currentV != MIN_V)
                currentV *= 2
        }
        findViewById<Button>(R.id.btnUp).setOnClickListener {
            if (currentOctave != MAX_OCTAVE)
                currentOctave++
        }
        findViewById<Button>(R.id.btnDn).setOnClickListener {
            if (currentOctave != MIN_OCTAVE)
                currentOctave--
        }
        findViewById<Button>(R.id.btnDot).setOnClickListener {
            val note = melody.pop()
            if (note != null) {
                note.dot = !note.dot
                editText.setText(editText.text.toString().dropLastWhile { it != ' ' } + note)
                melody.add(note)
                if (!musicPlaying)
                    PlaySoundTask().execute(toSound(arrayOf(note)))
            }
        }
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            if (!musicPlaying) {
                musicPlaying = true
                PlaySoundTask().execute(toSound(melody.toArray()))
            } // TODO: make stoppable
        }
        findViewById<Button>(R.id.btnTempo).setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.dialogTempo)
            val minValue = 30
            val maxValue = 240

            val textEdit = EditText(this)
            textEdit.setBackgroundColor(Color.TRANSPARENT)
            textEdit.setPadding(dp2px(DEFAULT_PADDING_SIZE, resources.displayMetrics.density))
            textEdit.textAlignment = EditText.TEXT_ALIGNMENT_CENTER
            textEdit.inputType = InputType.TYPE_CLASS_NUMBER
            textEdit.filters = textEdit.filters.plus(InputFilter.LengthFilter(3))
            textEdit.setText(tempo.toString())
            textEdit.setSelection(textEdit.text.length)
            builder.setView(textEdit)

            builder.setNegativeButton(R.string.cancel, null)
            builder.setPositiveButton(R.string.ok, null)
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = textEdit.text.toString().toInt()
                if (value in minValue..maxValue) {
                    tempo = textEdit.text.toString().toInt()
                    btnTempo.text = getString(R.string.btnTempo, tempo)
                    dialog.dismiss()
                } else if (value < minValue) {
                    textEdit.setText(minValue.toString())
                    textEdit.setSelection(textEdit.text.length)
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.tooSlow, minValue), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    textEdit.setText(maxValue.toString())
                    textEdit.setSelection(textEdit.text.length)
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.tooFast, maxValue), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuTextEditing -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.menuTextEditing)
                val textEdit = EditText(this)
                textEdit.setBackgroundColor(Color.TRANSPARENT)
                textEdit.setPadding(dp2px(DEFAULT_PADDING_SIZE, resources.displayMetrics.density))
                textEdit.setHint(R.string.emptyEditText)
                textEdit.text = editText.text
                textEdit.setSelection(textEdit.text.length)
                builder.setView(textEdit)
                builder.setNegativeButton(R.string.cancel, null)
                builder.setPositiveButton(R.string.apply, null)
                val dialog = builder.create()
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val newText = textEdit.text.toString()
                        .toLowerCase(Locale.ENGLISH)
                        .split(' ', '\n', '\t', '\u00A0')
                        .filter(String::isNotEmpty)
                        .joinToString(" ")
                    val parser = Parser()
                    val good = parser.parse(newText)
                    if (good) {
                        melody = parser.newMelody
                        editText.setText(newText)
                        Toast.makeText(
                            applicationContext,
                            R.string.textEditApplied, Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    } else {
                        textEdit.setText(newText)
                        textEdit.setSelection(parser.i)
                        Toast.makeText(
                            applicationContext,
                            R.string.textEditNotApplied, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return true
            }
            R.id.menuTranspose -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.dialogTranspose)
                val minValue = MIN_OCTAVE * 12 - melody.minPitch()
                val maxValue = MAX_OCTAVE * 12 + 11 - melody.maxPitch()
                builder.setMessage(getString(R.string.textTranspose, minValue, maxValue))

                val textEdit = EditText(this)
                textEdit.setBackgroundColor(Color.TRANSPARENT)
                textEdit.setPadding(dp2px(DEFAULT_PADDING_SIZE, resources.displayMetrics.density))
                textEdit.textAlignment = EditText.TEXT_ALIGNMENT_CENTER
                textEdit.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                textEdit.filters = textEdit.filters.plus(InputFilter.LengthFilter(3))
                textEdit.setText("0")
                textEdit.setSelection(1)
                builder.setView(textEdit)

                builder.setNegativeButton(R.string.cancel, null)
                builder.setPositiveButton(R.string.apply, null)
                val dialog = builder.create()
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value = textEdit.text.toString().toInt()
                    if (value in minValue..maxValue) {
                        melody.transpose(textEdit.text.toString().toInt())
                        editText.setText(melody.toString())
                        Toast.makeText(
                            applicationContext,
                            R.string.textEditApplied, Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    } else {
                        textEdit.setText(if (value < minValue) "$minValue" else "$maxValue")
                        textEdit.setSelection(textEdit.text.length)
                        Toast.makeText(
                            applicationContext,
                            R.string.cannotTranspose, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                return true
            }
            R.id.menuHelp -> {
                val builder = AlertDialog.Builder(this)
                val txt = TextView(this)
                txt.setTextColor(editText.currentTextColor)
                txt.setText(R.string.textHelp)
                txt.textSize = DEFAULT_TEXT_SIZE
                txt.setPadding(dp2px(DEFAULT_PADDING_SIZE, resources.displayMetrics.density))
                txt.movementMethod = ScrollingMovementMethod()
                builder.setView(txt)
                builder.show()
                return true
            }
            R.id.menuAbout -> {
                val builder = AlertDialog.Builder(this)
                val txt = TextView(this)
                txt.setTextColor(editText.currentTextColor)
                txt.textSize = DEFAULT_TEXT_SIZE
                txt.setPadding(dp2px(DEFAULT_PADDING_SIZE, resources.displayMetrics.density))
                @Suppress("DEPRECATION")
                txt.text = Html.fromHtml(
                    getString(
                        R.string.textAbout,
                        getString(R.string.app_name), BuildConfig.VERSION_NAME
                    )
                )
                builder.setView(txt)
                builder.show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

class Parser {
    val newMelody = Melody()
    var i: Int = 0
    fun parse(text: String): Boolean {
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
                return false
            val dot = text[i] == '.'
            if (dot)
                i++
            var pitchClass = String()
            while (i < end && text[i] !in '0'..'9')
                pitchClass += text[i++]
            if (pitchClass !in pitchClassStr)
                return false
            var octave = MIN_OCTAVE
            if (pitchClass != "-") {
                if (i >= end)
                    return false
                octave = 0
                while (i < end && text[i] in '0'..'9')
                    octave = 10 * octave + (text[i++].toInt() - '0'.toInt())
                if (octave !in MIN_OCTAVE..MAX_OCTAVE)
                    return false
            }
            if (i < end)
                return false
            newMelody.add(Note(v, pitchClassStr.indexOf(pitchClass), octave, dot))
            end = ++i
        }
        return true
    }
}

@Suppress("SameParameterValue")
private fun dp2px(x: Int, density: Float): Int {
    return (x * density + 0.5).toInt()
}

private fun toSound(notes: Array<Note>): ShortArray {
    var arrSize = 0
    for (note in notes)
        arrSize += note.getLen().toInt()
    val ans = Array<Short>(arrSize) { 0 }
    var len = 0

    for (note in notes) {
        for (i in 0 until note.getLen().toInt())
            ans[len++] = note.waveAt(i.toDouble() / RATE)
        for (i in 1 until min(fade, len))
            ans[len - i] = (ans[len - i] * i / fade.toDouble()).toShort()
    }
    return ans.toShortArray()
}

private class PlaySoundTask : AsyncTask<ShortArray, Int, Int>() {
    override fun doInBackground(vararg sound: ShortArray): Int {
        audioTrack.write(sound[0], 0, sound[0].size)
        audioTrack.play()
        musicPlaying = false
        return 0
    }
}

fun min(a: Int, b: Int): Int {
    if (a < b)
        return a
    return b
}

fun max(a: Int, b: Int): Int {
    if (a > b)
        return a
    return b
}

class Melody {

    private val maxSize = 256
    private var size = 0
    private val src: Array<Note?> = arrayOfNulls(maxSize)

    fun add(note: Note): Boolean {
        if (size == maxSize)
            return false
        src[size++] = note
        return true
    }

    fun pop(): Note? {
        if (size == 0)
            return null
        size--
        val note = src[size]
        src[size] = null
        return note
    }

    fun minPitch(): Int {
        var ans = MAX_OCTAVE * 12 + 11
        for (i in 0 until size)
            ans = min(ans, src[i]!!.getPitch())
        return ans
    }

    fun maxPitch(): Int {
        var ans = MIN_OCTAVE * 12
        for (i in 0 until size)
            ans = max(ans, src[i]!!.getPitch())
        return ans
    }

    fun transpose(delta: Int) {
        for (i in 0 until size)
            src[i] = src[i]!!.transposed(delta)
    }

    override fun toString(): String {
        if (size == 0)
            return ""
        var ans = ""
        for (i in 0..size - 2)
            ans += src[i].toString() + ' '
        ans += src[size - 1].toString()
        return ans
    }

    fun toArray(): Array<Note> {
        val notes = Array(size) { Note() }
        for (i in 0 until size)
            notes[i] = src[i]!!
        return notes
    }

}

class Note(
    private val v: Int = 4,
    private val pitchClass: Int = 9,
    private val octave: Int = 1,
    var dot: Boolean = false
) {
    fun getLen(): Double {
        if (dot)
            return RATE * 60.0 / tempo * 3 / 2 * 4 / v
        return RATE * 60.0 / tempo * 4 / v
    }

    fun getPitch(): Int {
        return octave * 12 + pitchClass
    }

    fun transposed(delta: Int): Note {
        if (pitchClass == 12)
            return this // is it ok?
        val pitch = getPitch() + delta
        return Note(v, pitch % 12, pitch / 12, dot)
    }

    fun waveAt(t: Double): Short {
        val w = noteFreq4[pitchClass] / 2.0.pow(4 - octave) * 2.0 * PI
        // TODO right volume correction
        //val minF = log2(noteFreq4[0] / 16)
        //val maxF = log2(noteFreq4[11] / 2)
        //val k = 0.5
        val volume = 1 - 0.9 * w / 2 / PI / noteFreq4[11]
        return ((sin(w * t) * Short.MAX_VALUE / 2
                + sin(2 * w * t) * Short.MAX_VALUE / 4
                + sin(3 * w * t) * Short.MAX_VALUE / 8
                + sin(4 * w * t) * Short.MAX_VALUE / 16
                ) * volume).toShort()
    }

    override fun toString(): String {
        return v.toString() +
                (if (dot) "." else "") +
                pitchClassStr[pitchClass] +
                (if (pitchClass < 12) octave.toString() else "")
    }
}
