package com.eclipselauncher.ui.createinstance

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
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.R
import com.eclipselauncher.model.Instance
import com.eclipselauncher.ui.navigation.NavigationDestination
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun LoaderPickerScreen(
    viewModel: CreateInstanceViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val selectedVersion by viewModel.selectedVersion.collectAsState()
    val selectedLoader by viewModel.selectedLoader.collectAsState()
    val selectedLoaderVersion by viewModel.selectedLoaderVersion.collectAsState()
    val loaderVersions by viewModel.loaderVersions.collectAsState()
    val loaderVersionsLoading by viewModel.loaderVersionsLoading.collectAsState()

    val versionId = selectedVersion?.id ?: ""

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
                Box(modifier = Modifier
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
                            .clickable { onNavigate("create_instance") }
                    )
                    Text(
                        text = versionId,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    // Install button
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = EclipseColors.DarkPrimary,
                        onClick = { 
                            val instance = viewModel.createInstance()
                            // Save instance and go home
                            onNavigate("home")
                        }
                    ) {
                        Text(
                            text = "Install",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = EclipseColors.DarkOnPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Loader heading
                Text(
                    text = "Loader",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                )

                // Loader cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoaderCard(
                        loader = Instance.LoaderType.VANILLA,
                        isSelected = selectedLoader == Instance.LoaderType.VANILLA,
                        isAvailable = true,
                        onClick = { viewModel.selectLoader(Instance.LoaderType.VANILLA) }
                    )
                    LoaderCard(
                        loader = Instance.LoaderType.OPTIFINE,
                        isSelected = selectedLoader == Instance.LoaderType.OPTIFINE,
                        isAvailable = loaderVersions[Instance.LoaderType.OPTIFINE]?.isNotEmpty() == true,
                        onClick = { viewModel.selectLoader(Instance.LoaderType.OPTIFINE) },
                        showDropdown = selectedLoader == Instance.LoaderType.OPTIFINE,
                        dropdownExpanded = selectedLoader == Instance.LoaderType.OPTIFINE,
                        versions = loaderVersions[Instance.LoaderType.OPTIFINE] ?: emptyList(),
                        selectedVersion = selectedLoaderVersion,
                        onVersionSelect = { viewModel.selectLoaderVersion(it) },
                        onCloseDropdown = { viewModel.selectLoader(Instance.LoaderType.VANILLA) }
                    )
                    LoaderCard(
                        loader = Instance.LoaderType.FABRIC,
                        isSelected = selectedLoader == Instance.LoaderType.FABRIC,
                        isAvailable = loaderVersions[Instance.LoaderType.FABRIC]?.isNotEmpty() == true,
                        onClick = { viewModel.selectLoader(Instance.LoaderType.FABRIC) },
                        showDropdown = selectedLoader == Instance.LoaderType.FABRIC,
                        dropdownExpanded = selectedLoader == Instance.LoaderType.FABRIC,
                        versions = loaderVersions[Instance.LoaderType.FABRIC] ?: emptyList(),
                        selectedVersion = selectedLoaderVersion,
                        onVersionSelect = { viewModel.selectLoaderVersion(it) },
                        onCloseDropdown = { viewModel.selectLoader(Instance.LoaderType.VANILLA) }
                    )
                    LoaderCard(
                        loader = Instance.LoaderType.FORGE,
                        isSelected = selectedLoader == Instance.LoaderType.FORGE,
                        isAvailable = loaderVersions[Instance.LoaderType.FORGE]?.isNotEmpty() == true,
                        onClick = { viewModel.selectLoader(Instance.LoaderType.FORGE) },
                        showDropdown = selectedLoader == Instance.LoaderType.FORGE,
                        dropdownExpanded = selectedLoader == Instance.LoaderType.FORGE,
                        versions = loaderVersions[Instance.LoaderType.FORGE] ?: emptyList(),
                        selectedVersion = selectedLoaderVersion,
                        onVersionSelect = { viewModel.selectLoaderVersion(it) },
                        onCloseDropdown = { viewModel.selectLoader(Instance.LoaderType.VANILLA) }
                    )
                    LoaderCard(
                        loader = Instance.LoaderType.NEOFORGE,
                        isSelected = selectedLoader == Instance.LoaderType.NEOFORGE,
                        isAvailable = loaderVersions[Instance.LoaderType.NEOFORGE]?.isNotEmpty() == true,
                        onClick = { viewModel.selectLoader(Instance.LoaderType.NEOFORGE) },
                        showDropdown = selectedLoader == Instance.LoaderType.NEOFORGE,
                        dropdownExpanded = selectedLoader == Instance.LoaderType.NEOFORGE,
                        versions = loaderVersions[Instance.LoaderType.NEOFORGE] ?: emptyList(),
                        selectedVersion = selectedLoaderVersion,
                        onVersionSelect = { viewModel.selectLoaderVersion(it) },
                        onCloseDropdown = { viewModel.selectLoader(Instance.LoaderType.VANILLA) }
                    )
                    LoaderCard(
                        loader = Instance.LoaderType.QUILT,
                        isSelected = selectedLoader == Instance.LoaderType.QUILT,
                        isAvailable = loaderVersions[Instance.LoaderType.QUILT]?.isNotEmpty() == true,
                        onClick = { viewModel.selectLoader(Instance.LoaderType.QUILT) },
                        showDropdown = selectedLoader == Instance.LoaderType.QUILT,
                        dropdownExpanded = selectedLoader == Instance.LoaderType.QUILT,
                        versions = loaderVersions[Instance.LoaderType.QUILT] ?: emptyList(),
                        selectedVersion = selectedLoaderVersion,
                        onVersionSelect = { viewModel.selectLoaderVersion(it) },
                        onCloseDropdown = { viewModel.selectLoader(Instance.LoaderType.VANILLA) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoaderCard(
    loader: Instance.LoaderType,
    isSelected: Boolean,
    isAvailable: Boolean,
    onClick: () -> Unit,
    showDropdown: Boolean = false,
    dropdownExpanded: Boolean = false,
    versions: List<String> = emptyList(),
    selectedVersion: String = "",
    onVersionSelect: (String) -> Unit = {},
    onCloseDropdown: () -> Unit = {}
) {
    val (glyph, name) = when (loader) {
        Instance.LoaderType.VANILLA -> "V" to "Vanilla"
        Instance.LoaderType.OPTIFINE -> "O" to "OptiFine"
        Instance.LoaderType.FABRIC -> "F" to "Fabric"
        Instance.LoaderType.FORGE -> "◈" to "Forge"
        Instance.LoaderType.NEOFORGE -> "N" to "NeoForge"
        Instance.LoaderType.QUILT -> "Q" to "Quilt"
    }

    val statusText = if (!isAvailable) "Unavailable" else if (isSelected && versions.isNotEmpty()) selectedVersion.ifBlank { "Latest" } else "Available"

    // Main card
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) 
            if (!isAvailable) EclipseColors.DarkSurfaceVariant.copy(alpha = 0.5f) 
            else EclipseColors.DarkPrimary
        else if (!isAvailable) 
            EclipseColors.DarkSurfaceVariant.copy(alpha = 0.5f) 
        else 
            EclipseColors.DarkSurface,
        border = if (isSelected && isAvailable) BorderStroke(2.dp, EclipseColors.DarkPrimary) else null,
        onClick = { if (isAvailable) onClick() }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glyph
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = glyph,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected && isAvailable) EclipseColors.DarkOnPrimary 
                            else if (!isAvailable) EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)
                            else EclipseColors.DarkPrimary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!isAvailable) EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)
                            else if (isSelected) EclipseColors.DarkOnPrimary
                            else EclipseColors.DarkOnSurface
                    )
                    if (!isSelected || versions.isEmpty()) {
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            color = if (!isAvailable) EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)
                                else if (isSelected) EclipseColors.DarkOnPrimary.copy(alpha = 0.7f)
                                else EclipseColors.DarkOnSurfaceVariant
                        )
                    }
                }

                // Chevron or clear
                if (showDropdown && dropdownExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (versions.isNotEmpty() && selectedVersion.isNotBlank()) {
                            Surface(
                                modifier = Modifier.padding(end = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EclipseColors.DarkOnPrimary.copy(alpha = 0.2f) else EclipseColors.DarkSurfaceVariant,
                                onClick = { onVersionSelect("") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear version",
                                    tint = if (isSelected) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp).padding(8.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Close",
                            tint = if (isSelected) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (showDropdown) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = if (!isAvailable) EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)
                            else if (isSelected) EclipseColors.DarkOnPrimary
                            else EclipseColors.DarkOnSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Dropdown version list
            if (showDropdown && dropdownExpanded && versions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        versions.forEach { version ->
                            val isSelectedVersion = version == selectedVersion
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelectedVersion) 
                                    if (isSelected) EclipseColors.DarkPrimary.copy(alpha = 0.3f) 
                                    else EclipseColors.DarkSurfaceVariant
                                else 
                                    EclipseColors.DarkSurface,
                                onClick = { onVersionSelect(version) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Radio
                                    Canvas(
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        val center = androidx.compose.ui.geometry.Offset(12.dp.toPx(), 12.dp.toPx())
                                        val radius = 10.dp.toPx()
                                        drawCircle(
                                            color = if (isSelectedVersion) EclipseColors.DarkPrimary 
                                                else EclipseColors.DarkOutlineVariant,
                                            center = center,
                                            radius = radius,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        if (isSelectedVersion) {
                                            drawCircle(
                                                color = EclipseColors.DarkPrimary,
                                                center = center,
                                                radius = 6.dp.toPx()
                                            )
                                        }
                                    }
                                    Text(
                                        text = version,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelectedVersion) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isSelected) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurface,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}