package com.inventory.industry

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.inventory.industry.data.IndustryDatabase
import com.inventory.industry.data.InventoryRepository
import com.inventory.industry.ui.layout.AppShell
import com.inventory.industry.ui.theme.AppTheme

fun main() =
    application {
        IndustryDatabase.connectAndMigrate()
        val repo = InventoryRepository()
        Window(
            onCloseRequest = ::exitApplication,
            title = "Inventario · Postes de luz de madera",
            state = rememberWindowState(size = DpSize(1280.dp, 800.dp)),
        ) {
            AppTheme {
                AppShell(repo)
            }
        }
    }
