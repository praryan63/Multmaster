package com.multmaster.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.multmaster.app.AppContainer
import com.multmaster.app.data.*
import com.multmaster.app.domain.Mastery
import com.multmaster.app.ui.*
import com.multmaster.app.util.TimeUtils
import com.multmaster.app.util.app
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

data class StatsUi(
    val tables: List<TableAccuracy> = emptyList(),
    val days: List<DayStat> = emptyList(),
    val totals: Totals = Totals(0, 0),
    val avgMs: Double = 0.0,
    val fastestMs: Long = 0,
    val sessions: List<SessionEntity> = emptyList(),
    val progress: List<TableProgressEntity> = emptyList(),
    val revised: Int = 0
)

class StatsViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<StatsUi> = combine(
        combine(container.progressRepo.perTable(), container.progressRepo.daily(), container.progressRepo.totals()) { a, b, c -> Triple(a, b, c) },
        combine(container.progressRepo.avgMs(), container.progressRepo.fastest(), container.progressRepo.sessions(20)) { a, b, c -> Triple(a, b, c) },
        combine(container.progressRepo.tableProgress(), container.mistakeRepo.resolved()) { a, b -> a to b.size }
    ) { (tables, days, totals), (avg, fastest, sessions), (progress, revised) ->
        StatsUi(tables, days, totals, avg, fastest, sessions, progress, revised)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUi())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { StatsViewModel(app().container) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(nav: NavHostController, vm: StatsViewModel = viewModel(factory = StatsViewModel.Factory)) {
    val t = LocalStrings.current
    val s by vm.state.collectAsStateWithLifecycle()

    val accuracy = if (s.totals.attempted == 0) 0 else s.totals.correct * 100 / s.totals.attempted
    val strongest = s.tables.filter { it.total >= 5 }.maxByOrNull { it.accuracy }
    val weakest = s.tables.filter { it.total >= 5 }.minByOrNull { it.accuracy }
    val last7 = (6 downTo 0).map { TimeUtils.isoDate(it) }
    val last7Values = last7.map { d -> (s.days.firstOrNull { it.date == d }?.total ?: 0).toFloat() }
    val last7Labels = last7.map { it.takeLast(2) }

    Scaffold(topBar = { TopAppBar(title = { Text(t["stats_title"], fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(t["attempted"], "${s.totals.attempted}", Modifier.weight(1f))
                    StatCard(t["accuracy"], "$accuracy%", Modifier.weight(1f))
                    StatCard(t["mastered"], "${s.progress.count { Mastery.of(it) == com.multmaster.app.domain.MasteryLevel.MASTERED }}",
                        Modifier.weight(1f), tint = MaterialTheme.colorScheme.tertiaryContainer)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(t["average_time"], TimeUtils.formatMs(s.avgMs.toLong()), Modifier.weight(1f))
                    StatCard(t["fastest_answer"], TimeUtils.formatMs(s.fastestMs), Modifier.weight(1f))
                    StatCard(t["resolved"], "${s.revised}", Modifier.weight(1f))
                }
            }
            item {
                SectionTitle(t["last_7_days"])
                Card(shape = RoundedCornerShape(16.dp)) {
                    Box(Modifier.padding(16.dp)) {
                        BarChart(last7Values, last7Labels)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(t["strongest"], strongest?.let { "${it.tableNo} (${(it.accuracy * 100).roundToInt()}%)" } ?: "—", Modifier.weight(1f))
                    StatCard(t["weakest"], weakest?.let { "${it.tableNo} (${(it.accuracy * 100).roundToInt()}%)" } ?: "—", Modifier.weight(1f))
                }
            }
            item { SectionTitle(t["calendar"]) }
            item {
                val days30 = (29 downTo 0).map { TimeUtils.isoDate(it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    days30.forEach { d ->
                        val has = s.days.firstOrNull { it.date == d }?.total ?: 0
                        Box(
                            Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (has > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }
            item { SectionTitle(t["table_accuracy"]) }
            items(s.tables.sortedBy { it.tableNo }.size) { i ->
                val row = s.tables.sortedBy { it.tableNo }[i]
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${row.tableNo} ×", fontWeight = FontWeight.Medium)
                        Text("${(row.accuracy * 100).roundToInt()}%  (${row.correct}/${row.total})",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(row.accuracy.toFloat()).height(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (row.accuracy >= 0.85) MaterialTheme.colorScheme.primary
                                    else if (row.accuracy >= 0.6) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.error
                                )
                        )
                    }
                }
            }
            item { SectionTitle(t["sessions"]) }
            items(s.sessions.size) { i ->
                val session = s.sessions[i]
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(session.mode, fontWeight = FontWeight.SemiBold)
                            Text(TimeUtils.formatDate(session.startedAt), style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${session.correct}/${session.total}")
                            Text(TimeUtils.formatMs(session.fastestMs), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            item { SectionTitle(t["achievements"]) }
            item { Achievements(s) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Achievements(s: StatsUi) {
    val unlocked = listOf(
        s.totals.attempted >= 10,
        s.totals.attempted >= 100,
        s.days.count { it.total > 0 } >= 3,
        s.progress.count { Mastery.of(it) == com.multmaster.app.domain.MasteryLevel.MASTERED } >= 5,
        s.totals.attempted >= 100 && s.totals.correct.toDouble() / s.totals.attempted >= 0.9,
        s.revised >= 20
    )
    val t = LocalStrings.current
    val names = listOf("badge_first", "badge_100", "badge_streak3", "badge_master5", "badge_accurate", "badge_reviser")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEachIndexed { i, key ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked[i]) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t[key], style = MaterialTheme.typography.bodyMedium)
                    Text(if (unlocked[i]) t["unlocked"] else t["locked"], style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}