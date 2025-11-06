package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow
import java.util.ArrayDeque

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    CalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    fun append(token: String) {
        if (result.isNotEmpty() && token.firstOrNull()?.isDigit() == true) {
            // Start new expression after showing result if a number key is pressed
            expression = ""
            result = ""
        }
        expression += token
    }

    fun clearAll() {
        expression = ""
        result = ""
    }

    fun backspace() {
        if (expression.isNotEmpty()) expression = expression.dropLast(1)
    }

    fun evaluate() {
        try {
            val value = ExpressionEvaluator.evaluate(expression)
            result = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        } catch (e: Exception) {
            result = "Error"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Display
        Text(
            text = expression.ifEmpty { "0" },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            fontSize = 36.sp,
            lineHeight = 42.sp,
            textAlign = TextAlign.End
        )
        Text(
            text = result,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )

        // Buttons grid
        val rows = listOf(
            listOf("C", "(", ")", "⌫"),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf("%", "0", ".", "+"),
            listOf("^", "=",)
        )

        fun onKey(k: String) {
            when (k) {
                "C" -> clearAll()
                "⌫" -> backspace()
                "=" -> evaluate()
                "×" -> append("*")
                "÷" -> append("/")
                else -> append(k)
            }
        }

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    CalculatorButton(
                        text = key,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        onClick = { onKey(key) },
                        emphasis = key in listOf("=", "+", "-", "×", "÷")
                    )
                }
                if (row.size == 2) {
                    // Make "=" row stretch properly (two wide buttons)
                    Spacer(modifier = Modifier.weight(2f))
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    emphasis: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                color = if (emphasis) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * Simple expression evaluator supporting +, -, *, /, %, ^, parentheses, and decimals.
 * Uses shunting-yard algorithm to convert to RPN then evaluates.
 */
object ExpressionEvaluator {
    private val ops = mapOf(
        '+' to 1, '-' to 1,
        '*' to 2, '/' to 2, '%' to 2,
        '^' to 3
    )

    fun evaluate(expr: String): Double {
        val tokens = tokenize(expr)
        val rpn = toRpn(tokens)
        return evalRpn(rpn)
    }

    private fun tokenize(s: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    i++
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    out += s.substring(start, i)
                }
                c in "+-*/%^()".toCharArray().toSet() -> {
                    // Handle unary minus
                    if (c == '-' && (out.isEmpty() || out.last() in listOf("+","-","*","/","%","^","("))) {
                        // negative number begins
                        var j = i + 1
                        if (j < s.length && (s[j].isDigit() || s[j] == '.')) {
                            // consume number
                            val start = j
                            j++
                            while (j < s.length && (s[j].isDigit() || s[j] == '.')) j++
                            out += "-" + s.substring(start, j)
                            i = j
                            continue
                        }
                    }
                    out += c.toString()
                    i++
                }
                else -> throw IllegalArgumentException("Bad token: $c")
            }
        }
        return out
    }

    private fun toRpn(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val stack = ArrayDeque<Char>()
        for (t in tokens) {
            when {
                t.toDoubleOrNull() != null -> output += t
                t.length == 1 && t[0] in ops.keys -> {
                    val o1 = t[0]
                    while (stack.isNotEmpty()) {
                        val o2 = stack.peek()
                        if (o2 in ops.keys) {
                            val p1 = ops[o1]!!
                            val p2 = ops[o2]!!
                            val leftAssoc = o1 != '^'
                            if ((leftAssoc && p1 <= p2) || (!leftAssoc && p1 < p2)) {
                                output += stack.pop().toString()
                                continue
                            }
                        }
                        break
                    }
                    stack.push(o1)
                }
                t == "(" -> stack.push('(')
                t == ")" -> {
                    while (stack.isNotEmpty() && stack.peek() != '(') {
                        output += stack.pop().toString()
                    }
                    if (stack.isEmpty() || stack.pop() != '(') {
                        throw IllegalArgumentException("Mismatched parentheses")
                    }
                }
                else -> throw IllegalArgumentException("Bad token in RPN: $t")
            }
        }
        while (stack.isNotEmpty()) {
            val op = stack.pop()
            if (op == '(') throw IllegalArgumentException("Mismatched parentheses")
            output += op.toString()
        }
        return output
    }

    private fun evalRpn(rpn: List<String>): Double {
        val stack = ArrayDeque<Double>()
        for (t in rpn) {
            val num = t.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
            } else if (t.length == 1 && t[0] in ops.keys) {
                val b = stack.pop()
                val a = stack.pop()
                val v = when (t[0]) {
                    '+' -> a + b
                    '-' -> a - b
                    '*' -> a * b
                    '/' -> a / b
                    '%' -> a % b
                    '^' -> a.pow(b)
                    else -> throw IllegalArgumentException("Unknown op")
                }
                stack.push(v)
            } else {
                throw IllegalArgumentException("Bad RPN token: $t")
            }
        }
        return stack.pop()
    }
}