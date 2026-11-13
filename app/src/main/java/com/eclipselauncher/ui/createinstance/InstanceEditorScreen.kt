package com.eclipselauncher.ui.createinstance

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eclipselauncher.R
import com.eclipselauncher.model.Instance
import com.eclipselauncher.ui.navigation.NavigationDestination
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun InstanceEditorScreen(
    viewModel: CreateInstanceViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val isEditingExisting by viewModel.isEditingExisting.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val instanceName by viewModel.instanceName.collectAsStateWithLifecycle()
    val controlMap by viewModel.controlMap.collectAsStateWithLifecycle()
    val sharedData by viewModel.sharedData.collectAsStateWithLifecycle()
    val jvmArguments by viewModel.jvmArguments.collectAsStateWithLifecycle()
    val javaRuntime by viewModel.javaRuntime.collectAsStateWithLifecycle()
    val renderer by viewModel.renderer.collectAsStateWithLifecycle()
    val customIconPath by viewModel.customIconPath.collectAsStateWithLifecycle()
    val selectedVersion by viewModel.selectedVersion.collectAsStateWithLifecycle()
    val selectedLoader by viewModel.selectedLoader.collectAsStateWithLifecycle()

    var nameText by remember { mutableStateOf(instanceName) }
    var jvmArgsText by remember { mutableStateOf(jvmArguments) }

    val versionText = "${selectedVersion?.id ?: ""} · ${selectedLoader?.name ?: ""}"

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
                            .fillMaxWidth(),
                        onClick = { onNavigate("create_instance/loader_picker/${selectedVersion?.id ?: ""}") }
                    )
                    Text(
                        text = "Edit instance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkOnSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditingExisting) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = EclipseColors.DarkSurfaceVariant,
                                onClick = { /* Copy instance */ }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = EclipseColors.DarkOnSurface,
                                    modifier = Modifier.size(24.dp).padding(12.dp)
                                )
                            }
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = EclipseColors.DarkError.copy(alpha = 0.2f),
                                onClick = { /* Delete confirmation */ }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = EclipseColors.DarkError,
                                    modifier = Modifier.size(24.dp).padding(12.dp)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = EclipseColors.DarkSurfaceVariant,
                            onClick = { onNavigate("create_instance/loader_picker/${selectedVersion?.id ?: ""}") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = EclipseColors.DarkOnSurface,
                                modifier = Modifier.size(24.dp).padding(12.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasChanges || nameText.isNotBlank()) EclipseColors.DarkPrimary else EclipseColors.DarkSurfaceVariant,
                            onClick = if (hasChanges || nameText.isNotBlank()) {
                                viewModel.updateInstanceName(nameText)
                                viewModel.updateJvmArguments(jvmArgsText)
                                val instance = viewModel.createInstance()
                                onNavigate("home")
                            } else null
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = if (hasChanges || nameText.isNotBlank()) EclipseColors.DarkOnPrimary else EclipseColors.DarkOnSurfaceVariant,
                                modifier = Modifier.size(24.dp).padding(12.dp)
                            )
                        }
                    }
                }

                // Scrollable form
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon with pencil
                        IconEditor(
                            customIconPath = customIconPath,
                            loader = selectedLoader ?: Instance.LoaderType.VANILLA,
                            onIconPick = { /* Pick image */ },
                            onIconReset = { viewModel.updateCustomIcon(null) }
                        )

                        // Version line
                        Text(
                            text = "Minecraft Version: $versionText",
                            fontSize = 14.sp,
                            color = EclipseColors.DarkOnSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // Name field
                        EditorField(
                            label = "Name",
                            value = nameText,
                            onValueChange = { 
                                nameText = it
                                viewModel.updateInstanceName(it)
                            },
                            placeholder = "Instance name"
                        )

                        // Default controlmap
                        EditorDropdownField(
                            label = "Default controlmap",
                            value = controlMap,
                            placeholder = "(Use global default)",
                            onClick = { /* Show controlmap picker */ }
                        )

                        // Shared data switch
                        EditorSwitchRow(
                            label = "Shared data (click to switch)",
                            subtitle = "Store game data in the instance directory",
                            checked = sharedData,
                            onCheckedChange = { viewModel.updateSharedData(it) }
                        )

                        // JVM arguments
                        EditorTextField(
                            label = "JVM arguments",
                            value = jvmArgsText,
                            onValueChange = { 
                                jvmArgsText = it
                                viewModel.updateJvmArguments(it)
                            },
                            placeholder = "(Use global default)",
                            multiline = true
                        )

                        // Java Runtime
                        EditorDropdownField(
                            label = "Java Runtime",
                            value = javaRuntime,
                            placeholder = "Auto",
                            onClick = { /* Show runtime picker */ }
                        )

                        // Renderer
                        EditorDropdownField(
                            label = "Renderer",
                            value = renderer,
                            placeholder = "Global",
                            onClick = { /* Show renderer picker */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconEditor(
    customIconPath: String?,
    loader: Instance.LoaderType,
    onIconPick: () -> Unit,
    onIconReset: () -> Unit
) {
    val hasCustomIcon = customIconPath != null

    Box(
        modifier = Modifier
            .size(120.dp)
            .padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(16.dp),
            color = EclipseColors.DarkSurface,
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (hasCustomIcon) {
                    // Custom image
                    Image(
                        painter = painterResource(id = R.drawable.steve), // Placeholder
                        contentDescription = "Custom icon",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Loader glyph
                    Text(
                        text = when (loader) {
                            Instance.LoaderType.VANILLA -> "V"
                            Instance.LoaderType.OPTIFINE -> "O"
                            Instance.LoaderType.FABRIC -> "F"
                            Instance.LoaderType.FORGE -> "◈"
                            Instance.LoaderType.NEOFORGE -> "N"
                            Instance.LoaderType.QUILT -> "Q"
                        },
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = EclipseColors.DarkPrimary
                    )
                }
            }

            // Pencil button
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = EclipseColors.DarkPrimary,
                onClick = onIconPick
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change icon",
                    tint = EclipseColors.DarkOnPrimary,
                    modifier = Modifier.size(20.dp).padding(8.dp)
                )
            }

            // Reset button (only for custom icons)
            if (hasCustomIcon) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = EclipseColors.DarkError.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, EclipseColors.DarkError),
                    onClick = onIconReset
                ) {
                    Text(
                        text = "Reset",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = EclipseColors.DarkError,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = EclipseColors.DarkOnSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = EclipseColors.DarkSurface,
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(text = placeholder, color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = EclipseColors.DarkSurface,
                    focusedContainerColor = EclipseColors.DarkSurface,
                    inputTextColor = EclipseColors.DarkOnSurface,
                    placeholderTextColor = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f),
                    cursorColor = EclipseColors.DarkPrimary,
                    focusedIndicatorColor = EclipseColors.DarkPrimary,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun EditorDropdownField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = EclipseColors.DarkOnSurfaceVariant
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            color = EclipseColors.DarkSurface,
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    fontSize = 16.sp,
                    color = if (value.isBlank()) EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f) else EclipseColors.DarkOnSurface
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Dropdown",
                    tint = EclipseColors.DarkOnSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EditorSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EclipseColors.DarkSurface
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
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = EclipseColors.DarkOnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = EclipseColors.DarkOnSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = if (checked) EclipseColors.DarkPrimary else EclipseColors.DarkOutlineVariant,
                    checkedTrackColor = if (checked) EclipseColors.DarkPrimary.copy(alpha = 0.3f) else EclipseColors.DarkSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    multiline: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = EclipseColors.DarkOnSurfaceVariant
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (multiline) 100.dp else 52.dp),
            shape = RoundedCornerShape(12.dp),
            color = EclipseColors.DarkSurface,
            border = BorderStroke(1.dp, EclipseColors.DarkOutlineVariant)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(text = placeholder, color = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                singleLine = !multiline,
                maxLines = if (multiline) 5 else 1,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = EclipseColors.DarkSurface,
                    focusedContainerColor = EclipseColors.DarkSurface,
                    inputTextColor = EclipseColors.DarkOnSurface,
                    placeholderTextColor = EclipseColors.DarkOnSurfaceVariant.copy(alpha = 0.5f),
                    cursorColor = EclipseColors.DarkPrimary,
                    focusedIndicatorColor = EclipseColors.DarkPrimary,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    }
}