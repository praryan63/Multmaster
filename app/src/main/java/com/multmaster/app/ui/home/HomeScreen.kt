package com.multmaster.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.multmaster.app.AppContainer
import com.multmaster.app.data.*
import com.multmaster.app.domain.*
import com.multmaster.app.ui.*
import com.multmaster.app.util.TimeUtils
import com.multmaster.app.util.app
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUi(
    val todayAttempts: Int = 0,
    val todayCorrect: Int = 0,
    val todayMs: Long = 0,
    val totalAttempts: Int = 0,
    val totalCorrect: Int = 0,
    val streak: Int = 0,
    val mastered: Int = 0,
    val weak: List<TableAccuracy> = emptyList(),
    val dueMistakes: Int = 0,
    val dailyQuestionGoal: Int = 30,
    val dailyMinuteGoal: Int = 10,
    val last7: List<DayStat> = emptyList()
) {
    val accuracy: Int = if (totalAttempts == 0) 0 else (totalCorrect * 100) / totalAttempts
    val todayMinutes: Long = todayMs / 60000
}

class HomeViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.progressRepo
    private val mistakes = container.mistakeRepo
    private val settings = container.settings

    val state: StateFlow<HomeUi> = combine(
        repo.perTable(),
        repo.daily(),
        repo.totals(),
        mistakes.active(),
        settings.current
    ) { tables, days, totals, mistakeList, s ->
        val today = TimeUtils.todayIso()
        val todayStat = days.firstOrNull { it.date == today }
        val daySet = days.filter { it.total > 0 }.map { it.date }.toSet()
        var streak = 0
        var offset = 0
        if (!daySet.contains(today)) offset = 1
        while (daySet.contains(TimeUtils.isoDate(offset))) { streak++; offset++ }
        HomeUi(
            todayAttempts = todayStat?.total ?: 0,
            todayCorrect = todayStat?.correct ?: 0,
            todayMs = 0,
            totalAttempts = totals.attempted,
            totalCorrect = totals.correct,
            streak = streak,
            mastered = tables.count { Mastery.of(TableProgressEntity(it.tableNo, it.total, it.correct)) == MasteryLevel.MASTERED },
            weak = tables.filter { it.total >= 5 && it.accuracy < 0.75 }.sortedBy { it.accuracy }.take(4),
            dueMistakes = mistakeList.count { it.dueAt <= System.currentTimeMillis() },
            dailyQuestionGoal = s.dailyQuestionGoal,
            dailyMinuteGoal = s.dailyMinuteGoal,
            last7 = days.takeLast(7)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUi())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(app().container) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController, vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val t = LocalStrings.current
    val s by vm.state.collectAsStateWithLifecycle()
    val dailyQuestion = QuestionGenerator.dailyQuestion(TimeUtils.dayIndex())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(t["app_name"], fontWeight = FontWeight.Bold)
                        Text(t["subtitle"], style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = t["settings"])
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(t["today"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(t["attempted"], "${s.todayAttempts}", Modifier.weight(1f))
                StatCard(t["correct"], "${s.todayCorrect}", Modifier.weight(1f),
                    tint = MaterialTheme.colorScheme.secondaryContainer)
                StatCard(t["streak"], "${s.streak}", Modifier.weight(1f),
                    hint = t["days"], tint = MaterialTheme.colorScheme.surfaceVariant)
            }

            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(t["daily_goal"], fontWeight = FontWeight.SemiBold)
                        Text("${s.todayAttempts} / ${s.dailyQuestionGoal}")
                    }
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(s.todayAttempts.toFloat() / s.dailyQuestionGoal.coerceAtLeast(1))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (s.todayAttempts >= s.dailyQuestionGoal) t["goal_reached"]
                        else "${s.dailyQuestionGoal - s.todayAttempts} ${t["questions"]}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(t["accuracy"], "${s.accuracy}%", Modifier.weight(1f))
                StatCard(t["mastered"], "${s.mastered} / 50", Modifier.weight(1f),
                    tint = MaterialTheme.colorScheme.tertiaryContainer)
                StatCard(t["revision_due"], "${s.dueMistakes}", Modifier.weight(1f),
                    tint = MaterialTheme.colorScheme.surfaceVariant)
            }

            Button(
                onClick = { nav.navigate("practiceSetup") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(t["quick_practice"]) }

            OutlinedButton(
                onClick = {
                    nav.navigate("practiceSetup")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("${t["revision_due"]} (${s.dueMistakes})") }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(t["daily_question"], fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(dailyQuestion.prompt, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(dailyQuestion.explanation, style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionTitle(t["weak_tables"])
            if (s.weak.isEmpty()) {
                Text(t["no_weak"], style = MaterialTheme.typography.bodyMedium)
            } else {
                s.weak.forEach { w ->
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${t["table"]} ${w.tableNo}", fontWeight = FontWeight.SemiBold)
                            Text("${(w.accuracy * 100).toInt()}%", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { nav.navigate("mental") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(t["mental_maths"]) }
                OutlinedButton(
                    onClick = { nav.navigate("learn") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(t["tables"]) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}