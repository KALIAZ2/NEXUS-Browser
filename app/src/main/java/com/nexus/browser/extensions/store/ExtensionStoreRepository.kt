package com.nexus.browser.extensions.store

import com.nexus.browser.extensions.compatibility.CompatibilityLevel
import com.nexus.browser.extensions.manager.NexusExtensionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

enum class ExtensionStoreCategory(val id: String, val labelFr: String) {
    ALL("all", "Toutes"),
    PRIVACY("privacy", "Confidentialité"),
    SECURITY("security", "Sécurité"),
    PRODUCTIVITY("productivity", "Productivité"),
    CUSTOMIZATION("customization", "Personnalisation"),
    DEVELOPMENT("development", "Développement"),
    TRANSLATION("translation", "Traduction"),
    MEDIA("media", "Médias"),
    UTILITIES("utilities", "Utilitaires")
}

data class StoreExtensionItem(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val category: ExtensionStoreCategory,
    val summary: String,
    val fullDescription: String,
    val xpiDownloadUrl: String,
    val permissions: List<String>,
    val compatibilityLevel: CompatibilityLevel,
    val screenshotsDescriptions: List<String>,
    val rating: Double = 4.8
)

@Singleton
class ExtensionStoreRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val extensionManager: NexusExtensionManager
) {
    /**
     * Catalogue officiel pré-configuré (Mozilla Add-ons Android + Extensions vérifiées NEXUS).
     * Inclut également des exemples clairement identifiés comme non-compatibles (CRX Chrome)
     * conformément à l'exigence de transparence.
     */
    private val curatedCatalog = listOf(
        StoreExtensionItem(
            id = "uBlock0@raymondhill.net",
            name = "uBlock Origin",
            version = "1.60.0",
            author = "Raymond Hill (gorhill)",
            category = ExtensionStoreCategory.PRIVACY,
            summary = "Bloqueur de publicités et de pisteurs ultra-performant et léger en mémoire.",
            fullDescription = "uBlock Origin est un bloqueur de contenu généraliste conçu pour l'efficacité CPU et mémoire sur GeckoView Android.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/ublock-origin/latest.xpi",
            permissions = listOf("<all_urls>", "webRequest", "webRequestBlocking", "storage", "tabs", "privacy"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Tableau de bord de filtrage temps réel", "Sélecteur d'éléments DOM"),
            rating = 4.9
        ),
        StoreExtensionItem(
            id = "addon@darkreader.org",
            name = "Dark Reader",
            version = "4.9.92",
            author = "Dark Reader Ltd",
            category = ExtensionStoreCategory.CUSTOMIZATION,
            summary = "Thème sombre dynamique pour chaque site web. Prenez soin de vos yeux.",
            fullDescription = "Analyse les feuilles de style des pages web en temps réel et génère un mode sombre équilibré avec réglage du contraste et de la luminosité.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/darkreader/latest.xpi",
            permissions = listOf("<all_urls>", "storage", "tabs", "contextMenus"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Contrôle de contraste et sépia", "Liste d'exclusion par site"),
            rating = 4.8
        ),
        StoreExtensionItem(
            id = "{446900e4-71c2-419f-a6a7-df9c091e268b}",
            name = "Bitwarden Gestionnaire de mots de passe",
            version = "2024.8.1",
            author = "Bitwarden Inc.",
            category = ExtensionStoreCategory.SECURITY,
            summary = "Coffre-fort open source sécurisé pour vos identifiants et clés d'accès.",
            fullDescription = "Intègre le remplissage automatique chiffré de bout en bout directement dans vos sessions GeckoView.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/bitwarden-password-manager/latest.xpi",
            permissions = listOf("<all_urls>", "storage", "tabs", "clipboardWrite", "contextMenus"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Coffre chiffré AES-256", "Générateur de mots de passe forts"),
            rating = 4.8
        ),
        StoreExtensionItem(
            id = "jid1-MnnxcxisBPnSXQ@jetpack",
            name = "Privacy Badger",
            version = "2024.7.17",
            author = "Electronic Frontier Foundation (EFF)",
            category = ExtensionStoreCategory.PRIVACY,
            summary = "Apprend automatiquement à bloquer les traqueurs invisibles.",
            fullDescription = "Développé par l'EFF pour détecter algorithmiquement les domaines tiers qui vous pistent sans consentement.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/privacy-badger17/latest.xpi",
            permissions = listOf("<all_urls>", "webRequest", "webRequestBlocking", "storage", "tabs"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Liste des traqueurs bloqués sur l'onglet actif"),
            rating = 4.7
        ),
        StoreExtensionItem(
            id = "{036a55b4-5e72-4d05-a06c-cba2dfcc134a}",
            name = "TWP — Translate Web Pages",
            version = "10.0.1",
            author = "Filipe Ps",
            category = ExtensionStoreCategory.TRANSLATION,
            summary = "Traduit vos pages web en temps réel sans quitter l'onglet.",
            fullDescription = "Traduction instantanée de la page entière ou du texte sélectionné sur mobile.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/traduzir-paginas-web/latest.xpi",
            permissions = listOf("<all_urls>", "storage", "activeTab"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Barre de traduction flottante mobile"),
            rating = 4.9
        ),
        StoreExtensionItem(
            id = "sponsorBlocker@ajay.app",
            name = "SponsorBlock pour YouTube",
            version = "5.8",
            author = "Ajay Ramachandran",
            category = ExtensionStoreCategory.MEDIA,
            summary = "Passe automatiquement les segments sponsorisés dans les vidéos YouTube.",
            fullDescription = "Extension communautaire open source permettant d'ignorer les introductions et sponsors sur YouTube mobile.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/sponsorblock/latest.xpi",
            permissions = listOf("https://*.youtube.com/*", "storage"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Timeline vidéo colorée par segment"),
            rating = 4.9
        ),
        StoreExtensionItem(
            id = "violentmonkey@nexus.store",
            name = "Violentmonkey (Gestionnaire Userscripts)",
            version = "2.21.0",
            author = "gera2ld",
            category = ExtensionStoreCategory.DEVELOPMENT,
            summary = "Exécutez vos scripts utilisateurs personnalisés sur n'importe quel site.",
            fullDescription = "Support complet des scripts Greasemonkey / Tampermonkey sur GeckoView Android.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/violentmonkey/latest.xpi",
            permissions = listOf("<all_urls>", "tabs", "storage", "webRequest", "webRequestBlocking", "notifications"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Éditeur de scripts intégré", "Synchronisation de scripts"),
            rating = 4.8
        ),
        StoreExtensionItem(
            id = "clearurls@kevinr.de",
            name = "ClearURLs",
            version = "1.26.0",
            author = "Kevin Roebert",
            category = ExtensionStoreCategory.UTILITIES,
            summary = "Nettoie automatiquement les éléments de suivi dans les adresses URL.",
            fullDescription = "Supprime plus de 250 règles de paramètres de pistage d'URL en arrière-plan.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/clearurls/latest.xpi",
            permissions = listOf("<all_urls>", "webRequest", "webRequestBlocking", "storage"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Compteur d'URL nettoyées"),
            rating = 4.7
        ),
        StoreExtensionItem(
            id = "singlefile@gildas.lormeau",
            name = "SingleFile — Archivage Web",
            version = "1.22.58",
            author = "Gildas Lormeau",
            category = ExtensionStoreCategory.PRODUCTIVITY,
            summary = "Enregistrez une page web complète (CSS, images, polices) dans un seul fichier HTML.",
            fullDescription = "Outil de productivité permettant d'archiver des articles et documentations pour une consultation hors-ligne fidèle.",
            xpiDownloadUrl = "https://addons.mozilla.org/android/downloads/latest/single-file/latest.xpi",
            permissions = listOf("<all_urls>", "downloads", "storage", "activeTab"),
            compatibilityLevel = CompatibilityLevel.COMPATIBLE,
            screenshotsDescriptions = listOf("Export HTML autonome en un clic"),
            rating = 4.8
        ),
        StoreExtensionItem(
            id = "chrome-desktop-devtools-crx@google.com",
            name = "Chrome Desktop SidePanel Inspector (CRX)",
            version = "3.1.0",
            author = "Chrome Web Store (Format CRX)",
            category = ExtensionStoreCategory.DEVELOPMENT,
            summary = "Extension Chrome utilisant chrome.sidePanel et chrome.debugger (Non compatible GeckoView).",
            fullDescription = "Affichée à titre de transparence : cette extension utilise le format CRX3 et l'API chrome.sidePanel exclusive à Chromium Desktop. NEXUS bloque son installation sur le moteur GeckoView.",
            xpiDownloadUrl = "",
            permissions = listOf("sidePanel", "debugger", "offscreen"),
            compatibilityLevel = CompatibilityLevel.INCOMPATIBLE_CHROME_CRX,
            screenshotsDescriptions = listOf("Requiert un moteur Chromium compatible CRX"),
            rating = 3.9
        )
    )

    fun getCuratedCatalog(
        category: ExtensionStoreCategory = ExtensionStoreCategory.ALL,
        searchQuery: String = "",
        onlyCompatible: Boolean = false
    ): List<StoreExtensionItem> {
        return curatedCatalog.filter { item ->
            val matchesCategory = category == ExtensionStoreCategory.ALL || item.category == category
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.summary.lowercase().contains(q) ||
                item.author.lowercase().contains(q)
            val matchesCompat = !onlyCompatible || item.compatibilityLevel.isInstallableInGecko
            matchesCategory && matchesSearch && matchesCompat
        }
    }

    /**
     * Recherche en direct sur l'API publique officielle Mozilla Add-ons (AMO v5) sans aucun scraping.
     */
    suspend fun searchOfficialMozillaAddons(query: String): List<StoreExtensionItem> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val url = "https://addons.mozilla.org/api/v5/addons/search/?app=android&type=extension&page_size=10&q=$encoded"
                val request = Request.Builder().url(url).get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val bodyStr = response.body?.string() ?: return@use emptyList()
                    val root = JSONObject(bodyStr)
                    val results = root.optJSONArray("results") ?: return@use emptyList()
                    val items = mutableListOf<StoreExtensionItem>()
                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        val guid = obj.optString("guid")
                        if (guid.isBlank()) continue
                        val nameObj = obj.optJSONObject("name")
                        val name = nameObj?.optString("fr")
                            ?: nameObj?.optString("en-US")
                            ?: nameObj?.keys()?.asSequence()?.firstOrNull()?.let { nameObj.optString(it) }
                            ?: guid
                        val summaryObj = obj.optJSONObject("summary")
                        val summary = summaryObj?.optString("fr")
                            ?: summaryObj?.optString("en-US")
                            ?: "Extension officielle Mozilla Add-ons"
                        val currentVersion = obj.optJSONObject("current_version")
                        val version = currentVersion?.optString("version", "1.0") ?: "1.0"
                        val fileObj = currentVersion?.optJSONObject("file")
                        val xpiUrl = fileObj?.optString("url", "") ?: ""
                        if (xpiUrl.isBlank()) continue
                        val permsArray = fileObj?.optJSONArray("permissions")
                        val perms = mutableListOf<String>()
                        if (permsArray != null) {
                            for (p in 0 until permsArray.length()) {
                                perms.add(permsArray.optString(p))
                            }
                        }
                        val authorsArr = obj.optJSONArray("authors")
                        val author = authorsArr?.optJSONObject(0)?.optString("name") ?: "Mozilla Community"
                        val rating = obj.optJSONObject("ratings")?.optDouble("average", 4.5) ?: 4.5

                        items.add(
                            StoreExtensionItem(
                                id = guid,
                                name = name,
                                version = version,
                                author = author,
                                category = ExtensionStoreCategory.UTILITIES,
                                summary = summary,
                                fullDescription = summary,
                                xpiDownloadUrl = xpiUrl,
                                permissions = perms,
                                compatibilityLevel = CompatibilityLevel.COMPATIBLE,
                                screenshotsDescriptions = listOf("Catalogue officiel Mozilla Add-ons Android"),
                                rating = rating
                            )
                        )
                    }
                    items
                }
            }.getOrDefault(emptyList())
        }
    }

    suspend fun installStoreItem(item: StoreExtensionItem): Boolean {
        if (!item.compatibilityLevel.isInstallableInGecko || item.xpiDownloadUrl.isBlank()) {
            return false
        }
        return extensionManager.installFromRemoteXpiUrl(
            downloadUrl = item.xpiDownloadUrl,
            fallbackId = item.id,
            fallbackName = item.name,
            fallbackVersion = item.version,
            fallbackDescription = item.summary,
            fallbackAuthor = item.author,
            fallbackPermissions = item.permissions
        )
    }
}
