package io.github.mobdev

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvExpression: TextView

    private var current = "0"
    private var previous = ""
    private var operator: String? = null
    private var resetOnNext = false

    private fun backspace() {
        if (resetOnNext || current == "Error") return
        current = if (current.length <= 1 || (current.length == 2 && current.startsWith("-"))) "0"
        else current.dropLast(1)
        tvResult.text = current
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvExpression = findViewById(R.id.tvExpression)

        val numIds = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )
        numIds.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { inputDigit(digit) }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener { inputDot() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clear() }
        findViewById<Button>(R.id.btnSign).setOnClickListener { toggleSign() }
        findViewById<Button>(R.id.btnPercent).setOnClickListener { percent() }
        findViewById<Button>(R.id.btnEq).setOnClickListener { equals() }
        findViewById<Button>(R.id.backspace).setOnClickListener { backspace() }

        mapOf(R.id.btnAdd to "+", R.id.btnSub to "−",
            R.id.btnMul to "×", R.id.btnDiv to "÷"
        ).forEach { (id, op) ->
            findViewById<Button>(id).setOnClickListener { setOperator(op) }
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putString("current", current)
        out.putString("previous", previous)
        out.putString("operator", operator)
        out.putBoolean("reset", resetOnNext)
    }

    override fun onRestoreInstanceState(saved: Bundle) {
        super.onRestoreInstanceState(saved)
        current = saved.getString("current", "0")!!
        previous = saved.getString("previous", "")!!
        operator = saved.getString("operator")
        resetOnNext = saved.getBoolean("reset", false)
        tvResult.text = current
        tvExpression.text = if (operator != null) "$previous $operator" else ""
    }

    private fun inputDigit(d: String) {
        if (resetOnNext) { current = d; resetOnNext = false }
        else current = if (current == "0") d else current + d
        if (current.length > 12) current = current.dropLast(1)
        tvResult.text = current
    }

    private fun inputDot() {
        if (resetOnNext) { current = "0."; resetOnNext = false; tvResult.text = current; return }
        if (!current.contains('.')) { current += "."; tvResult.text = current }
    }

    private fun setOperator(op: String) {
        if (operator != null && !resetOnNext) calculate()
        previous = current; operator = op; resetOnNext = true
        tvExpression.text = "$previous $op"
    }

    private fun calculate() {
        val a = previous.toDoubleOrNull() ?: return
        val b = current.toDoubleOrNull() ?: return
        val result = when (operator) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) null else a / b
            else -> return
        }
        tvExpression.text = "$previous $operator $current ="
        current = if (result == null) "Error" else fmt(result)
        tvResult.text = current
        operator = null; resetOnNext = true
    }

    private fun equals() = calculate()

    private fun clear() {
        current = "0"; previous = ""; operator = null; resetOnNext = false
        tvResult.text = "0"; tvExpression.text = ""
    }

    private fun toggleSign() {
        if (current == "0" || current == "Error") return
        current = if (current.startsWith("-")) current.drop(1) else "-$current"
        tvResult.text = current
    }

    private fun percent() {
        val v = current.toDoubleOrNull() ?: return
        current = fmt(v / 100)
        tvResult.text = current
    }

    private fun fmt(v: Double): String {
        val long = v.toLong()
        return if (v == long.toDouble()) long.toString() else "%.10g".format(v)
    }
}