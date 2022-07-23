package com.example.freecalc_material3test

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.freecalc_material3test.databinding.FragmentFirstBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import java.util.*
import kotlin.math.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    // CALC VALUES
    var decAccu = 5
    var mem: Double = 0.0
    var deg = false

    // STATUS VALUES
    var funcMode = false
    var consMode = false

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
                                (cos(Math.toRadians(num))/sin(Math.toRadians(num))).toString()
                            } else {
                                (cos(num)/sin(num)).toString()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sliderDesc.text = "%s%d".format(getText(R.string.accuracy), decAccu)
        val keyboard_buttons = Array<Button>(20) { MaterialButton(requireContext()) }

        binding.accuracySlider.addOnChangeListener(accuracySliderListener())
        binding.modeSelect.setOnCheckedChangeListener(modeSelectSwitchListener())
        binding.calcButton.setOnClickListener(calcButtonListener())
        binding.keyboardButton.setOnClickListener(dedicatedKeyboardButtonListener())

        // Keyboard upper 8 buttons
        binding.kbMc.setOnClickListener {
            performHaptic(it)
            setM(0.0)
        }
        binding.kbMp.setOnClickListener {
            performHaptic(it)
            try {
                binding.calcButton.performClick()
                setM(mem + binding.resText.text.toString().toDouble())
            } catch (e: Exception) {
                Toast.makeText(context, getText(R.string.invalid_expression), Toast.LENGTH_SHORT).show()
                binding.resText.text = getText(R.string.invalid_expression)
            }
        }
        binding.kbMm.setOnClickListener {
            performHaptic(it)
            try {
                binding.calcButton.performClick()
                setM(mem - binding.resText.text.toString().toDouble())
            } catch (e: Exception) {
                Toast.makeText(context, getText(R.string.invalid_expression), Toast.LENGTH_SHORT).show()
                binding.resText.text = getText(R.string.invalid_expression)
            }
        }
        binding.kbMr.setOnClickListener {
            performHaptic(it)
            val s = binding.eqForm.text.toString()
            val temp = binding.eqForm.selectionStart
            binding.eqForm.setText(s.substring(0 until binding.eqForm.selectionStart) + "M" + s.substring(binding.eqForm.selectionEnd))
            binding.eqForm.setSelection(temp+1)
        }
        // func & cons listeners are after lower 20 buttons' code
        binding.kbC.setOnClickListener {
            performHaptic(it)
            binding.eqForm.setText("")
            binding.resText.text = ""
        }
        binding.kbBack.setOnClickListener {
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

        // Keyboard lower 20 buttons
        val keyboard_buttonTexts = "()^%123+456-789*,0./"
        val keyboard_isOp = arrayOf(
            true, true, true, true,
            false, false, false, true,
            false, false, false, true,
            false, false, false, true,
            true, false, true, true
        )
        for ((i, kb) in keyboard_buttons.withIndex()) {
            kb.text = when(i){
                3 -> "mod"
                else -> keyboard_buttonTexts[i].toString()
            }
            kb.setBackgroundColor(when (keyboard_isOp[i]) {
                true -> 285212842
                false -> 285239039
            })
            kb.textSize = 20.0f
            kb.setOnClickListener {
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
            binding.keyboardGrid.addView(kb)
        }

        // func
        val funcMode_kbButtonTexts = arrayOf(
            "sin", "cos", "tan",
            "cot", "sqr", "abs",
            "flo", "log", "cei"
        )
        binding.kbFunc.setOnClickListener {
            performHaptic(it)
            if (!funcMode) {
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
            } else {
                funcMode = false
                binding.kbFunc.setBackgroundColor(Color.parseColor("#1100aa00"))
                for ((i, kb) in keyboard_buttons.withIndex()) {
                    if (i < 4 || i % 4 == 3 || i > 15) continue
                    kb.text = keyboard_buttonTexts[i].toString()
                }
            }
        }

        // const
        binding.kbConst.setOnClickListener {
            performHaptic(it)
            if (!consMode) {
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
            } else {
                consMode = false
                binding.kbConst.setBackgroundColor(Color.parseColor("#1100aa00"))
                for ((i, kb) in keyboard_buttons.withIndex()) {
                    if (i < 4 || i % 4 == 3 || i > 15) continue
                    kb.text = keyboard_buttonTexts[i].toString()
                }
            }
        }
    }

    private fun performHaptic(view: View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun dedicatedKeyboardButtonListener(): View.OnClickListener {
        return View.OnClickListener {
            if (binding.keyboardButton.text == getText(R.string.show_dedicated_keyboard)) {
                binding.keyboardButton.text = getText(R.string.hide_dedicated_keyboard)
                binding.keyboardGrid.alpha = 1.0f
            } else {
                binding.keyboardButton.text = getText(R.string.show_dedicated_keyboard)
                binding.keyboardGrid.alpha = 0.0f
            }
        }
    }

    private fun modeSelectSwitchListener(): CompoundButton.OnCheckedChangeListener {
        return CompoundButton.OnCheckedChangeListener{ _, checkedId ->
            when (checkedId) {
                true -> {
                    deg = true
                    binding.modeSelect.text = getText(R.string.mode_deg)
                }
                false -> {
                    deg = false
                    binding.modeSelect.text = getText(R.string.mode_rad)
                }
            }
        }
    }

    private fun accuracySliderListener(): Slider.OnChangeListener {
        return Slider.OnChangeListener { slider, _, _ ->
            decAccu = slider.value.toInt()
            binding.sliderDesc.text = "%s%d".format(getText(R.string.accuracy), decAccu)
        }
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

    private fun tryCalculation(s: String) {
        try {
            binding.resText.text = calc(s).toString()
        } catch (e: Exception) {
            showError(getText(R.string.invalid_expression).toString())
        }
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        binding.resText.text = message
    }

    private fun setM(m: Double) {
        mem = m
        binding.memText.text = "Mem: ${m}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
