package com.example.freecalc_material3test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.freecalc_material3test.databinding.FragmentFirstBinding
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

    // CALC FUNCS
    private fun isFunc(eq: String, i: Int): Boolean {
        val funcs = arrayOf( "sqr", "sin", "cos", "tan", "cot", "abs", "cei", "flo" )
        if (i >= eq.length) return false
        if (eq.substring(i).length < 3) return false
        return funcs.contains(eq.substring(i, i+3))
    }

    private fun priorTo(tk1: Char, tk2: Char): Int {
        val priority = mapOf<Char, Int>(
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
        if (i >= eq.length) return false;
        if (eq.substring(i).length < 3) return false;
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
        for (token in tokens) {
            // Numbers
            if (token[0] in '0'..'9' || token[0] == '-' && token.length > 1)
                s.push(token)
            // Operators
            else if (isOp(token)) {
                val op1 = s.pop()
                val op2 = s.pop()
                when (token) {
                    "+" -> s.push((op2.toDouble() + op1.toDouble()).toString())
                    "-" -> s.push((op2.toDouble() - op1.toDouble()).toString())
                    "%" -> s.push((op2.toDouble() % op1.toDouble()).toString())
                    "*" -> s.push((op2.toDouble() * op1.toDouble()).toString())
                    "/" -> s.push((op2.toDouble() / op1.toDouble()).toString())
                    "^" -> s.push((op2.toDouble().pow(op1.toInt())).toString())
                }
            }
            // Constants
            else if (isConst(token, 0)) {
                when (token) {
                    "E" -> s.push(E.toString())
                    "P" -> s.push(PI.toString())
                    "M" -> s.push(mem.toString())
                }
            }
            // Functions
            else if (isFunc(token, 0)) {
                val num = s.pop().toDouble()
                when (token) {
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
            else if (isSpecialFunc(token, 0)) {
                when (token) {
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
        binding.calcButton.setOnClickListener {
            try {
                var s = binding.eqForm.text.toString()
                var i = 0
                while (i < s.length) {
                    if (s[i] == ' ') {
                        s = s.removeRange(i, i)
                        i--
                    }
                    i++
                }
                binding.resText.text = calc(s).toString()
            } catch (e: Exception) {
                Toast.makeText(context, "Invalid expression.", Toast.LENGTH_SHORT).show()
                binding.resText.text = "Invalid expression."
            }
        }
        binding.accuracySlider.addOnChangeListener { slider, _, _ ->
            decAccu = slider.value.toInt()
            binding.sliderDesc.text = "Accuracy: $decAccu"
        }
        binding.modeSelect.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                true -> {
                    deg = true
                    binding.modeSelect.text = "Mode: DEG"
                }
                false -> {
                    deg = false
                    binding.modeSelect.text = "Mode: RAD"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}