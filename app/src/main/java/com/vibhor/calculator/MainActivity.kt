package com.vibhor.calculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vibhor.calculator.databinding.ActivityMainBinding
import java.util.ArrayDeque
import kotlin.math.floor

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private val exprBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        fun put(s: String) {
            exprBuilder.append(s)
            b.tvExpression.text = exprBuilder.toString()
        }

        fun evaluateAndShow() {
            val expr = exprBuilder.toString()
            if (expr.isBlank()) {
                b.tvResult.text = "0"
                return
            }
            try {
                val res = ExpressionEvaluator.evaluate(expr)
                val text = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                b.tvResult.text = text
            } catch (e: Exception) {
                b.tvResult.text = "Error"
            }
        }

        // Numbers
        val numButtons = listOf(
            b.btn0 to "0", b.btn1 to "1", b.btn2 to "2", b.btn3 to "3",
            b.btn4 to "4", b.btn5 to "5", b.btn6 to "6", b.btn7 to "7", b.btn8 to "8", b.btn9 to "9",
            b.btnDot to ".", b.btnParenL to "("
        )
        for ((btn, s) in numButtons) btn.setOnClickListener { put(s) }

        // Operators
        b.btnPlus.setOnClickListener { put("+") }
        b.btnMinus.setOnClickListener { put("-") }
        b.btnMul.setOnClickListener { put("*") }
        b.btnDiv.setOnClickListener { put("/") }
        b.btnPercent.setOnClickListener { put("%") }

        b.btnClear.setOnClickListener {
            exprBuilder.clear()
            b.tvExpression.text = ""
            b.tvResult.text = "0"
        }
        b.btnDel.setOnClickListener {
            if (exprBuilder.isNotEmpty()) {
                exprBuilder.deleteAt(exprBuilder.lastIndex)
                b.tvExpression.text = exprBuilder.toString()
            }
        }
        b.btnEq.setOnClickListener {
            evaluateAndShow()
        }
    }
}

/**
 * Simple expression evaluator supporting +,-,*,/,%, parentheses, and decimals.
 * Percent is treated like (x % y) modulo for integers and remainder for decimals.
 */
object ExpressionEvaluator {

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/', '%' -> 2
        else -> 0
    }

    private fun applyOp(a: Double, b: Double, op: Char): Double = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> {
            if (b == 0.0) throw ArithmeticException("Division by zero")
            a / b
        }
        '%' -> {
            if (b == 0.0) throw ArithmeticException("Division by zero")
            a % b
        }
        else -> 0.0
    }

    fun evaluate(expression: String): Double {
        val ops = ArrayDeque<Char>()
        val vals = ArrayDeque<Double>()

        var i = 0
        fun pushNumber(start: Int, end: Int) {
            if (end <= start) return
            val num = expression.substring(start, end)
            vals.addLast(num.toDouble())
        }

        while (i < expression.length) {
            val c = expression[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    i++
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.')) i++
                    pushNumber(start, i)
                }
                c == '(' -> { ops.addLast(c); i++ }
                c == ')' -> {
                    while (ops.isNotEmpty() && ops.last() != '(') {
                        val op = ops.removeLast()
                        val b = vals.removeLast()
                        val a = vals.removeLast()
                        vals.addLast(applyOp(a, b, op))
                    }
                    if (ops.isNotEmpty() && ops.last() == '(') ops.removeLast()
                    i++
                }
                c in charArrayOf('+', '-', '*', '/', '%') -> {
                    while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(c)) {
                        val op = ops.removeLast()
                        val b = vals.removeLast()
                        val a = vals.removeLast()
                        vals.addLast(applyOp(a, b, op))
                    }
                    ops.addLast(c)
                    i++
                }
                else -> throw IllegalArgumentException("Bad char: $c")
            }
        }
        while (ops.isNotEmpty()) {
            val op = ops.removeLast()
            val b = vals.removeLast()
            val a = vals.removeLast()
            vals.addLast(applyOp(a, b, op))
        }
        return vals.last()
    }
}
