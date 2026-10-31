# Système d'Extensions WebExtensions — NEXUS Browser

Ce document décrit le fonctionnement technique du système d'extensions de **NEXUS Browser**, la compatibilité **Mozilla GeckoView WebExtensions**, les extensions pré-intégrées et le catalogue d'extensions.

---

## 1. Architecture WebExtensions sur GeckoView

NEXUS Browser utilise l'API officielle `org.mozilla.geckoview.WebExtensionController` rattachée au `GeckoRuntime`.

### 1.1 Positionnement technique (WebExtensions vs Chrome `.crx`)
- **Support natif** : Les extensions au standard **WebExtensions (Firefox / Gecko `.xpi` et `.zip` contenant un `manifest.json` avec `browser_specific_settings.gecko.id`)** sont nativement prises en charge et exécutées par GeckoView.
- **Archives `.zip` WebExtension sans ID Gecko** : `ExtensionXpiPackager` inspecte automatiquement le `manifest.json`, injecte si nécessaire un identifiant `browser_specific_settings.gecko.id` conforme et reconditionne l'archive en paquet `.xpi` installable par GeckoView.
- **Archives Chrome `.crx`** : Un fichier `.crx` signé pour Google Chrome Desktop possède un en-tête binaire spécifique (`Cr24`) et cible parfois des API exclusives à Chromium Desktop (`chrome.declarativeContent`, `chrome.tabCapture`, `chrome.offscreen`). L'application inspecte l'en-tête et le manifeste via `ExtensionCompatibilityAnalyzer`, affiche un rapport de compatibilité détaillé en français et indique les API éventuellement non supportées sur mobile.

---

## 2. Extensions Pré-intégrées (Assets)

Trois extensions fonctionnelles sont livrées dans `app/src/main/assets/extensions/` et installées via `webExtensionController.ensureBuiltIn(uri, id)` :

1. **NEXUS Dark Mode Pro (`dark-reader-lite@nexus.browser`)**
   - Emplacement : `assets/extensions/nexus_dark_mode/`
   - Fonction : Applique un thème sombre adaptatif sur les pages web tout en préservant le rendu des images, vidéos et éléments `<canvas>`.
2. **NEXUS Privacy Shield (`privacy-shield@nexus.browser`)**
   - Emplacement : `assets/extensions/nexus_privacy_shield/`
   - Fonction : Supprime automatiquement les paramètres de pistage des URL (`utm_source`, `utm_medium`, `fbclid`, `gclid`, `mc_cid`), masque les bannières publicitaires intrusives courantes et renforce le signal `navigator.globalPrivacyControl`.
3. **NEXUS Mode Lecture (`reading-enhancer@nexus.browser`)**
   - Emplacement : `assets/extensions/nexus_reading_mode/`
   - Fonction : Améliore la typographie, l'interlignage et le confort de lecture des articles longs sur mobile.

---

## 3. Installer et Gérer une Extension dans NEXUS Browser

Les extensions sont accessibles à tout moment depuis le **bouton dédié dans la barre supérieure** (avec badge du nombre d'extensions actives) ou depuis **Menu ⋮ → Extensions** :

1. **Depuis le Catalogue d'Extensions (`ExtensionStoreScreen`)** :
   - Ouvrez **Extensions → Catalogue** (ou **Menu ⋮ → Catalogue d'extensions**).
   - Parcourez la sélection d'extensions vérifiées (*uBlock Origin*, *Dark Reader*, *Bitwarden*, *Privacy Badger*, *Tampermonkey*, *Decentraleyes*, *ClearURLs*, *SponsorBlock*...) ou effectuez une recherche en direct sur l'API publique officielle **Mozilla Add-ons (AMO v5)**.
   - Appuyez sur **Installer**, vérifiez les permissions traduites en français, puis confirmez. NEXUS télécharge le `.xpi` officiel en HTTPS et l'installe dans `WebExtensionController`.
2. **Depuis un fichier local (`.xpi`, `.zip` ou diagnostic `.crx`)** :
   - Ouvrez **Extensions → Importer .xpi / .zip / .crx**.
   - Sélectionnez votre fichier depuis le sélecteur de documents Android.
   - `ExtensionCompatibilityAnalyzer` vérifie l'archive, `ExtensionXpiPackager` prépare le paquet `.xpi` dans le stockage interne sécurisé de l'application (`files/extensions/`), et `NexusExtensionManager` l'enregistre dans GeckoView via `webExtensionController.install(fileUri)`.
