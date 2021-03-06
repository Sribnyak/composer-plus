package com.sribnyak.composer_plus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import kotlin.math.max
import kotlin.math.min

const val MIN_V = 32
const val MAX_V = 1
const val MIN_OCTAVE = 0 // change pitches and transposition to change this
const val MAX_OCTAVE = 3

val pitchClassStr: Array<String> = arrayOf(
    "c", "#c", "d", "#d", "e",
    "f", "#f", "g", "#g", "a", "#a", "b",
    "-"
)

lateinit var btnPlay: ImageButton
fun btnPlayToPlay() {
    btnPlay.setImageResource(R.drawable.ic_play_arrow_black_24dp)
}

fun btnPlayToStop() {
    btnPlay.setImageResource(R.drawable.ic_stop_black_24dp)
}

class MainActivity : AppCompatActivity() {

    private var currentOctave = 1
    private var currentV = 4
    private var melody = Melody()

    private val defaultTextSize = 18f
    private var defaultPaddingSize = 16
    private lateinit var editText: EditText

    private fun defaultTextView(context: Context): TextView {
        val textView = TextView(context)
        textView.setPadding(defaultPaddingSize)
        textView.setTextColor(editText.currentTextColor)
        textView.textSize = defaultTextSize
        textView.movementMethod = ScrollingMovementMethod()
        return textView
    }

    private fun editTextNumberPicker(context: Context, minValue: Int, maxValue: Int, init: Int): EditText {
        val inputType: Int = if (minValue >= 0) InputType.TYPE_CLASS_NUMBER
            else InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        val maxLen: Int = max(minValue.toString().length, maxValue.toString().length)
        val textEdit = EditText(context)
        textEdit.setBackgroundColor(Color.TRANSPARENT)
        textEdit.setPadding(defaultPaddingSize)
        textEdit.textAlignment = EditText.TEXT_ALIGNMENT_CENTER
        textEdit.inputType = inputType
        textEdit.filters = textEdit.filters.plus(InputFilter.LengthFilter(maxLen))
        textEdit.setText(init.toString())
        textEdit.setSelection(init.toString().length)
        return textEdit
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        defaultPaddingSize = (defaultPaddingSize * resources.displayMetrics.density + 0.5).toInt()
        btnPlay = findViewById(R.id.btnPlay)
        editText = findViewById(R.id.editText)
        val btnTempo: Button = findViewById(R.id.btnTempo)
        btnTempo.text = getString(R.string.btnTempo, Sound.tempo)
        Player.init()

        val noteKeysId: Array<Int> = arrayOf(
            R.id.keyC, R.id.keyCis, R.id.keyD, R.id.keyEs, R.id.keyE,
            R.id.keyF, R.id.keyFis, R.id.keyG, R.id.keyGis, R.id.keyA, R.id.keyB, R.id.keyH,
            R.id.btnRest
        )

        for (pitchClass in 0..12) {
            findViewById<Button>(noteKeysId[pitchClass]).setOnClickListener {
                val note = Note(currentV, pitchClass, currentOctave)
                if (melody.add(note)) {
                    val text = editText.text.toString()
                    if (text.isNotEmpty()) {
                        editText.setText("$text $note")
                        findViewById<ScrollView>(R.id.scrollView).fullScroll(View.FOCUS_DOWN)
                    } else {
                        editText.setText(note.toString())
                    }
                    if (Player.state != Player.MUSIC_PLAYING)
                        Player.play(note)
                } else {
                    showToast(getString(R.string.maxLen))
                }
            }
        }

        findViewById<ImageButton>(R.id.btnBackspace).setOnClickListener {
            val text = editText.text.toString()
            if (text.isNotEmpty()) {
                editText.setText(text.dropLastWhile { it != ' ' }.dropLast(1))
                melody.pop()
            }
        }

        findViewById<ImageButton>(R.id.btnBackspace).setOnLongClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.confirm)
            val txt = defaultTextView(this)
            txt.setText(R.string.clearAllText)
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
                if (Player.state != Player.MUSIC_PLAYING)
                    Player.play(note)
            }
        }
        btnPlay.setOnClickListener {
            if (Player.state == Player.NOT_PLAYING)
                Player.play(melody)
            else if (Player.state == Player.MUSIC_PLAYING)
                Player.stop()
            else
                showToast(getString(R.string.waitToPlay))
        }
        btnTempo.setOnClickListener {
            val minValue = 30
            val maxValue = 240
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.dialogTempo)
            builder.setMessage(getString(R.string.textTempo, minValue, maxValue))
            val textEdit = editTextNumberPicker(this,
                minValue, maxValue, Sound.tempo)
            builder.setView(textEdit)
            builder.setNegativeButton(R.string.cancel, null)
            builder.setPositiveButton(R.string.ok, null)
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = textEdit.text.toString().toInt()
                if (value in minValue..maxValue) {
                    Sound.tempo = value
                    btnTempo.text = getString(R.string.btnTempo, Sound.tempo)
                    dialog.dismiss()
                } else {
                    textEdit.setText(if (value < minValue) "$minValue" else "$maxValue")
                    textEdit.setSelection(textEdit.text.length)
                    showToast(getString(R.string.valueOutOfRange))
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
                textEdit.setPadding(defaultPaddingSize)
                textEdit.setHint(R.string.emptyEditText)
                textEdit.text = editText.text
                textEdit.setSelection(textEdit.text.length)
                builder.setView(textEdit)
                builder.setNegativeButton(R.string.cancel, null)
                builder.setPositiveButton(R.string.apply, null)
                val dialog = builder.create()
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val newText = Parser.cleanText(textEdit.text.toString())
                    val parserResult = Parser.parse(newText)
                    if (parserResult.newMelody != null) {
                        melody = parserResult.newMelody
                        if (parserResult.code == Parser.TOO_LONG) {
                            editText.setText(parserResult.newMelody.toString())
                            showToast(getString(R.string.textEditTooLong))
                        } else {
                            editText.setText(newText)
                            showToast(getString(R.string.textEditApplied))
                        }
                        dialog.dismiss()
                    } else {
                        textEdit.setText(newText)
                        textEdit.setSelection(parserResult.selection)
                        showToast(getString(R.string.textEditNotApplied))
                    }
                }
                return true
            }
            R.id.menuTranspose -> {
                val minValue = MIN_OCTAVE * 12 - melody.minPitch()
                val maxValue = MAX_OCTAVE * 12 + 11 - melody.maxPitch()
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.dialogTranspose)
                builder.setMessage(getString(R.string.textTranspose, minValue, maxValue))
                val textEdit = editTextNumberPicker(this,
                    minValue, maxValue, 0)
                builder.setView(textEdit)
                builder.setNegativeButton(R.string.cancel, null)
                builder.setPositiveButton(R.string.apply, null)
                val dialog = builder.create()
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val value = textEdit.text.toString().toInt()
                    if (value in minValue..maxValue) {
                        melody.transpose(value)
                        editText.setText(melody.toString())
                        showToast(getString(R.string.textEditApplied))
                        dialog.dismiss()
                    } else {
                        textEdit.setText(if (value < minValue) "$minValue" else "$maxValue")
                        textEdit.setSelection(textEdit.text.length)
                        showToast(getString(R.string.valueOutOfRange))
                    }
                }
                return true
            }
            R.id.menuShareMelody -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, SaveAndShare.makeText(editText.text.toString()))
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
                return true
            }
            R.id.menuHelp -> {
                val builder = AlertDialog.Builder(this)
                val txt = defaultTextView(this)
                txt.setText(R.string.textHelp)
                builder.setView(txt)
                builder.show()
                return true
            }
            R.id.menuAbout -> {
                val builder = AlertDialog.Builder(this)
                val txt = defaultTextView(this)
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

class Melody {

    private val maxSize = 256
    private var size = 0
    private val src: Array<Note?> = arrayOfNulls(maxSize)

    private fun isEmpty(): Boolean {
        return size == 0
    }

    fun isFull(): Boolean {
        return size == maxSize
    }

    fun add(note: Note): Boolean {
        if (isFull())
            return false
        src[size++] = note
        return true
    }

    fun pop(): Note? {
        if (isEmpty())
            return null
        size--
        val note = src[size]
        src[size] = null
        return note
    }

    fun minPitch(): Int {
        var ans = MAX_OCTAVE * 12 + 11
        for (i in 0 until size) if (src[i]!!.pitchClass != 12)
            ans = min(ans, src[i]!!.pitch)
        return ans
    }

    fun maxPitch(): Int {
        var ans = MIN_OCTAVE * 12
        for (i in 0 until size) if (src[i]!!.pitchClass != 12)
            ans = max(ans, src[i]!!.pitch)
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
    val v: Int = 4,
    val pitchClass: Int = 9,
    val octave: Int = 1,
    var dot: Boolean = false
) {
    val pitch = octave * 12 + pitchClass

    fun transposed(delta: Int): Note {
        if (pitchClass == 12)
            return this // is it ok?
        val newPitch = pitch + delta
        return Note(v, newPitch % 12, newPitch / 12, dot)
    }

    override fun toString(): String {
        return v.toString() +
                (if (dot) "." else "") +
                pitchClassStr[pitchClass] +
                (if (pitchClass < 12) octave.toString() else "")
    }
}
