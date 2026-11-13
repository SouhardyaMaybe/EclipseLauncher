/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * HOME: header (app mark left; utils, settings and tasks buttons top-right,
 * left to right), the centre block, and the bottom stack.
 *
 * Centre: when an account is signed in, its static skin head (base + hat
 * overlay) is itself a button that opens ACCOUNTS, with a nametag plate
 * below it — the plate carries the username and a caret button that opens
 * the account-switcher dialog (radio + head + username list). With no
 * account, a giant "+" panel invites adding one. The shapes stay one
 * seamless rounded family, but each role takes its own radius from
 * EclipseRadius — plates are tighter than panels, controls tighter still —
 * so the screen has hierarchy instead of one stamped shape everywhere.
 * They react to touch with the in-house squeeze/push animations.
 *
 * Bottom: the instance panel (icon, name, version, a responsive caret and a
 * pencil in one seamless row; the caret opens a fixed-height dropdown
 * anchored under the panel — list scrollable over the instances, "Create
 * instance" pinned at its foot) over the full-width play button. Play
 * requires an account, so pressing it without one opens the account gate.
 */
package com.eclipselauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import com.eclipselauncher.R
import com.eclipselauncher.data.AccountState
import com.eclipselauncher.data.Instance
import com.eclipselauncher.data.MediaStorage
import com.eclipselauncher.ui.animation.caretRotation
import com.eclipselauncher.ui.animation.pressPushCorner
import com.eclipselauncher.ui.animation.pressSqueeze
import com.eclipselauncher.ui.character.SkinHeadFace
import com.eclipselauncher.ui.components.AccountSwitcherDialog
import com.eclipselauncher.ui.components.AddAccountGate
import com.eclipselauncher.ui.components.InstanceIcon
import com.eclipselauncher.ui.components.loaderName
import com.eclipselauncher.ui.theme.EclipseRadius

@Composable
fun HomeScreen(
    accountState: AccountState,
    instances: List<Instance>,
    media: MediaStorage,
    onOpenAccounts: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onCreateInstance: () -> Unit,
    onEditInstance: (Instance) -> Unit,
    onPlay: (Instance) -> Unit,
    onOpenFileManager: () -> Unit,
    onOpenManageLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTasks: () -> Unit,
    pendingSelectionId: String? = null,
) {
    // The account gate no longer blocks launch; it opens only when play is
    // pressed without an account — logging in is a precondition for playing.
    var gateOpen by remember { mutableStateOf(false) }
    // The nametag's account switcher.
    var switcherOpen by remember { mutableStateOf(false) }
    // Selection by id so list edits don't reset it; falls back to the first.
    var selectedId by remember { mutableStateOf<String?>(null) }
    // A freshly created instance selects itself on HOME.
    LaunchedEffect(pendingSelectionId) {
        if (pendingSelectionId != null) selectedId = pendingSelectionId
    }
    val selected = instances.firstOrNull { it.id == selectedId } ?: instances.firstOrNull()
    val account = accountState.selected

    fun attemptPlay() {
        val target = selected ?: return
        if (accountState.isEmpty) {
            gateOpen = true
        } else {
            onPlay(target)
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding().semantics { 
            set(androidx.compose.ui.semantics.SemanticsProperties.TestTag, "home_screen") 
        }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: app mark on the left; utils, settings, tasks on the
            // right, placed left to right, each with a corner-push press.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                HeaderButton(
                    icon = Icons.Filled.Folder,
                    contentDescription = stringResource(R.string.utils_file_manager),
                    onClick = onOpenFileManager,
                )
                HeaderButton(
                    icon = Icons.Filled.Article,
                    contentDescription = stringResource(R.string.utils_manage_logs),
                    onClick = onOpenManageLogs,
                )
                HeaderButton(
                    icon = Icons.Filled.ListAlt,
                    contentDescription = stringResource(R.string.cd_tasks),
                    onClick = onOpenTasks,
                )
                HeaderButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_nav_settings),
                    onClick = onOpenSettings,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Centre block: the head (button to ACCOUNTS) + nametag, or the
            // empty-account invite. Both are tap-to-open-accounts.
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (account != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // The head is a button: pressing it (and the whole
                        // block) opens ACCOUNTS.
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .pressPushCorner(
                                    pushOrigin = TransformOrigin(0.5f, 1f),
                                    onClick = onOpenAccounts,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            SkinHeadFace(
                                skinFileName = account.skinFileName,
                                media = media,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        NametagPlate(
                            username = account.username,
                            switcherOpen = switcherOpen,
                            onOpenSwitcher = { switcherOpen = true },
                        )
                    }
                } else {
                    EmptyAccountPanel(
                        onClick = onOpenAccounts,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Instance panel, or the empty-state button.
            if (instances.isEmpty()) {
                NoInstancesButton(
                    onClick = onCreateInstance,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                InstancePanel(
                    instances = instances,
                    selected = selected!!,
                    media = media,
                    onSelect = { picked -> selectedId = picked.id },
                    onCreate = onCreateInstance,
                    onEdit = { onEditInstance(selected) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full-width play button at the very bottom; disabled until an
            // instance exists to launch. Squeezes on press.
            PlayButton(
                enabled = selected != null,
                onClick = ::attemptPlay,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    AccountSwitcherDialog(
        visible = switcherOpen,
        accountState = accountState,
        media = media,
        onSelect = onSelectAccount,
        onAddAccount = onOpenAccounts,
        onDismiss = { switcherOpen = false },
    )

    if (gateOpen) {
        // One path only: continue into ACCOUNTS, where both add-account
        // buttons (Microsoft sign-in, local account) live.
        AddAccountGate(
            onConnect = {
                gateOpen = false
                onOpenAccounts()
            },
            onDismiss = { gateOpen = false },
        )
    }
}

/** A top-right header icon that pushes its top-right corner on press. */
@Composable
private fun HeaderButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressPushCorner(
                // Push the top-right corner in; the opposite corner stays
                // pinned — the Xiaomi-style corner push.
                pushOrigin = TransformOrigin(0f, 1f),
                onClick = onClick,
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * The nametag: a seamless plate with the username (Minecraft typeface, large)
 * and a caret button that opens the account switcher. The caret flips with
 * the switcher's open/closed state.
 */
@Composable
private fun NametagPlate(
    username: String,
    switcherOpen: Boolean,
    onOpenSwitcher: () -> Unit,
) {
    val tagFontFamily = remember { FontFamily(Font(R.font.minecraft_nametag)) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(EclipseRadius.tile))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(EclipseRadius.tile),
            )
            .padding(start = 18.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = username,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            fontFamily = tagFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        // The caret button: reacts (rotates) to the switcher opening/closing.
        Box(
            modifier = Modifier
                .size(40.dp)
                .pressSqueeze(onClick = onOpenSwitcher),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.cd_switch_account),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .caretRotation(switcherOpen),
            )
        }
    }
}

/** No account yet: a giant "+" and an invitation, tapping opens ACCOUNTS. */
@Composable
private fun EmptyAccountPanel(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.pressSqueeze(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(EclipseRadius.panel))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(EclipseRadius.panel),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(84.dp),
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.no_account_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.no_account_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The empty state: a full-width "No instances — tap to create" button. */
@Composable
private fun NoInstancesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(EclipseRadius.panel))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(EclipseRadius.panel),
            )
            .pressSqueeze(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.action_create_instance),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(30.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(R.string.home_no_instances),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.home_no_instances_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One seamless panel holding the selected instance (icon, name, version),
 * a responsive caret, and a pencil. The caret opens a fixed-height list of
 * instances — scrollable over however many exist, with "Create instance"
 * pinned at its foot; the pencil edits the current instance. The caret
 * rotates with the list's open/closed state.
 */
@Composable
private fun InstancePanel(
    instances: List<Instance>,
    selected: Instance,
    media: MediaStorage,
    onSelect: (Instance) -> Unit,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // The dropdown mirrors the panel's own width, so the panel measures
    // itself and hands the width down in dp. The fallback only covers the
    // first frame — layout (and this measurement) always precedes any tap
    // that could open the menu.
    var panelWidth by remember { mutableStateOf(300.dp) }
    val density = LocalDensity.current

    Box(
        modifier = modifier.onSizeChanged { size ->
            panelWidth = with(density) { size.width.toDp() }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(EclipseRadius.panel))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(EclipseRadius.panel),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Main tap area: opens the full-width instance list.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pressSqueeze(onClick = { expanded = true })
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InstanceIcon(instance = selected, media = media, size = 30.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = selected.minecraftVersion + " · " + loaderName(selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                // Responsive caret: flips with the list's state.
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.cd_select_instance),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .caretRotation(expanded),
                )
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // The pencil lives inside the same panel.
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .pressSqueeze(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_edit_instance),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Anchored instance dropdown: a menu popup under the panel (it
        // flips above when the screen has no room below), as wide as the
        // panel itself and half as tall as a full list, with the instance
        // list scrolling inside it and "Create instance" pinned at the
        // foot — a hundred instances can't cover the screen.
        InstanceSwitcherDropdown(
            visible = expanded,
            instances = instances,
            selected = selected,
            media = media,
            menuWidth = panelWidth,
            onSelect = { picked ->
                onSelect(picked)
                expanded = false
            },
            onCreate = {
                expanded = false
                onCreate()
            },
            onDismiss = { expanded = false },
        )
    }
}

/**
 * The instance picker as a real anchored dropdown: [DropdownMenu] anchors
 * the popup under the instance panel instead of centring it like a dialog.
 * The popup takes the panel's own measured width ([menuWidth]) so it reads
 * as an expansion of the panel, not a floating card. Inside, a fixed
 * frame — half the height of a full-screen list — whose upper region
 * scrolls through the instances and whose foot holds the pinned create
 * action. The frame fits only a couple of rows at a time, so every open
 * snaps the list to the selected instance: the one the panel is showing
 * must be on screen without hunting for it.
 */
@Composable
private fun InstanceSwitcherDropdown(
    visible: Boolean,
    instances: List<Instance>,
    selected: Instance,
    media: MediaStorage,
    menuWidth: Dp,
    onSelect: (Instance) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Opening the menu scrolls the list to the selected row. The state
    // lives at this level (not inside the popup) so it survives the
    // menu's enter/exit transitions, and keying the effect on `visible`
    // re-snaps on every open — even a close/reopen mid-animation.
    val listState = rememberLazyListState()
    LaunchedEffect(visible) {
        if (visible) {
            val index = instances.indexOfFirst { it.id == selected.id }
            if (index >= 0) {
                listState.scrollToItem(index)
            }
        }
    }

    DropdownMenu(
        expanded = visible,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(EclipseRadius.panel),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        // Fixed frame: the menu adds its own vertical padding; the list
        // takes the remaining height, the create row never scrolls. The
        // width comes from the panel so the dropdown matches it edge to
        // edge. Only a couple of rows fit, which is why the open snap
        // above matters.
        Column(
            modifier = Modifier
                .width(menuWidth)
                .height(170.dp)
                .padding(horizontal = 6.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(instances, key = { it.id }) { instance ->
                    val isPicked = instance.id == selected.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(instance) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InstanceIcon(instance = instance, media = media, size = 30.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = instance.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = instance.minecraftVersion + " · " + loaderName(instance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        if (isPicked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))

            // Pinned footer: create an instance from anywhere in the list.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(EclipseRadius.tile))
                    .clickable { onCreate() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.instance_create),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/** The full-width play button; squeezes on press, dimmed when disabled. */
@Composable
private fun PlayButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container =
        if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        }
    val content =
        if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(EclipseRadius.panel))
            .background(container)
            .pressSqueeze(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.action_play),
            style = MaterialTheme.typography.titleMedium,
            color = content,
        )
    }
}
