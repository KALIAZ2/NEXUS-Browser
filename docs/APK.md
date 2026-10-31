# Gestionnaire et Inspecteur de Fichiers APK — NEXUS Browser

Ce document détaille la chaîne de détection, d'inspection cryptographique et d'installation sécurisée des fichiers `.apk` au sein de **NEXUS Browser**.

---

## 1. Architecture du Module APK (`com.nexus.browser.apk`)

Le module est structuré en quatre composants :
1. **`ApkScanner` (`apk/scanner/ApkScanner.kt`)** :
   - Vérifie la signature d'archive ZIP (`PK\x03\x04`) et la présence d'`AndroidManifest.xml` et `classes.dex`.
   - Calcule l'empreinte cryptographique **`SHA-256`** complète du fichier APK par flux de 16 Ko.
   - Interroge `PackageManager.getPackageArchiveInfo(filePath, PackageManager.GET_PERMISSIONS)` pour extraire :
     - Le nom d'application (`applicationInfo.loadLabel`),
     - L'identifiant de package (`packageName`),
     - La version (`versionName` & `longVersionCode`),
     - Les niveaux d'API (`minSdkVersion` & `targetSdkVersion`),
     - La liste complète des permissions Android demandées.
   - Évalue un niveau de risque (`SAFE`, `MODERATE`, `HIGH_RISK`) en croisant les permissions avec la liste des permissions sensibles Android (`REQUEST_INSTALL_PACKAGES`, `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`, `READ_SMS`, `SEND_SMS`, `MANAGE_EXTERNAL_STORAGE`, etc.).
2. **`ApkInstaller` (`apk/installer/ApkInstaller.kt`)** :
   - Vérifie sur Android 8.0+ (API 26+) si l'application dispose de l'autorisation système `packageManager.canRequestPackageInstalls()`.
   - Si l'autorisation n'est pas encore accordée, ouvre directement l'écran officiel des paramètres système `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` pour `package:com.nexus.browser`.
   - Génère une URI sécurisée `content://com.nexus.browser.fileprovider/...` via `androidx.core.content.FileProvider` et lance l'installateur système Android (`Intent.ACTION_VIEW` avec le type MIME `application/vnd.android.package-archive` et `FLAG_GRANT_READ_URI_PERMISSION`).
3. **`NexusAPKManager` (`apk/manager/NexusAPKManager.kt`)** :
   - Orchestre l'importation de fichiers `.apk` externes, le balayage automatique du dossier de téléchargements de NEXUS (`files/downloads/`) et la suppression propre des fichiers.
4. **`ApkRepository` (`apk/repository/ApkRepository.kt`)** :
   - Synchronise les métadonnées des APK analysés avec la table Room `apk_packages` (`ApkDao`) et expose un `Flow<List<ApkMetadata>>` à l'interface Compose (`DownloadsAndApkScreen.kt`).

---

## 2. Parcours Utilisateur lors du Téléchargement d'un APK

1. Lorsqu'un lien `.apk` ou un flux `application/vnd.android.package-archive` est intercepté par `GeckoSession.ContentDelegate.onExternalResponse`, `NexusDownloadManager` affiche un **avertissement de sécurité** et télécharge le fichier dans le répertoire dédié.
2. Une fois le téléchargement terminé, si l'option *Analyser automatiquement les APK téléchargés* est active (par défaut), `NexusAPKManager` déclenche `ApkScanner.inspectApkFile()`.
3. Dans l'écran **Centre APK**, l'utilisateur consulte :
   - Le nom de package, la version et les SDK min/cible,
   - Le condensat `SHA-256` complet (copiable ou vérifiable),
   - Les permissions sensibles mises en évidence en orange/rouge,
   - Les boutons **Installer**, **Partager** et **Supprimer**.
