# NEXUS Browser — Navigateur Android Natif (GeckoView, WebExtensions & APK)

**Version :** `1.2.0` (`versionCode = 3`)  
**Package :** `com.nexus.browser`  
**Moteur Web :** Mozilla GeckoView (`130.0.20240913135723`)  
**Plateforme :** Android 8.0+ (API 26 à API 34, optimisé pour smartphones & tablettes Samsung Galaxy et Android modernes)

---

## 1. Présentation

**NEXUS Browser** est un navigateur mobile Android natif rapide, léger et épuré, conçu autour du moteur **Mozilla GeckoView** :
- **Navigation web complète & ergonomique** : Rendu natif GeckoView (`GeckoRuntime` & pool LRU de `GeckoSession`), gestion fluide du focus clavier entre `GeckoView` et la barre d'adresse Compose (`NexusToolbar`), barre de navigation inférieure pensée pour une utilisation quotidienne à une main (*Retour, Avancer, Accueil, Onglets / Nouvel onglet, Menu*), sélection automatique de l'adresse au toucher, copie/collage rapide d'URL et suggestions pertinentes (historique + favoris).
- **Système de mises à jour automatisées via GitHub Releases** : Vérification périodique et manuelle des nouvelles versions publiées sur GitHub Releases (`AppUpdateManager`), comparaison de versions sémantiques, affichage des notes de version (changelog), téléchargement direct de l'APK avec barre de progression, vérification d'intégrité (taille, signature/structure APK, `applicationId = com.nexus.browser`) et lancement de l'installateur système Android via `FileProvider`.
- **Système d'extensions WebExtensions réel** : Exécution native des WebExtensions via `WebExtensionController`, avec 3 extensions intégrées dès l'installation (*NEXUS Dark Mode Pro*, *NEXUS Privacy Shield*, *NEXUS Mode Lecture*), catalogue d'extensions intégré (recherche officielle Mozilla Add-ons), installation locale de paquets `.xpi` / `.zip`, et analyseur de compatibilité pour les archives `.crx`.
- **Gestionnaire de téléchargements & Centre APK** : Téléchargements multi-fichiers avec suivi de progression, vérification d'intégrité **SHA-256**, inspection statique des fichiers `.apk` (`PackageManager.getPackageArchiveInfo`), analyse des permissions sensibles et installation sécurisée via `FileProvider` + `ACTION_VIEW`.
- **Confidentialité & Sécurité** : Onglets privés isolés (`GeckoSessionSettings.Builder().usePrivateMode(true)`), HTTPS-Only, Do Not Track, Global Privacy Control (GPC), blocage des popups et cookies tiers, effacement sélectif des données.
- **Écran d'accueil minimaliste & Design System NEXUS** : Interface Jetpack Compose & Material 3 centrée sur la barre de recherche, raccourcis personnalisables et réorganisables, accès rapide aux favoris et aux sites récemment visités, thèmes Sombre, Clair et Système.

---

## 2. Structure du Projet

```text
NEXUS/
├── .github/
│   └── workflows/
│       └── nexus-release.yml               # Pipeline CI/CD GitHub Actions (Tests, Build APK signé, Release GitHub)
├── app/
│   └── src/
│       ├── main/
│       │   ├── assets/extensions/          # 3 WebExtensions intégrées (Dark Mode, Privacy Shield, Reading Mode)
│       │   ├── java/com/nexus/browser/
│       │   │   ├── core/                   # Common, DesignSystem, Room Database, DataStore, Security, Network, Navigation
│       │   │   ├── browser/                # Engine (GeckoView + LRU Session Pool), Sessions, Tabs, Toolbar, Permissions, Downloads
│       │   │   ├── extensions/             # Manager, Installer (.xpi), Compatibility (.crx/.zip), Permissions, Store, Repository
│       │   │   ├── apk/                    # Manager, Scanner (SHA-256 + PackageInfo), Installer (FileProvider), Repository
│       │   │   ├── updates/                # AppUpdateManager (GitHub Releases API, vérification & installation OTA)
│       │   │   ├── bookmarks/              # Gestion des favoris, dossiers & raccourcis d'accueil
│       │   │   ├── history/                # Historique de navigation & recherche
│       │   │   ├── privacy/                # Bouclier de confidentialité & nettoyage des données
│       │   │   ├── settings/               # ViewModel & gestion des préférences globales
│       │   │   └── ui/                     # Écrans Jetpack Compose & NexusBrowserViewModel
│       │   └── res/                        # Icônes adaptatives NEXUS, thèmes, XML FileProvider & NetworkSecurityConfig
│       └── test/                           # Suite de tests unitaires JUnit 4 (URL, Navigation, Sécurité, Extensions, APK, Updates)
├── docs/
│   ├── ARCHITECTURE.md                     # Architecture Clean + MVVM + GeckoView
│   ├── SECURITY.md                         # Modèle de sécurité, CSP, WebExtensions & FileProvider
│   ├── EXTENSIONS.md                       # Fonctionnement du moteur WebExtensions & catalogue
│   ├── APK.md                              # Chaîne de téléchargement, scan SHA-256 et installation d'APK
│   ├── BUILD.md                            # Guide complet de compilation Windows, génération APK et installation Quick Share / USB
│   └── ROADMAP.md                          # Feuille de route et évolutions futures
├── scripts/
│   └── publish-nexus.ps1                   # Script PowerShell d'automatisation complète de release GitHub
├── publish-nexus.bat                       # Lanceur Windows en une commande pour compiler, taguer et publier une Release
├── gradle/
│   └── libs.versions.toml                  # Catalogue centralisé des dépendances
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 3. Démarrage Rapide & Publication Automatisée

### Publier une nouvelle mise à jour sur GitHub Releases (1 commande)
Depuis Windows, exécutez simplement :
```powershell
.\publish-nexus.bat -VersionName 1.2.1 -Changelog "Corrections et améliorations"
```
Ce script :
1. Vérifie l'état Git et l'authentification GitHub CLI (`gh`),
2. Met à jour ou incrémente automatiquement `versionCode` et `versionName` dans `app/build.gradle.kts`,
3. Exécute les tests unitaires (`testDebugUnitTest`) et compile l'APK Release signé (`assembleRelease`),
4. Vérifie la signature cryptographique v2/v3 de l'APK avec `apksigner`,
5. Crée le commit, le tag `vX.Y.Z`, pousse vers GitHub et publie la GitHub Release avec `NEXUS-vX.Y.Z.apk` et `NEXUS.apk`.

### Mettre à jour depuis l'application NEXUS Browser
Sur votre smartphone Android, ouvrez **NEXUS Browser → Paramètres → À propos de NEXUS → Mises à jour** et appuyez sur **Rechercher des mises à jour**, puis **Télécharger la mise à jour** et **Installer la mise à jour**.

### Transférer et installer l'APK sur un smartphone Android (Quick Share ou USB)
Le fichier **`NEXUS.apk`** (ainsi que **`NEXUS-release.apk`**) à la racine du projet est prêt à être installé sur votre smartphone Android (Samsung Galaxy ou tout appareil ARM64 sous Android 8.0+) :

1. **Via Quick Share (Samsung / Windows)** :
   - Faites un clic droit sur `NEXUS\NEXUS.apk` → **Partager** → **Quick Share** (ou glissez-déposez le fichier dans l'application *Quick Share* sur Windows).
   - Acceptez la réception sur votre smartphone, appuyez sur le fichier reçu dans *Mes Fichiers* (`Téléchargements` / `Quick Share`) et lancez l'installation.
2. **Via câble USB (ADB)** :
   ```powershell
   adb install -r NEXUS.apk
   ```

### Compiler le projet sur Windows
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease
```
Pour générer un APK universel (incluant `x86_64` pour émulateur PC en plus de `arm64-v8a`) :
```powershell
.\gradlew.bat assembleDebug -PuniversalApk=true
```
