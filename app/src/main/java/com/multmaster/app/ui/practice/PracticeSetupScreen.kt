package com.multmaster.app.ui.practice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.multmaster.app.MultiplicationApp
import com.multmaster.app.domain.PracticeConfig
import com.multmaster.app.domain.PracticeMode
import com.multmaster.app.ui.LocalStrings
import com.multmaster.app.ui.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSetupScreen(nav: NavHostController) {
    val t = LocalStrings.current
    val app = LocalContext.current.applicationContext as MultiplicationApp
    val settings by app.container.settings.current.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(PracticeMode.MIXED) }
    var selectedTables by remember { mutableStateOf((1..50).toSet()) }
    var maxMultiplier by remember { mutableFloatStateOf(20f) }
    var count by remember { mutableFloatStateOf(20f) }
    var timerSeconds by remember { mutableStateOf<Int?>(if (settings.timer) 120 else null) }
    var difficulty by remember { mutableStateOf(settings.difficulty) }

    val modes = listOf(
        PracticeMode.SEQUENTIAL to t["mode_sequential_desc"],
        PracticeMode.RANDOM to t["mode_random_desc"],
        PracticeMode.MIXED to t["mode_mixed_desc"],
        PracticeMode.REVERSE to t["mode_reverse_desc"],
        PracticeMode.MISSING to t["mode_missing_desc"],
        PracticeMode.SPEED to t["mode_speed_desc"]
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t["practice"], fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t["previous"])
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle(t["select_mode"])
            modes.forEach { (m, desc) ->
                Card(
                    onClick = { mode = m; if (m == PracticeMode.SPEED) timerSeconds = 60 },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (mode == m) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(t["mode_" + m.name.lowercase()], fontWeight = FontWeight.SemiBold)
                        Text(desc, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            SectionTitle(t["select_tables"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selectedTables = (1..50).toSet() }) { Text(t["all"]) }
                OutlinedButton(onClick = { selectedTables = (1..10).toSet() }) { Text("1–10") }
                OutlinedButton(onClick = { selectedTables = (11..25).toSet() }) { Text("11–25") }
                OutlinedButton(onClick = { selectedTables = (26..50).toSet() }) { Text("26–50") }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items((1..50).toList()) { n ->
                    FilterChip(
                        selected = selectedTables.contains(n),
                        onClick = {
                            selectedTables = if (selectedTables.contains(n)) selectedTables - n else selectedTables + n
                        },
                        label = { Text("$n") }
                    )
                }
            }

            Text("${t["select_multipliers"]}: ${maxMultiplier.toInt()}", fontWeight = FontWeight.SemiBold)
            Slider(value = maxMultiplier, onValueChange = { maxMultiplier = it },
                valueRange = 1f..20f, steps = 18)

            Text("${t["question_count"]}: ${count.toInt()}", fontWeight = FontWeight.SemiBold)
            Slider(value = count, onValueChange = { count = it }, valueRange = 5f..100f, steps = 18)

            Text("${t["timer"]}: ${timerSeconds?.let { "$it s" } ?: t["timer_off"]}", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 120, 300).forEach { sec ->
                    FilterChip(selected = timerSeconds == sec, onClick = { timerSeconds = sec },
                        label = { Text("$sec") })
                }
                FilterChip(selected = timerSeconds == null, onClick = { timerSeconds = null },
                    label = { Text(t["timer_off"]) })
            }

            Text(t["difficulty"], fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("easy", "normal", "hard").forEach { d ->
                    FilterChip(selected = difficulty == d, onClick = { difficulty = d },
                        label = { Text(t[d]) })
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val tables = selectedTables.toList().sorted().ifEmpty { (1..50).toList() }
                    val multRange = when (difficulty) {
                        "easy" -> 1..5
                        "hard" -> 1..maxMultiplier.toInt().coerceAtLeast(10)
                        else -> 1..maxMultiplier.toInt().coerceAtLeast(1)
                    }
                    app.container.pendingConfig = PracticeConfig(
                        mode = mode,
                        tables = tables,
                        multiplierRange = multRange,
                        count = count.toInt(),
                        timerSeconds = timerSeconds,
                        instantFeedback = settings.instantFeedback,
                        sequential = mode == PracticeMode.SEQUENTIAL
                    )
                    nav.navigate("practice")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(t["start"]) }
            Spacer(Modifier.height(24.dp))
        }
    }
}