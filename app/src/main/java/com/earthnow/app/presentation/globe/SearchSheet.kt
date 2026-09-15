package com.earthnow.app.presentation.globe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.earthnow.app.R
import com.earthnow.app.domain.model.Place

@Composable
fun SearchSheet(
    recent: List<Place>,
    results: List<Place>,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (Place) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 16.dp)
                .heightIn(max = 480.dp)
        ) {
            DismissBar()
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onQueryChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp)
                ) { Icon(Icons.Default.Close, stringResource(R.string.close_search)) }
            }
            Spacer(Modifier.height(10.dp))

            if (loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    CircularProgressIndicator(Modifier.width(24.dp).height(24.dp), strokeWidth = 2.dp)
                }
            } else if (results.isNotEmpty()) {
                LazyColumn {
                    items(results) { place ->
                        PlaceRow(place, "search") { onSelect(place) }
                    }
                }
            } else if (query.isBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.recent_places),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    if (recent.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) { Text(stringResource(R.string.clear)) }
                    }
                }
                LazyColumn {
                    items(recent) { place ->
                        PlaceRow(place, "history") { onSelect(place) }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaceRow(place: Place, source: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (place.kind) {
                "ocean" -> Icons.Default.Public
                else -> if (source == "history") Icons.Default.History else Icons.Default.LocationOn
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(place.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val sub = listOfNotNull(
                place.country?.takeIf { it != place.name },
                place.kind.replaceFirstChar { it.uppercase() }
            ).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
}