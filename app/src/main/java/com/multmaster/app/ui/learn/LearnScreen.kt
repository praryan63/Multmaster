package com.multmaster.app.ui.learn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.multmaster.app.MultiplicationApp
import com.multmaster.app.data.*
import com.multmaster.app.domain.Mastery
import com.multmaster.app.domain.MasteryLevel
import com.multmaster.app.ui.*
import com.multmaster.app.util.app
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LearnUi(
    val progress: Map<Int, TableProgressEntity> = emptyMap(),
    val favorites: Set<Int> = emptySet()
)

class LearnViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<LearnUi> = combine(
        container.progressRepo.tableProgress(),
        container.db.favoriteDao().flow()
    ) { list, favs ->
        LearnUi(list.associateBy { it.tableNo }, favs.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LearnUi())

    fun toggleFavorite(table: Int) = viewModelScope.launch {
        if (container.db.favoriteDao().all().contains(table)) container.db.favoriteDao().delete(table)
        else container.db.favoriteDao().insert(FavoriteEntity(table))
    }

    fun setLearned(table: Int, learned: Boolean) = viewModelScope.launch {
        container.progressRepo.setLearned(table, learned)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { LearnViewModel(app().container) }
        }
    }
}

private fun levelColor(level: MasteryLevel, scheme: ColorScheme) = when (level) {
    MasteryLevel.NOT_STARTED -> scheme.surfaceVariant
    MasteryLevel.LEARNING -> scheme.primaryContainer
    MasteryLevel.PRACTICING -> scheme.secondaryContainer
    MasteryLevel.IMPROVING -> scheme.tertiaryContainer
    MasteryLevel.MASTERED -> scheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(nav: NavHostController, vm: LearnViewModel = viewModel(factory = LearnViewModel.Factory)) {
    val t = LocalStrings.current
    val s by vm.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    Scaffold(topBar = { TopAppBar(title = { Text(t["tables"], fontWeight = FontWeight.Bold) }) }) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.padding(pad).fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..50).toList()) { n ->
                val p = s.progress[n]
                val level = if (p == null) MasteryLevel.NOT_STARTED else Mastery.of(p)
                Card(
                    onClick = { nav.navigate("learn/$n") },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = levelColor(level, scheme))
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$n",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (level == MasteryLevel.MASTERED) scheme.onPrimary else scheme.onSurface
                        )
                        if (s.favorites.contains(n)) {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp),
                                tint = if (level == MasteryLevel.MASTERED) scheme.onPrimary else scheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnDetailScreen(table: Int, nav: NavHostController, vm: LearnViewModel = viewModel(factory = LearnViewModel.Factory)) {
    val t = LocalStrings.current
    val s by vm.state.collectAsStateWithLifecycle()
    var showAnswers by remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MultiplicationApp
    val settings by app.container.settings.current.collectAsStateWithLifecycle()
    val tts = app.container.tts
    LaunchedEffect(settings.language) { tts.setLanguage(settings.language) }
    LaunchedEffect(table, showAnswers) { if (!showAnswers) tts.stop() }

    val p = s.progress[table]
    val learned = p?.learned == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${t["table"]} $table") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t["previous"])
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite(table) }) {
                        Icon(
                            if (s.favorites.contains(table)) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = t["favorite"]
                        )
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = {
                    if (showAnswers) {
                        (1..20).forEach { b -> tts.speakRow(table, b, table * b) }
                    }
                }) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(t["read_aloud"])
                }
                OutlinedButton(onClick = { showAnswers = !showAnswers }) {
                    Text(if (showAnswers) t["hide_answers"] else t["show_answers"])
                }
            }
            Spacer(Modifier.height(12.dp))

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(20) { i ->
                    val b = i + 1
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$table × $b",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (showAnswers) "$table × $b = ${table * b}" else "= ?",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (table > 1) nav.navigate("learn/${table - 1}") { popUpTo("learn/$table") { inclusive = true } } },
                    enabled = table > 1,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null)
                    Text(t["previous"])
                }
                OutlinedButton(
                    onClick = { if (table < 50) nav.navigate("learn/${table + 1}") { popUpTo("learn/$table") { inclusive = true } } },
                    enabled = table < 50,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(t["next"])
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    app.container.pendingConfig = com.multmaster.app.domain.PracticeConfig(
                        mode = com.multmaster.app.domain.PracticeMode.SEQUENTIAL,
                        tables = listOf(table),
                        count = 20,
                        title = "${t["table"]} $table"
                    )
                    nav.navigate("practice")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text(t["practice_this_table"]) }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { vm.setLearned(table, !learned) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (learned) t["learned"] else t["mark_learned"]) }
        }
    }
}