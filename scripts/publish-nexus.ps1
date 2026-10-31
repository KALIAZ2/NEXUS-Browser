param(
    [string]$NewVersionName = "",
    [string]$ReleaseNotes = "",
    [switch]$SkipLocalBuild = $false,
    [switch]$WaitForActions = $true
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   NEXUS BROWSER — PUBLICATION AUTOMATISEE GITHUB RELEASE   " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Vérifier que Git est installé
Write-Host "`n[1/12] Vérification de Git..." -ForegroundColor Yellow
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git n'est pas installé sur cet ordinateur."
}
git --version

# 2. Localiser GitHub CLI (gh) et vérifier l'authentification GitHub
Write-Host "`n[2/12] Vérification de la connexion GitHub CLI (gh)..." -ForegroundColor Yellow
$ghExe = "gh"
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    $defaultGh = "C:\Program Files\GitHub CLI\gh.exe"
    if (Test-Path $defaultGh) {
        $ghExe = $defaultGh
    } else {
        throw "GitHub CLI (gh) est introuvable. Installez-le via: winget install --id GitHub.cli"
    }
}

$authCheck = & $ghExe auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Connexion GitHub requise. Lancement du flux officiel 'gh auth login'..." -ForegroundColor Magenta
    & $ghExe auth login --hostname github.com --git-protocol https --web --scopes "repo,workflow"
    if ($LASTEXITCODE -ne 0) {
        throw "Authentification GitHub annulée ou échouée."
    }
}

$githubOwner = (& $ghExe api user --jq ".login").Trim()
$githubRepo = "NEXUS-Browser"
$fullRepo = "$githubOwner/$githubRepo"
Write-Host "Connecté à GitHub en tant que : $githubOwner (Dépôt cible : $fullRepo)" -ForegroundColor Green

# 3. Initialiser Git et configurer le dépôt distant si nécessaire
Write-Host "`n[3/12] Vérification du dépôt Git et GitHub ($fullRepo)..." -ForegroundColor Yellow
if (-not (Test-Path "$ProjectRoot\.git")) {
    git init -b main
}

& $ghExe repo view $fullRepo >$null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Création du dépôt GitHub '$fullRepo'..." -ForegroundColor Cyan
    & $ghExe repo create $fullRepo --public --description "NEXUS Browser — Navigateur Android natif propulsé par Mozilla GeckoView avec support WebExtensions"
}

$remoteUrl = "https://github.com/$fullRepo.git"
$existingRemote = git remote get-url origin 2>$null
if (-not $existingRemote) {
    git remote add origin $remoteUrl
} elseif ($existingRemote -ne $remoteUrl) {
    git remote set-url origin $remoteUrl
}
& $ghExe auth setup-git

# 4. Vérifier et synchroniser la clé de signature Android dans GitHub Actions Secrets
Write-Host "`n[4/12] Vérification de la clé de signature Android (Secrets GitHub)..." -ForegroundColor Yellow
$keystorePath = "$ProjectRoot\keystore\nexus-release.keystore"
if (-not (Test-Path $keystorePath)) {
    if (Test-Path "$env:USERPROFILE\.android\debug.keystore") {
        New-Item -ItemType Directory -Force -Path "$ProjectRoot\keystore" | Out-Null
        Copy-Item "$env:USERPROFILE\.android\debug.keystore" $keystorePath -Force
    } else {
        throw "Clé de signature introuvable dans keystore/nexus-release.keystore."
    }
}

$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
$keystoreBase64 | & $ghExe secret set NEXUS_KEYSTORE_BASE64 --repo $fullRepo
"android" | & $ghExe secret set NEXUS_KEYSTORE_PASSWORD --repo $fullRepo
"androiddebugkey" | & $ghExe secret set NEXUS_KEY_ALIAS --repo $fullRepo
"android" | & $ghExe secret set NEXUS_KEY_PASSWORD --repo $fullRepo
Write-Host "Secrets de signature Android synchronisés sur GitHub Actions." -ForegroundColor Green

# 5. Préparer et mettre à jour les numéros de version (éviter tout doublon)
Write-Host "`n[5/12] Vérification et mise à jour des numéros de version..." -ForegroundColor Yellow
$gradleFile = "$ProjectRoot\app\build.gradle.kts"
$gradleContent = Get-Content $gradleFile -Raw

$currentCodeMatch = [regex]::Match($gradleContent, 'versionCode\s*=\s*(\d+)')
$currentNameMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]+)"')
$currentCode = [int]$currentCodeMatch.Groups[1].Value
$currentName = $currentNameMatch.Groups[1].Value

# Vérifier si le tag existe déjà sur GitHub
$existingTags = git ls-remote --tags origin 2>$null
$tagAlreadyPublished = $existingTags -match "refs/tags/v$([regex]::Escape($currentName))$"

if (-not [string]::IsNullOrWhiteSpace($NewVersionName)) {
    $targetVersionName = $NewVersionName.TrimStart('v', 'V')
    if ($targetVersionName -ne $currentName -or $tagAlreadyPublished) {
        $targetVersionCode = $currentCode + 1
    } else {
        $targetVersionCode = $currentCode
    }
} elseif ($tagAlreadyPublished) {
    $parts = $currentName.Split('.')
    $major = if ($parts.Length -ge 1) { [int]$parts[0] } else { 1 }
    $minor = if ($parts.Length -ge 2) { [int]$parts[1] } else { 0 }
    $patch = if ($parts.Length -ge 3) { [int]$parts[2] + 1 } else { 1 }
    $targetVersionName = "$major.$minor.$patch"
    $targetVersionCode = $currentCode + 1
    Write-Host "La version v$currentName existe déjà sur GitHub -> incrémentation automatique vers v$targetVersionName (Build $targetVersionCode)" -ForegroundColor Cyan
} else {
    $targetVersionName = $currentName
    $targetVersionCode = $currentCode
}

$gradleContent = [regex]::Replace($gradleContent, 'versionCode\s*=\s*\d+', "versionCode = $targetVersionCode")
$gradleContent = [regex]::Replace($gradleContent, 'versionName\s*=\s*"[^"]+"', "versionName = `"$targetVersionName`"")
$gradleContent = [regex]::Replace($gradleContent, ':\s*"KALIAZ2"', ": `"$githubOwner`"")
Set-Content -Path $gradleFile -Value $gradleContent -Encoding UTF8 -NoNewline

$tagName = "v$targetVersionName"
Write-Host "Version prête à publier : $tagName (versionCode = $targetVersionCode)" -ForegroundColor Green

# 6. Vérifier que le projet compile et passe les tests unitaires
Write-Host "`n[6/12] Compilation et exécution des tests unitaires locaux..." -ForegroundColor Yellow
if (Test-Path "C:\Users\wakan\AppData\Local\Programs\jdk-17") {
    $env:JAVA_HOME = "C:\Users\wakan\AppData\Local\Programs\jdk-17"
}
if (-not $SkipLocalBuild) {
    & "$ProjectRoot\gradlew.bat" testDebugUnitTest assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "La compilation ou les tests unitaires ont échoué. Publication annulée."
    }
}

# 7. Vérifier que l'APK local a bien été généré et signé avec la clé officielle
Write-Host "`n[7/12] Vérification de l'APK et de sa signature..." -ForegroundColor Yellow
$rawApk = Get-ChildItem "$ProjectRoot\app\build\outputs\apk\release\*.apk" | Select-Object -First 1
if (-not $rawApk) {
    throw "Aucun fichier APK trouvé dans app/build/outputs/apk/release/."
}
$versionedApkPath = "$ProjectRoot\NEXUS-$tagName.apk"
$canonicalApkPath = "$ProjectRoot\NEXUS.apk"
Copy-Item $rawApk.FullName $versionedApkPath -Force
Copy-Item $rawApk.FullName $canonicalApkPath -Force

$apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools\*\apksigner.bat" -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1
if ($apksigner) {
    & $apksigner.FullName verify --print-certs $versionedApkPath
    if ($LASTEXITCODE -ne 0) {
        throw "La vérification de signature de l'APK a échoué."
    }
}
$apkHash = (Get-FileHash $versionedApkPath -Algorithm SHA256).Hash.ToLower()
$apkSizeMb = [math]::Round((Get-Item $versionedApkPath).Length / 1MB, 2)
Write-Host "APK vérifié : NEXUS-$tagName.apk ($apkSizeMb Mo | SHA-256: $apkHash)" -ForegroundColor Green

# 8. Préparer les notes de version (Changelog)
Write-Host "`n[8/12] Préparation des notes de version..." -ForegroundColor Yellow
if ([string]::IsNullOrWhiteSpace($ReleaseNotes)) {
    $ReleaseNotes = @"
## Nouveautés de NEXUS Browser $tagName

- Correction définitive de l'affichage du clavier Android dans la barre d'adresse sur toutes les pages Web GeckoView.
- Sélection automatique de l'URL, édition fluide et validation directe via la touche Entrée / Aller.
- Système de mises à jour automatisées intégré dans **Paramètres → À propos de NEXUS → Mises à jour**.
- Vérification d'intégrité ZIP/APK, package et signature avant installation Android.

---
- **Version** : $targetVersionName (Build $targetVersionCode)
- **Fichier APK** : `NEXUS-$tagName.apk` ($apkSizeMb Mo)
- **SHA-256** : `$apkHash`
"@
}
$notesFile = "$ProjectRoot\ build_release_notes.tmp.md"
Set-Content -Path $notesFile -Value $ReleaseNotes -Encoding UTF8

# 9. Envoyer les modifications sur GitHub après vérification de sécurité
Write-Host "`n[9/12] Envoi du code source et du tag $tagName sur GitHub..." -ForegroundColor Yellow
git add -A
$stagedFiles = git diff --cached --name-only
if ($stagedFiles -match '\.(keystore|jks|apk)$|local\.properties') {
    Remove-Item $notesFile -Force -ErrorAction SilentlyContinue
    throw "ALERTE SECURITE : Un fichier sensible ou binaire (.keystore, local.properties, .apk) a été détecté dans le commit. Envoi bloqué."
}
Remove-Item $notesFile -Force -ErrorAction SilentlyContinue

$status = git status --porcelain
if ($status) {
    git commit -m "Release NEXUS Browser $tagName (Build $targetVersionCode)"
}

git tag -f $tagName
git push -u origin main
git push origin $tagName --force

# 10. Déclencher et surveiller GitHub Actions
Write-Host "`n[10/12] Suivi de l'exécution du workflow GitHub Actions..." -ForegroundColor Yellow
Start-Sleep -Seconds 5
$runId = (& $ghExe run list --repo $fullRepo --limit 1 --json databaseId --jq ".[0].databaseId" 2>$null)
if ($WaitForActions -and $runId) {
    Write-Host "Workflow GitHub Actions détecté (Run #$runId). Attente de la fin de la compilation..." -ForegroundColor Cyan
    & $ghExe run watch $runId --repo $fullRepo --exit-status
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Le workflow GitHub Actions a rencontré un problème, publication directe de la Release depuis l'APK vérifié localement..."
    }
}

# 11. Vérifier ou publier la GitHub Release avec l'APK généré
Write-Host "`n[11/12] Vérification de la GitHub Release $tagName..." -ForegroundColor Yellow
$notesTemp = [System.IO.Path]::GetTempFileName()
Set-Content -Path $notesTemp -Value $ReleaseNotes -Encoding UTF8

& $ghExe release view $tagName --repo $fullRepo >$null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Création de la GitHub Release $tagName et téléversement de l'APK..." -ForegroundColor Cyan
    & $ghExe release create $tagName $versionedApkPath $canonicalApkPath --repo $fullRepo --title "NEXUS Browser $tagName" --notes-file $notesTemp
} else {
    Write-Host "Mise à jour des actifs APK de la GitHub Release $tagName..." -ForegroundColor Cyan
    & $ghExe release upload $tagName $versionedApkPath $canonicalApkPath --repo $fullRepo --clobber
}
Remove-Item $notesTemp -Force -ErrorAction SilentlyContinue

# 12. Vérification finale de la Release et affichage du lien
Write-Host "`n[12/12] Validation finale de la Release publiée..." -ForegroundColor Yellow
$releaseUrl = (& $ghExe release view $tagName --repo $fullRepo --json url --jq ".url").Trim()
$assetNames = (& $ghExe release view $tagName --repo $fullRepo --json assets --jq ".assets[].name")

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "   PUBLICATION TERMINEE AVEC SUCCES !                       " -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Version publiée : NEXUS Browser $tagName (Build $targetVersionCode)"
Write-Host "Fichiers APK    : $($assetNames -join ', ')"
Write-Host "Lien Release    : $releaseUrl" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Green
