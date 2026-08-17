package bs.wahgwaan.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.InputChip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bs.wahgwaan.model.BahamianIsland
import bs.wahgwaan.model.DateRangeFilter
import bs.wahgwaan.model.Event
import bs.wahgwaan.model.EventCategory
import bs.wahgwaan.model.LocationFilter
import bs.wahgwaan.ui.theme.accent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onEventClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLocationSearch by remember { mutableStateOf(false) }

    LaunchedEffect(state.syncError) {
        state.syncError?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshIfStale()
        onPauseOrDispose { }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("wah gwaan", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { showLocationSearch = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search location")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            FilterBar(
                filters = state.filters,
                onDateChipClick = { preset ->
                    if (preset is DateRangeFilter.Custom) showDatePicker = true
                    else viewModel.setDateRange(preset)
                },
                onCustomRangeClick = { showDatePicker = true },
                onIslandSelected = viewModel::setIsland,
                onCategoryToggle = viewModel::toggleCategory,
                onClearKeyword = { viewModel.setKeyword("") },
            )
            FreshnessLine(state.lastSyncEpochMs)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.events.isEmpty() && !state.isRefreshing) {
                    EmptyState()
                } else {
                    EventList(
                        events = state.events,
                        onEventClick = onEventClick,
                        onToggleFavorite = viewModel::toggleFavorite,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        CustomDateRangeSheet(
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                viewModel.setDateRange(DateRangeFilter.Custom(start, end))
                showDatePicker = false
            },
        )
    }
    if (showLocationSearch) {
        SearchSheet(
            currentKeyword = state.filters.keyword,
            onDismiss = { showLocationSearch = false },
            onApply = { keyword, location ->
                viewModel.setKeyword(keyword)
                if (location.isNotBlank()) viewModel.searchLocation(location)
                showLocationSearch = false
            },
        )
    }
}

@Composable
private fun FreshnessLine(lastSyncEpochMs: Long) {
    // Re-evaluate each minute so "Updated 2 min ago" doesn't freeze while
    // the screen stays open.
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastSyncEpochMs) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000)
        }
    }
    val label = if (lastSyncEpochMs == 0L) "Not synced yet — pull to refresh"
    else {
        val mins = (now - lastSyncEpochMs) / 60_000
        when {
            mins < 1 -> "Updated just now"
            mins < 60 -> "Updated $mins min ago"
            mins < 60 * 24 -> "Updated ${mins / 60}h ago"
            else -> "Updated ${mins / (60 * 24)}d ago"
        }
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
    )
}

@Composable
private fun FilterBar(
    filters: FilterState,
    onDateChipClick: (DateRangeFilter) -> Unit,
    onCustomRangeClick: () -> Unit,
    onIslandSelected: (BahamianIsland?) -> Unit,
    onCategoryToggle: (EventCategory) -> Unit,
    onClearKeyword: () -> Unit,
) {
    Column {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(DateRangeFilter.presets) { preset ->
                FilterChip(
                    selected = filters.dateRange::class == preset::class,
                    onClick = { onDateChipClick(preset) },
                    label = { Text(preset.label) },
                )
            }
            item {
                FilterChip(
                    selected = filters.dateRange is DateRangeFilter.Custom,
                    onClick = onCustomRangeClick,
                    label = {
                        Text((filters.dateRange as? DateRangeFilter.Custom)?.label
                            ?: "Custom…")
                    },
                )
            }
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = filters.location is LocationFilter.Everywhere,
                    onClick = { onIslandSelected(null) },
                    label = { Text("All islands") },
                )
            }
            items(BahamianIsland.entries.toList()) { island ->
                FilterChip(
                    selected = (filters.location as? LocationFilter.IslandTag)
                        ?.island == island,
                    onClick = { onIslandSelected(island) },
                    label = { Text(island.displayName) },
                )
            }
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(EventCategory.entries.filter { it != EventCategory.UNKNOWN }) { cat ->
                FilterChip(
                    selected = cat in filters.categories,
                    onClick = { onCategoryToggle(cat) },
                    label = { Text(cat.label) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(cat.accent, CircleShape))
                    },
                )
            }
        }
        if (filters.keyword.isNotBlank()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                InputChip(
                    selected = true,
                    onClick = onClearKeyword,
                    label = { Text("“${filters.keyword}”") },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Clear search",
                            modifier = Modifier.size(16.dp))
                    },
                )
            }
        }
    }
}

@Composable
private fun EventList(
    events: List<Event>,
    onEventClick: (String) -> Unit,
    onToggleFavorite: (Event) -> Unit,
) {
    val headerFmt = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    val grouped = remember(events) { events.groupBy { it.date } }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grouped.forEach { (date, dayEvents) ->
            item(key = "header-$date") {
                Text(
                    date.format(headerFmt),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(dayEvents, key = { it.id }) { event ->
                EventCard(event, onClick = { onEventClick(event.id) },
                    onToggleFavorite = { onToggleFavorite(event) })
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp, 44.dp)
                    .background(event.category.accent, RoundedCornerShape(5.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline)
                    Text(
                        event.venue.ifBlank { event.island?.displayName ?: "TBA" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    event.timeStart?.let {
                        Text(it.format(DateTimeFormatter.ofPattern("h:mm a")),
                            style = MaterialTheme.typography.labelMedium)
                    }
                    if (event.priceLabel.isNotBlank()) {
                        Text(event.priceLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (event.isSaved) Icons.Default.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = if (event.isSaved) "Unsave" else "Save",
                    tint = if (event.isSaved) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏝️", style = MaterialTheme.typography.displayMedium)
            Text("No events match these filters",
                style = MaterialTheme.typography.titleMedium)
            Text("Try widening the date range or island",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeSheet(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val pickerState = rememberDateRangePickerState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f, false))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = {
                        val start = pickerState.selectedStartDateMillis
                        val end = pickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            onConfirm(toLocalDate(start), toLocalDate(end))
                        }
                    },
                ) { Text("Apply") }
            }
        }
    }
}

private fun toLocalDate(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSheet(
    currentKeyword: String,
    onDismiss: () -> Unit,
    onApply: (keyword: String, location: String) -> Unit,
) {
    var keyword by remember { mutableStateOf(currentKeyword) }
    var location by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("What you lookin' for?", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Keyword — reggae, brunch, junkanoo…") },
                singleLine = true,
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Location — island, zip code, or region") },
                singleLine = true,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BahamianIsland.entries.toList()) { island ->
                    AssistChip(
                        onClick = { location = island.displayName },
                        label = { Text(island.displayName) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = { onApply(keyword, location) },
                ) { Text("Apply") }
            }
        }
    }
}
