package com.example.freecalc_material3test

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.ComponentDialog
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import com.example.freecalc_material3test.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import kotlin.math.*


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // CALC VALUES
    var mem: Double = 0.0
    var decAccu = 5
    var deg = false

    // STATUS VALUES
    var funcMode = false
    var consMode = false


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // if (intent.hasExtra("settingBundle")) {
        //     val deg = intent?.extras?.getBundle("settingsBundle")?.getBoolean("degMode")!!
        //     val decAccu = intent?.extras?.getBundle("settingsBundle")?.getInt("decAccu")!!
        // }

        // val navController = findNavController(R.id.nav_host_fragment_content_main)
        // appBarConfiguration = AppBarConfiguration(navController.graph)
        // setupActionBarWithNavController(navController, appBarConfiguration)

        // TODO: Request file permissions and read / write settings
        // while (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        //     != PackageManager.PERMISSION_GRANTED )
        // {
        //     ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE), 101)
        // }

        val keyboard_buttons = Array<Button>(20) { MaterialButton(this) }
        val keyboard_buttonTexts = "()^%123+456-789*,0./"
        val funcMode_kbButtonTexts = arrayOf(
            "sin", "cos", "tan",
            "cot", "sqr", "abs",
            "flo", "log", "cei"
        )

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
    }

    override fun onStart() {
        super.onStart()

        // val file = File("/settings.txt")
        // if (!file.exists()) {
        //     file.createNewFile()
        // }
        // var settings = file.readLines()
        // if (settings.isEmpty()) {
        //     writeLocal(file)
        // } else {
        //     deg = settings[0].toBoolean()
        //     decAccu = settings[1].toInt()
        //     when (deg) {
        //         true -> binding.toolbar[0].isSelected = true
        //         false -> binding.toolbar[1].isSelected = true
        //     }
        // }
        // binding.toolbar[2].setOnClickListener({
        //     showError("aaaaa")
        // })
    }

    // override fun onRequestPermissionsResult(requestCode: Int,
    //                                         permissions: Array<String>,
    //                                         grantResults: IntArray) {
    //     super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    //     if (requestCode == 100) {
    //         if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    //             Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
    //         } else {
    //             Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
    //         }
    //     }
    // }
//
    // private fun writeLocal(file: File) {
    //     file.writeText("%s\n%d".format(deg, decAccu))
    // }

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
            var i = 0
            while (i < s.length) {
                if (s[i] == ' ') {
                    s = s.removeRange(i, i)
                    i--
                }
                i++
            }
            tryCalculation(s)
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
    private fun kbMRListener(): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            val s = binding.eqForm.text.toString()
            val temp = binding.eqForm.selectionStart
            binding.eqForm.setText(s.substring(0 until binding.eqForm.selectionStart) + "M" + s.substring(binding.eqForm.selectionEnd))
            binding.eqForm.setSelection(temp+1)
        }
    }
    private fun kbCListener(): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            binding.eqForm.setText("")
            binding.resText.text = ""
        }
    }
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
        val param = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f))
        param.marginStart = dp2px(5)
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
        kb.textSize = 20.0f
    }

    private fun dp2px(dp: Int): Int {
        return (Resources.getSystem().displayMetrics.density * dp + 0.5f).toInt()
    }

    private fun kbLower20ButtonClickListener(i: Int, kb: Button): View.OnClickListener {
        return View.OnClickListener {
            performHaptic(it)
            val s = binding.eqForm.text.toString()
            val temp = binding.eqForm.selectionStart
            binding.eqForm.setText(s.substring(0 until binding.eqForm.selectionStart) +
                    (when(i) {
                        3 -> "%"
                        else -> kb.text.toString()})
                    + s.substring(binding.eqForm.selectionEnd)
            )
            if (i == 3) binding.eqForm.setSelection(temp+1)
            else binding.eqForm.setSelection(temp+kb.text.length)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // TODO: implement action bar menu actions
        return when (item.itemId) {
            R.id.action_deg_mode -> {
                deg = true
                true
            }
            R.id.action_rad_mode -> {
                deg = false
                true
            }
            R.id.action_accuracy_dialog -> {
                // val dialog = AlertDialog.Builder(this)
                // dialog.setTitle("Decimal Accuracy")
                // dialog.setPositiveButton("OK") { it, _ -> it.dismiss() }
                // dialog.setCancelable(true)
                // dialog.setNegativeButton("Cancel") { it, _ -> it.dismiss()}
                // dialog.show()
                MaterialAlertDialogBuilder(this)
                    .setTitle("Decimal Accuracy")
                    .setPositiveButton("OK") { it, _ -> it.dismiss() }
                    .setNegativeButton("Cancel") { it, _ -> it.dismiss()}
                    .setMessage("但是这里怎么加滑动条这种控件？？？")
                    .show()
                true
            }
            R.id.action_about -> {
                val intent = Intent(this, SettingsActivity::class.java)
                this.startActivity(intent)
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
/*
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
 */
}
