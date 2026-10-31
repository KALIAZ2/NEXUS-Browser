# Feuille de Route (Roadmap) — NEXUS Browser

Ce document présente l'état de la version **1.0.0** livrée ainsi que les évolutions planifiées pour les prochaines versions de **NEXUS Browser**.

---

## Version 1.0.0 (Livrée)
- [x] Moteur **Mozilla GeckoView 130** natif (arm64-v8a & mode universel).
- [x] Design System **NEXUS** complet (Jetpack Compose, Material 3, thèmes Sombre `#090B10`, Clair `#F6F8FC` et Système).
- [x] Page d'accueil personnalisable (raccourcis éditables, favoris, sites fréquents, historique récent, fonds d'écran).
- [x] Barre d'adresse intelligente (5 moteurs de recherche, autocomplétion, indicateur HTTPS, recherche vocale, raccourcis clavier physique).
- [x] Gestionnaire d'onglets avancé (onglets normaux, onglets privés isolés, groupes d'onglets, onglets épinglés, restauration de session).
- [x] Système d'extensions **WebExtensions** complet :
  - 3 extensions intégrées (*Dark Mode Pro*, *Privacy Shield*, *Mode Lecture*),
  - Boutique d'extensions avec catalogue vérifié + recherche live API Mozilla Add-ons (AMO v5),
  - Importateur `.xpi` / `.zip` et analyseur de compatibilité `.crx`.
- [x] Gestionnaire de téléchargements (Pause/Reprise HTTP Range, progression, vérification SHA-256).
- [x] Centre APK (Scan `PackageManager`, permissions à risque, SHA-256, installation `FileProvider`).
- [x] Assistant **NEXUS AI** (Résumé de page, explication, traduction, Q&R, générateur d'extensions `.xpi` par IA) + Backend Proxy Node.js/TypeScript.

---

## Version 1.1.0 — Synchronisation & Multilingue
- [ ] Internationalisation complète (Anglais `values-en`, Espagnol `values-es`, Allemand `values-de`).
- [ ] Synchronisation chiffrée de bout en bout (E2EE) des favoris, raccourcis et paramètres entre appareils.
- [ ] Export / Import complet des favoris au format standard HTML Netscape Bookmark.

---

## Version 1.2.0 — Extensions & Productivité Avancée
- [ ] Fenêtres contextuelles d'action d'extension (`ActionDelegate` popup HTML dédiée dans un BottomSheet redimensionnable).
- [ ] Mode écran partagé double-onglet pour les tablettes Android et Samsung Galaxy Z Fold.
- [ ] Lecteur PDF et lecteur multimédia Picture-in-Picture (PiP) enrichis.

---

## Version 2.0.0 — IA Locale On-Device (Gemini Nano / AICore)
- [ ] Intégration optionnelle de **Gemini Nano via Android AICore** sur les appareils compatibles (Samsung Galaxy S24/S25, Pixel 8/9) pour l'exécution 100 % locale sur NPU sans serveur proxy.
- [ ] Traduction intégrale du DOM à la volée via WebExtension native pilotée par l'IA locale.
