package com.eclipselauncher.ui.managecontent.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipselauncher.model.ModInfo
import com.eclipselauncher.ui.managecontent.ContentTypePills
import com.eclipselauncher.ui.managecontent.ManageContentViewModel
import com.eclipselauncher.ui.theme.EclipseColors

@Composable
fun ColumnScope.BrowseContentSection(
    viewModel: ManageContentViewModel,
    browseType: ManageContentViewModel.ContentType,
    browseProvider: ManageContentViewModel.Provider,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onTypeSelect: (ManageContentViewModel.ContentType) -> Unit,
    onProviderSelect: (ManageContentViewModel.Provider) -> Unit,
    isSearching: Boolean,
    searchResults: List<ModInfo>,
    searchError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Provider pills (top)
        ProviderPills(
            selectedProvider = browseProvider,
            onProviderSelect = onProviderSelect
        )

        // Content type pills
        ContentTypePills(
            selectedType = browseType,
            onTypeSelect = onTypeSelect
        )

        // Search field
        SearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearchClick = onSearchClick
        )

        // Provider pills (bottom - repeated)
        ProviderPills(
            selectedProvider = browseProvider,
            onProviderSelect = onProviderSelect
        )

        // Results area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    color = EclipseColors.DarkPrimary,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else if (searchError != null) {
                Text(
                    text = searchError.orEmpty(),
                    fontSize = 14.sp,
                    color = EclipseColors.DarkError,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Text(
                    text = "No content yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else if (searchResults.isNotEmpty()) {
                BrowseResultsList(
                    items = searchResults,
                    onItemClick = { /* Open project page */ }
                )
            } else {
                Text(
                    text = "Search for mods to get started",
                    fontSize = 14.sp,
                    color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }
        }
    }
}

@Composable
fun ProviderPills(
    selectedProvider: ManageContentViewModel.Provider,
    onProviderSelect: (ManageContentViewModel.Provider) -> Unit
) {
    val providers = listOf(
        ManageContentViewModel.Provider.MODRINTH to "Modrinth",
        ManageContentViewModel.Provider.CURSEFORGE to "CurseForge"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        providers.forEach { (provider, label) ->
            val isSelected = provider == selectedProvider
            val isDisabled = provider == ManageContentViewModel.Provider.CURSEFORGE
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) 
                    if (isDisabled) EclipseColors.DarkPrimary.copy(alpha = 0.5f) 
                    else EclipseColors.DarkPrimary 
                else if (isDisabled) 
                    EclipseColors.DarkSurfaceVariant.copy(alpha = 0.5f) 
                else 
                    EclipseColors.DarkSurfaceVariant,
                onClick = { if (!isDisabled) onProviderSelect(provider) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "",
                            tint = if (isDisabled) EclipseColors.DarkOnPrimary.copy(alpha = 0.5f) else EclipseColors.DarkOnPrimary,
                            modifier = Modifier.size(16.dp).padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) 
                            if (isDisabled) EclipseColors.DarkOnPrimary.copy(alpha = 0.5f) 
                            else EclipseColors.DarkOnPrimary 
                        else if (isDisabled) 
                            EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f) 
                        else 
                            EclipseColors.DarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search field
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = EclipseColors.DarkSurface,
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier.size(20.dp).padding(end = 8.dp)
                )
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(text = "Search content", color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedTextColor = EclipseColors.DarkOnSurface,
                        unfocusedTextColor = EclipseColors.DarkOnSurface,
                        focusedPlaceholderColor = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f),
                        unfocusedPlaceholderColor = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f),
                        cursorColor = EclipseColors.DarkPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }
        }
        
        // Search button
        Surface(
            modifier = Modifier
                .width(48.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = EclipseColors.DarkPrimary,
            onClick = onSearchClick
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = EclipseColors.DarkOnPrimary,
                modifier = Modifier.size(24.dp).padding(12.dp)
            )
        }
    }
}

@Composable
fun BrowseResultsList(
    items: List<ModInfo>,
    onItemClick: (ModInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            BrowseResultCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun BrowseResultCard(
    item: ModInfo,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = EclipseColors.DarkSurface,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Artwork (56dp)
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = EclipseColors.DarkSurfaceVariant,
                        border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.name.first().uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = EclipseColors.DarkPrimary
                            )
                        }
                    }
                }

                // Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkOnSurface
                    )
                    Text(
                        text = "${item.author} · ${formatDownloads(item.downloads)}",
                        fontSize = 13.sp,
                        color = EclipseColors.DarkPrimary
                    )
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = EclipseColors.DarkOnSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.categories.isNotEmpty() || item.gameVersions.isNotEmpty()) {
                        Text(
                            text = "${item.categories.joinToString(", ")} · ${item.gameVersions.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Download button (placeholder)
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Download",
                    tint = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier.size(24.dp).padding(8.dp)
                )
            }
        }
    }
}

fun formatDownloads(count: Long): String {
    return when {
        count >= 1000000 -> "${(count / 1000000.0).toString().take(4)}M"
        count >= 1000 -> "${(count / 1000.0).toString().take(4)}k"
        else -> count.toString()
    }
}