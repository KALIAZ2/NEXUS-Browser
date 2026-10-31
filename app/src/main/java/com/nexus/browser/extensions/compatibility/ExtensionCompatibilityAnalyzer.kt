package com.nexus.browser.extensions.compatibility

import com.nexus.browser.core.security.SecurityPolicy
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class CompatibilityLevel(val labelFr: String, val isInstallableInGecko: Boolean) {
    COMPATIBLE("Compatible GeckoView (WebExtension)", true),
    PARTIALLY_COMPATIBLE("Partiellement compatible (certaines APIs de bureau absentes)", true),
    INCOMPATIBLE_CHROME_CRX("Incompatible : Archive Chrome CRX (moteur Chromium requis)", false),
    INCOMPATIBLE_MANIFEST("Incompatible : APIs Chrome exclusives ou Service Worker non supporté", false),
    INVALID_ARCHIVE("Archive XPI/ZIP invalide ou manifest.json manquant", false)
}

data class ExtensionCompatibilityReport(
    val level: CompatibilityLevel,
    val extensionId: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val manifestVersion: Int,
    val permissions: List<String>,
    val hostPermissions: List<String>,
    val hasContentScripts: Boolean,
    val hasBackgroundScripts: Boolean,
    val usesServiceWorker: Boolean,
    val detectedApis: List<String>,
    val unavailableApis: List<String>,
    val warnings: List<String>,
    val errors: List<String>,
    val permissionRisk: SecurityPolicy.PermissionRiskReport,
    val fileEntries: List<String> = emptyList()
)

@Singleton
class ExtensionCompatibilityAnalyzer @Inject constructor() {

    private val UNSUPPORTED_GECKOVIEW_MOBILE_APIS = setOf(
        "chrome.sidePanel",
        "chrome.offscreen",
        "chrome.tabGroups",
        "chrome.declarativeContent",
        "chrome.desktopCapture",
        "chrome.tabCapture",
        "chrome.debugger",
        "chrome.system.cpu",
        "chrome.system.memory",
        "chrome.system.storage",
        "chrome.tts",
        "chrome.fontSettings",
        "chrome.enterprise",
        "chrome.processes"
    )

    private val SUPPORTED_WEBEXTENSION_APIS = listOf(
        "runtime",
        "storage",
        "tabs",
        "scripting",
        "webRequest",
        "declarativeNetRequest",
        "cookies",
        "alarms",
        "i18n",
        "notifications",
        "permissions",
        "contextMenus"
    )

    /**
     * Analyse un fichier local (.xpi, .zip ou .crx) et retourne un rapport complet de compatibilité.
     */
    fun analyzePackageFile(file: File): ExtensionCompatibilityReport {
        if (!file.exists() || !file.isFile || file.length() < 4L) {
            return invalidReport("Le fichier d'extension est introuvable ou vide.")
        }

        // 1. Détection honnête des fichiers Chrome .crx (Magic header "Cr24")
        val header = ByteArray(4)
        FileInputStream(file).use { fis ->
            fis.read(header)
        }
        val magic = String(header, Charsets.US_ASCII)
        if (magic == "Cr24" || file.name.endsWith(".crx", ignoreCase = true)) {
            return ExtensionCompatibilityReport(
                level = CompatibilityLevel.INCOMPATIBLE_CHROME_CRX,
                extensionId = "crx:${file.name}",
                name = file.nameWithoutExtension,
                version = "Inconnue",
                description = "Extension empaquetée au format Google Chrome (.crx).",
                author = "Inconnu",
                manifestVersion = 3,
                permissions = emptyList(),
                hostPermissions = emptyList(),
                hasContentScripts = false,
                hasBackgroundScripts = false,
                usesServiceWorker = true,
                detectedApis = emptyList(),
                unavailableApis = listOf("Format binaire CRX3 Chrome"),
                warnings = emptyList(),
                errors = listOf(
                    "GeckoView ne prend pas en charge nativement les paquets .crx signés pour Google Chrome.",
                    "Utilisez une extension au format WebExtension .xpi compatible Mozilla Android."
                ),
                permissionRisk = SecurityPolicy.classifyExtensionPermissions(emptyList())
            )
        }

        // 2. Lecture de l'archive ZIP/XPI
        var manifestJsonStr: String? = null
        val fileNames = mutableListOf<String>()
        val jsSnippets = StringBuilder()

        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    fileNames.add(entryName)
                    if (entryName == "manifest.json") {
                        manifestJsonStr = zis.readBytes().toString(Charsets.UTF_8)
                    } else if (entryName.endsWith(".js", ignoreCase = true) && entry.size < 512_000) {
                        val content = zis.readBytes().toString(Charsets.UTF_8)
                        if (jsSnippets.length < 300_000) {
                            jsSnippets.append("\n").append(content)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            return invalidReport("Archive XPI corrompue ou illisible : ${e.localizedMessage}")
        }

        val rawManifest = manifestJsonStr
            ?: return invalidReport("Aucun fichier manifest.json trouvé à la racine de l'archive XPI.")

        return analyzeManifestAndScripts(
            manifestJsonString = rawManifest,
            combinedScripts = jsSnippets.toString(),
            fileEntries = fileNames
        )
    }

    /**
     * Analyse directe d'un manifest.json et du code JavaScript associé (utilisé aussi par le générateur IA et les tests unitaires).
     */
    fun analyzeManifestAndScripts(
        manifestJsonString: String,
        combinedScripts: String = "",
        fileEntries: List<String> = emptyList()
    ): ExtensionCompatibilityReport {
        val json = try {
            JSONObject(manifestJsonString)
        } catch (e: Exception) {
            return invalidReport("Le fichier manifest.json contient une erreur de syntaxe JSON : ${e.localizedMessage}")
        }

        val manifestVersion = json.optInt("manifest_version", -1)
        if (manifestVersion != 2 && manifestVersion != 3) {
            return invalidReport("Version de manifest non supportée ($manifestVersion). Seuls Manifest V2 et Manifest V3 sont reconnus.")
        }

        val name = json.optString("name", "Extension Sans Nom")
        val version = json.optString("version", "1.0.0")
        val description = json.optString("description", "Aucune description fournie.")
        val author = json.optString("author").ifBlank {
            json.optJSONObject("developer")?.optString("name") ?: "Développeur tiers"
        }

        // Extract Gecko extension ID if present
        val bssGeckoId = json.optJSONObject("browser_specific_settings")
            ?.optJSONObject("gecko")
            ?.optString("id")
        val legacyGeckoId = json.optJSONObject("applications")
            ?.optJSONObject("gecko")
            ?.optString("id")
        val resolvedId = when {
            !bssGeckoId.isNullOrBlank() -> bssGeckoId
            !legacyGeckoId.isNullOrBlank() -> legacyGeckoId
            else -> "${name.lowercase().replace(Regex("[^a-z0-9]+"), "-")}@nexus.local"
        }

        val permissions = jsonArrayToList(json.optJSONArray("permissions"))
        val optionalPermissions = jsonArrayToList(json.optJSONArray("optional_permissions"))
        val hostPermissions = jsonArrayToList(json.optJSONArray("host_permissions"))
        val allPermissions = (permissions + optionalPermissions + hostPermissions).distinct()

        val contentScriptsArray = json.optJSONArray("content_scripts")
        val hasContentScripts = contentScriptsArray != null && contentScriptsArray.length() > 0

        val backgroundObj = json.optJSONObject("background")
        val hasBackgroundScripts = backgroundObj?.optJSONArray("scripts")?.let { it.length() > 0 } == true ||
            backgroundObj?.optString("page")?.isNotBlank() == true
        val serviceWorker = backgroundObj?.optString("service_worker", "") ?: ""
        val usesServiceWorker = serviceWorker.isNotBlank()

        val detectedApis = mutableListOf<String>()
        SUPPORTED_WEBEXTENSION_APIS.forEach { api ->
            if (combinedScripts.contains("browser.$api") || combinedScripts.contains("chrome.$api") || api in allPermissions) {
                detectedApis.add(api)
            }
        }

        val unavailableApis = mutableListOf<String>()
        UNSUPPORTED_GECKOVIEW_MOBILE_APIS.forEach { unsupported ->
            val shortName = unsupported.removePrefix("chrome.")
            if (combinedScripts.contains(unsupported) ||
                combinedScripts.contains("browser.$shortName") ||
                shortName in allPermissions
            ) {
                unavailableApis.add(unsupported)
            }
        }

        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        if (bssGeckoId.isNullOrBlank() && legacyGeckoId.isNullOrBlank()) {
            warnings.add("browser_specific_settings.gecko.id absent du manifest.json : requis par GeckoView pour le stockage persistant.")
        }

        if (usesServiceWorker && !hasBackgroundScripts) {
            warnings.add("Ce Manifest V3 utilise background.service_worker sans background.scripts de repli pour Gecko.")
        }

        if (unavailableApis.isNotEmpty()) {
            warnings.add("APIs non disponibles sur GeckoView mobile détectées : ${unavailableApis.joinToString(", ")}.")
        }

        val compatibilityLevel = when {
            unavailableApis.any { it in listOf("chrome.sidePanel", "chrome.offscreen", "chrome.debugger", "chrome.desktopCapture") } &&
                !hasContentScripts -> {
                errors.add("Cette extension repose exclusivement sur des APIs Chrome de bureau absentes de GeckoView Android.")
                CompatibilityLevel.INCOMPATIBLE_MANIFEST
            }
            unavailableApis.isNotEmpty() || (usesServiceWorker && !hasBackgroundScripts) -> {
                CompatibilityLevel.PARTIALLY_COMPATIBLE
            }
            else -> CompatibilityLevel.COMPATIBLE
        }

        return ExtensionCompatibilityReport(
            level = compatibilityLevel,
            extensionId = resolvedId,
            name = name,
            version = version,
            description = description,
            author = author,
            manifestVersion = manifestVersion,
            permissions = permissions,
            hostPermissions = hostPermissions,
            hasContentScripts = hasContentScripts,
            hasBackgroundScripts = hasBackgroundScripts,
            usesServiceWorker = usesServiceWorker,
            detectedApis = detectedApis,
            unavailableApis = unavailableApis,
            warnings = warnings,
            errors = errors,
            permissionRisk = SecurityPolicy.classifyExtensionPermissions(allPermissions),
            fileEntries = fileEntries
        )
    }

    private fun jsonArrayToList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val v = array.optString(i)
            if (v.isNotBlank()) list.add(v)
        }
        return list
    }

    private fun invalidReport(reason: String): ExtensionCompatibilityReport {
        return ExtensionCompatibilityReport(
            level = CompatibilityLevel.INVALID_ARCHIVE,
            extensionId = "invalid",
            name = "Extension Invalide",
            version = "0.0.0",
            description = reason,
            author = "Inconnu",
            manifestVersion = 0,
            permissions = emptyList(),
            hostPermissions = emptyList(),
            hasContentScripts = false,
            hasBackgroundScripts = false,
            usesServiceWorker = false,
            detectedApis = emptyList(),
            unavailableApis = emptyList(),
            warnings = emptyList(),
            errors = listOf(reason),
            permissionRisk = SecurityPolicy.classifyExtensionPermissions(emptyList())
        )
    }
}
