package bs.wahgwaan.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bs.wahgwaan.data.EventRepository
import bs.wahgwaan.model.Event
import bs.wahgwaan.ui.calendar.CalendarExporter
import bs.wahgwaan.ui.theme.accent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventRepository,
) : ViewModel() {
    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    val event: StateFlow<Event?> = repository.observeEvent(eventId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleFavorite() {
        event.value?.let { viewModelScope.launch { repository.toggleFavorite(it) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val event by viewModel.event.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCalendarSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    event?.let { ev ->
                        IconButton(onClick = {
                            val text = buildString {
                                append(ev.name)
                                append(" — ${ev.date}")
                                ev.timeStart?.let { append(" $it") }
                                if (ev.venue.isNotBlank()) append(" @ ${ev.venue}")
                                if (ev.sourceUrl.isNotBlank()) append("\n${ev.sourceUrl}")
                            }
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }, "Share event"))
                        }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                if (ev.isSaved) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = "Save",
                                tint = if (ev.isSaved) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val ev = event ?: return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = ev.category.accent.copy(alpha = 0.12f),
                contentColor = ev.category.accent,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(ev.category.label,
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge)
            }
            Text(ev.name, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)

            InfoRow(icon = { Icon(Icons.Default.CalendarMonth, null) },
                text = ev.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")))
            ev.timeStart?.let { start ->
                val fmt = DateTimeFormatter.ofPattern("h:mm a")
                val label = ev.timeEnd?.let { "${start.format(fmt)} – ${it.format(fmt)}" }
                    ?: start.format(fmt)
                InfoRow(icon = { Icon(Icons.Default.Schedule, null) }, text = label)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoRow(icon = { Icon(Icons.Default.LocationOn, null) },
                    text = listOfNotNull(
                        ev.venue.ifBlank { null },
                        ev.island?.displayName).joinToString(" · ")
                        .ifBlank { "Location TBA" })
                if (ev.venue.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        val q = Uri.encode("${ev.venue}, Bahamas")
                        context.startSafely(
                            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")))
                    }) { Text("Directions") }
                }
            }
            if (ev.priceLabel.isNotBlank()) {
                Text(ev.priceLabel, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showCalendarSheet = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add to Calendar")
                }
                if (ev.sourceUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            context.startSafely(Intent(Intent.ACTION_VIEW,
                                Uri.parse(ev.sourceUrl)))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Get Tickets") }
                }
            }

            if (ev.description.isNotBlank()) {
                Text("About", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(ev.description, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }

        if (showCalendarSheet) {
            CalendarTargetSheet(
                onDismiss = { showCalendarSheet = false },
                onTarget = { target ->
                    showCalendarSheet = false
                    val intent = when (target) {
                        CalendarTarget.GOOGLE_DEVICE -> CalendarExporter.systemInsertIntent(ev)
                        CalendarTarget.OUTLOOK -> CalendarExporter.outlookIntent(ev)
                        CalendarTarget.ICAL_FILE ->
                            Intent.createChooser(
                                CalendarExporter.icsShareIntent(context, ev),
                                "Share calendar file")
                    }
                    context.startSafely(intent)
                },
            )
        }
    }
}

@Composable
private fun InfoRow(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        icon()
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

enum class CalendarTarget { GOOGLE_DEVICE, OUTLOOK, ICAL_FILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTargetSheet(
    onDismiss: () -> Unit,
    onTarget: (CalendarTarget) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Add to calendar",
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text("Google / device calendar") },
                supportingContent = { Text("Opens your calendar app's event editor") },
                modifier = Modifier.clickableTarget { onTarget(CalendarTarget.GOOGLE_DEVICE) },
            )
            ListItem(
                headlineContent = { Text("Outlook") },
                supportingContent = { Text("Opens Outlook's new-event page") },
                modifier = Modifier.clickableTarget { onTarget(CalendarTarget.OUTLOOK) },
            )
            ListItem(
                headlineContent = { Text("iCal file (.ics)") },
                supportingContent = { Text("Share a universal calendar file") },
                modifier = Modifier.clickableTarget { onTarget(CalendarTarget.ICAL_FILE) },
            )
        }
    }
}

private fun Modifier.clickableTarget(onClick: () -> Unit): Modifier =
    fillMaxWidth().clickable(onClick = onClick)

/** Every outbound intent (ticket links, Outlook deep links, geo URIs) can
 *  hit a device with no matching app — never let that crash the screen. */
private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app on this phone can open that",
            Toast.LENGTH_SHORT).show()
    }
}
