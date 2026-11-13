package com.eclipselauncher.ui.managecontent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.model.ModInfo
import com.eclipselauncher.ui.managecontent.components.BrowseResultsList
import com.eclipselauncher.ui.managecontent.components.ProviderPills
import com.eclipselauncher.ui.managecontent.components.SearchField
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun ModpackBrowserScreen(
    viewModel: ManageContentViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val browseProvider by viewModel.browseProvider.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchError by viewModel.searchError.collectAsState()

    var searchText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }

    EclipseLauncherTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EclipseColors.DarkBackground
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                // Status bar spacer
                androidx.compose.foundation.layout.Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                )

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = EclipseColors.DarkOnSurface,
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxWidth()
                            .clickable { onNavigate("home") }
                    )
                    Text(
                        text = "Modpack Browser",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    // Import icon
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = EclipseColors.DarkSurfaceVariant,
                        onClick = { /* Import local .zip */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Import modpack",
                            tint = EclipseColors.DarkOnSurface,
                            modifier = Modifier.size(24.dp).padding(12.dp)
                        )
                    }
                }

                // Provider pills
                ProviderPills(
                    selectedProvider = browseProvider,
                    onProviderSelect = { viewModel.setBrowseProvider(it) }
                )

                // Search field
                SearchField(
                    query = searchText,
                    onQueryChange = { searchText = it; viewModel.setSearchQuery(it) },
                    onSearchClick = { viewModel.performSearch() }
                )

                // Results area
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isSearching) {
                        androidx.compose.material3.CircularProgressIndicator(
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
                            text = "No modpacks found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = EclipseColors.DarkOnSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    } else if (searchResults.isNotEmpty()) {
                        BrowseResultsList(
                            items = searchResults,
                            onItemClick = { /* Open Modrinth page */ }
                        )
                    } else {
                        Text(
                            text = "Search for modpacks to get started",
                            fontSize = 14.sp,
                            color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }
            }
        }
    }
}