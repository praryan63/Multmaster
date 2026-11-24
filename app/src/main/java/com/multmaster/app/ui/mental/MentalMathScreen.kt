package com.multmaster.app.ui.mental

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
import androidx.navigation.NavHostController
import com.multmaster.app.MultiplicationApp
import com.multmaster.app.domain.PracticeConfig
import com.multmaster.app.domain.PracticeMode
import com.multmaster.app.ui.LocalStrings
import com.multmaster.app.ui.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentalMathScreen(nav: NavHostController) {
    val t = LocalStrings.current
    val app = LocalContext.current.applicationContext as MultiplicationApp
    var a by remember { mutableIntStateOf(16) }
    var b by remember { mutableIntStateOf(7) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t["mental_title"], fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(t["mental_desc"], style = MaterialTheme.typography.bodyMedium)
            SectionTitle(t["tips"])
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$a × $b", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("= (10 × $b) + (${a - 10} × $b)")
                    Text("= ${10 * b} + ${(a - 10) * b}")
                    Text("= ${a * b}", fontWeight = FontWeight.Bold)
                    Text("Distributive property: split the table into 10 × n plus the remainder.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { a = (11..19).random(); b = (2..9).random() },
                    modifier = Modifier.weight(1f)) { Text("11–19 × 2–9") }
                OutlinedButton(onClick = { a = (21..49).random(); b = (2..9).random() },
                    modifier = Modifier.weight(1f)) { Text("21–49 × 2–9") }
            }
            Button(
                onClick = {
                    app.container.pendingConfig = PracticeConfig(
                        mode = PracticeMode.MENTAL,
                        tables = (11..49).toList(),
                        multiplierRange = 2..9,
                        count = 15,
                        timerSeconds = 180,
                        title = t["mental_maths"]
                    )
                    nav.navigate("practice")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(t["drill"]) }
        }
    }
}