package com.soapy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soapy.app.data.LockStore

private const val PIN_LENGTH = 4

private val SECURITY_QUESTIONS = listOf(
    "What was the name of your first school?",
    "What is your mother's maiden name?",
    "What was your childhood nickname?",
    "In what town were you born?",
    "What was the name of your first pet?"
)

enum class LockMode { SETUP, CHANGE, DISABLE, RECOVER }

private enum class Step { VERIFY_PIN, VERIFY_ANSWER, NEW_PIN, CONFIRM_PIN, QUESTION }

// ------------------------------------------------------------------ opening the app

/** Shown when the app opens with a lock set. */
@Composable
fun LockGate(store: LockStore, onUnlocked: () -> Unit) {
    var recovering by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    if (recovering) {
        BackHandler { recovering = false }
        LockFlowScreen(
            mode = LockMode.RECOVER,
            store = store,
            onFinished = onUnlocked,
            onCancel = { recovering = false }
        )
    } else {
        PinScreen(
            title = "Welcome back",
            subtitle = "Enter your PIN to open Soapy",
            error = error,
            resetKey = attempt,
            onComplete = { pin ->
                if (store.verifyPin(pin)) {
                    onUnlocked()
                } else {
                    error = "Wrong PIN. Try again."
                    attempt++
                }
            },
            footer = {
                TextButton(onClick = { recovering = true }) {
                    Text("Forgot PIN?", color = SoapyColors.Lavender, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// ------------------------------------------------------------------ set up, change, turn off, recover

@Composable
fun LockFlowScreen(
    mode: LockMode,
    store: LockStore,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    var step by remember {
        mutableStateOf(
            when (mode) {
                LockMode.SETUP -> Step.NEW_PIN
                LockMode.CHANGE -> Step.VERIFY_PIN
                LockMode.DISABLE -> Step.VERIFY_PIN
                LockMode.RECOVER -> Step.VERIFY_ANSWER
            }
        )
    }
    var firstPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    when (step) {
        Step.VERIFY_PIN -> PinScreen(
            title = "Enter your PIN",
            subtitle = if (mode == LockMode.DISABLE) {
                "Enter your PIN to turn off the lock"
            } else {
                "Confirm it's you before changing your PIN"
            },
            error = error,
            resetKey = step to attempt,
            onBack = onCancel,
            onComplete = { pin ->
                if (store.verifyPin(pin)) {
                    error = null
                    if (mode == LockMode.DISABLE) {
                        store.disable()
                        onFinished()
                    } else {
                        step = Step.NEW_PIN
                    }
                } else {
                    error = "Wrong PIN. Try again."
                    attempt++
                }
            }
        )

        Step.VERIFY_ANSWER -> AnswerForm(
            question = store.question().orEmpty(),
            onBack = onCancel,
            onVerify = { answer ->
                val ok = store.verifyAnswer(answer)
                if (ok) {
                    error = null
                    step = Step.NEW_PIN
                }
                ok
            }
        )

        Step.NEW_PIN -> PinScreen(
            title = if (mode == LockMode.SETUP) "Create a PIN" else "Choose a new PIN",
            subtitle = "Pick $PIN_LENGTH digits you will remember",
            error = error,
            resetKey = step to attempt,
            onBack = onCancel,
            onComplete = { pin ->
                firstPin = pin
                error = null
                step = Step.CONFIRM_PIN
            }
        )

        Step.CONFIRM_PIN -> PinScreen(
            title = "Confirm your PIN",
            subtitle = "Enter the same $PIN_LENGTH digits again",
            error = error,
            resetKey = step to attempt,
            onBack = {
                error = null
                step = Step.NEW_PIN
            },
            onComplete = { pin ->
                if (pin == firstPin) {
                    error = null
                    if (mode == LockMode.SETUP) {
                        step = Step.QUESTION
                    } else {
                        store.changePin(pin)
                        onFinished()
                    }
                } else {
                    error = "Those PINs didn't match. Try again."
                    attempt++
                    step = Step.NEW_PIN
                }
            }
        )

        Step.QUESTION -> QuestionForm(
            onBack = { step = Step.NEW_PIN },
            onSave = { question, answer ->
                store.enable(firstPin, question, answer)
                onFinished()
            }
        )
    }
}

// ------------------------------------------------------------------ forms

@Composable
private fun FormScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SoapyColors.Lavender
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 15.sp, color = SoapyColors.TextMuted)
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
private fun AnswerForm(
    question: String,
    onBack: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FormScaffold(
        title = "Reset your PIN",
        subtitle = "Answer your security question to continue",
        onBack = onBack
    ) {
        Text(question, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = SoapyColors.Lavender)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your answer") },
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(14.dp),
            supportingText = { error?.let { Text(it) } }
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = "Continue",
            onClick = {
                if (answer.isBlank() || !onVerify(answer)) {
                    error = "That answer doesn't match."
                }
            }
        )
    }
}

@Composable
private fun QuestionForm(
    onBack: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selected by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    FormScaffold(
        title = "Set a security question",
        subtitle = "If you forget your PIN, you will answer this to reset it. " +
            "Choose something only you know.",
        onBack = onBack
    ) {
        SECURITY_QUESTIONS.forEachIndexed { index, question ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selected = index }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == index, onClick = { selected = index })
                Text(question, fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = {
                answer = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your answer") },
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(14.dp),
            supportingText = { Text(error ?: "Not case sensitive.") }
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Save and turn on lock",
            onClick = {
                if (answer.trim().length < 3) {
                    error = "Use at least 3 characters."
                } else {
                    onSave(SECURITY_QUESTIONS[selected], answer)
                }
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ PIN pad

/**
 * A PIN screen with dots and a keypad. It calls [onComplete] once [PIN_LENGTH] digits are
 * entered. Change [resetKey] to clear the dots (after a wrong PIN, or when the step changes).
 */
@Composable
fun PinScreen(
    title: String,
    subtitle: String?,
    error: String?,
    resetKey: Any,
    onComplete: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    var pin by remember(resetKey) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SoapyColors.Lavender
                    )
                }
            }
        }

        Spacer(Modifier.weight(0.5f))

        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = SoapyColors.Primary,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                fontSize = 15.sp,
                color = SoapyColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(PIN_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (index < pin.length) SoapyColors.Primary else SoapyColors.Outline)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = error ?: "",
            fontSize = 13.sp,
            color = SoapyColors.Danger,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(20.dp)
        )

        Spacer(Modifier.weight(0.4f))

        Keypad(
            onDigit = { digit ->
                if (pin.length < PIN_LENGTH) {
                    pin += digit
                    if (pin.length == PIN_LENGTH) onComplete(pin)
                }
            },
            onBackspace = {
                if (pin.isNotEmpty()) pin = pin.dropLast(1)
            }
        )

        Spacer(Modifier.height(12.dp))
        if (footer != null) footer()
        Spacer(Modifier.height(24.dp))
    }
}

private val KEY_SIZE = 72.dp

@Composable
private fun Keypad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { digit ->
                    KeyButton(onClick = { onDigit(digit) }) {
                        Text(digit, fontSize = 26.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Spacer(Modifier.size(KEY_SIZE))
            KeyButton(onClick = { onDigit("0") }) {
                Text("0", fontSize = 26.sp)
            }
            KeyButton(onClick = onBackspace, background = Color.Transparent) {
                BackspaceIcon(SoapyColors.TextMuted)
            }
        }
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    background: Color = SoapyColors.SurfaceHigh,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(KEY_SIZE)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun BackspaceIcon(tint: Color) {
    Canvas(modifier = Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        val outline = Path().apply {
            moveTo(w * 0.32f, h * 0.2f)
            lineTo(w * 0.92f, h * 0.2f)
            lineTo(w * 0.92f, h * 0.8f)
            lineTo(w * 0.32f, h * 0.8f)
            lineTo(w * 0.06f, h * 0.5f)
            close()
        }
        drawPath(
            path = outline,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.38f),
            end = Offset(w * 0.74f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.74f, h * 0.38f),
            end = Offset(w * 0.5f, h * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
