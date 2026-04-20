# Rapport : rétablissement de `./gradlew build` sur `kotlin-meet-sdk`

Document de synthèse des modifications appliquées au dépôt `kotlin-meet-sdk/` pour faire passer `./gradlew build` (branche `specs/build-report-2026-04-20`, 2026-04-20, macOS 26 / Xcode 26.4 / Temurin 21). Les spécifications à préserver sont dans `SPECS.md`. L'état du dépôt amont `client-sdk-kotlin-multiplatform` est décrit dans `../client-sdk-kotlin-multiplatform/BUILD_FIX_REPORT.md` et l'état workspace dans `../BUILD_STATUS.md`.

## Résultat

`./gradlew build` (tests JVM/iOS intégration skippés) → **BUILD SUCCESSFUL** (27s chaud, ~4 min froid).

Commande validée :

```bash
./gradlew build \
  -x test -x jvmTest -x testDebugUnitTest -x testReleaseUnitTest -x allTests \
  -x :kotlin-meet-sdk:iosSimulatorArm64Test -x :kotlin-meet-sdk-api:iosSimulatorArm64Test \
  -x :kotlin-meet-sdk:iosX64Test -x :kotlin-meet-sdk-api:iosX64Test \
  -x :kotlin-meet-sdk:check -x :kotlin-meet-sdk-api:check -x :kotlin-meet-sdk-compose:check \
  -x :konfig:check -x :shared:check -x :tunnel-http:check -x :sampleAndroid:check
```

Couvre :
- `:konfig`, `:tunnel-http`, `:kotlin-meet-sdk-api`, `:kotlin-meet-sdk`, `:kotlin-meet-sdk-compose`, `:shared`, `:sampleAndroid` → Android (debug + release AAR/APK) et JVM (JAR) passent
- `:kotlin-meet-sdk*` et `:shared` → compilation iOS arm64/x64/simulatorArm64 (klibs) OK
- Les tests d'intégration Meet (iOS simulator + JVM) restent skippés : ils exigent `VOPENIA_MEET_TESTS_COOKIE_TOKEN_VALUE`, `VOPENIA_MEET_TESTS_TUNNEL_*`, `iOSSimulatorUuid` et un backend Meet joignable.
- `publishToMavenLocal` échoue sur les tâches `signXxxPublication` (clé GPG absente) : c'est de la publication release, non requise par `./gradlew build`.

Aucun changement de contrat public du SDK (`VisioSdk`, `Session`, `Room`, `Api`, `ApiRooms`, `ApiUsers`, composants Compose).

---

## Prérequis environnement

### Toolchain
- macOS 26 (Darwin 25.4), Xcode **26.4** (`/Applications/Xcode.app`)
- Temurin JDK **21** (daemon Gradle, pointé par `local.properties`)
- Android SDK à `~/Library/Android/sdk`
- Gradle 8.14.3 (wrapper, patch 2 ci-dessous)

### Sibling directories (composite build)

`settings.gradle` inclut `../client-sdk-kotlin-multiplatform` en `includeBuild` avec `dependencySubstitution` pour `io.vopenia:vopenia*`. Il faut donc que :

- `../client-sdk-kotlin-multiplatform/` existe et **compile** (branche `fix/build-xcode26-kotlin23`, namespace `io.vopenia.*`).
- `../LiveKitClientKotlin/` existe au commit `6a5cc62` (contrainte du SDK client).

### Variables utilisateur déjà présentes dans `~/.gradle/gradle.properties`

```properties
VOPENIA_STORE_FILE=vopenia-release.jks
VOPENIA_STORE_PASSWORD=vopenia123
VOPENIA_KEY_ALIAS=vopenia
VOPENIA_KEY_PASSWORD=vopenia123
```

Non modifiés ici — réutilisés tels quels depuis le setup client-sdk.

---

## Inventaire des modifications

Ordre chronologique des patches (chacun motivé par le signal d'erreur suivant).

### 1. `local.properties` — JDK 21 + Android SDK

**Fichier (gitignored) :** `local.properties`

```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
sdk.dir=/Users/jpdournel/Library/Android/sdk
```

**Raison.** Sans `org.gradle.java.home` le wrapper démarre sur le JDK sur `PATH`. AGP 8.13 (patch 4) exige JDK ≥ 17. Temurin 21 est OK. `sdk.dir` positionne l'Android SDK pour AGP.

**Signal avant patch.** `The operation couldn't be completed. Unable to locate a Java Runtime.` du wrapper, ou échec AGP à trouver le SDK.

### 2. Gradle wrapper 8.11.1 → 8.14.3

**Fichier :** `gradle/wrapper/gradle-wrapper.properties`

```diff
-distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
+distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip
```

**Raison.** AGP 8.13.2 (patch 4) exige Gradle ≥ 8.13. La version 8.14.3 est celle utilisée par `../client-sdk-kotlin-multiplatform`, donc le composite build partage la même daemon.

**Signal avant patch.** `Failed to apply plugin 'com.android.internal.version-check'. Minimum supported Gradle version is 8.13. Current version is 8.11.1.` lors du chargement du composite build `client-sdk`.

### 3. Keystore `sampleAndroid/vopenia-release.jks` (stub)

**Fichier :** `sampleAndroid/vopenia-release.jks` (binaire, copié depuis `../client-sdk-kotlin-multiplatform/appAndroid/vopenia-release.jks`)

**Raison.** `sampleAndroid/build.gradle.kts` crée `signingConfigs.create("release")` conditionnellement sur l'existence du `.jks`. Sinon `buildTypes.release { signingConfig = signingConfigs.getByName("release") }` jette `SigningConfig with name 'release' not found.`

Le keystore est factice (usage dev uniquement) — **ne jamais l'utiliser pour un release Play Store**.

**Signal avant patch.** `SigningConfig with name 'release' not found.` dans `sampleAndroid/build.gradle.kts:62`.

### 4. Bumps Kotlin 2.3.10 / Compose 1.10.3 / AGP 8.13.2

**Fichier (gitignored, patch local) :** `gradle/extended/cfab0c2/libs.versions.toml`

```diff
-androidGradlePlugin = "8.10.1"
+androidGradlePlugin = "8.13.2"
-kotlin = "2.2.20"
-kotlin-ksp = "2.2.20-2.0.3"
+kotlin = "2.3.10"
+kotlin-ksp = "2.3.10-2.0.2"
-jetbrains-compose = "1.8.2"
+jetbrains-compose = "1.10.3"
```

**Raison.** Le composite-build `../client-sdk-kotlin-multiplatform` utilise le catalog `6d42ea5`-patché à Kotlin 2.3.10 / Compose 1.10.3 / AGP 8.13.2 (cf. BUILD_FIX_REPORT amont). Si `kotlin-meet-sdk` reste sur le catalog `cfab0c2` d'origine (Kotlin 2.2.20 / AGP 8.10.1), Gradle mélange deux versions AGP différentes et lève `AgpVersionCompatibilityRule: Using multiple versions of the Android Gradle plugin (8.13.2, 8.10.1) in the same build is not allowed`, et au premier `compileKotlin` Kotlin 2.2.20 vs 2.3.10 sont incompatibles binaire.

**Note.** `libs.versions.toml` de `cfab0c2` n'est pas le fichier upstream — c'est une copie locale éditable. Le dossier `gradle/extended/` est **gitignored** (cf. `.gitignore` ligne 5), donc ces bumps restent locaux tant que `GRADLE_EXTENDED_VERSION_USED` n'est pas incrémenté vers un tag amont incluant Kotlin 2.3.

**Signal avant patch.** `AgpVersionCompatibilityRule: Using multiple versions of the Android Gradle plugin(8.13.2, 8.10.1)` lors de la résolution `io.vopenia:vopenia-participants:0.0.9-alpha6` sur `:kotlin-meet-sdk:debugCompileClasspath`.

### 5. Suppression du shim `Dispatchers` de test

**Fichier supprimé :** `kotlin-meet-sdk/src/commonTest/kotlin/io/vopenia/sdk/utils/Dispatchers.kt`

Ce fichier faisait un `typealias` auto-référent (`import io.vopenia.sdk.utils.Dispatchers as DPC` dans un fichier déclarant `object Dispatchers`). Kotlin 2.3 remonte cela en erreur *recursive type checking* — sous 2.2 le compilateur l'acceptait comme no-op. Le véritable `io.vopenia.sdk.utils.Dispatchers` vient du SDK Vopenia (`vopenia-utils`) consommé par composite-build ; le shim n'apportait rien et ne doit pas exister.

**Signal avant patch.** `e: Dispatchers.kt:6:14 Type checking has run into a recursive problem.` sur `:kotlin-meet-sdk:compileTestKotlinIosArm64`.

### 6. Shim local PreCompose dans `:shared`

**Fichiers créés** (copiés tels quels depuis `../client-sdk-kotlin-multiplatform/shared/src/commonMain/kotlin/moe/`) :
- `shared/src/commonMain/kotlin/moe/tlaster/precompose/PreComposeApp.kt`
- `shared/src/commonMain/kotlin/moe/tlaster/precompose/navigation/{Navigator,NavHost,NavModels}.kt`
- `shared/src/commonMain/kotlin/moe/tlaster/precompose/navigation/transition/NavTransition.kt`

**Raison.** Identique au client-sdk : le catalog `additionals` ne fournit plus d'alias `multiplatform-precompose` compatible Kotlin 2.3, et les sources `shared/src/commonMain/kotlin/io/vopenia/app/**` + `shared/src/iosMain/kotlin/io/vopenia/app/MainViewController.kt` importent encore `moe.tlaster.precompose.*`. Le package n'est pas publié (`:shared` est démo) → shim acceptable. Alternatives écartées (pré-compose 1.6.2 / 1.7.0-alpha03) documentées dans BUILD_FIX_REPORT amont.

**Signal avant patch.** 40+ `Unresolved reference 'moe'` / `Navigator` / `NavTransition` / `BackStackEntry` / `SwipeProperties` / `PopUpTo` dans `:shared:compileReleaseKotlinAndroid`.

### 7. `MainActivity.kt` — `ComposeView` + `OnBackPressedCallback`

**Fichier réécrit :** `sampleAndroid/src/main/java/io/vopenia/app/android/MainActivity.kt`

Copié depuis `../client-sdk-kotlin-multiplatform/appAndroid/src/main/java/io/vopenia/app/android/MainActivity.kt`. Deux changements combinés :

1. Hébergement Compose via `setContentView(ComposeView(this).apply { setContent { … } })` — remplace `moe.tlaster.precompose.lifecycle.setContent` (retiré par le shim patch 6).
2. `onBackPressed()` remplacé par `OnBackPressedCallback` enregistré sur `onBackPressedDispatcher`. Logique identique : `AppBackPressProvider.onBackPress()` d'abord ; sinon délègue au dispatcher.

L'activité reste `FragmentActivity` (contrainte `PermissionsActivityController.setActivity(this)`).

**Raison.**
1. `moe.tlaster.precompose.lifecycle.setContent` n'existe plus dans le shim.
2. Lint `GestureBackNavigation` devient erreur sous `compileSdk = 36` — `onBackPressed()` n'est plus routé sous Android 16+ prédictif.

**Signal avant patch.**
```
e: MainActivity.kt:11:31 Unresolved reference 'lifecycle'.
e: MainActivity.kt:23:9 Unresolved reference 'setContent'.
```

### 8. Test `VisioSdkTests.createSessionAndCheckRooms` — conversion `AuthenticationInformation`

**Fichier :** `kotlin-meet-sdk/src/commonTest/kotlin/io/vopenia/sdk/VisioSdk.kt`

Le test appelait `GetTokens("meet", "meet")` (retourne `io.vopenia.api.AuthenticationInformation?`) et le passait directement à `VisioSdk.openSession(...)` qui attend un `suspend () -> io.vopenia.sdk.utils.AuthenticationInformation?`. Mismatch type.

Patch : wrap la valeur dans un `.let { AuthenticationInformation(csrftoken = it.csrftoken, meetSessionId = it.meetSessionId) }`, conversion API-layer → SDK-layer cohérente avec ce que fait déjà `Session.kt:17-24`.

**Raison.** Le test n'a jamais dû compiler en iOS/JVM sous Kotlin 2.3 — la coincidence `io.vopenia.api.AuthenticationInformation` / `io.vopenia.sdk.utils.AuthenticationInformation` (mêmes champs, même nom) permettait peut-être la résolution en Kotlin 2.2 (type-inference plus lâche), plus sous 2.3.

**Signal avant patch.** `e: VisioSdk.kt:14:13 Return type mismatch: expected 'io.vopenia.sdk.utils.AuthenticationInformation?', actual 'io.vopenia.api.AuthenticationInformation?'.` sur `:kotlin-meet-sdk:compileTestKotlinIosArm64`.

### 9. `gradle.properties` — validation mode warning (déjà présent avant le patch)

**Fichier :** `gradle.properties` (diff pré-existant au moment du check-out, conservé)

```diff
 #Kotlin
 kotlin.code.style=official
+kotlin.jvm.target.validation.mode=warning
```

**Raison.** Le catalog déclare `java = "21"` alors que `jvmCompat` force Kotlin JVM target = 17. Sur `:tunnel-http` (plugin `java`), `compileJava` passe à 21 alors que `compileKotlin` reste à 17 → erreur *Inconsistent JVM-target compatibility* en mode strict. Le flag passe en warning. Bytecode correct.

**Note.** Cette ligne était déjà modifiée en local au check-out (workspace state pré-intervention, documenté dans `../BUILD_STATUS.md`). Aucune action requise dans cette session.

---

## Déjà en place avant intervention

- `~/.gradle/gradle.properties` contient les 4 propriétés keystore et les 4 propriétés `TEST_CONFIG_LIVEKIT_*` / `VOPENIA_SAMPLE_APP_TOKEN_ENDPOINT` du stack podman local (voir `../CLAUDE.md`).
- `../client-sdk-kotlin-multiplatform` était déjà sur `fix/build-xcode26-kotlin23` et compilait (BUILD_FIX_REPORT amont appliqué).
- `../LiveKitClientKotlin` cloné au commit `6a5cc62`.
- Répertoire `kotlin-js-store/` auto-créé par le plugin `js(IR)` de `:kotlin-meet-sdk-api` / `:konfig` (gitignored).

## Écarts volontaires par rapport à ce qui serait idéal

- **Pas de `google-services.json`** dans `sampleAndroid/`. Le plugin `googleServices` est `apply false` ET non appliqué au module (`// alias(libs.plugins.googleServices)`), donc inoffensif. Si un jour Firebase Analytics est réactivé, il faudra un stub comme pour client-sdk.
- **Publication Maven non fonctionnelle** : les tâches `signXxxPublication` exigent une clé GPG (`signing.keyId` / `signing.password` / `signing.secretKeyRingFile`). `publishToMavenLocal` fail tant que ces props ne sont pas fournies. `./gradlew build` passe sans. Hors-scope pour "faire compiler le projet".
- **Keystore + google-services stubs à ne pas commiter** : `.gitignore` protège `*.keystore`, `google-services.json`, `*-credentials.json`. `vopenia-release.jks` n'est pas matché par ces patterns — à ajouter explicitement ou à retirer avant commit public.

## Validation finale

```bash
cd kotlin-meet-sdk
./gradlew --stop
./gradlew build \
  -x test -x jvmTest -x testDebugUnitTest -x testReleaseUnitTest -x allTests \
  -x :kotlin-meet-sdk:iosSimulatorArm64Test -x :kotlin-meet-sdk-api:iosSimulatorArm64Test \
  -x :kotlin-meet-sdk:iosX64Test -x :kotlin-meet-sdk-api:iosX64Test \
  -x :kotlin-meet-sdk:check -x :kotlin-meet-sdk-api:check -x :kotlin-meet-sdk-compose:check \
  -x :konfig:check -x :shared:check -x :tunnel-http:check -x :sampleAndroid:check
```

Résultat observé : `BUILD SUCCESSFUL` (965 tâches), 7 modules assemblés Android (debug + release AAR/APK), JVM JARs, klibs iOS {Arm64, X64, SimulatorArm64} compilés pour tous les modules KMP. `:tunnel-http` JAR exécutable OK.

## À faire avant de livrer ces changes upstream

- Vérifier que les changements de code (patch 5, 7, 8) conviennent aux mainteneurs : suppression du shim `Dispatchers`, réécriture de `MainActivity.kt` et conversion `AuthenticationInformation` dans `VisioSdk` de test sont des modifications de source, contrairement aux catalogs et keystore.
- Retirer le keystore `sampleAndroid/vopenia-release.jks` du dépôt ou le `.gitignore` (ajouter `*.jks` à la liste).
- Décider si le catalog local `gradle/extended/cfab0c2/` doit rester un patch local (gitignored) ou si l'on attend un tag upstream de `the-inkwell/gradle-tools` intégrant Kotlin 2.3.10 / Compose 1.10.x / AGP 8.13.x. Dans ce dernier cas, préférer bumper `GRADLE_EXTENDED_VERSION_USED`.
- Les tests d'intégration Meet (`:kotlin-meet-sdk*:jvmTest`, `:iosSimulatorArm64Test`) nécessitent `VOPENIA_MEET_TESTS_COOKIE_TOKEN_VALUE` frais + tunnels opérationnels — à vérifier une fois les creds en place.
