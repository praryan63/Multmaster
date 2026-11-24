package com.multmaster.app.ui.mistakes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.multmaster.app.data.MistakeEntity
import com.multmaster.app.domain.*
import com.multmaster.app.ui.*
import com.multmaster.app.util.TimeUtils
import com.multmaster.app.util.app
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MistakeFilter { ALL, DUE, TODAY, FREQUENT }

class MistakeViewModel(private val container: AppContainer) : ViewModel() {
    private val filter = MutableStateFlow(MistakeFilter.DUE)

    val items: StateFlow<List<MistakeEntity>> = combine(container.mistakeRepo.active(), filter) { list, f ->
        val start = TimeUtils.startOfDay()
        val end = TimeUtils.endOfDay()
        val now = System.currentTimeMillis()
        when (f) {
            MistakeFilter.ALL -> list
            MistakeFilter.DUE -> list.filter { it.dueAt <= now }
            MistakeFilter.TODAY -> list.filter { it.createdAt in start until end }
            MistakeFilter.FREQUENT -> list.filter { it.mistakeCount >= 2 }.sortedByDescending { it.mistakeCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(f: MistakeFilter) { filter.value = f }

    fun toggleBookmark(id: Long, current: Boolean) = viewModelScope.launch {
        container.mistakeRepo.setBookmarked(id, !current)
    }

    fun startPractice(list: List<MistakeEntity>, mode: PracticeMode = PracticeMode.MISTAKE) {
        if (list.isEmpty()) return
        container.pendingConfig = PracticeConfig(
            mode = mode,
            count = list.size,
            explicitQuestions = list.map { Question(it.a, it.b) },
            title = "Mistakes"
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MistakeViewModel(app().container) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(nav: NavHostController, vm: MistakeViewModel = viewModel(factory = MistakeViewModel.Factory)) {
    val t = LocalStrings.current
    val list by vm.items.collectAsStateWithLifecycle()
    var currentFilter by remember { mutableStateOf(MistakeFilter.DUE) }
    val app = LocalContext.current.applicationContext as MultiplicationApp

    Scaffold(topBar = { TopAppBar(title = { Text(t["mistakes"], fontWeight = FontWeight.Bold) }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MistakeFilter.entries.forEach { f ->
                    FilterChip(
                        selected = currentFilter == f,
                        onClick = { currentFilter = f; vm.setFilter(f) },
                        label = {
                            Text(
                                when (f) {
                                    MistakeFilter.ALL -> t["all"]
                                    MistakeFilter.DUE -> t["due"]
                                    MistakeFilter.TODAY -> t["today_mistakes"]
                                    MistakeFilter.FREQUENT -> t["frequent"]
                                }
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.startPractice(list); nav.navigate("practice") },
                    enabled = list.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(t["practice_all"]) }
            }
            Spacer(Modifier.height(10.dp))

            if (list.isEmpty()) {
                EmptyState(t["no_mistakes"], t["no_mistakes_desc"])
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listItems(list, key = { it.id }) { m ->
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${m.a} × ${m.b} = ${m.correct}", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${t["mistake_count"]}: ${m.mistakeCount}  ·  ${t["your_answer"]}: ${m.lastGiven}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Revision: ${SpacedRepetition.label(m.intervalIndex)}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                IconButton(onClick = { vm.toggleBookmark(m.id, m.bookmarked) }) {
                                    Icon(
                                        if (m.bookmarked) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = t["bookmark"]
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}