# Kotlin Meet SDK — Spécifications Techniques et Fonctionnelles

Document de référence pour le SDK `kotlin-meet-sdk`. Sert de base à toute modification du build ou du code : toute patch doit respecter les contrats listés ici. Complément de `../client-sdk-kotlin-multiplatform/SPECS.md` (dépendance upstream) et `../BUILD_STATUS.md` (état workspace).

---

## 1. Spécifications fonctionnelles

### 1.1 Rôle du SDK

`kotlin-meet-sdk` est la couche "produit Meet" au-dessus du SDK Vopenia (`client-sdk-kotlin-multiplatform`). Il expose :

- un **client REST** typé pour la plateforme Meet (rooms, users, request-entry, waiting-participants, invitations)
- une couche **session / room** qui relie la REST API au SDK LiveKit via le SDK Vopenia
- des composants **Compose Multiplatform** qui adaptent les `VideoView` / `CameraPreviewView` Vopenia à un `Room` Meet
- un helper **tunnel HTTP** (Ktor server) pour tester depuis mobile en exposant un backend local derrière une URL publique (ngrok-like)
- un module **konfig** qui capte les propriétés Gradle côté tests (`VOPENIA_MEET_TESTS_TUNNEL_*`) et les rend accessibles en runtime test

Cible : applications Kotlin (Android, iOS, JVM/Desktop) qui veulent intégrer l'expérience Meet sans ré-implémenter l'orchestration REST + LiveKit.

### 1.2 Modules publics

| Module | Rôle | Publié |
|--------|------|--------|
| `:kotlin-meet-sdk` | Core SDK Meet. Expose `VisioSdk`, `Session`, `Room`, gestion `RequestEntry` / waiting room, wrapping du `io.vopenia.livekit.Room` Vopenia. | oui |
| `:kotlin-meet-sdk-api` | Client REST pur (Ktor) : `Api`, `ApiUsers`, `ApiRooms`, modèles `@Serializable`. Aucun lien avec LiveKit. | oui |
| `:kotlin-meet-sdk-compose` | Composants Compose : `VideoView`, `CameraPreviewView`, `TranscriptionAnimated`, `ScaleType`. Mappe un `Room` Meet vers les composants Vopenia. | oui |
| `:konfig` | `BuildKonfig`-généré : expose les propriétés utilisateur (`tunnelEndpointTokenForwarder`, `tunnelApiForwarder`) comme constantes cross-plateforme. | oui |
| `:tunnel-http` | Serveur Ktor JVM autonome (binaire `application` + `Jar`). Permet à un device mobile de joindre un backend local via un tunnel. | oui (JVM) |
| `:shared` | Module démo KMP pour l'app sample (Compose MP, navigation PreCompose, ViewModel, safearea). **Non publié.** | non |
| `:sampleAndroid` | App Android de démo consommant `:shared`. | non |

### 1.3 Plateformes cibles

| Module | Android | JVM | iOS (arm64+sim) | JS (IR) | macOS | mingw | linux |
|--------|---------|-----|-----------------|---------|-------|-------|-------|
| `:kotlin-meet-sdk` | ✅ | ✅ | ✅ (x64+arm64+sim) | ❌ | ❌ | ❌ | ❌ |
| `:kotlin-meet-sdk-api` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `:kotlin-meet-sdk-compose` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| `:konfig` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `:shared` | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| `:tunnel-http` | — | ✅ only | — | — | — | — | — |

- **Android** : `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36`, Java 17
- **iOS** : `iosArm64`, `iosX64`, `iosSimulatorArm64`. Deployment target **16.0**. Bloc `cocoapods { ... }` actuellement **commenté** dans `shared/build.gradle.kts` et `kotlin-meet-sdk-compose/build.gradle.kts` — l'intégration iOS des modules non-api passe par le composite-build upstream (`client-sdk-kotlin-multiplatform`) qui embarque déjà `LiveKitClient` + `LiveKitClientKotlin`.
- **JVM/Desktop** : Java 17 (imposé par `jvmCompat`)

### 1.4 API principales

#### `VisioSdk` (module `:kotlin-meet-sdk`)

```kotlin
object VisioSdk {
    fun openSession(
        prefixHttp: String,
        enableHttpLog: Boolean,
        refreshAuthentication: suspend () -> AuthenticationInformation?
    ): Session
}
```

Point d'entrée singleton. `refreshAuthentication` est rappelée à chaque requête pour relire le cookie `sessionid` (expire silencieusement côté serveur).

#### `Session` (module `:kotlin-meet-sdk`)

- `createRoom(name, accessLevel): Room`
- `rooms(): List<Room>` (paginé en interne via `getAllRooms`)
- `room(slug): Room?`
- `roomFromPushedValues(id, name, slug, accessLevel, isAdministrable, livekitUrl?, livekitRoom?, livekitToken?)` — chemin d'entrée pour notifications push (FCM/APNs) qui poussent les credentials LiveKit hors-bande.

`Session` maintient en interne une `mutableListOf<Room>` et dédoublonne par `id`.

#### `Room` (module `:kotlin-meet-sdk`)

- `connect(enableMicrophone = true)` — délègue au `io.vopenia.livekit.Room` sous-jacent avec l'URL/token LiveKit issus soit du `request-entry`, soit du payload pushé, soit de l'`ApiRoom` directement.
- `disconnect()`
- `connectionState: StateFlow<ConnectionState>` — `Default`, `Connecting`, `Connected`, `Disconnected`, `ConnectionError(err)`. Mappé depuis celui du SDK Vopenia via `map(scope) { it.to() }`.
- `localParticipant`, `remoteParticipant` (= `remoteParticipants`) — exposition directe du Vopenia SDK.
- `requestEntry(userName): RequestEntryManagement` — démarre le flux lobby waiting-room.
- `acceptWaitingParticipant(participantId, accept)`
- `waitingParticipants(): List<RequestEntryAnswer>`
- `overriddenUsername: String?` — nom réellement utilisé (celui du request-entry l'emporte sur le profil).

#### `Room.RequestEntryManagement` (module `:kotlin-meet-sdk`)

- `checkEntry(): RequestEntryAnswer` — poll du statut waiting-room côté backend.
- `currentRequestEntryStatus`, `currentApiRequestEntryStatus`

#### `Api` (module `:kotlin-meet-sdk-api`)

```kotlin
class Api(
    prefix: String,
    enableHttpLogs: Boolean = false,
    getAuthent: suspend () -> AuthenticationInformation?
) {
    val users: ApiUsers
    val rooms: ApiRooms
}
```

`AuthenticationInformation` : `@Serializable data class AuthenticationInformation(val csrftoken: String, val meetSessionId: String)`.

#### `ApiRooms` (module `:kotlin-meet-sdk-api`)

- `createRoom(param: NewRoomParam): ApiRoom`
- `createRoom(id, param): ApiRoom` / `updateRoom(id, param)` / `deleteRoom(id)`
- `room(slug): ApiRoom?`
- `rooms(): Page<ApiRoom>` / `rooms(page: Int): Page<ApiRoom>`
- `invite(id, emails: List<String>)` / `invite(id, vararg emails)`
- `validateParticipantEntry(id, participantId, allowEntry)`
- `requestEntry(id, userName): ApiRequestEntryAnswer`
- `requestEntry(id, previousRequest): ApiRequestEntryAnswer`
- `waitingParticipants(id): WaitingParticipants`

#### `ApiUsers` (module `:kotlin-meet-sdk-api`)

- `users(): Page<ApiUser>`
- `me(): ApiUser`

#### Compose (module `:kotlin-meet-sdk-compose`)

- `@Composable VideoView(modifier, room: Room, track: IVideoTrack, scaleType: ScaleType, isMirror: Boolean = false)` — délègue à `io.vopenia.livekit.compose.VideoView` via `RoomProxyAccessor`.
- `@Composable CameraPreviewView(...)`
- `@Composable TranscriptionAnimated(...)`
- `enum ScaleType { Fill, Fit }` — mappé sur `io.vopenia.livekit.compose.ScaleType`.

#### `PermissionsActivityController` (Android-only, module `:kotlin-meet-sdk`)

Proxy de `io.vopenia.livekit.PermissionsActivityController` ; l'app hôte doit appeler `setActivity(FragmentActivity)` avant `Room.connect(enableMicrophone = true)`.

### 1.5 Modèles REST (`:kotlin-meet-sdk-api`)

Tous `@Serializable` (kotlinx.serialization JSON). Principaux :

- `ApiRoom(id, name, slug, accessLevel, isAdministrable, accesses, livekit?)`
- `NewRoomParam(name, configuration, accessLevel)`
- `ApiRoomAccessLevel` (enum)
- `ApiRequestEntryAnswer(id, username, status, livekit?)` / `ApiRequestEntryStatus` (enum)
- `RequestEntryParameter(username)` / `RoomEnterParameter(participantId, accept)`
- `WaitingParticipants(participants: List<ApiRequestEntryAnswer>)`
- `Livekit(url, room, token)` + `Livekit.Companion.from(url?, room?, token?)` (extension dans `:kotlin-meet-sdk/Session.kt`)
- `InviteEmails(emails: List<String>)`
- `Page<T>(count, next?, previous?, results: List<T>)`
- `ApiUser(...)`

### 1.6 `:tunnel-http` — serveur d'entraide mobile

Petit serveur Ktor JVM :
- main class : `io.vopenia.ApplicationKt`
- Ktor server (Netty) + content-negotiation + compression + CORS + JSON
- Usage : exposer un endpoint local (port 8181) accessible via ngrok depuis un device mobile, pour échanger les credentials `sessionid` durant les tests `:kotlin-meet-sdk-api:*Test` sur Android/iOS.
- Packagé en JAR exécutable (`Main-Class` dans le manifest).

### 1.7 `:konfig` — BuildKonfig

Génère `io.vopenia.konfig.BuildKonfig` à partir des propriétés Gradle :

- `VOPENIA_MEET_TESTS_TUNNEL_ENDPOINT` → `Konfig.tunnelEndpointTokenForwarder`
- `VOPENIA_MEET_TESTS_TUNNEL_API` → `Konfig.tunnelApiForwarder`

Ces valeurs sont **requises au moment de la configuration Gradle** (elles sont lues via `rootProject.extra[...]`). Des valeurs vides sont acceptées et renvoient une chaîne vide à runtime ; elles font juste échouer les tests d'intégration.

### 1.8 `:shared` + `:sampleAndroid` — app de démo

- `:shared` : Compose MP, navigation PreCompose (`moe.tlaster.precompose:*`), ViewModel MP, safearea, widgets `eu.codlab`, viewmodel, sentry. Navigation multi-écrans : Initialize → Join → Room → Settings.
- `:sampleAndroid` : `Application`-less (via `appDistribution`), une `MainActivity` qui monte `App(isDarkTheme)` de `:shared`. Firebase Analytics (BoM 33.1.1). Plugin `googleServices` **commenté** dans le build courant (le plugin est déclaré `apply false` dans `build.gradle`).
- iOS : pas de repo `appIos` dans ce dépôt — l'intégration iOS consommatrice vit dans `../kotlin-multiplatform-visio/`.

---

## 2. Spécifications techniques

### 2.1 Structure KMP

Identique à client-sdk :
```
<module>/src/
  commonMain/kotlin/
  androidMain/kotlin/      (uniquement pour :kotlin-meet-sdk)
  iosMain/kotlin/          (uniquement pour :shared)
  jvmMain/kotlin/          (uniquement pour :shared)
  commonTest/kotlin/       (:kotlin-meet-sdk, :kotlin-meet-sdk-api)
```
Pas d'`expect`/`actual` dans les sources Meet ; les pattern-matchs plateforme sont délégués au SDK Vopenia.

`:tunnel-http` n'est **pas** KMP : module Kotlin/JVM pur sous `src/main/kotlin`.

### 2.2 Stack technique (baseline actuel du dépôt, avant patches)

| Composant | Version |
|-----------|---------|
| Kotlin | 2.2.20 (catalog `cfab0c2`) |
| KSP | 2.2.20-2.0.3 |
| Compose Multiplatform | 1.8.2 |
| Android Gradle Plugin | 8.10.1 |
| Gradle wrapper | 8.14.3 |
| Ktor | 3.0.1 (`libs.versions.toml`) |
| kotlinx-coroutines | 1.9.0 |
| kotlinx-serialization | JSON |
| BuildKonfig | (via `additionals.plugins.multiplatform.buildkonfig`) |
| SDK Vopenia (`io.vopenia:vopenia*`) | `0.0.9-alpha6` en CI Maven / **composite-build local** via `includeBuild("../client-sdk-kotlin-multiplatform")` |
| Google Services | 4.4.2 (plugin `apply false` uniquement) |
| Firebase App Distribution Gradle | 5.1.1 |
| Dokka / Detekt / KtLint / Sonarqube | 1.9.20 / 1.21.0 / 1.7.1 / 2.8 |

### 2.3 Plugin Gradle partagé

- Source : `https://github.com/the-inkwell/gradle-tools`
- Pinné par `GRADLE_EXTENDED_VERSION_USED=cfab0c2` dans `gradle.properties` (≠ client-sdk qui utilise `6d42ea5`)
- Fichier local : `gradle/extended/cfab0c2/` — copie éditable
- `settings.gradle` applique `extended.gradle` depuis le raw GitHub et `includeBuild("gradle/extended/cfab0c2/kt-plugins")`
- Plugins id-locaux fournis : `jvmCompat` (Java 17), `iosSimulatorConfiguration`, `publication`
- Version catalog partagé (alias `additionals`) : `gradle/extended/cfab0c2/libs.versions.toml`

### 2.4 Composite build avec client-sdk

`settings.gradle` :
```groovy
includeBuild("../client-sdk-kotlin-multiplatform") {
    dependencySubstitution {
        substitute module("io.vopenia:vopenia") using project(":vopenia")
        substitute module("io.vopenia:vopenia-compose") using project(":vopenia-compose")
        substitute module("io.vopenia:vopenia-participants") using project(":vopenia-participants")
        substitute module("io.vopenia:vopenia-utils") using project(":vopenia-utils")
    }
}
```

- **Répertoires sibling obligatoires** : `../client-sdk-kotlin-multiplatform/` et (transitivement) `../LiveKitClientKotlin/`.
- Les versions Kotlin/Compose/AGP **doivent rester compatibles** entre les deux catalogs. Si client-sdk bump à Kotlin 2.3.10 (catalog `6d42ea5`-patché), kotlin-meet-sdk doit bumper aussi, sinon la compilation composite mélange deux metadata Kotlin incompatibles.
- En CI/release : publier d'abord client-sdk (`./gradlew publishToMavenLocal`) puis build meet-sdk — le `dependencySubstitution` reste inerte si le composite directory n'existe pas mais il faut avoir les artefacts Maven.

### 2.5 Namespaces

| Zone | Namespace |
|------|-----------|
| SDK core | `io.vopenia.sdk` (Android namespace = `io.vopenia`, lu via `rootProject.ext.group`) |
| API | `io.vopenia.api` (Android namespace = `io.vopenia.api`) |
| Compose | `io.vopenia.sdk.compose` (Android namespace = `io.vopenia.compose`) |
| Konfig | `io.vopenia.konfig` |
| Tunnel HTTP | `io.vopenia` (JVM only) |
| App démo partagée | `io.vopenia.meet.shared` |
| App Android sample | `io.vopenia.meet` |
| Group Maven publication | `io.vopenia` (via `defaultGroup` dans `gradle/versions.gradle`) |

Sources Kotlin sous `io/vopenia/...`, cohérent avec le namespace courant.

**Alignement upstream :** le SDK client (`../client-sdk-kotlin-multiplatform`) expose ses classes sous `io.vopenia.*` (`io.vopenia.livekit.*`, `io.vopenia.sdk.utils.*`, etc.) — les imports meet-sdk résolvent directement dessus, aucun typealias requis.

### 2.6 Conventions

- **Serialization** : `kotlinx.serialization` JSON (via le catalog `additionals`). Pas de Moshi/Gson dans le SDK ; `:sampleAndroid` peut tirer Firebase Gson transitivement sans impact.
- **State** : pas de `StateFlow` directement côté Meet ; le state exposé (`connectionState`) provient du SDK Vopenia (mapping via extension `map`).
- **Typesafe project accessors** : activés (`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`). Utiliser `projects.kotlinMeetSdkApi` plutôt que `project(":kotlin-meet-sdk-api")`.
- **Composite-build accessors** : pas d'alias typesafe cross-build pour `io.vopenia:vopenia*` — se référer à `libs.vopenia*` du catalog local (version Maven de repli) ou laisser la substitution `dependencySubstitution` basculer sur le projet local.
- **HTTP** : Ktor (client + server). Le client passe par `eu.codlab.http.createClient` (alias `additionals.multiplatform.http.client`).
- **Build files** : jamais éditer sous `build/`.

### 2.7 Configuration Android

- `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36`
- `useAndroidX = true`, `nonTransitiveRClass = true`
- Signature release `:sampleAndroid` : propriétés `VOPENIA_STORE_FILE`, `VOPENIA_STORE_PASSWORD`, `VOPENIA_KEY_ALIAS`, `VOPENIA_KEY_PASSWORD` (dans `~/.gradle/gradle.properties`). Le bloc `signingConfigs` ne crée `release` que si le `.jks` existe.
- Firebase App Distribution : active uniquement si `vopenia-service-crendentials.json` est présent dans `sampleAndroid/`.
- `google-services.json` pour `:sampleAndroid` : **pas stubé** à date (plugin `googleServices` commenté, donc non bloquant pour `./gradlew build`).

### 2.8 Configuration iOS

- Deployment target **16.0**
- `kotlin.mpp.enableCInteropCommonization=true`
- Pas de bloc `cocoapods { ... }` actif dans ce dépôt : tous les blocs cocoapods sont commentés dans `:shared` et `:kotlin-meet-sdk-compose`. La consommation iOS de LiveKit vient entièrement du SDK Vopenia via le composite-build.
- Configuration cache : non désactivée explicitement ici, mais incompatible avec `podInstallSyntheticIos` du SDK Vopenia upstream — s'aligner si nécessaire.
- `iOSSimulatorUuid` (gradle.properties) requis pour `:kotlin-meet-sdk-api:iosSimulatorArm64Test`.

### 2.9 Configuration JVM/Desktop

- `JvmTarget.JVM_17` imposé par `jvmCompat`
- `:tunnel-http` packagé via plugin `application`, JAR manifest `Main-Class=io.vopenia.ApplicationKt`
- Pas d'app Compose Desktop dans ce dépôt (contrairement à client-sdk / visio) — seul `:shared:jvmMain` tire `compose.desktop.currentOs` pour préviews.

### 2.10 Publication

- Repo : Maven Central via Sonatype (Nexus Publish Plugin 2.3.1)
- Endpoints : `https://ossrh-staging-api.central.sonatype.com/service/local/`, snapshot `https://central.sonatype.com/repository/maven-snapshots/`
- Credentials : `sonatypeUsername`, `sonatypePassword`
- Version courante : `0.0.1-alpha8` (`gradle/versions.gradle`)
- Group : `io.vopenia` (override via propriété `GROUP_ID`)
- Suffix version : via propriété `SUFFIX` (ex. `-SNAPSHOT`)

### 2.11 Tests

- Tests unitaires : `:kotlin-meet-sdk:commonTest`, `:kotlin-meet-sdk-api:commonTest` — nécessitent `Konfig.tunnelApiForwarder` non vide et un cookie Meet valide (`VOPENIA_MEET_TESTS_COOKIE_TOKEN_VALUE`).
- Cookie `sessionid` copié depuis le web Meet (dev tools → cookies) ; expire silencieusement → re-copier quand les tests commencent à 401.
- Tests iOS simulator : `:kotlin-meet-sdk-api:iosSimulatorArm64Test` nécessite `iOSSimulatorUuid`.
- Meet-SDK teste essentiellement sur **JVM** — autres plateformes peuvent tomber sur des erreurs certificat backend.

### 2.12 Workspace parent

```
Sources/
  client-sdk-kotlin-multiplatform/   (dépendance upstream, composite-build)
  kotlin-meet-sdk/                   (ce dépôt)
  kotlin-multiplatform-visio/        (consommateur aval : app Compose MP)
  LiveKitClientKotlin/               (wrapper pod, sibling requis par client-sdk)
  vopenia-podman-testenv/            (stack LiveKit local facultatif)
```

---

## 3. Invariants de build à respecter

Toute modification de build doit préserver :

1. `./gradlew :kotlin-meet-sdk-api:assembleDebug :kotlin-meet-sdk-api:jvmJar` passe
2. `./gradlew :kotlin-meet-sdk:assembleDebug :kotlin-meet-sdk:jvmJar` passe
3. `./gradlew :kotlin-meet-sdk-compose:assembleDebug :kotlin-meet-sdk-compose:jvmJar` passe
4. `./gradlew :konfig:assembleDebug :konfig:jvmJar` passe
5. `./gradlew :tunnel-http:jar` produit un JAR exécutable (`Main-Class` dans manifest)
6. Publications Maven-local : `./gradlew publishToMavenLocal` produit 4 artefacts `io.vopenia:{kotlin-meet-sdk,kotlin-meet-sdk-api,kotlin-meet-sdk-compose,konfig}:0.0.1-alpha8` (+ tunnel JVM si activé)
7. `./gradlew :kotlin-meet-sdk-api:jvmTest :kotlin-meet-sdk:jvmTest` verts sous condition `VOPENIA_MEET_TESTS_COOKIE_TOKEN_VALUE` + `VOPENIA_MEET_TESTS_TUNNEL_API` présents (skippable si variables vides)
8. Contrats publics préservés :
   - `VisioSdk.openSession(...)` / `Session.{createRoom,rooms,room,roomFromPushedValues}`
   - `Room.{connect,disconnect,connectionState,localParticipant,remoteParticipant,requestEntry,acceptWaitingParticipant,waitingParticipants}`
   - `Room.RequestEntryManagement.{checkEntry, currentRequestEntryStatus}`
   - `Api.{users,rooms}` + signatures `ApiRooms` / `ApiUsers` inchangées
   - Compose : `VideoView`, `CameraPreviewView`, `TranscriptionAnimated`, `ScaleType`
9. Le `dependencySubstitution` composite-build vers `../client-sdk-kotlin-multiplatform/` reste fonctionnel (pas de substitution via `io.vopenia:vopenia-test-config` — ce module n'est pas consommé ici).
10. `:shared` et `:sampleAndroid` restent démo (non publiés), les 5 modules `:kotlin-meet-sdk*` / `:konfig` / `:tunnel-http` restent les unités publiées.
