# Politique de Sécurité et Confidentialité — NEXUS Browser

Ce document détaille l'architecture de sécurité, de confidentialité et de protection des données mise en œuvre dans **NEXUS Browser**.

---

## 1. Principes Fondamentaux

1. **Aucune clé API secrète dans l'APK** :
   - Le binaire Android ne contient aucun secret codé en dur (`GEMINI_API_KEY`).
   - Par défaut, les requêtes IA passent par le backend proxy (`backend/gemini-proxy/src/server.ts`) qui détient la clé côté serveur et applique une limitation de débit (*rate limiting*) ainsi qu'une validation stricte des entrées.
2. **Validation et Assainissement des URL (`SecurityPolicy.kt` & `UrlUtils.kt`)** :
   - Rejet systématique des schémas d'injection de code (`javascript:`, `vbscript:`, `data:text/html` non sollicité) dans la barre d'adresse et dans les intents externes.
   - Protection contre la traversée de répertoires (*Path Traversal*) lors de l'extraction ou de l'analyse d'archives `.xpi` / `.zip` (`SecurityPolicy.isSafeArchiveEntryName`).
   - Assainissement systématique des noms de fichiers téléchargés (`SecurityPolicy.sanitizeFileName`).
3. **Mode HTTPS-Only** :
   - Activé par défaut dans `NexusPreferencesRepository`.
   - `SecurityPolicy.upgradeToHttps` promeut automatiquement les navigations `http://` vers `https://` (hors adresses de bouclage `localhost` / `127.0.0.1` / `10.0.2.2` utilisées pour le développement local).

---

## 2. Isolation de la Navigation Privée

Lorsqu'un onglet privé est ouvert (`isPrivate = true`) :
- **Isolation moteur GeckoView** : La session est créée avec `GeckoSessionSettings.Builder().usePrivateMode(true).build()`. Les cookies, le cache HTTP, `localStorage` et `IndexedDB` résident uniquement en mémoire vive dans un contexte de navigation éphémère distinct des onglets classiques.
- **Absence d'écriture en base locale** :
  - `TabRepository` exclut explicitement les onglets privés de la table Room `tabs`.
  - `HistoryRepository` n'enregistre aucune visite provenant d'un onglet privé.
- **Destruction immédiate** : La fermeture d'un onglet privé appelle `GeckoSession.close()` et libère immédiatement le contexte mémoire associé.

---

## 3. Bouclier Anti-Pistage (Tracking Protection) & En-têtes de Confidentialité

- **GeckoView ContentBlocking** :
  - Configuré dans `BrowserEngineManager` avec les listes anti-pistage de Mozilla (`AntiTracking.DEFAULT`, `AntiTracking.STP`, blocage des cryptomineurs et empreintes numériques *Fingerprinting*).
  - Le nombre de traqueurs bloqués est incrémenté en temps réel via `ContentBlocking.Delegate.onContentBlocked` et visible par l'utilisateur.
- **Signaux de confidentialité** :
  - Prise en charge de **Do Not Track (DNT)** et **Global Privacy Control (GPC)**.
  - Option de blocage des cookies tiers (`CookieBehavior.ACCEPT_FIRST_PARTY`) et d'effacement automatique des données à la fermeture.

---

## 4. Sécurité des Extensions WebExtensions

- **Permissions explicites** : Avant toute installation d'extension depuis la boutique ou un fichier local, `ExtensionPermissionsFormatter` traduit chaque permission technique (`<all_urls>`, `tabs`, `cookies`, `webRequestBlocking`, `scripting`, etc.) en français clair et met en évidence les permissions sensibles.
- **Validation des scripts générés par IA (`AiExtensionGenerator.kt`)** :
  - Analyse statique via `SecurityPolicy.containsForbiddenExtensionCode` bloquant `eval()`, `new Function()`, l'injection de `<script src="http...">` distants ou l'exfiltration de cookies.
  - Génération d'un manifeste restreint aux seules permissions nécessaires (`activeTab`, `storage`, `scripting`).

---

## 5. Sécurité du Gestionnaire d'APK

- **Aucun contournement des sécurités Android** : NEXUS Browser n'exécute jamais d'installation silencieuse ou root. Toute installation d'un fichier `.apk` passe obligatoirement par l'installateur système officiel d'Android (`Intent.ACTION_VIEW` + `FileProvider`) après confirmation explicite de l'utilisateur.
- **Vérification d'intégrité SHA-256** : Chaque APK téléchargé ou importé fait l'objet d'un calcul d'empreinte cryptographique `SHA-256` (`ApkScanner.kt`) affiché avant l'installation.
- **Détection des permissions à haut risque** : `ApkScanner` extrait la liste des permissions Android du manifeste binaire de l'APK et signale les permissions critiques (`REQUEST_INSTALL_PACKAGES`, `BIND_ACCESSIBILITY_SERVICE`, `READ_SMS`, `SYSTEM_ALERT_WINDOW`, etc.).
