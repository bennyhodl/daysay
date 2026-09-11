package dev.bennyb.daysay.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import dev.bennyb.daysay.model.ModelCatalog
import dev.bennyb.daysay.model.Transcript
import dev.bennyb.daysay.model.TranscriptStore
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val items by TranscriptStore.items.collectAsState()
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Page(
        title = "History",
        onBack = onBack,
        actions = {
            if (items.isNotEmpty()) {
                if (confirmClear) {
                    TextButton(onClick = { TranscriptStore.clear(); confirmClear = false }) { Text("Delete all") }
                    TextButton(onClick = { confirmClear = false }) { Text("Keep") }
                } else {
                    TextButton(onClick = { confirmClear = true }) { Text("Clear") }
                }
            }
        },
    ) {
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Spacer(Modifier.height(48.dp))
            Text("No transcripts yet.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Every dictation lands here, newest first, whether it was inserted or only copied.",
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.graphite,
            )
        } else {
            Text("${items.size} ${if (items.size == 1) "transcript" else "transcripts"}", style = MaterialTheme.typography.bodySmall, color = Paper.ash)
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                HorizontalDivider(color = Paper.mist)
                TranscriptRow(item)
            }
            HorizontalDivider(color = Paper.mist)
        }
    }
}

@Composable
private fun TranscriptRow(item: Transcript) {
    val context = LocalContext.current
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var copied by rememberSaveable(item.id) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 18.dp)
            .animateContentSize(),
    ) {
        MetaLine("${formatWhen(item.createdAt)}  ·  ${formatDuration(item.durationSeconds)}  ·  ${ModelCatalog.displayName(item.engine)}")
        Spacer(Modifier.height(8.dp))
        Text(
            item.text,
            style = MonoStyle,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Dictation", item.text))
                copied = true
            }) { Text(if (copied) "Copied" else "Copy") }
            TextButton(onClick = { TranscriptStore.delete(item.id) }) { Text("Delete") }
        }
    }
}

private fun formatWhen(epochMs: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = epochMs }
    val now = Calendar.getInstance()
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMs))
    val sameDay = then.get(Calendar.YEAR) == now.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = then.get(Calendar.YEAR) == now.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    return when {
        sameDay -> "Today $time"
        yesterday -> "Yesterday $time"
        else -> "${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs))} $time"
    }
}
