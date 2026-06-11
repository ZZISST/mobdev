package io.github.mobdev

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF9500),
    onPrimary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE3E3E8),
    onSurfaceVariant = Color(0xFF1C1C1E),
    secondaryContainer = Color(0xFFD1D1D6),
    onSecondaryContainer = Color(0xFF1C1C1E),
    outline = Color(0xFF8E8E93)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF9F0A),
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color.White,
    secondaryContainer = Color(0xFF505054),
    onSecondaryContainer = Color.White,
    outline = Color(0xFF8E8E93)
)

@Composable
fun CalcTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val prefs = remember { ctx.getSharedPreferences("settings", Context.MODE_PRIVATE) }
            var themeMode by rememberSaveable {
                mutableStateOf(
                    ThemeMode.valueOf(
                        prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
                    )
                )
            }
            CalcTheme(themeMode) {
                CalculatorScreen(
                    themeMode = themeMode,
                    onThemeChange = { mode ->
                        themeMode = mode
                        prefs.edit { putString("theme", mode.name) }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    var current by rememberSaveable { mutableStateOf("0") }
    var previous by rememberSaveable { mutableStateOf("") }
    var operator by rememberSaveable { mutableStateOf("") }
    var expression by rememberSaveable { mutableStateOf("") }
    var resetOnNext by rememberSaveable { mutableStateOf(false) }
    var repeatOp by rememberSaveable { mutableStateOf("") }
    var repeatOperand by rememberSaveable { mutableStateOf("") }
    var sessionBase by rememberSaveable { mutableStateOf("") }
    var lastWasEquals by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val errorText = stringResource(R.string.calc_error)

    fun fmt(v: Double): String {
        val long = v.toLong()
        return if (v == long.toDouble()) long.toString() else "%.10g".format(v)
    }

    fun compute(a: Double, op: String, b: Double): Double? = when (op) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b == 0.0) null else a / b
        else -> null
    }

    fun calculate() {
        if (operator.isEmpty()) return
        val a = previous.toDoubleOrNull() ?: return
        val b = current.toDoubleOrNull() ?: return
        val op = operator
        val result = compute(a, op, b)
        expression = "$previous $op $current ="
        operator = ""
        resetOnNext = true
        if (result == null) {
            current = errorText
            sessionBase = ""
            lastWasEquals = false
        } else {
            repeatOp = op
            repeatOperand = current
            current = fmt(result)
        }
    }

    fun addHistoryEntry() {
        val entry = "$sessionBase = $current"
        history = if (lastWasEquals && history.isNotEmpty()) {
            listOf(entry) + history.drop(1)
        } else {
            listOf(entry) + history
        }
        lastWasEquals = true
    }

    fun equalsAction() {
        if (current == errorText) return
        if (operator.isNotEmpty()) {
            val operand = current
            calculate()
            if (operator.isNotEmpty() || current == errorText) return
            sessionBase = "$sessionBase $operand"
            addHistoryEntry()
        } else if (repeatOp.isNotEmpty()) {
            val a = current.toDoubleOrNull() ?: return
            val b = repeatOperand.toDoubleOrNull() ?: return
            val startVal = current
            val result = compute(a, repeatOp, b)
            expression = "$startVal $repeatOp $repeatOperand ="
            resetOnNext = true
            if (result == null) {
                current = errorText
                sessionBase = ""
                lastWasEquals = false
                return
            }
            current = fmt(result)
            sessionBase = if (sessionBase.isEmpty()) {
                "$startVal $repeatOp $repeatOperand"
            } else {
                "$sessionBase $repeatOp $repeatOperand"
            }
            addHistoryEntry()
        }
    }

    fun setOperator(op: String) {
        if (current == errorText) return
        if (operator.isNotEmpty() && resetOnNext) {
            operator = op
            sessionBase = sessionBase.substringBeforeLast(" ") + " $op"
            expression = "$previous $op"
            return
        }
        if (operator.isNotEmpty()) {
            val operand = current
            calculate()
            if (operator.isNotEmpty() || current == errorText) return
            sessionBase = "$sessionBase $operand $op"
        } else {
            lastWasEquals = false
            sessionBase = "$current $op"
        }
        previous = current
        operator = op
        resetOnNext = true
        expression = "$previous $op"
    }

    fun inputDigit(d: String) {
        if (resetOnNext) {
            if (lastWasEquals) {
                lastWasEquals = false
                sessionBase = ""
                expression = ""
            }
            current = d
            resetOnNext = false
        } else {
            current = if (current == "0") d else current + d
        }
        if (current.length > 12) current = current.dropLast(1)
    }

    fun inputDot() {
        if (resetOnNext) {
            if (lastWasEquals) {
                lastWasEquals = false
                sessionBase = ""
                expression = ""
            }
            current = "0."
            resetOnNext = false
            return
        }
        if (!current.contains('.')) current += "."
    }

    fun clear() {
        current = "0"
        previous = ""
        operator = ""
        expression = ""
        resetOnNext = false
        repeatOp = ""
        repeatOperand = ""
        sessionBase = ""
        lastWasEquals = false
    }

    fun backspace() {
        if (resetOnNext || current == errorText) return
        current = if (current.length <= 1 || (current.length == 2 && current.startsWith("-"))) "0"
        else current.dropLast(1)
    }

    fun toggleSign() {
        if (current == "0" || current == errorText) return
        current = if (current.startsWith("-")) current.drop(1) else "-$current"
    }

    fun percent() {
        val v = current.toDoubleOrNull() ?: return
        current = fmt(v / 100)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { showHistory = true }) {
                    Text("🕓", fontSize = 20.sp)
                }
                IconButton(onClick = { showSettings = true }) {
                    Text("⚙", fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = expression,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 18.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = current,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(20.dp))

            val rows = listOf(
                listOf("⌫", "C", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("+/−", "0", ".", "=")
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { label ->
                            CalcButton(
                                label = label,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    when (label) {
                                        "⌫" -> backspace()
                                        "C" -> clear()
                                        "%" -> percent()
                                        "+/−" -> toggleSign()
                                        "." -> inputDot()
                                        "=" -> equalsAction()
                                        "+", "−", "×", "÷" -> setOperator(label)
                                        else -> inputDigit(label)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(stringResource(R.string.settings_title)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeChange(mode) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { onThemeChange(mode) }
                            )
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    }
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showHistory) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.history_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { history = emptyList() }) {
                            Text(stringResource(R.string.history_clear))
                        }
                    }
                }
                if (history.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.history_empty),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(history) { entry ->
                            val idx = entry.lastIndexOf(" = ")
                            val expr = if (idx >= 0) entry.substring(0, idx) else entry
                            val res = if (idx >= 0) entry.substring(idx + 3) else ""
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        current = res
                                        resetOnNext = true
                                        lastWasEquals = false
                                        sessionBase = ""
                                        showHistory = false
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = expr,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = res,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalcButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val colors = when (label) {
        "÷", "×", "−", "+", "=" -> scheme.primary to scheme.onPrimary
        "⌫", "C", "%" -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    Button(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.4f),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.first,
            contentColor = colors.second
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
