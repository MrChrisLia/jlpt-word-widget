package com.jlpt.wordoftheday

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jlpt.wordoftheday.data.DictionarySources
import com.jlpt.wordoftheday.data.Word
import com.jlpt.wordoftheday.data.WordRepository
import com.jlpt.wordoftheday.ui.theme.JlptWordOfDayTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = WordRepository(applicationContext)

        setContent {
            JlptWordOfDayTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JlptWordScreen(repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JlptWordScreen(repository: WordRepository) {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentWord by remember { mutableStateOf(repository.getCurrentWord()) }
    var selectedLevels by remember { mutableStateOf(repository.getEnabledLevels()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    var refreshHour by remember { mutableStateOf(repository.getDailyRefreshHour()) }
    var refreshMinute by remember { mutableStateOf(repository.getDailyRefreshMinute()) }

    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                refreshHour = hour
                refreshMinute = minute
                repository.setDailyRefreshTime(hour, minute)
                DailyWordScheduler.reschedule(appContext)
            },
            refreshHour,
            refreshMinute,
            DateFormat.is24HourFormat(context)
        ).show()
    }

    fun refreshWord() {
        if (isRefreshing) return
        isRefreshing = true
        scope.launch {
            currentWord = withContext(Dispatchers.IO) {
                repository.refreshRandomWord()
            }
            JlptWordWidgetProvider.triggerUpdate(appContext)
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        if (currentWord == null) {
            isRefreshing = true
            currentWord = withContext(Dispatchers.IO) {
                repository.refreshRandomWord()
            }
            JlptWordWidgetProvider.triggerUpdate(appContext)
            isRefreshing = false
        } else {
            withContext(Dispatchers.IO) {
                repository.ensureSeeded()
            }
        }
    }

    if (showSources) {
        SourcesDialog(onDismiss = { showSources = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "JLPT Word") },
                actions = {
                    IconButton(onClick = { showSources = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Sources"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LevelSelector(
                selectedLevels = selectedLevels,
                onToggleLevel = { level ->
                    val updated = if (level in selectedLevels) {
                        if (selectedLevels.size == 1) selectedLevels else selectedLevels - level
                    } else {
                        selectedLevels + level
                    }
                    selectedLevels = updated
                    repository.setEnabledLevels(updated)
                    refreshWord()
                }
            )

            Button(
                onClick = ::refreshWord,
                enabled = !isRefreshing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.refresh_now))
            }

            DailyTimeCard(
                timeText = formatTimeOfDay(context, refreshHour, refreshMinute),
                onChangeClick = ::openTimePicker
            )

            currentWord?.let { word ->
                WordCard(word = word)
            } ?: EmptyWordCard(isRefreshing = isRefreshing)
        }
    }
}

private fun formatTimeOfDay(
    context: android.content.Context,
    hour: Int,
    minute: Int
): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

@Composable
private fun DailyTimeCard(
    timeText: String,
    onChangeClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.daily_word_time_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.daily_word_time_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onChangeClick) {
                Text(text = stringResource(R.string.change))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelSelector(
    selectedLevels: Set<String>,
    onToggleLevel: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "JLPT Levels",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WordRepository.DefaultLevels.forEach { level ->
                FilterChip(
                    selected = level in selectedLevels,
                    onClick = { onToggleLevel(level) },
                    label = { Text(text = level) }
                )
            }
        }
    }
}

@Composable
private fun WordCard(word: Word) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = word.jlptLevel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = word.wordTypeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = word.pronunciation,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = word.displayKanji,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DetailRow(label = "Hiragana", value = word.hiragana)
            DetailRow(label = "Katakana", value = word.katakana)
            DetailRow(label = "Part of speech", value = word.wordTypeLabel)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Example",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = word.exampleSentenceJa,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = word.exampleSentenceEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EmptyWordCard(isRefreshing: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text = stringResource(R.string.no_word_loaded))
        }
    }
}

@Composable
private fun SourcesDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Sources") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = DictionarySources.acknowledgement,
                    style = MaterialTheme.typography.bodyMedium
                )
                DictionarySources.all.forEach { source ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = source.use, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = source.license,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { uriHandler.openUri(source.url) }) {
                            Text(text = source.url)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done")
            }
        }
    )
}
