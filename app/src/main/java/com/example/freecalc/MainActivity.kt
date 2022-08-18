package com.example.freecalc

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.GridLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.marginStart
import com.example.freecalc.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.util.*
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // CALC VALUES
    var mem: Double = 0.0
    var decAccu = 5
    var deg = false

    // STATUS VALUES
    var funcMode = false
    var consMode = false
    var prevForm = ""

    // LOCAL SETTINGS
    lateinit var file: File
    var ovrForm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        file = File(this.filesDir, "settings")
        if (file.exists()) {
            val settingsStr = file.readLines()
            if (settingsStr.size < 3) {
                file.writeText("deg\n5\nnotOvr")
            } else {
                deg = settingsStr[0] == "deg"
                decAccu = settingsStr[1].toInt()
                ovrForm = settingsStr[2] == "ovr"
            }
        } else {
            file.createNewFile()
            file.writeText("deg\n5\nnotOvr")
        }

        // TODO: Implement a FreeCalc logo on toolbar
        val actionBar = binding.toolbar
        // actionBar.setLogo(R.drawable.ic_toolbar_logo_xml)
        actionBar.setTitle(R.string.first_fragment_label)
        setSupportActionBar(actionBar)

        binding.eqForm.inputType = EditorInfo.TYPE_NULL

        val keyboard_buttons = Array<Button>(20) { MaterialButton(this) }
        val keyboard_buttonTexts = "()^%123+456-789*,0./"
        val funcMode_kbButtonTexts = arrayOf(
            "sin", "cos", "tan",
            "cot", "sqr", "abs",
            "flo", "log", "cei"
        )
        // binding.eqForm.inputType = EditorInfo.TYPE_NULL

        binding.calcButton.setOnClickListener(calcButtonListener())
        binding.keyboardButton.setOnClickListener(dedicatedKeyboardButtonListener())

        binding.kbMc.setOnClickListener(kbMCListener())
        binding.kbMp.setOnClickListener(kbMPlusListener())
        binding.kbMm.setOnClickListener(kbMMinusListener())
        binding.kbMr.setOnClickListener(kbMRListener())
        binding.kbC.setOnClickListener(kbCListener())
        binding.kbBack.setOnClickListener(kbBackListener())

        configureKbLower20Buttons(keyboard_buttons, keyboard_buttonTexts)

        binding.kbFunc.setOnClickListener(kbFuncButtonListener(keyboard_buttons, funcMode_kbButtonTexts, keyboard_buttonTexts))
        binding.kbConst.setOnClickListener(kbConsButtonListener(keyboard_buttons, keyboard_buttonTexts))

        binding.cursorLeft.setOnClickListener { _ ->
            val temp = binding.eqForm.selectionStart
            if (temp != 0) {
                val s = binding.eqForm.text.toString()
                binding.eqForm.setText(s.substring(0 until temp-1) + "_" + s[temp-1] + s.substring(temp+1))
                binding.eqForm.setSelection(temp - 1)
            }
        }
        binding.cursorRight.setOnClickListener { _ ->
            val temp = binding.eqForm.selectionStart
            if (temp != binding.eqForm.length()-1) {
                val s = binding.eqForm.text.toString()
                binding.eqForm.setText(s.substring(0 until temp) + s[temp+1] + "_" + s.substring(temp+2))
                binding.eqForm.setSelection(temp + 1)
            }
        }
    }

    // CALC FUNCS
    private fun isFunc(eq: String, i: Int): Boolean {
        val funcs = arrayOf( "sqr", "sin", "cos", "tan", "cot", "abs", "cei", "flo" )
        if (i >= eq.length) return false
        if (eq.substring(i).length < 3) return false
        return funcs.contains(eq.substring(i, i+3))
    }
    private fun priorTo(tk1: Char, tk2: Char): Int {
        val priority = mapOf(
            '+' to 1,
            '-' to 1,
            '%' to 2,
            '*' to 3,
            '/' to 3,
            '^' to 4
        )
        if (!priority.containsKey(tk1) || !priority.containsKey(tk2))
            return -2

        return if (priority[tk1]!! > priority[tk2]!!)
            1
        else if (priority[tk1]!! < priority[tk2]!!)
            -1
        else
            0
    }
    private fun isOp(op: String): Boolean {
        return "+-*/^".contains(op)
    }
    private fun isConst(eq: String, i: Int): Boolean {
        return "EPM".contains(eq[i])
    }
    private fun isSpecialFunc(eq: String, i: Int): Boolean {
        val funcs = arrayOf("log")
        if (i >= eq.length) return false
        if (eq.substring(i).length < 3) return false
        return funcs.contains(eq.substring(i, i+3))
    }

    private fun transformToRPN(eq: String): Stack<String> {
        val s1: Stack<String> = Stack()
        val s2: Stack<String> = Stack()
        var i = 0
        while (i < eq.length) {
            // Numbers
            if ((eq[i] in '0'..'9') ||
                (eq[i] == '-' && (i == 0 || !(eq[i - 1] in '0'..'9' || eq[i - 1] == ')')))) {
                var j = i+1
                while (j < eq.length && (eq[j] in '0'..'9' || eq[j] == '.')) { j++ }
                j--
                s2.push(eq.substring(i, j+1))
                i = j
            }
            // Constants
            else if (isConst(eq, i)) {
                s2.push(eq[i].toString())
            }
            // Functions
            else if (isFunc(eq, i)) {
                s1.push(eq.substring(i, i+3))
                i += 2
            }
            // Special functions
            else if (isSpecialFunc(eq, i)) {
                s1.push(eq.substring(i, i+3))
                i += 2
            }
            // Operators
            else if (isOp(eq[i].toString())) {
                // Higher priority than previous
                if (s1.isEmpty() || s1.peek() == "(" ||
                    (!isFunc(s1.peek(), 0) && !isSpecialFunc(s1.peek(), 0) &&
                            priorTo(eq[i], s1.peek()[0]) > 0)) {
                    s1.push(eq[i].toString())
                }
                // Lower or equal priority than previous
                else {
                    while (!s1.isEmpty() && (isFunc(s1.peek(), 0) || isSpecialFunc(s1.peek(), 0) ||
                                (s1.peek() != "(" && priorTo(s1.peek()[0], eq[i]) >= 0))) {
                        s2.push(s1.pop())
                    }
                    s1.push(eq[i].toString())
                }
            }
            // Brackets
            else if (eq[i] == '(') {
                s1.push("(")
            }
            else if (eq[i] == ')') {
                while (!s1.isEmpty() && s1.peek() != "(") {
                    s2.push(s1.pop())
                }
                s1.pop()
            }
            i++
        }
        while (!s1.isEmpty()) {
            s2.push(s1.pop())
        }
        return s2
    }
    private fun calc(eq: String): Double {
        // Transformation
        var s = transformToRPN(eq)
        val tokens = Array(s.count()) { "" }
        for (i in 0 until s.count()) {
            tokens[tokens.size - i - 1] = s.pop()
        }

        // Calculation
        s = Stack()
        tokens.forEach {
            // Numbers
            if (it[0] in '0'..'9' || it[0] == '-' && it.length > 1)
                s.push(it)
            // Operators
            else if (isOp(it)) {
                val op1 = s.pop()
                val op2 = s.pop()
                when (it) {
                    "+" -> s.push((op2.toDouble() + op1.toDouble()).toString())
                    "-" -> s.push((op2.toDouble() - op1.toDouble()).toString())
                    "%" -> s.push((op2.toDouble() % op1.toDouble()).toString())
                    "*" -> s.push((op2.toDouble() * op1.toDouble()).toString())
                    "/" -> s.push((op2.toDouble() / op1.toDouble()).toString())
                    "^" -> s.push((op2.toDouble().pow(op1.toInt())).toString())
                }
            }
            // Constants
            else if (isConst(it, 0)) {
                when (it) {
                    "E" -> s.push(E.toString())
                    "P" -> s.push(PI.toString())
                    "M" -> s.push(mem.toString())
                }
            }
            // Functions
            else if (isFunc(it, 0)) {
                val num = s.pop().toDouble()
                when (it) {
                    "sqr" -> s.push(sqrt(num).toString())
                    "tan" -> s.push(if (deg) {
                        tan(Math.toRadians(num)).toString()
                    } else {
                        tan(num).toString()
                    })
                    "sin" -> s.push(if (deg) {
                        sin(Math.toRadians(num)).toString()
                    } else {
                        sin(num).toString()
                    })
                    "cot" -> s.push(if (deg) {
                        (cos(Math.toRadians(num)) / sin(Math.toRadians(num))).toString()
                    } else {
                        (cos(num) / sin(num)).toString()
                    })
                    "cos" -> s.push(if (deg) {
                        cos(Math.toRadians(num)).toString()
                    } else {
                        cos(num).toString()
                    })
                    "abs" -> s.push(abs(num).toString())
                    "cei" -> s.push(if (num.toInt().toDouble() == num){
                        num.toString()
                    } else if (num > 0) {
                        (num.toInt() + 1).toString()
                    } else {
                        (num.toInt()).toString()
                    })
                    "flo" -> s.push(if (num.toInt().toDouble() == num){
                        num.toString()
                    } else if (num > 0) {
                        (num.toInt()).toString()
                    } else {
                        (num.toInt() - 1).toString()
                    })
                }
            }
            // Special functions
            else if (isSpecialFunc(it, 0)) {
                when (it) {
                    "log" -> {
                        val num1 = s.pop().toDouble()
                        val num2 = s.pop().toDouble()
                        s.push(log(num1, num2).toString())
                    }
                }
            }
        }
        // return (s.pop().toDouble() * 10.0.pow(decAccu)).roundToInt() / 10.0.pow(decAccu)
        return "%.${decAccu}f".format(s.pop().toDouble()).toDouble()
        // return s.pop().toDouble()
    }

    private fun calcButtonListener(): View.OnClickListener {
        return View.OnClickListener {
            var s = binding.eqForm.text.toString()
            setUndoState(true, binding.toolbar.menu.findItem(R.id.action_undo))
            var i = 0
            while (i < s.length) {
                if (s[i] == ' ' || s[i] == '_') {
                    s = s.removeRange(i..i)
                    i--
                }
                i++
            }
            prevForm = s + "_"
            tryCalculation(s)
            if (ovrForm) {
                binding.eqForm.setText(binding.resText.text.toString() + "_")
                binding.eqForm.setSelection(binding.eqForm.text!!.length-1)
            }
        }
    }

    private fun dedicatedKeyboardButtonListener(): View.OnClickListener {
        return View.OnClickListener {
            if (binding.keyboardButton.text == getText(R.string.show_dedicated_keyboard)) {
                binding.keyboardButton.text = getText(R.string.hide_dedicated_keyboard)
                binding.keyboardGrid.visibility = View.VISIBLE
            } else {
                binding.keyboardButton.text = getText(R.string.show_dedicated_keyboard)
                binding.keyboardGrid.visibility = View.GONE
            }
        }
    }
    private fun kbMCListener(): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            setM(0.0)
        }
    }
    private fun kbMPlusListener(): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            try {
                binding.calcButton.performClick()
                setM(mem + binding.resText.text.toString().toDouble())
            } catch (e: Exception) {
                showError(getText(R.string.invalid_expression).toString())
            }
        }
    }
    private fun kbMMinusListener(): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            try {
                binding.calcButton.performClick()
                setM(mem - binding.resText.text.toString().toDouble())
            } catch (e: Exception) {
                showError(getText(R.string.invalid_expression).toString())
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun kbMRListener(): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            val s = binding.eqForm.text.toString()
            val temp = binding.eqForm.selectionStart
            binding.eqForm.setText(s.substring(0 until binding.eqForm.selectionStart)
                    + "M"
                    + if (s.length == 0) { '_' } else { "" }
                    + s.substring(binding.eqForm.selectionEnd))
            binding.eqForm.setSelection(temp+1)
        }
    }
    private fun kbCListener(): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            prevForm = binding.eqForm.text.toString()
            setUndoState(true, binding.toolbar.menu.findItem(R.id.action_undo))
            binding.eqForm.setText("")
            binding.resText.text = ""
        }
    }
    @SuppressLint("SetTextI18n")
    private fun kbBackListener(): View.OnClickListener {
        return View.OnClickListener  {
            performHaptic(it)
            val s = binding.eqForm.text.toString()
            val temp = binding.eqForm.selectionStart
            if (temp != 0) {
                binding.eqForm.setText(
                    s.substring(0 until binding.eqForm.selectionStart - 1) + s.substring(
                        binding.eqForm.selectionEnd
                    )
                )
                binding.eqForm.setSelection(temp - 1)
            }
        }
    }
    private fun setKbButtonProperties(index: Int, kb: Button, keyboard_buttonTexts: String) {
        // val param = GridLayout.LayoutParams()
        // param.rowSpec = (binding.kbC.layoutParams as GridLayout.LayoutParams).rowSpec
        // param.columnSpec = (binding.kbC.layoutParams as GridLayout.LayoutParams).columnSpec
        // param.marginStart = binding.kbC.marginStart
        // kb.layoutParams = param
        kb.minHeight = binding.kbC.minHeight
        // kb.width = binding.kbC.minWidth
        kb.textSize = px2sp(binding.kbC.textSize)

        val param = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f))
        param.marginStart = binding.kbC.marginStart
        param.bottomMargin = (binding.kbC.layoutParams as GridLayout.LayoutParams).bottomMargin
        kb.layoutParams = param

        val keyboard_isOp = arrayOf(
            true, true, true, true,
            false, false, false, true,
            false, false, false, true,
            false, false, false, true,
            true, false, true, true
        )
        kb.text = when(index){
            3 -> "mod"
            else -> keyboard_buttonTexts[index].toString()
        }
        kb.setBackgroundColor(when (keyboard_isOp[index]) {
            true -> 285212842
            false -> 285239039
        })
    }

    private fun px2sp(px: Float): Float {
        return px / Resources.getSystem().displayMetrics.scaledDensity
    }

    @SuppressLint("SetTextI18n")
    private fun kbLower20ButtonClickListener(i: Int, kb: Button): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            val s = binding.eqForm.text.toString()
            var temp = binding.eqForm.selectionStart
            binding.eqForm.setText(s.substring(0 until temp) +
                    (when(i) {
                        3 ->  {
                            temp++
                            "%"
                        }
                        18 -> {
                            if (temp == 0 || s[temp-1] !in '0'..'9') {
                                temp += 2
                                "0."
                            } else {
                                temp++
                                "."
                            }
                        }
                        else -> {
                            temp += kb.text.length
                            kb.text.toString()
                        }})
                    + if (s.length == 0) { '_' } else { "" }
                    + s.substring(binding.eqForm.selectionEnd)
            )
            binding.eqForm.setSelection(temp)

            if (funcMode) {
                binding.kbFunc.performClick()
            } else if (consMode) {
                binding.kbConst.performClick()
            }
        }
    }
    private fun configureKbLower20Buttons(keyboard_buttons: Array<Button>, keyboard_buttonTexts: String) {
        for ((i, kb) in keyboard_buttons.withIndex()) {
            setKbButtonProperties(i, kb, keyboard_buttonTexts)
            kb.setOnClickListener(kbLower20ButtonClickListener(i, kb))
            binding.keyboardGrid.addView(kb)
        }
    }
    private fun setFuncMode(keyboard_buttons: Array<Button>, funcMode_kbButtonTexts: Array<String>) {
        funcMode = true
        binding.kbFunc.setBackgroundColor(Color.parseColor("#8800aa00"))
        if (consMode) {
            consMode = false
            binding.kbConst.setBackgroundColor(Color.parseColor("#1100aa00"))
        }
        var j = 0
        for ((i, kb) in keyboard_buttons.withIndex()) {
            if (i < 4 || i % 4 == 3 || i > 15) continue
            kb.text = funcMode_kbButtonTexts[j]
            j++
        }
    }
    private fun setConsMode(keyboard_buttons: Array<Button>) {
        consMode = true
        binding.kbConst.setBackgroundColor(Color.parseColor("#8800aa00"))
        if (funcMode) {
            funcMode = false
            binding.kbFunc.setBackgroundColor(Color.parseColor("#1100aa00"))
        }
        for ((i, kb) in keyboard_buttons.withIndex()) {
            if (i < 4 || i % 4 == 3 || i > 15) continue
            if (i == 4) {
                kb.text = "E"; continue }
            if (i == 5) {
                kb.text = "P"; continue }
            kb.text = ""
        }
    }
    private fun resetModes(keyboard_buttons: Array<Button>, keyboard_buttonTexts: String) {
        if (funcMode) {
            funcMode = false
            binding.kbFunc.setBackgroundColor(Color.parseColor("#1100aa00"))
            for ((i, kb) in keyboard_buttons.withIndex()) {
                if (i < 4 || i % 4 == 3 || i > 15) continue
                kb.text = keyboard_buttonTexts[i].toString()
            }
        } else {
            consMode = false
            binding.kbConst.setBackgroundColor(Color.parseColor("#1100aa00"))
            for ((i, kb) in keyboard_buttons.withIndex()) {
                if (i < 4 || i % 4 == 3 || i > 15) continue
                kb.text = keyboard_buttonTexts[i].toString()
            }
        }
    }
    private fun kbFuncButtonListener(keyboard_buttons: Array<Button>, funcMode_kbButtonTexts: Array<String>, keyboard_buttonTexts: String): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            if (!funcMode) {
                setFuncMode(keyboard_buttons, funcMode_kbButtonTexts)
            } else {
                resetModes(keyboard_buttons, keyboard_buttonTexts)
            }
        }
    }
    private fun kbConsButtonListener(keyboard_buttons: Array<Button>, keyboard_buttonTexts: String): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            if (!consMode) {
                setConsMode(keyboard_buttons)
            } else {
                resetModes(keyboard_buttons, keyboard_buttonTexts)
            }
        }
    }

    private fun tryCalculation(s: String) {
        try {
            binding.resText.text = calc(s).toString()
        } catch (e: Exception) {
            showError(getText(R.string.invalid_expression).toString())
        }
    }
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        binding.resText.text = message
    }
    private fun setM(m: Double) {
        mem = m
        binding.memText.text = "Mem: ${m}"
    }
    private fun performHaptic(view: View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
    private fun saveSettings() {
        file.writeText("%s\n%d\n%s".format(
            when (deg) {true -> "deg"
            false -> "rad"},
        decAccu,
        when (ovrForm) {
            true -> "ovr"
            false -> "notOvr"
        }))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when (deg) {
            true -> menu.findItem(R.id.action_deg_mode).isChecked = true
            false -> menu.findItem(R.id.action_rad_mode).isChecked = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_undo -> {
                setUndoState(false, item)
                binding.eqForm.setText(prevForm)
                if (prevForm.length > 1) {
                    binding.eqForm.setSelection(prevForm.length - 1)
                }
                true
            }
            R.id.action_deg_mode -> {
                item.isChecked = true
                deg = true
                binding.resText.text = getString(R.string.deg_item_return_msg)
                saveSettings()
                true
            }
            R.id.action_rad_mode -> {
                item.isChecked = true
                deg = false
                binding.resText.text = getString(R.string.rad_item_return_msg)
                saveSettings()
                true
            }
            R.id.action_accuracy_dialog -> {
                val customAlertDialogView = LayoutInflater.from(this).inflate(R.layout.fragment_accuracy_dialog, null, false)
                customAlertDialogView.findViewById<Slider>(R.id.accuracy_slider).value = decAccu.toFloat()
                MaterialAlertDialogBuilder(this)
                    .setView(customAlertDialogView)
                    .setIcon(R.drawable.ic_dec_accu_new)
                    .setTitle(getString(R.string.dec_accu_dialog_title))
                    .setPositiveButton(getString(R.string.dialog_button_ok)) { it, _ ->
                        decAccu = customAlertDialogView.findViewById<Slider>(R.id.accuracy_slider).value.toInt()
                        binding.resText.text = getString(R.string.dec_accu_dialog_save_return_msg).format(decAccu)
                        saveSettings()
                        Toast.makeText(this, getString(R.string.toast_msg_saved), Toast.LENGTH_SHORT).show()
                        it.dismiss()
                    }
                    .setNegativeButton(getString(R.string.dialog_button_cancel)) { it, _ ->
                        Toast.makeText(this, getString(R.string.toast_msg_cancelled), Toast.LENGTH_SHORT).show()
                        it.dismiss()
                    }
                    .setMessage(getString(R.string.dec_accu_dialog_msg).format(decAccu))
                    .show()
                true
            }
            R.id.action_more_dialog -> {
                val customAlertDialogView =
                    LayoutInflater.from(this).inflate(R.layout.fragment_more_options, null, false)
                customAlertDialogView.findViewById<SwitchMaterial>(R.id.ovr_switch).isChecked =
                    ovrForm
                MaterialAlertDialogBuilder(this)
                    .setView(customAlertDialogView)
                    .setIcon(R.drawable.ic_settings)
                    .setTitle(getString(R.string.more_options))
                    .setPositiveButton(getString(R.string.dialog_button_ok)) { it, _ ->
                        ovrForm = customAlertDialogView.findViewById<SwitchMaterial>(R.id.ovr_switch).isChecked
                        saveSettings()
                        Toast.makeText(
                            this,
                            getString(R.string.toast_msg_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        it.dismiss()
                    }
                    .show()
                true
            }
            R.id.action_about -> {
                val customAlertDialogView = LayoutInflater.from(this).inflate(R.layout.fragment_about_dialog, null, false)
                MaterialAlertDialogBuilder(this)
                    .setView(customAlertDialogView)
                    .setIcon(R.drawable.ic_about)
                    .setTitle(getString(R.string.about_freecalc))
                    .setMessage(getString(R.string.made_by_horace))
                    .setPositiveButton(getString(R.string.dialog_button_ok)) { it, _ ->
                        it.dismiss()
                    }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUndoState(setEnabled: Boolean, item: MenuItem) {
        if (setEnabled) {
            item.isEnabled = true
            item.icon = getDrawable(R.drawable.ic_undo)
        } else {
            item.isEnabled = false
            item.icon = getDrawable(R.drawable.ic_undo_disabled)
        }
    }
}
