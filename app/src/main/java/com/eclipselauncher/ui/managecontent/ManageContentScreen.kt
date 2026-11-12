package com.eclipselauncher.ui.managecontent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filed.Folder
import androidx.compose.material.icons.filed.FileDownload
import androidx.compose.runtime.Composable
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
import com.eclipselauncher.ui.managecontent.components.InstalledContentList
import com.eclipselauncher.ui.managecontent.components.BrowseContentSection
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun ManageContentScreen(
    viewModel: ManageContentViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val installedType by viewModel.installedType.collectAsStateWithLifecycle()
    val browseType by viewModel.browseType.collectAsStateWithLifecycle()
    val browseProvider by viewModel.browseProvider.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()

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
                        imageVector = ArrowBack,
                        contentDescription = "Back",
                        tint = EclipseColors.DarkOnSurface,
                        modifier = Modifier
                            .size(48.dp)
                            .fillMaxWidth(),
                        onClick = { onNavigate("home") }
                    )
                    Text(
                        text = "Manage Content",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    // Import icon
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = EclipseColors.DarkSurfaceVariant,
                        onClick = { /* Import archive */ }
                    ) {
                        Icon(
                            imageVector = Folder,
                            contentDescription = "Import",
                            tint = EclipseColors.DarkOnSurface,
                            modifier = Modifier.size(24.dp).padding(12.dp)
                        )
                    }
                }

                // Main tabs: Installed / Browse
                TabRow(
                    selectedTab = currentTab,
                    onTabSelect = { viewModel.setTab(it) }
                )

                // Content
                if (currentTab == ManageContentViewModel.ContentTab.INSTALLED) {
                    InstalledContentSection(
                        viewModel = viewModel,
                        installedType = installedType,
                        onTypeSelect = { viewModel.setInstalledType(it) }
                    )
                } else {
                    BrowseContentSection(
                        viewModel = viewModel,
                        browseType = browseType,
                        browseProvider = browseProvider,
                        searchQuery = searchText,
                        onSearchQueryChange = { searchText = it; viewModel.setSearchQuery(it) },
                        onSearchClick = { viewModel.performSearch() },
                        onTypeSelect = { viewModel.setBrowseType(it) },
                        onProviderSelect = { viewModel.setBrowseProvider(it) },
                        isSearching = isSearching,
                        searchResults = searchResults,
                        searchError = searchError
                    )
                }
            }
        }
    }
}

@Composable
fun TabRow(
    selectedTab: ManageContentViewModel.ContentTab,
    onTabSelect: (ManageContentViewModel.ContentTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabButton(
            label = "Installed",
            isSelected = selectedTab == ManageContentViewModel.ContentTab.INSTALLED,
            onClick = { onTabSelect(ManageContentViewModel.ContentTab.INSTALLED) }
        )
        TabButton(
            label = "Browse",
            isSelected = selectedTab == ManageContentViewModel.ContentTab.BROWSE,
            onClick = { onTabSelect(ManageContentViewModel.ContentTab.BROWSE) }
        )
    }
}

@Composable
fun TabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (isSelected) EclipseColors.DarkPrimary else EclipseColors.DarkSurfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = androidx.compose.material.icons.default.Check,
                    contentDescription = "",
                    tint = EclipseColors.DarkOnPrimary,
                    modifier = Modifier.size(16.dp).padding(end = 6.dp)
                )
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant
            )
        }
    }
}

@Composable
fun InstalledContentSection(
    viewModel: ManageContentViewModel,
    installedType: ManageContentViewModel.ContentType,
    onTypeSelect: (ManageContentViewModel.ContentType) -> Unit
) {
    val installedContent = viewModel.getInstalledContent()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Content type pills
        ContentTypePills(
            selectedType = installedType,
            onTypeSelect = onTypeSelect
        )

        // Divider
        androidx.compose.foundation.Divider(
            color = EclipseColors.DarkOutlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Content list
        if (installedContent.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No content yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurfaceVariant
                )
            }
        } else {
            InstalledContentList(
                items = installedContent,
                onItemClick = { /* Open detail */ },
                onRefreshClick = { /* Refresh */ },
                onToggleClick = { /* Toggle enabled */ },
                onDeleteClick = { /* Delete confirmation */ }
            )
        }
    }
}

@Composable
fun ContentTypePills(
    selectedType: ManageContentViewModel.ContentType,
    onTypeSelect: (ManageContentViewModel.ContentType) -> Unit
) {
    val types = listOf(
        ManageContentViewModel.ContentType.MODS to "≡ Mods",
        ManageContentViewModel.ContentType.RESOURCE_PACKS to "≡ Resource Packs",
        ManageContentViewModel.ContentType.DATA_PACKS to "≡ Data Packs",
        ManageContentViewModel.ContentType.SHADERS to "≡ Shaders",
        ManageContentViewModel.ContentType.WORLDS to "≡ Worlds"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (type, label) ->
            val isSelected = type == selectedType
            Surface(
                modifier = Modifier
                    .height(32.dp)
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) EclipseColors.DarkPrimary else EclipseColors.DarkSurfaceVariant,
                onClick = { onTypeSelect(type) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = androidx.compose.material.icons.default.Check,
                            contentDescription = "",
                            tint = EclipseColors.DarkOnPrimary,
                            modifier = Modifier.size(14.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant
                    )
                }
            }
        }
    }
}