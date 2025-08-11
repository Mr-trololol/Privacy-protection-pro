package com.scram.systems.privacyprotection.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.scram.systems.privacyprotection.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreenContent(
    paddingValues: PaddingValues,
    uiState: FirewallUiState,
    actions: FirewallViewModel
) {
    val appsToShow = when (uiState.selectedTab) {
        0 -> uiState.userApps
        else -> uiState.systemApps
    }

    Column(modifier = Modifier.padding(paddingValues)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // New: Firewall Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Firewall", style = MaterialTheme.typography.headlineSmall)
                Switch(
                    checked = uiState.isFirewallActive,
                    onCheckedChange = { actions.onFirewallToggled(it) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (uiState.isFirewallActive) "Firewall is ON. Custom DNS is disabled." else "Firewall is OFF. Custom DNS is active.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { actions.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { actions.onBlockAll() }, label = { Text("Block All") })
                FilterChip(selected = false, onClick = { actions.onAllowAll() }, label = { Text("Allow All") })
            }
            SortByDropdown(
                selectedSortBy = uiState.sortBy,
                onSortChanged = { actions.onSortChanged(it) }
            )
        }

        TabRow(selectedTabIndex = uiState.selectedTab) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { actions.onTabSelected(0) },
                text = { Text("User Apps (${uiState.userApps.size})") }
            )
            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { actions.onTabSelected(1) },
                text = { Text("System Apps (${uiState.systemApps.size})") }
            )
        }

        Divider()

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(appsToShow, key = { it.packageName }) { app ->
                    FirewallAppRow(
                        appInfo = app,
                        onBlockToggle = { isBlocked -> actions.onBlockAppToggled(app, isBlocked) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortByDropdown(selectedSortBy: SortBy, onSortChanged: (SortBy) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = "Sort by")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { onSortChanged(SortBy.NAME_AZ); expanded = false })
            DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { onSortChanged(SortBy.NAME_ZA); expanded = false })
            DropdownMenuItem(text = { Text("Blocked First") }, onClick = { onSortChanged(SortBy.BLOCKED_FIRST); expanded = false })
        }
    }
}

@Composable
private fun FirewallAppRow(appInfo: AppInfo, onBlockToggle: (Boolean) -> Unit) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberDrawablePainter(drawable = appInfo.icon),
                contentDescription = stringResource(R.string.app_icon_content_description, appInfo.name),
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = appInfo.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = appInfo.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = appInfo.isBlocked, onCheckedChange = onBlockToggle)
        }
    }
}
