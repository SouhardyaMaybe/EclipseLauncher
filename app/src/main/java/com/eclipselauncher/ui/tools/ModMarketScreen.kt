package com.eclipselauncher.ui.tools

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.filled.ArrowBack
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
import com.eclipselauncher.ui.managecontent.components.BrowseResultsList
import com.eclipselauncher.ui.managecontent.components.ProviderPills
import com.eclipselauncher.ui.managecontent.components.SearchField
import com.eclipselauncher.ui.theme.EclipseColors
import com.eclipselauncher.ui.theme.EclipseLauncherTheme

@Composable
fun ModMarketScreen(
    onNavigate: (String) -> Unit
) {
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

                // Bare back arrow + title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ArrowBack,
                            contentDescription = "Back",
                            tint = EclipseColors.DarkPrimary,
                            modifier = Modifier
                                .size(48.dp),
                            onClick = { onNavigate("tools") }
                        )
                        Text(
                            text = "Mod market",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EclipseColors.DarkPrimary
                        )
                    }
                }

                // Provider pills
                ProviderPills(
                    selectedProvider = com.eclipselauncher.ui.managecontent.ManageContentViewModel.Provider.MODRINTH,
                    onProviderSelect = { /* Only Modrinth supported */ }
                )

                // Search field
                var searchText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                SearchField(
                    query = searchText,
                    onQueryChange = { searchText = it },
                    onSearchClick = { /* Perform search */ }
                )

                // Results placeholder
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
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
}