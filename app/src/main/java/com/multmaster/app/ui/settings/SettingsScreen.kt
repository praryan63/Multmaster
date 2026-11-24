package com.multmaster.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.multmaster.app.MultiplicationApp
import com.multmaster.app.data.AppSettings
import com.multmaster.app.ui.LocalStrings
import com.multmaster.app.ui.SectionTitle
import com.multmaster.app.work.Reminders
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavHostController) {
    val t = LocalStrings.current
    val context = LocalContext.current
    val app = context.applicationContext as MultiplicationApp
    val repo = app.container.settings
    val settings by repo.current.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showReset by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = app.container.backupRepo.export()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            snackbar.showSnackbar(t["export_ok"])
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            val ok = json != null && app.container.backupRepo.import(json).isSuccess
            snackbar.showSnackbar(if (ok) t["import_ok"] else t["import_fail"])
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t["settings_title"], fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t["previous"])
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle(t["appearance"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to t["theme_system"], "light" to t["theme_light"], "dark" to t["theme_dark"])
                    .forEach { (key, label) ->
                        FilterChip(selected = settings.theme == key,
                            onClick = { repo.setTheme(key) }, label = { Text(label) })
                    }
            }

            SectionTitle(t["language"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = settings.language == "en", onClick = { repo.setLanguage("en") },
                    label = { Text(t["english"]) })
                FilterChip(selected = settings.language == "hi", onClick = { repo.setLanguage("hi") },
                    label = { Text(t["hindi"]) })
            }

            SectionTitle(t["practice"])
            SwitchRow(t["sound"], settings.sound) { repo.setSound(it) }
            SwitchRow(t["haptics"], settings.haptics) { repo.setHaptics(it) }
            SwitchRow(t["timer"], settings.timer) { repo.setTimer(it) }
            SwitchRow(t["instant_feedback"], settings.instantFeedback) { repo.setInstantFeedback(it) }
            SwitchRow(t["reminders"], settings.reminders) {
                repo.setReminders(it)
                Reminders.schedule(context, force = it)
            }

            SectionTitle(t["daily_goal"])
            Text("${t["daily_question_goal"]}: ${settings.dailyQuestionGoal}")
            Slider(
                value = settings.dailyQuestionGoal.toFloat(),
                onValueChange = { repo.setDailyQuestionGoal(it.toInt()) },
                valueRange = 10f..200f, steps = 18
            )
            Text("${t["daily_minute_goal"]}: ${settings.dailyMinuteGoal}")
            Slider(
                value = settings.dailyMinuteGoal.toFloat(),
                onValueChange = { repo.setDailyMinuteGoal(it.toInt()) },
                valueRange = 5f..60f, steps = 10
            )

            SectionTitle(t["difficulty"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("easy", "normal", "hard").forEach { d ->
                    FilterChip(selected = settings.difficulty == d, onClick = { repo.setDifficulty(d) },
                        label = { Text(t[d]) })
                }
            }

            SectionTitle(t["data"])
            OutlinedButton(onClick = { exportLauncher.launch("multmaster_backup.json") },
                modifier = Modifier.fillMaxWidth()) { Text(t["export"]) }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()) { Text(t["import"]) }
            Button(
                onClick = { showReset = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text(t["reset_progress"]) }

            SectionTitle(t["about"])
            TextButton(onClick = { showPrivacy = true }) { Text(t["privacy"]) }
            TextButton(onClick = { showAbout = true }) { Text(t["about"]) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(t["reset_progress"]) },
            text = { Text(t["reset_confirm"]) },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    scope.launch { app.container.backupRepo.resetAll() }
                }) { Text(t["confirm"]) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(t["cancel"]) } }
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(t["privacy"]) },
            text = { Text(t["privacy_text"]) },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text(t["ok"]) } }
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(t["about"]) },
            text = { Text(t["about_text"]) },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(t["ok"]) } }
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(label)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}