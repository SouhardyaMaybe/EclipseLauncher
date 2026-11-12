package com.eclipselauncher.ui.createinstance

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.R
import com.eclipselauncher.model.GameVersion
import com.eclipselauncher.model.Instance
import com.eclipselauncher.ui.navigation.NavigationDestination
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun CreateInstanceScreen(
    viewModel: CreateInstanceViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val versions by viewModel.filteredVersions.collectAsStateWithLifecycle()
    val selectedVersionTab by viewModel.selectedVersionTab.collectAsStateWithLifecycle()
    val selectedVersion by viewModel.selectedVersion.collectAsStateWithLifecycle()
    val versionsLoading by viewModel.versionsLoading.collectAsStateWithLifecycle()
    val versionsError by viewModel.versionsError.collectAsStateWithLifecycle()

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
                        text = "Create instance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    // Archive icon for modpack browser
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = EclipseColors.DarkSurfaceVariant,
                        onClick = { onNavigate("tools/mod_market") }
                    ) {
                        Icon(
                            imageVector = Archive,
                            contentDescription = "Modpack browser",
                            tint = EclipseColors.DarkOnSurface,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(12.dp)
                        )
                    }
                }

                // Filter chips
                VersionFilterChips(
                    selectedTab = selectedVersionTab,
                    onTabSelect = { viewModel.setVersionTab(it) }
                )

                // Version list
                if (versionsLoading) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(color = EclipseColors.DarkPrimary)
                    }
                } else if (versionsError != null) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = versionsError,
                            fontSize = 16.sp,
                            color = EclipseColors.DarkError,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    VersionList(
                        versions = versions,
                        selectedVersion = selectedVersion,
                        onVersionSelect = { viewModel.selectVersion(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun VersionFilterChips(
    selectedTab: GameVersion.VersionType,
    onTabSelect: (GameVersion.VersionType) -> Unit
) {
    val tabs = listOf(
        GameVersion.VersionType.RELEASE to "Releases",
        GameVersion.VersionType.SNAPSHOT to "Snapshots",
        GameVersion.VersionType.OLD_BETA to "Old Betas",
        GameVersion.VersionType.OLD_ALPHA to "Old Alphas"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (type, label) ->
            val isSelected = type == selectedTab
            Surface(
                modifier = Modifier
                    .height(36.dp)
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) EclipseColors.DarkPrimary else EclipseColors.DarkSurfaceVariant,
                onClick = { onTabSelect(type) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Check,
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
    }
}

@Composable
fun VersionList(
    versions: List<GameVersion>,
    selectedVersion: GameVersion?,
    onVersionSelect: (GameVersion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        versions.forEach { version ->
            VersionCard(
                version = version,
                isSelected = version.id == selectedVersion?.id,
                onClick = { onVersionSelect(version) }
            )
        }
    }
}

@Composable
fun VersionCard(
    version: GameVersion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val badgeText = when (version.type) {
        GameVersion.VersionType.RELEASE -> "Releases"
        GameVersion.VersionType.SNAPSHOT -> "Snapshots"
        GameVersion.VersionType.OLD_BETA -> "Old Betas"
        GameVersion.VersionType.OLD_ALPHA -> "Old Alphas"
    }
    val badgeColor = when (version.type) {
        GameVersion.VersionType.RELEASE -> EclipseColors.DarkPrimary
        GameVersion.VersionType.SNAPSHOT -> Color(0xFFFFD600) // Yellow
        GameVersion.VersionType.OLD_BETA -> Color(0xFFFF9100) // Orange
        GameVersion.VersionType.OLD_ALPHA -> Color(0xFFFF3D00) // Red
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) EclipseColors.DarkPrimary.copy(alpha = 0.15f) else EclipseColors.DarkSurface,
        border = if (isSelected) androidx.compose.ui.graphics.BorderStroke(2.dp, EclipseColors.DarkPrimary) else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = version.id,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface
                )
                Text(
                    text = version.releaseDate,
                    fontSize = 13.sp,
                    color = EclipseColors.DarkOnSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge
                Surface(
                    modifier = Modifier.padding(end = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = ChevronRight,
                    contentDescription = "Select",
                    tint = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}