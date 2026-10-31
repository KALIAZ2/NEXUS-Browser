package com.nexus.browser.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.browser.BuildConfig
import com.nexus.browser.browser.permissions.SitePermissionManager
import com.nexus.browser.core.common.SearchEngine
import com.nexus.browser.core.database.ExtensionMetadataEntity
import com.nexus.browser.core.database.SitePermissionEntity
import com.nexus.browser.core.designsystem.LocalNexusPalette
import com.nexus.browser.core.designsystem.NexusBrandLogo
import com.nexus.browser.core.designsystem.NexusButton
import com.nexus.browser.core.designsystem.NexusColors
import com.nexus.browser.core.designsystem.NexusDialog
import com.nexus.browser.core.designsystem.NexusIconButton
import com.nexus.browser.core.designsystem.NexusWordmark
import com.nexus.browser.core.designsystem.NexusSettingsItem
import com.nexus.browser.core.designsystem.NexusTextField
import com.nexus.browser.core.navigation.NexusDestination
import com.nexus.browser.core.preferences.NexusSettingsState
import com.nexus.browser.extensions.permissions.ExtensionPermissionsFormatter
import com.nexus.browser.settings.SettingsViewModel
import com.nexus.browser.updates.AppUpdateStatus

enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    GENERAL(
        title = "Général",
        subtitle = "Page d'accueil, moteur de recherche, nouveaux onglets, langue et taille du texte",
        icon = Icons.Default.Settings
    ),
    APPEARANCE(
        title = "Apparence",
        subtitle = "Thème sombre/clair/auto, couleur d'accentuation, barre d'adresse et écran d'accueil",
        icon = Icons.Default.Palette
    ),
    NAVIGATION(
        title = "Navigation",
        subtitle = "Gestion des onglets, ouverture des liens, arrière-plan, barre d'adresse et plein écran",
        icon = Icons.Default.Explore
    ),
    PRIVACY_SECURITY(
        title = "Confidentialité et sécurité",
        subtitle = "Navigation privée, effacement des données, cookies, anti-pistage et permissions des sites",
        icon = Icons.Default.Shield
    ),
    DOWNLOADS(
        title = "Téléchargements",
        subtitle = "Dossier de destination, confirmation avant téléchargement, historique et sécurité APK",
        icon = Icons.Default.Download
    ),
    EXTENSIONS(
        title = "Extensions",
        subtitle = "Extensions installées, activation, permissions, suppression et boutique WebExtensions",
        icon = Icons.Default.Extension
    ),
    ABOUT(
        title = "À propos de NEXUS",
        subtitle = "Mises à jour automatisées GitHub, version installée, moteur GeckoView et licences",
        icon = Icons.Default.Info
    )
}

@Composable
fun SettingsScreen(
    settings: NexusSettingsState,
    viewModel: SettingsViewModel,
    sitePermissions: List<SitePermissionEntity> = emptyList(),
    installedExtensions: List<ExtensionMetadataEntity> = emptyList(),
    downloadsCount: Int = 0,
    onToggleExtension: (String, Boolean) -> Unit = { _, _ -> },
    onUninstallExtension: (String) -> Unit = {},
    onOpenDestination: (NexusDestination) -> Unit = {},
    onOpenNewPrivateTab: () -> Unit = {},
    onReloadCurrentTab: () -> Unit = {},
    onToggleBrowserFullScreen: () -> Unit = {},
    onOpenSystemDownloadsFolder: () -> Unit = {},
    onShowBanner: (String) -> Unit = {},
    onOpenPrivacyScreen: () -> Unit = {},
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // En-tête dynamique
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexusIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Retour",
                onClick = {
                    if (selectedCategory != null) {
                        selectedCategory = null
                    } else {
                        onBack()
                    }
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedCategory?.title ?: "Paramètres",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedCategory != null) {
                    Text(
                        text = "Paramètres NEXUS Browser",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val currentCat = selectedCategory
        if (currentCat == null) {
            // Page principale des paramètres : liste claire des 7 catégories
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // En-tête de marque NEXUS avec le logo unifié
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, palette.border),
                        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NexusBrandLogo(size = 44.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "NEXUS Browser",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Version ${BuildConfig.VERSION_NAME} • Moteur Mozilla GeckoView",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                items(SettingsCategory.entries) { category ->
                    SettingsCategoryCard(
                        category = category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        } else {
            when (currentCat) {
                SettingsCategory.GENERAL -> GeneralSettingsContent(
                    settings = settings,
                    viewModel = viewModel,
                    onShowBanner = onShowBanner
                )
                SettingsCategory.APPEARANCE -> AppearanceSettingsContent(
                    settings = settings,
                    viewModel = viewModel
                )
                SettingsCategory.NAVIGATION -> NavigationSettingsContent(
                    settings = settings,
                    viewModel = viewModel,
                    onOpenTabs = { onOpenDestination(NexusDestination.TABS) },
                    onReloadCurrentTab = {
                        onReloadCurrentTab()
                        onShowBanner("Actualisation de l'onglet actif lancée")
                    },
                    onToggleBrowserFullScreen = {
                        onToggleBrowserFullScreen()
                        onBack()
                    }
                )
                SettingsCategory.PRIVACY_SECURITY -> PrivacyAndSecurityContent(
                    settings = settings,
                    sitePermissions = sitePermissions,
                    viewModel = viewModel,
                    onOpenNewPrivateTab = {
                        onOpenNewPrivateTab()
                        onBack()
                    },
                    onShowBanner = onShowBanner
                )
                SettingsCategory.DOWNLOADS -> DownloadsSettingsContent(
                    settings = settings,
                    viewModel = viewModel,
                    downloadsCount = downloadsCount,
                    onOpenDownloadsScreen = { onOpenDestination(NexusDestination.DOWNLOADS) },
                    onOpenApkManagerScreen = { onOpenDestination(NexusDestination.APK_MANAGER) },
                    onOpenSystemDownloadsFolder = onOpenSystemDownloadsFolder
                )
                SettingsCategory.EXTENSIONS -> ExtensionsSettingsContent(
                    settings = settings,
                    viewModel = viewModel,
                    installedExtensions = installedExtensions,
                    onToggleExtension = onToggleExtension,
                    onUninstallExtension = onUninstallExtension,
                    onOpenExtensionsManager = { onOpenDestination(NexusDestination.EXTENSIONS) },
                    onOpenExtensionStore = { onOpenDestination(NexusDestination.EXTENSION_STORE) }
                )
                SettingsCategory.ABOUT -> AboutNexusSettingsContent(
                    settings = settings,
                    viewModel = viewModel,
                    onShowBanner = onShowBanner
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = category.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ouvrir",
                tint = palette.textSecondary
            )
        }
    }
}

// ============================================================================
// 1. GÉNÉRAL
// ============================================================================
@Composable
private fun GeneralSettingsContent(
    settings: NexusSettingsState,
    viewModel: SettingsViewModel,
    onShowBanner: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    var showCustomHomepageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSectionTitle("Page d'accueil par défaut")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Page d'accueil actuelle : ${settings.homePageUrl}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.homePageUrl == "nexus://home",
                            onClick = { viewModel.setHomePageUrl("nexus://home") },
                            label = { Text("Accueil NEXUS") }
                        )
                        FilterChip(
                            selected = settings.homePageUrl == "https://www.google.com",
                            onClick = { viewModel.setHomePageUrl("https://www.google.com") },
                            label = { Text("Google") }
                        )
                        FilterChip(
                            selected = settings.homePageUrl == "https://duckduckgo.com",
                            onClick = { viewModel.setHomePageUrl("https://duckduckgo.com") },
                            label = { Text("DuckDuckGo") }
                        )
                        FilterChip(
                            selected = settings.homePageUrl !in listOf(
                                "nexus://home",
                                "https://www.google.com",
                                "https://duckduckgo.com"
                            ),
                            onClick = { showCustomHomepageDialog = true },
                            label = { Text("URL personnalisée...") }
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("Moteur de recherche")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Moteur utilisé dans la barre d'adresse et sur l'accueil",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchEngine.entries.forEach { engine ->
                            FilterChip(
                                selected = settings.searchEngine == engine,
                                onClick = { viewModel.setSearchEngine(engine) },
                                label = { Text(engine.displayName) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("Ouverture des nouveaux onglets")
        }
        item {
            NexusSettingsItem(
                title = "Ouvrir les liens externes dans un nouvel onglet",
                subtitle = "Crée automatiquement un nouvel onglet pour les liens provenant d'autres applications",
                icon = Icons.Default.Tab,
                trailingContent = {
                    Switch(
                        checked = settings.openLinksInNewTab,
                        onCheckedChange = { viewModel.setOpenLinksInNewTab(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Restaurer les onglets ouverts au démarrage",
                subtitle = "Rétablit votre session de navigation à l'ouverture de NEXUS",
                icon = Icons.Default.Public,
                trailingContent = {
                    Switch(
                        checked = settings.restoreTabsOnStartup,
                        onCheckedChange = { viewModel.setRestoreTabsOnStartup(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Langue de l'application")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Langue d'interface et d'en-tête HTTP Accept-Language",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "FR" to "Français (Par défaut)",
                            "EN" to "English",
                            "SYSTEM" to "Automatique (Système)"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = settings.appLanguage == code,
                                onClick = {
                                    viewModel.setAppLanguage(code)
                                    onShowBanner("Langue définie sur : $label")
                                },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("Apparence générale")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Échelle du texte de l'interface : ${settings.textScalePercent}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = settings.textScalePercent.toFloat(),
                        onValueChange = { viewModel.setTextScalePercent(it.toInt()) },
                        valueRange = 85f..135f
                    )
                }
            }
        }
    }

    if (showCustomHomepageDialog) {
        var customUrl by remember { mutableStateOf(settings.homePageUrl) }
        NexusDialog(
            title = "Définir la page d'accueil",
            onDismissRequest = { showCustomHomepageDialog = false },
            onConfirm = {
                viewModel.setHomePageUrl(customUrl.ifBlank { "nexus://home" })
                showCustomHomepageDialog = false
            }
        ) {
            NexusTextField(
                value = customUrl,
                onValueChange = { customUrl = it },
                label = "URL ou nexus://home",
                placeholder = "https://..."
            )
        }
    }
}

// ============================================================================
// 2. APPARENCE
// ============================================================================
@Composable
private fun AppearanceSettingsContent(
    settings: NexusSettingsState,
    viewModel: SettingsViewModel
) {
    val palette = LocalNexusPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSectionTitle("Thème de l'application")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Choisissez le mode d'affichage de NEXUS Browser",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "DARK" to "Mode sombre",
                            "LIGHT" to "Mode clair",
                            "SYSTEM" to "Automatique (Android)"
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = settings.themeMode == id,
                                onClick = { viewModel.setThemeMode(id) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("Couleur d'accentuation")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Personnalisez la teinte principale des boutons, puces et indicateurs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    val accents = listOf(
                        "#1E90FF" to "Bleu NEXUS",
                        "#8B5CF6" to "Violet Cosmique",
                        "#00E5FF" to "Cyan Électrique",
                        "#10B981" to "Émeraude",
                        "#F59E0B" to "Ambre",
                        "#EC4899" to "Magenta"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        accents.forEach { (hex, name) ->
                            val isSelected = settings.accentHex.equals(hex, ignoreCase = true)
                            val colorObj = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                .getOrDefault(NexusColors.BrandCyan)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.setAccentHex(hex) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(colorObj)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else palette.border,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) palette.primary else palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("Personnalisation de la barre de navigation")
        }
        item {
            NexusSettingsItem(
                title = "Placer la barre d'adresse en bas de l'écran",
                subtitle = "Rapproche la barre d'URL du pouce pour une navigation à une main",
                icon = Icons.Default.Palette,
                trailingContent = {
                    Switch(
                        checked = settings.toolbarAtBottom,
                        onCheckedChange = { viewModel.setToolbarAtBottom(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Personnalisation de l'écran d'accueil")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Nombre de colonnes de raccourcis : ${settings.homeShortcutColumns}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 4, 5).forEach { colCount ->
                            FilterChip(
                                selected = settings.homeShortcutColumns == colCount,
                                onClick = { viewModel.setHomeShortcutColumns(colCount) },
                                label = { Text("$colCount colonnes") }
                            )
                        }
                    }
                }
            }
        }
        item {
            NexusSettingsItem(
                title = "Afficher les cartes d'accès rapide",
                subtitle = "Boutons directs vers vos onglets ouverts et vos téléchargements",
                icon = Icons.Default.Home,
                trailingContent = {
                    Switch(
                        checked = settings.showHomeQuickAccess,
                        onCheckedChange = { viewModel.setHomeQuickAccess(it) }
                    )
                }
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Style visuel du fond d'écran d'accueil",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "COSMIC" to "Ondes Cosmiques NEXUS",
                            "AURORA" to "Aurore Boréale",
                            "MINIMAL" to "Minimaliste"
                        ).forEach { (styleId, label) ->
                            FilterChip(
                                selected = settings.homeBackgroundStyle == styleId,
                                onClick = { viewModel.setHomeBackgroundStyle(styleId) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
        item {
            NexusSettingsItem(
                title = "Afficher les raccourcis rapides sur l'accueil",
                subtitle = "Grille de sites favoris personnalisables sous la barre de recherche",
                icon = Icons.Default.Home,
                trailingContent = {
                    Switch(
                        checked = settings.showHomeShortcuts,
                        onCheckedChange = {
                            viewModel.setHomeSections(
                                shortcuts = it,
                                bookmarks = settings.showHomeBookmarks,
                                recentHistory = settings.showHomeRecentHistory
                            )
                        }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Afficher les favoris sur l'accueil",
                subtitle = "Accès rapide à vos favoris enregistrés",
                icon = Icons.Default.Home,
                trailingContent = {
                    Switch(
                        checked = settings.showHomeBookmarks,
                        onCheckedChange = {
                            viewModel.setHomeSections(
                                shortcuts = settings.showHomeShortcuts,
                                bookmarks = it,
                                recentHistory = settings.showHomeRecentHistory
                            )
                        }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Afficher l'historique récent sur l'accueil",
                subtitle = "Affiche les dernières pages visitées en bas de la page d'accueil",
                icon = Icons.Default.Home,
                trailingContent = {
                    Switch(
                        checked = settings.showHomeRecentHistory,
                        onCheckedChange = {
                            viewModel.setHomeSections(
                                shortcuts = settings.showHomeShortcuts,
                                bookmarks = settings.showHomeBookmarks,
                                recentHistory = it
                            )
                        }
                    )
                }
            )
        }
    }
}

// ============================================================================
// 3. NAVIGATION
// ============================================================================
@Composable
private fun NavigationSettingsContent(
    settings: NexusSettingsState,
    viewModel: SettingsViewModel,
    onOpenTabs: () -> Unit,
    onReloadCurrentTab: () -> Unit,
    onToggleBrowserFullScreen: () -> Unit
) {
    val palette = LocalNexusPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSectionTitle("Gestion des onglets & Présentation")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Mode d'affichage des onglets : ${if (settings.tabsLayoutMode == "LIST") "Liste compacte" else "Grille visuelle"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.tabsLayoutMode == "GRID",
                            onClick = { viewModel.setTabsLayoutMode("GRID") },
                            label = { Text("Grille visuelle (Défaut)") }
                        )
                        FilterChip(
                            selected = settings.tabsLayoutMode == "LIST",
                            onClick = { viewModel.setTabsLayoutMode("LIST") },
                            label = { Text("Liste compacte") }
                        )
                    }
                }
            }
        }
        item {
            NexusSettingsItem(
                title = "Restaurer les onglets à l'ouverture",
                subtitle = "Conserve vos onglets actifs après la fermeture de l'application",
                icon = Icons.Default.Tab,
                trailingContent = {
                    Switch(
                        checked = settings.restoreTabsOnStartup,
                        onCheckedChange = { viewModel.setRestoreTabsOnStartup(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Ouvrir le gestionnaire d'onglets et de sessions",
                subtitle = "Gérer les onglets ouverts, privés et rouvrir les onglets récemment fermés",
                icon = Icons.Default.Tab,
                onClick = onOpenTabs
            )
        }

        item {
            SettingsSectionTitle("Ouverture des liens & Affichage des pages")
        }
        item {
            NexusSettingsItem(
                title = "Ouvrir les liens externes dans un nouvel onglet",
                subtitle = "Évite de remplacer la page en cours lors d'un clic depuis une autre application",
                icon = Icons.Default.Public,
                trailingContent = {
                    Switch(
                        checked = settings.openLinksInNewTab,
                        onCheckedChange = { viewModel.setOpenLinksInNewTab(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Version ordinateur (Desktop) par défaut",
                subtitle = "Demande automatiquement la version PC des sites web aux serveurs",
                icon = Icons.Default.Public,
                trailingContent = {
                    Switch(
                        checked = settings.desktopModeDefault,
                        onCheckedChange = { viewModel.setDesktopModeDefault(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Actualisation des pages & Moteur JavaScript")
        }
        item {
            NexusSettingsItem(
                title = "Autoriser l'exécution JavaScript",
                subtitle = "Requis pour les applications web modernes et les lecteurs vidéo",
                icon = Icons.Default.Refresh,
                trailingContent = {
                    Switch(
                        checked = settings.javaScriptEnabled,
                        onCheckedChange = { viewModel.setJavaScriptEnabled(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Actualiser la page active maintenant",
                subtitle = "Force le rechargement réseau de l'onglet en cours",
                icon = Icons.Default.Refresh,
                onClick = onReloadCurrentTab
            )
        }

        item {
            SettingsSectionTitle("Navigation en arrière-plan")
        }
        item {
            NexusSettingsItem(
                title = "Suspendre les onglets inactifs en arrière-plan",
                subtitle = "Libère automatiquement les sessions GeckoView en cas de forte pression mémoire",
                icon = Icons.Default.Explore,
                trailingContent = {
                    Switch(
                        checked = settings.suspendInactiveTabs,
                        onCheckedChange = { viewModel.setSuspendInactiveTabs(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Comportement de la barre d'adresse")
        }
        item {
            NexusSettingsItem(
                title = "Afficher les suggestions automatiques",
                subtitle = "Propose vos favoris, votre historique et vos raccourcis pendant la saisie",
                icon = Icons.Default.Search,
                trailingContent = {
                    Switch(
                        checked = settings.addressBarSuggestions,
                        onCheckedChange = { viewModel.setAddressBarSuggestions(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Paramètres du plein écran")
        }
        item {
            NexusSettingsItem(
                title = "Immersion totale en plein écran",
                subtitle = "Masque la barre d'état Android, la barre de navigation système et les barres NEXUS",
                icon = Icons.Default.Fullscreen,
                trailingContent = {
                    Switch(
                        checked = settings.fullScreenHideBars,
                        onCheckedChange = { viewModel.setFullScreenHideBars(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Basculer le navigateur en plein écran maintenant",
                subtitle = "Affiche la page web sur 100% de l'écran avec bouton flottant de sortie",
                icon = Icons.Default.Fullscreen,
                onClick = onToggleBrowserFullScreen
            )
        }
    }
}

// ============================================================================
// 4. CONFIDENTIALITÉ ET SÉCURITÉ
// ============================================================================
@Composable
private fun PrivacyAndSecurityContent(
    settings: NexusSettingsState,
    sitePermissions: List<SitePermissionEntity>,
    viewModel: SettingsViewModel,
    onOpenNewPrivateTab: () -> Unit,
    onShowBanner: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    var clearHistory by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var clearSiteData by remember { mutableStateOf(true) }
    var clearPermissions by remember { mutableStateOf(false) }
    var showAddPermissionDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSectionTitle("Navigation privée & Chiffrement")
        }
        item {
            NexusSettingsItem(
                title = "Ouvrir un nouvel onglet de navigation privée",
                subtitle = "Aucun historique, cookie ni cache n'est conservé après fermeture de l'onglet",
                icon = Icons.Default.Shield,
                onClick = onOpenNewPrivateTab
            )
        }
        item {
            NexusSettingsItem(
                title = "Mode HTTPS uniquement",
                subtitle = "Redirige automatiquement les connexions HTTP non chiffrées vers HTTPS",
                icon = Icons.Default.Lock,
                trailingContent = {
                    Switch(
                        checked = settings.httpsOnlyMode,
                        onCheckedChange = { viewModel.setHttpsOnlyMode(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Protection contre le pistage (GeckoView ContentBlocking)")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Bloque les traqueurs publicitaires, analytiques, sociaux et les scripts de cryptominage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "STANDARD" to "Protection Standard (Équilibrée)",
                            "STRICT" to "Protection Stricte (Maximale)"
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = settings.trackingProtectionLevel == id,
                                onClick = { viewModel.setTrackingProtection(id) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("Politique et suppression des cookies")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    listOf(
                        "REJECT_TRACKERS" to "Bloquer les cookies de pistage inter-sites (Recommandé)",
                        "REJECT_THIRD_PARTY" to "Bloquer tous les cookies tiers",
                        "ACCEPT_ALL" to "Autoriser tous les cookies"
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = settings.cookiePolicy == id,
                            onClick = { viewModel.setCookiePolicy(id) },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NexusButton(
                            text = "Supprimer les cookies",
                            icon = Icons.Default.Cookie,
                            isSecondary = true,
                            onClick = {
                                viewModel.clearBrowsingData(
                                    clearHistory = false,
                                    clearCookies = true,
                                    clearCache = false,
                                    clearSiteData = false,
                                    clearPermissions = false
                                )
                                onShowBanner("Tous les cookies ont été supprimés")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        NexusButton(
                            text = "Effacer l'historique",
                            icon = Icons.Default.DeleteForever,
                            isSecondary = true,
                            onClick = {
                                viewModel.clearBrowsingData(
                                    clearHistory = true,
                                    clearCookies = false,
                                    clearCache = false,
                                    clearSiteData = false,
                                    clearPermissions = false
                                )
                                onShowBanner("Historique de navigation effacé")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            SettingsSectionTitle("Gestion des données des sites & Nettoyage complet")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    CheckOptionRow("Historique de navigation", clearHistory) { clearHistory = it }
                    CheckOptionRow("Cookies et sessions actives", clearCookies) { clearCookies = it }
                    CheckOptionRow("Cache réseau et images", clearCache) { clearCache = it }
                    CheckOptionRow("Données des sites (DOM Storage / IndexedDB)", clearSiteData) { clearSiteData = it }
                    CheckOptionRow("Autorisations accordées aux sites", clearPermissions) { clearPermissions = it }
                    Spacer(Modifier.height(10.dp))
                    NexusButton(
                        text = "Effacer les données sélectionnées",
                        icon = Icons.Default.DeleteForever,
                        onClick = {
                            viewModel.clearBrowsingData(
                                clearHistory = clearHistory,
                                clearCookies = clearCookies,
                                clearCache = clearCache,
                                clearSiteData = clearSiteData,
                                clearPermissions = clearPermissions
                            )
                            onShowBanner("Données de navigation sélectionnées effacées")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsSectionTitle("Gestion des autorisations des sites (${sitePermissions.size})")
                TextButton(onClick = { showAddPermissionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ajouter un site")
                }
            }
        }

        if (sitePermissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.border),
                    colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Aucune permission spécifique enregistrée",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Lorsqu'un site demande l'accès à la caméra, au microphone, à la localisation, aux notifications ou au stockage, votre choix apparaît ici et peut être modifié ou révoqué à tout moment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            items(sitePermissions, key = { it.id }) { perm ->
                SitePermissionRowCard(
                    permission = perm,
                    onToggle = { viewModel.toggleSitePermission(perm.id, !perm.granted) },
                    onDelete = { viewModel.deleteSitePermission(perm.id) }
                )
            }
        }
    }

    if (showAddPermissionDialog) {
        AddSitePermissionDialog(
            onDismiss = { showAddPermissionDialog = false },
            onSave = { origin, type, granted ->
                viewModel.addOrUpdateSitePermission(origin, type, granted)
                showAddPermissionDialog = false
                onShowBanner("Autorisation enregistrée pour $origin")
            }
        )
    }
}

@Composable
private fun SitePermissionRowCard(
    permission: SitePermissionEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, palette.border),
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = if (permission.granted) NexusColors.Success else palette.error
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permission.origin,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${SitePermissionManager.formatPermissionTypeFr(permission.permissionType)} • ${
                        if (permission.granted) "Autorisé" else "Bloqué"
                    }",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (permission.granted) NexusColors.Success else palette.error
                )
            }
            Switch(
                checked = permission.granted,
                onCheckedChange = { onToggle() }
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Révoquer",
                    tint = palette.error
                )
            }
        }
    }
}

@Composable
private fun AddSitePermissionDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var originInput by remember { mutableStateOf("https://") }
    var selectedType by remember { mutableStateOf("GEOLOCATION") }
    var granted by remember { mutableStateOf(false) }

    val types = listOf(
        "CAMERA" to "Caméra",
        "MICROPHONE" to "Microphone",
        "GEOLOCATION" to "Localisation",
        "NOTIFICATIONS" to "Notifications",
        "STORAGE" to "Stockage"
    )

    NexusDialog(
        title = "Autorisation d'un site Web",
        onDismissRequest = onDismiss,
        confirmText = "Enregistrer",
        onConfirm = {
            if (originInput.isNotBlank()) {
                onSave(originInput.trim(), selectedType, granted)
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NexusTextField(
                value = originInput,
                onValueChange = { originInput = it },
                label = "Domaine ou URL du site",
                placeholder = "https://exemple.com"
            )
            Text("Type d'autorisation :", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                types.forEach { (id, label) ->
                    FilterChip(
                        selected = selectedType == id,
                        onClick = { selectedType = id },
                        label = { Text(label) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (granted) "Statut : Autoriser l'accès" else "Statut : Bloquer l'accès")
                Switch(checked = granted, onCheckedChange = { granted = it })
            }
        }
    }
}

// ============================================================================
// 5. TÉLÉCHARGEMENTS
// ============================================================================
@Composable
private fun DownloadsSettingsContent(
    settings: NexusSettingsState,
    viewModel: SettingsViewModel,
    downloadsCount: Int,
    onOpenDownloadsScreen: () -> Unit,
    onOpenApkManagerScreen: () -> Unit,
    onOpenSystemDownloadsFolder: () -> Unit
) {
    val palette = LocalNexusPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSectionTitle("Dossier de téléchargement")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Emplacement actif : ${settings.downloadDirectoryLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Downloads/NEXUS",
                            "Downloads",
                            "Documents/NEXUS"
                        ).forEach { folder ->
                            FilterChip(
                                selected = settings.downloadDirectoryLabel == folder,
                                onClick = { viewModel.setDownloadDirectoryLabel(folder) },
                                label = { Text(folder) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    NexusButton(
                        text = "Ouvrir le dossier Téléchargements Android",
                        icon = Icons.Default.FolderOpen,
                        isSecondary = true,
                        onClick = onOpenSystemDownloadsFolder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            SettingsSectionTitle("Comportement des téléchargements")
        }
        item {
            NexusSettingsItem(
                title = "Demander confirmation avant téléchargement",
                subtitle = "Affiche le nom et la taille du fichier avant de lancer le téléchargement",
                icon = Icons.Default.Download,
                trailingContent = {
                    Switch(
                        checked = settings.askBeforeDownload,
                        onCheckedChange = { viewModel.setAskBeforeDownload(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Notifications de progression des téléchargements",
                subtitle = "Affiche la vitesse et l'avancement dans la barre de notification",
                icon = Icons.Default.Download,
                trailingContent = {
                    Switch(
                        checked = settings.downloadNotificationsEnabled,
                        onCheckedChange = { viewModel.setDownloadNotifications(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Ouvrir automatiquement les fichiers terminés",
                subtitle = "Propose l'ouverture dès la fin du téléchargement",
                icon = Icons.Default.Download,
                trailingContent = {
                    Switch(
                        checked = settings.autoOpenCompletedDownloads,
                        onCheckedChange = { viewModel.setAutoOpenDownloads(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Analyser automatiquement les fichiers APK téléchargés",
                subtitle = "Vérifie l'intégrité ZIP, le manifeste Android et la signature SHA-256",
                icon = Icons.Default.Android,
                trailingContent = {
                    Switch(
                        checked = settings.autoAnalyzeDownloadedApks,
                        onCheckedChange = { viewModel.setAutoAnalyzeApks(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Gestion et historique des fichiers téléchargés")
        }
        item {
            NexusSettingsItem(
                title = "Historique et gestionnaire de téléchargements ($downloadsCount)",
                subtitle = "Mettre en pause, reprendre, ouvrir, partager ou supprimer vos fichiers",
                icon = Icons.Default.Download,
                onClick = onOpenDownloadsScreen
            )
        }
        item {
            NexusSettingsItem(
                title = "Gestionnaire d'applications APK",
                subtitle = "Inspecter les permissions et installer vos fichiers APK via Android",
                icon = Icons.Default.Android,
                onClick = onOpenApkManagerScreen
            )
        }
    }
}

// ============================================================================
// 6. EXTENSIONS
// ============================================================================
@Composable
private fun ExtensionsSettingsContent(
    settings: NexusSettingsState,
    viewModel: SettingsViewModel,
    installedExtensions: List<ExtensionMetadataEntity>,
    onToggleExtension: (String, Boolean) -> Unit,
    onUninstallExtension: (String) -> Unit,
    onOpenExtensionsManager: () -> Unit,
    onOpenExtensionStore: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var inspectedExtension by remember { mutableStateOf<ExtensionMetadataEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSectionTitle("Installation et mises à jour")
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NexusButton(
                    text = "Catalogue d'extensions",
                    icon = Icons.Default.Storefront,
                    onClick = onOpenExtensionStore,
                    modifier = Modifier.weight(1f)
                )
                NexusButton(
                    text = "Gérer / .XPI",
                    icon = Icons.Default.Extension,
                    isSecondary = true,
                    onClick = onOpenExtensionsManager,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            NexusSettingsItem(
                title = "Autoriser l'installation de fichiers .XPI locaux",
                subtitle = "Permet d'importer une extension WebExtension depuis le stockage de l'appareil",
                icon = Icons.Default.Extension,
                trailingContent = {
                    Switch(
                        checked = settings.allowLocalXpiInstall,
                        onCheckedChange = { viewModel.setAllowLocalXpiInstall(it) }
                    )
                }
            )
        }
        item {
            NexusSettingsItem(
                title = "Mise à jour automatique des extensions",
                subtitle = "Vérifie périodiquement les nouvelles versions signées sur Mozilla Add-ons",
                icon = Icons.Default.Update,
                trailingContent = {
                    Switch(
                        checked = settings.autoUpdateExtensions,
                        onCheckedChange = { viewModel.setAutoUpdateExtensions(it) }
                    )
                }
            )
        }

        item {
            SettingsSectionTitle("Extensions installées (${installedExtensions.size})")
        }
        items(installedExtensions, key = { it.extensionId }) { ext ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "${ext.name} • v${ext.version}",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ext.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = ext.enabled,
                            onCheckedChange = { onToggleExtension(ext.extensionId, it) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { inspectedExtension = ext }) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Permissions")
                        }
                        if (ext.sourceType != "BUILT_IN") {
                            TextButton(onClick = { onUninstallExtension(ext.extensionId) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = palette.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Supprimer", color = palette.error)
                            }
                        } else {
                            Text(
                                text = "Extension système intégrée",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary
                            )
                        }
                    }
                }
            }
        }
    }

    inspectedExtension?.let { ext ->
        val rawPerms = ext.permissionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val formattedPerms = ExtensionPermissionsFormatter.describePermissions(rawPerms)
        NexusDialog(
            title = "Permissions : ${ext.name}",
            onDismissRequest = { inspectedExtension = null },
            confirmText = "Fermer",
            dismissText = "",
            onConfirm = { inspectedExtension = null }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ID WebExtension : ${ext.extensionId}", fontWeight = FontWeight.SemiBold)
                Text("Version : ${ext.version} • Manifest V${ext.manifestVersion}")
                Spacer(Modifier.height(4.dp))
                if (formattedPerms.isEmpty()) {
                    Text("Aucune permission sensible demandée.")
                } else {
                    formattedPerms.forEach { perm ->
                        Text("• ${perm.titleFr} (${perm.rawPermission}) : ${perm.descriptionFr}")
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. À PROPOS DE NEXUS
// ============================================================================
@Composable
private fun AboutNexusSettingsContent(
    settings: NexusSettingsState,
    viewModel: SettingsViewModel,
    onShowBanner: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    val updateState by viewModel.appUpdateState.collectAsState()
    var showLicensesDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NexusBrandLogo(size = 64.dp)
                    Spacer(Modifier.height(6.dp))
                    NexusWordmark()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "NEXUS Browser v${updateState.installedVersion} (Build ${updateState.installedVersionCode})",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Package Android : ${BuildConfig.APPLICATION_ID}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // Rubrique officielle : À propos de NEXUS → Mises à jour
        item {
            SettingsSectionTitle("Mises à jour")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (updateState.isUpdateAvailable) palette.primary else palette.border
                ),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Mises à jour",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Version installée : ${updateState.installedVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )

                    Text(
                        text = "Dernière version : ${updateState.latestVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )

                    Text(
                        text = "Dernière vérification : ${updateState.formattedLastCheckDate}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )

                    val statusColor = when (updateState.status) {
                        AppUpdateStatus.UPDATE_AVAILABLE,
                        AppUpdateStatus.READY_TO_INSTALL -> palette.primary
                        AppUpdateStatus.UP_TO_DATE -> NexusColors.Success
                        AppUpdateStatus.ERROR -> palette.error
                        else -> palette.textSecondary
                    }

                    Text(
                        text = if (updateState.isUpdateAvailable) {
                            "État : ${updateState.statusMessageFr}"
                        } else if (updateState.status == AppUpdateStatus.UP_TO_DATE) {
                            "NEXUS est à jour."
                        } else {
                            "État : ${updateState.statusMessageFr}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (updateState.isUpdateAvailable && updateState.releaseNotes.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surface)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Nouveautés :",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            updateState.releaseNotes.forEach { note ->
                                Text(
                                    text = "- $note",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    if (updateState.status == AppUpdateStatus.DOWNLOADING) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { (updateState.downloadProgressPercent.coerceIn(0, 100)) / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = palette.primary,
                                trackColor = palette.surface
                            )
                            Text(
                                text = "Téléchargement de l'APK : ${updateState.downloadProgressPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary
                            )
                        }
                    }

                    if (updateState.isUpdateAvailable) {
                        NexusButton(
                            text = when (updateState.status) {
                                AppUpdateStatus.DOWNLOADING -> "Téléchargement en cours..."
                                AppUpdateStatus.READY_TO_INSTALL -> "Installer la mise à jour (v${updateState.latestVersion})"
                                else -> "Télécharger et installer"
                            },
                            icon = Icons.Default.Download,
                            enabled = updateState.status != AppUpdateStatus.DOWNLOADING,
                            onClick = {
                                viewModel.downloadAndInstallAppUpdate(onShowBanner)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.checkForAppUpdates(onShowBanner) },
                        enabled = updateState.status != AppUpdateStatus.CHECKING &&
                            updateState.status != AppUpdateStatus.DOWNLOADING,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (updateState.status == AppUpdateStatus.CHECKING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = palette.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Vérification en cours...", color = palette.textPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Rechercher des mises à jour", color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        item {
            NexusSettingsItem(
                title = "Vérification automatique périodique",
                subtitle = "Vérifie discrètement les nouvelles versions GitHub Releases au lancement (sans surconsommation)",
                icon = Icons.Default.Update,
                trailingContent = {
                    Switch(
                        checked = settings.autoCheckAppUpdates,
                        onCheckedChange = { viewModel.setAutoCheckAppUpdates(it) }
                    )
                },
                onClick = { viewModel.setAutoCheckAppUpdates(!settings.autoCheckAppUpdates) }
            )
        }

        item {
            SettingsSectionTitle("Moteur de navigation")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.border),
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Mozilla GeckoView 130.0",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Moteur de rendu : Mozilla Gecko Quantum (Multi-processus isolé)\n" +
                            "• Extensions : API WebExtensions native (.xpi / Mozilla Add-ons)\n" +
                            "• Sécurité : ContentBlocking anti-pistage, HTTPS-Only, navigation privée isolée\n" +
                            "• Outils intégrés : Mode Lecture, Traduction, Recherche dans la page, Capture PNG haute résolution",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }

        item {
            SettingsSectionTitle("Licences et composants utilisés")
        }
        item {
            NexusSettingsItem(
                title = "Licences des logiciels libres et open-source",
                subtitle = "Mozilla Public License 2.0, Apache License 2.0 (Jetpack Compose, Room, Hilt, Kotlin)",
                icon = Icons.Default.Info,
                onClick = { showLicensesDialog = true }
            )
        }
    }

    if (showLicensesDialog) {
        NexusDialog(
            title = "Composants & Licences Open-Source",
            onDismissRequest = { showLicensesDialog = false },
            confirmText = "Fermer",
            dismissText = "",
            onConfirm = { showLicensesDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Mozilla GeckoView 130.0 — Mozilla Public License 2.0 (MPL-2.0)", fontWeight = FontWeight.SemiBold)
                Text("• AndroidX & Jetpack Compose Material 3 — Apache License 2.0")
                Text("• Kotlin & Kotlin Coroutines / Flow — Apache License 2.0")
                Text("• Room SQLite Database & DataStore Preferences — Apache License 2.0")
                Text("• Dagger Hilt Dependency Injection — Apache License 2.0")
                Text("• OkHttp Network Stack — Apache License 2.0")
            }
        }
    }
}

// ============================================================================
// ÉCRAN DÉDIÉ CONFIDENTIALITÉ & SÉCURITÉ (Accessible aussi depuis le menu)
// ============================================================================
@Composable
fun PrivacySecurityScreen(
    settings: NexusSettingsState,
    sitePermissions: List<SitePermissionEntity>,
    viewModel: SettingsViewModel,
    onOpenNewPrivateTab: () -> Unit = {},
    onShowBanner: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NexusIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Retour",
                onClick = onBack
            )
            Text(
                text = "Confidentialité & Sécurité",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        PrivacyAndSecurityContent(
            settings = settings,
            sitePermissions = sitePermissions,
            viewModel = viewModel,
            onOpenNewPrivateTab = onOpenNewPrivateTab,
            onShowBanner = onShowBanner
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    val palette = LocalNexusPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = palette.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun CheckOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            color = palette.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
