package bs.wahgwaan.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bs.wahgwaan.ui.theme.AquaDeep
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bs.wahgwaan.data.EventRepository
import bs.wahgwaan.model.Event
import bs.wahgwaan.ui.feed.EventCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: EventRepository,
) : ViewModel() {
    val saved: StateFlow<List<Event>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(event: Event) {
        viewModelScope.launch { repository.toggleFavorite(event) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    onEventClick: (String) -> Unit,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(AquaDeep, MaterialTheme.colorScheme.primary))),
        ) {
            Text(
                "Saved 💛",
                Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 6.dp, bottom = 12.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
    }) { padding ->
        if (saved.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💛", style = MaterialTheme.typography.displayMedium)
                    Text("Nothing saved yet",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Tap the heart on any event to keep it here — even offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(saved, key = { it.id }) { event ->
                    EventCard(event,
                        onClick = { onEventClick(event.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(event) })
                }
            }
        }
    }
}
