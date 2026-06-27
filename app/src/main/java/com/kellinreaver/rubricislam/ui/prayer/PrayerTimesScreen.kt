package com.kellinreaver.rubricislam.ui.prayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kellinreaver.rubricislam.ui.theme.RubricIslamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(viewModel: PrayerTimeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    RubricIslamTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Prayer Times") })
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.prayerTimes) { prayer ->
                            PrayerTimeCard(prayer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerTimeCard(prayer: com.kellinreaver.rubricislam.domain.model.PrayerTime) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = prayer.name, style = MaterialTheme.typography.titleLarge)
            Text(text = prayer.time, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
