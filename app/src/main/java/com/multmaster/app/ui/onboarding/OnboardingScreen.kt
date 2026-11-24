package com.multmaster.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multmaster.app.MultiplicationApp
import com.multmaster.app.ui.LocalStrings

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val t = LocalStrings.current
    val app = LocalContext.current.applicationContext as MultiplicationApp

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(t["app_name"], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(t["subtitle"], style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(40.dp))
        Text(t["onboard_title"], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(t["onboard_body"], style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = { app.container.settings.setOnboarded(true); onDone() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text(t["get_started"]) }
    }
}