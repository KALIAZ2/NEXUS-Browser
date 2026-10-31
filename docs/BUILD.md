# Guide de Compilation, Génération APK et Installation — NEXUS Browser

Ce guide couvre l'ensemble des procédures pour ouvrir, compiler, tester et installer **NEXUS Browser** depuis un ordinateur **Windows 10 / 11** vers un smartphone Android physique (notamment Samsung Galaxy).

---

## 1. Ouvrir le Projet dans Android Studio

1. Lancez **Android Studio** (Hedgehog, Iguana, Jellyfish, Koala ou Ladybug).
2. Cliquez sur **Open** et sélectionnez le dossier racine du projet :
   ```text
   C:\Users\wakan\.gemini\antigravity\scratch\NEXUS
   ```
3. Vérifiez dans **File → Settings → Build, Execution, Deployment → Build Tools → Gradle** que le **Gradle JDK** pointe vers **JDK 17** (préconfiguré sur `C:\Users\wakan\AppData\Local\Programs\jdk-17` dans `gradle.properties`).
4. Android Studio synchronise automatiquement le projet à partir de `gradle/libs.versions.toml` et `local.properties` (`sdk.dir=C:\\Users\\wakan\\AppData\\Local\\Android\\Sdk`).

---

## 2. Compiler le Projet sur Windows (Ligne de Commande PowerShell)

Ouvrez un terminal **PowerShell** dans le répertoire du projet :

```powershell
cd "C:\Users\wakan\.gemini\antigravity\scratch\NEXUS"
```

### 2.1 Exécuter les tests unitaires automatisés
```powershell
.\gradlew.bat testDebugUnitTest
```
Les rapports HTML JUnit sont générés dans `app\build\reports\tests\testDebugUnitTest\index.html`.

### 2.2 Générer l'APK Debug (optimisé ARM64 pour smartphones physiques)
```powershell
.\gradlew.bat assembleDebug
```
- L'APK compilé est produit dans :
  `app\build\outputs\apk\debug\app-debug.apk`
- Une copie directe est également disponible à la racine du projet :
  `NEXUS.apk`

### 2.3 Générer un APK Universel (ARM64 + x86_64 pour émulateurs PC)
Par défaut, le projet utilise `geckoview-arm64-v8a` afin d'optimiser le poids de l'APK (~85 Mo au lieu de ~260 Mo) pour tous les smartphones Android modernes. Si vous souhaitez exécuter l'APK sur un émulateur Android x86_64 sur PC :
```powershell
.\gradlew.bat assembleDebug -PuniversalApk=true
```

### 2.4 Générer l'APK Release (avec R8 / ProGuard)
```powershell
.\gradlew.bat assembleRelease
```
Le fichier est généré dans `app\build\outputs\apk\release\app-release-unsigned.apk` (ou signé si vous configurez votre keystore de production dans `app/build.gradle.kts`). Les règles de préservation GeckoView, Room, Hilt et sérialisation JSON sont définies dans `app/proguard-rules.pro`.

---

## 3. Installer l'APK sur un Smartphone Android (Samsung Galaxy / Android 8.0+)

### 3.1 Activer les Options Développeur et le Débogage USB
1. Sur votre smartphone Android, ouvrez **Paramètres → À propos du téléphone → Informations sur le logiciel**.
2. Appuyez **7 fois** de suite sur **Numéro de build** jusqu'à voir le message *« Le mode développeur a été activé »*.
3. Revenez dans **Paramètres → Options de développement** (tout en bas).
4. Activez **Débogage USB**.

### 3.2 Méthode A — Installation via câble USB (ADB)
1. Branchez votre smartphone à l'ordinateur Windows avec un câble USB de données.
2. Déverrouillez l'écran du téléphone et appuyez sur **Autoriser** sur la fenêtre *« Autoriser le débogage USB ? »*.
3. Dans PowerShell :
   ```powershell
   & "C:\Users\wakan\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
   & "C:\Users\wakan\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\wakan\.gemini\antigravity\scratch\NEXUS\NEXUS.apk"
   ```
4. Pour lancer immédiatement NEXUS Browser depuis le PC :
   ```powershell
   & "C:\Users\wakan\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.nexus.browser/.MainActivity
   ```

### 3.3 Méthode B — Installation par Transfert Direct (Sans ADB)
1. Copiez le fichier `C:\Users\wakan\.gemini\antigravity\scratch\NEXUS\NEXUS.apk` sur votre smartphone (via l'Explorateur Windows en mode *Transfert de fichiers MTP*, Quick Share, Google Drive ou clé USB-C).
2. Sur le smartphone, ouvrez l'application **Mes Fichiers** (*My Files* sur Samsung Galaxy) et touchez **`NEXUS.apk`**.
3. Si Android affiche *« Pour votre sécurité, votre téléphone n'est pas autorisé à installer des applications inconnues de cette source »*, appuyez sur **Paramètres**, activez **Autoriser depuis cette source**, puis appuyez sur **Installer**.
4. Si Google Play Protect propose d'analyser l'application (car elle est signée avec la clé de développement Debug), appuyez sur **Installer quand même**.

---

## 4. Configurer une Extension dans NEXUS Browser

1. Ouvrez **NEXUS Browser** sur votre téléphone.
2. Les **3 extensions intégrées** (*NEXUS Dark Mode Pro*, *NEXUS Privacy Shield*, *NEXUS Mode Lecture*) sont automatiquement installées au premier démarrage.
3. Appuyez sur le bouton **Menu (⋮)** ou sur le raccourci **Extensions** de la page d'accueil :
   - Activez/désactivez une extension d'un simple interrupteur.
   - Ouvrez **Boutique d'extensions** pour installer des extensions vérifiées en un clic depuis Mozilla Add-ons (AMO).
   - Appuyez sur **Importer (.xpi / .zip / .crx)** pour installer un paquet local ou analyser un fichier `.crx`.
   - Ouvrez **NEXUS AI → Créer Extension IA** pour générer et installer une extension sur mesure à partir d'une consigne en français.

---

## 5. Configurer la Clé API Gemini via le Backend Proxy

Pour garantir qu'aucune clé API secrète ne soit exposée dans l'APK, NEXUS inclut un serveur proxy Node.js / TypeScript dans `backend/gemini-proxy/`.

1. Ouvrez un terminal dans `backend/gemini-proxy` :
   ```powershell
   cd "C:\Users\wakan\.gemini\antigravity\scratch\NEXUS\backend\gemini-proxy"
   cp .env.example .env
   ```
2. Éditez le fichier `.env` et renseignez votre clé Gemini :
   ```env
   PORT=8787
   GEMINI_API_KEY=votre_vraie_cle_api_gemini
   GEMINI_MODEL=gemini-1.5-flash
   NEXUS_CLIENT_TOKEN=nexus_client_token_change_me
   ```
3. Installez les dépendances et démarrez le serveur :
   ```powershell
   npm install
   npm run dev
   ```
4. Dans **NEXUS Browser → Paramètres → Intelligence Artificielle (NEXUS AI)** :
   - Sur émulateur Android : l'URL par défaut `http://10.0.2.2:8787` pointe déjà vers votre PC hôte.
   - Sur smartphone physique connecté au même Wi-Fi : renseignez `http://<IP_LOCALE_DE_VOTRE_PC>:8787` (ou utilisez `adb reverse tcp:8787 tcp:8787` avec `http://127.0.0.1:8787`).
   - *Note* : Si le serveur proxy n'est pas joignable, `GeminiProvider` bascule automatiquement sur le moteur d'analyse locale embarqué de NEXUS afin que le résumé de page, l'explication et le générateur d'extensions IA restent 100 % fonctionnels hors-ligne.
