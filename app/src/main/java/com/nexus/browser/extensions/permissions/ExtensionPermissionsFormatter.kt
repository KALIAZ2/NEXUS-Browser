package com.nexus.browser.extensions.permissions

data class FormattedExtensionPermission(
    val rawPermission: String,
    val titleFr: String,
    val descriptionFr: String,
    val isSensitive: Boolean
)

object ExtensionPermissionsFormatter {

    private data class PermMeta(
        val titleFr: String,
        val descriptionFr: String,
        val isSensitive: Boolean
    )

    private val PERMISSION_META_FR = mapOf(
        "<all_urls>" to PermMeta("Tous les sites web", "Accéder aux données de tous les sites web consultés", true),
        "*://*/*" to PermMeta("Tous les sites HTTP/HTTPS", "Accéder aux données de tous les sites HTTP et HTTPS", true),
        "activeTab" to PermMeta("Onglet actif", "Accéder à l'onglet actif lorsque vous interagissez avec l'extension", false),
        "tabs" to PermMeta("Onglets de navigation", "Consulter les adresses et titres des onglets ouverts", true),
        "storage" to PermMeta("Stockage local", "Stocker des préférences locales sur l'appareil", false),
        "cookies" to PermMeta("Cookies", "Lire et modifier les cookies des sites autorisés", true),
        "webRequest" to PermMeta("Requêtes réseau", "Observer et analyser les requêtes réseau du navigateur", true),
        "webRequestBlocking" to PermMeta("Filtrage réseau", "Bloquer ou modifier des requêtes réseau à la volée", true),
        "declarativeNetRequest" to PermMeta("Règles réseau déclaratives", "Bloquer du contenu réseau selon des règles déclaratives", false),
        "history" to PermMeta("Historique", "Lire et modifier l'historique de navigation", true),
        "bookmarks" to PermMeta("Favoris", "Lire et organiser les favoris", false),
        "downloads" to PermMeta("Téléchargements", "Gérer et initier des téléchargements", true),
        "clipboardRead" to PermMeta("Lecture presse-papiers", "Lire le contenu du presse-papiers", true),
        "clipboardWrite" to PermMeta("Écriture presse-papiers", "Copier du texte dans le presse-papiers", false),
        "notifications" to PermMeta("Notifications", "Afficher des notifications système", false),
        "contextMenus" to PermMeta("Menus contextuels", "Ajouter des options au menu contextuel", false),
        "scripting" to PermMeta("Injection de scripts/CSS", "Injecter des scripts ou feuilles CSS dans les pages autorisées", true),
        "alarms" to PermMeta("Planificateur", "Planifier des tâches périodiques en arrière-plan", false),
        "privacy" to PermMeta("Paramètres de confidentialité", "Consulter et modifier les paramètres de confidentialité", true)
    )

    fun describePermissionFr(permission: String): String {
        val clean = permission.trim()
        PERMISSION_META_FR[clean]?.let { return it.descriptionFr }
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("*://")) {
            return "Accéder à vos données sur le domaine $clean"
        }
        return "Permission technique WebExtension : $clean"
    }

    fun formatPermissionListFr(permissions: List<String>): List<String> {
        if (permissions.isEmpty()) {
            return listOf("Aucune permission spéciale requise.")
        }
        return permissions.map { describePermissionFr(it) }
    }

    fun describePermissions(permissions: List<String>): List<FormattedExtensionPermission> {
        return permissions.map { raw ->
            val clean = raw.trim()
            val meta = PERMISSION_META_FR[clean]
            if (meta != null) {
                FormattedExtensionPermission(
                    rawPermission = clean,
                    titleFr = meta.titleFr,
                    descriptionFr = meta.descriptionFr,
                    isSensitive = meta.isSensitive
                )
            } else if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("*://")) {
                FormattedExtensionPermission(
                    rawPermission = clean,
                    titleFr = "Accès domaine",
                    descriptionFr = "Accéder à vos données sur le domaine $clean",
                    isSensitive = clean.contains("*")
                )
            } else {
                FormattedExtensionPermission(
                    rawPermission = clean,
                    titleFr = clean,
                    descriptionFr = "Permission technique WebExtension : $clean",
                    isSensitive = false
                )
            }
        }
    }
}
