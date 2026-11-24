package com.multmaster.app.ui.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multmaster.app.MultiplicationApp
import com.multmaster.app.ui.*
import com.multmaster.app.util.TimeUtils

@Composable
fun PracticeSessionScreen(onExit: () -> Unit, vm: PracticeViewModel = viewModel(factory = PracticeViewModel.Factory)) {
    val t = LocalStrings.current
    val state by vm.state.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as MultiplicationApp
    val settings by app.container.settings.current.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        app.container.pendingConfig?.let {
            app.container.pendingConfig = null
            vm.start(it)
        }
    }

    LaunchedEffect(state.feedback) {
        state.feedback?.let { f ->
            if (settings.sound) app.container.sound.beep(f.correct)
            if (settings.haptics) haptic.performHapticFeedback(
                if (f.correct) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
    }

    if (!state.started) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (state.finished) {
        ResultScreen(state, t, onExit, onPracticeAgain = onExit)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("${state.index} / ${state.total}", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = { vm.finishSession(); onExit() }) {
                        Icon(Icons.Filled.Close, contentDescription = t["close"])
                    }
                },
                actions = {
                    state.remaining?.let { r ->
                        Text("$r s", modifier = Modifier.padding(end = 16.dp),
                            color = if (r <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressBar(state.index.toFloat() / state.total.coerceAtLeast(1))

            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        state.question?.prompt ?: "",
                        fontSize = 34.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Text(
                if (state.input.isEmpty()) "—" else state.input,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(t["your_answer"], style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

            state.feedback?.let { f ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (f.correct) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (f.correct) t["correct"] else t["correct_answer"] + ": ${f.answer}",
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(f.explanation, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) {
                            Text(t["next_question"])
                        }
                    }
                }
            }

            if (state.feedback == null) {
                NumberPad(
                    onDigit = vm::onDigit,
                    onBackspace = vm::onBackspace,
                    onClear = vm::onClear,
                    enabled = !state.paused
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = vm::skip,
                        modifier = Modifier.weight(1f)
                    ) { Text(t["skip"]) }
                    Button(
                        onClick = vm::submit,
                        modifier = Modifier.weight(1.4f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(t["submit"]) }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(state: PracticeUiState, t: Strings, onExit: () -> Unit, onPracticeAgain: () -> Unit) {
    val total = state.results.size
    val correct = state.results.count { it.isCorrect }
    val accuracy = if (total == 0) 0 else correct * 100 / total
    val avgMs = if (total == 0) 0L else state.results.sumOf { it.ms } / total

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(t["session_complete"], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t["score"]); Text("$correct / $total", fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t["accuracy"]); Text("$accuracy%", fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t["average_time"]); Text(TimeUtils.formatMs(avgMs))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t["fastest_answer"]); Text(TimeUtils.formatMs(state.fastestMs))
                }
            }
        }
        if (state.results.any { !it.isCorrect }) {
            SectionTitle(t["review_mistakes"])
            state.results.filter { !it.isCorrect }.take(5).forEach {
                Text("${it.question.prompt}  ${it.question.explanation}", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onPracticeAgain, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)) { Text(t["practice_again"]) }
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)) { Text(t["back_to_home"]) }
    }
}