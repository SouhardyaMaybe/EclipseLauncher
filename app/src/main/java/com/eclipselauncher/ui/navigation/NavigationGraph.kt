package com.eclipselauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eclipselauncher.ui.accounts.AccountsScreen
import com.eclipselauncher.ui.createinstance.CreateInstanceScreen
import com.eclipselauncher.ui.createinstance.InstanceEditorScreen
import com.eclipselauncher.ui.createinstance.LoaderPickerScreen
import com.eclipselauncher.ui.home.HomeScreen
import com.eclipselauncher.ui.settings.SettingsScreen
import com.eclipselauncher.ui.tools.ToolsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        
        composable("accounts") {
            AccountsScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        
        composable("create_instance") {
            CreateInstanceScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        
        composable(
            route = "create_instance/loader_picker/{version}",
            arguments = listOf(androidx.navigation.navArgument("version") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val version = backStackEntry.getString() ?: ""
            LoaderPickerScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        
        composable(
            route = "create_instance/editor/{instanceId}",
            arguments = listOf(androidx.navigation.navArgument("instanceId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val instanceId = backStackEntry.getString()
            InstanceEditorScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        
        composable("settings") {
            SettingsScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        
        composable("tools") {
            ToolsScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
    }
}

sealed class NavigationDestination(val route: String) {
    object Home : NavigationDestination("home")
    object Accounts : NavigationDestination("accounts")
    object CreateInstance : NavigationDestination("create_instance")
    object Settings : NavigationDestination("settings")
    object Tools : NavigationDestination("tools")
    
    // Create Instance sub-screens
    data class LoaderPicker(val version: String) : NavigationDestination("create_instance/loader_picker/$version")
    data class InstanceEditor(val instanceId: String?) : NavigationDestination("create_instance/editor/${instanceId ?: "new"}")
    
    // Accounts sub-screens
    object MicrosoftSignIn : NavigationDestination("accounts/microsoft_signin")
    object SkinAndCape : NavigationDestination("accounts/skin_cape")
    object CreateLocalAccount : NavigationDestination("accounts/create_local")
    
    // Settings sub-screens
    object VideoRenderer : NavigationDestination("settings/video_renderer")
    object ControlCustomization : NavigationDestination("settings/control_customization")
    object JavaTweaks : NavigationDestination("settings/java_tweaks")
    object Miscellaneous : NavigationDestination("settings/miscellaneous")
    object Experimental : NavigationDestination("settings/experimental")
    
    // Tools sub-screens
    object ModMarket : NavigationDestination("tools/mod_market")
    object FileManager : NavigationDestination("tools/file_manager")
    object ManageContent : NavigationDestination("tools/manage_content")
    object ManageLogs : NavigationDestination("tools/manage_logs")
    object ExecuteJar : NavigationDestination("tools/execute_jar")
    object EditControls : NavigationDestination("tools/edit_controls")
}