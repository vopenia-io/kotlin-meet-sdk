# Kotlin Meet SDK - Documentation de l'API

## Vue d'ensemble

Le **Kotlin Meet SDK** est un SDK Kotlin Multiplatform qui fournit une couche métier complète pour intégrer un client de visioconférence dans une application. Il s'appuie sur [LiveKit](https://livekit.io/) pour le transport audio/vidéo en temps réel et expose une API REST pour la gestion des salles, des utilisateurs et du contrôle d'accès.

Le SDK est structuré en **3 modules publiés** qui peuvent être consommés indépendamment :

| Module | Artifact Maven | Rôle |
|--------|---------------|------|
| `:kotlin-meet-sdk` | `io.vopenia:kotlin-meet-sdk` | Couche métier principale : sessions, salles, connexion LiveKit, salle d'attente |
| `:kotlin-meet-sdk-api` | `io.vopenia:kotlin-meet-sdk-api` | Client REST pur (Ktor) : appels HTTP vers le backend Meet |
| `:kotlin-meet-sdk-compose` | `io.vopenia:kotlin-meet-sdk-compose` | Composants UI Compose Multiplatform : rendu vidéo, prévisualisation caméra, transcription |

**Plateformes supportées** : Android, iOS (arm64, x64, simulateur), JVM/Desktop. Le module API supporte en plus JS, macOS, Linux et Windows.

---

## Architecture en couches

```
┌─────────────────────────────────────────────┐
│              Application cliente             │
├─────────────────────────────────────────────┤
│         kotlin-meet-sdk-compose             │  ← Composants UI (VideoView, CameraPreview)
├─────────────────────────────────────────────┤
│            kotlin-meet-sdk                  │  ← Orchestration métier (Session, Room)
├─────────────────────────────────────────────┤
│          kotlin-meet-sdk-api                │  ← Client REST HTTP
├──────────────────────┬──────────────────────┤
│   Backend Meet REST  │  LiveKit (WebSocket) │  ← Serveurs distants
└──────────────────────┴──────────────────────┘
```

---

## 1. Authentification

### Concept métier

L'authentification repose sur un mécanisme de **cookies de session** (CSRF token + session ID). Le SDK ne gère pas lui-même l'obtention des credentials : c'est l'application cliente qui fournit un callback `refreshAuthentication` appelé **avant chaque requête HTTP**. Cela permet de gérer :

- La relecture de cookies navigateur (WebView)
- Le rafraîchissement de tokens OAuth
- L'expiration silencieuse côté serveur

### Modèle

```kotlin
data class AuthenticationInformation(
    val csrftoken: String,     // Token CSRF pour la protection des requêtes
    val meetSessionId: String  // Identifiant de session Meet (cookie "sessionid")
)
```

### Mécanisme interne

Chaque requête HTTP est envoyée avec :
- Un header `Cookie` contenant `csrftoken=...;sessionid=...`
- Un header `x-csrftoken` pour la validation CSRF
- Un header `Referer` pointant vers le host du backend

---

## 2. Point d'entrée : `VisioSdk`

### Concept métier

`VisioSdk` est le **singleton d'entrée** du SDK. Son unique rôle est de créer une `Session` configurée avec l'URL du backend et le mécanisme d'authentification.

### API

```kotlin
object VisioSdk {
    fun openSession(
        prefixHttp: String,
        enableHttpLog: Boolean,
        refreshAuthentication: suspend () -> AuthenticationInformation?
    ): Session
}
```

| Paramètre | Description |
|-----------|-------------|
| `prefixHttp` | URL de base de l'API REST Meet (ex: `https://meet.example.com/api/v1.0`) |
| `enableHttpLog` | Active les logs HTTP Ktor pour le debug |
| `refreshAuthentication` | Callback appelé avant chaque requête pour fournir les credentials. Retourner `null` signifie "non authentifié". |

### Exemple d'utilisation

```kotlin
val session = VisioSdk.openSession(
    prefixHttp = "https://meet.example.com/api/v1.0",
    enableHttpLog = BuildConfig.DEBUG,
    refreshAuthentication = {
        // Lire les cookies depuis le WebView ou le storage
        val csrf = cookieStore.get("csrftoken")
        val sessionId = cookieStore.get("sessionid")
        if (csrf != null && sessionId != null) {
            AuthenticationInformation(csrf, sessionId)
        } else null
    }
)
```

---

## 3. Session

### Concept métier

La `Session` représente **la connexion authentifiée d'un utilisateur** au service Meet. Elle gère le cycle de vie des salles de réunion : création, récupération, déduplication.

Un point important : la Session **maintient un cache interne des objets `Room`**. Si une salle est récupérée plusieurs fois (par slug, par liste, ou via push), le même objet Kotlin est réutilisé et mis à jour. Cela garantit que les `StateFlow` (état de connexion, participants) restent cohérents.

### API

```kotlin
class Session {

    // Récupérer l'utilisateur authentifié courant
    suspend fun me(): User

    // Créer une nouvelle salle (accessLevel optionnel — valeur par défaut côté backend si null)
    suspend fun createRoom(name: String, accessLevel: RoomAccessLevel? = null): Room

    // Lister toutes les salles de l'utilisateur (avec pagination automatique)
    suspend fun rooms(): List<Room>

    // Récupérer une salle par son slug (identifiant URL-friendly)
    suspend fun room(slug: String): Room?

    // Construire une Room à partir de données push (Firebase/APNs)
    fun roomFromPushedValues(
        id: String,
        name: String,
        slug: String,
        accessLevel: RoomAccessLevel,
        isAdministrable: Boolean,
        livekitUrl: String? = null,
        livekitRoom: String? = null,
        livekitToken: String? = null
    ): Room

    // Enregistrement des devices pour les notifications push (voir section Devices)
    val devices: Devices

    // Enregistrements (recordings) — listing, récupération, suppression
    suspend fun recordings(page: Int = 0): List<Recording>
    suspend fun recording(id: String): Recording?
    suspend fun deleteRecording(id: String)
}
```

### Détails des méthodes

#### `createRoom(name, accessLevel)`

Crée une salle sur le backend via `POST /rooms/` et retourne un objet `Room` prêt à l'emploi. Le backend génère automatiquement le `slug` et l'`id`.

#### `rooms()`

Récupère **toutes** les salles de l'utilisateur (gestion automatique de la pagination côté API). Met à jour les métadonnées des salles déjà connues et crée des objets `Room` pour les nouvelles.

#### `room(slug)`

Récupère une salle spécifique par son slug. Retourne `null` si la salle n'existe pas ou n'est pas accessible. Le slug est l'identifiant URL-friendly de la salle (ex: `reunion-equipe-dev`).

#### `roomFromPushedValues(...)`

**Cas d'usage push notification** : quand l'app reçoit une notification Firebase/APNs contenant les informations d'une salle et potentiellement les credentials LiveKit, cette méthode permet de construire un objet `Room` **sans appel réseau**. Si les credentials LiveKit sont fournis, l'utilisateur peut se connecter directement à la visio.

#### `me()`

Récupère le profil de l'utilisateur authentifié courant (`GET /users/me`). Lève `ApiException` si l'utilisateur n'est pas authentifié.

#### `recordings(page)` / `recording(id)` / `deleteRecording(id)`

Façade haut niveau sur le module `ApiRecordings`. Voir la section dédiée aux modèles façade `Recording` / `RecordingStatus`.

- `recordings(page)` : liste les enregistrements appartenant à l'utilisateur courant. **Convention : `page = 0` retourne la première page** (équivalent de l'endpoint sans paramètre) ; toute valeur strictement positive correspond à la page demandée côté backend.
- `recording(id)` : récupère un enregistrement unique. Retourne `null` si l'enregistrement n'existe pas ou n'est pas accessible (404/403 captés silencieusement).
- `deleteRecording(id)` : supprime un enregistrement. Le backend ne l'autorise que lorsque le statut est final (cf. `RecordingStatus.isFinal`).

#### `devices`

Accesseur sur la façade `Devices` (enregistrement des devices pour les notifications push). Voir la section « Devices ».

---

## 4. Room (Salle de réunion)

### Concept métier

La `Room` est **l'objet central du SDK**. Elle représente une salle de visioconférence et encapsule :

- Les **métadonnées** (nom, slug, niveau d'accès, droits d'administration)
- La **connexion LiveKit** (audio/vidéo en temps réel)
- La **gestion de la salle d'attente** (waiting room / lobby)
- Les **participants** (local + distants)

### Propriétés

`Room` est une `data class` dont **seul `id` (et la `ApiRoom` originale) sont des paramètres de constructeur** ; les autres champs publics ci-dessous sont des propriétés calculées dérivées de l'`ApiRoom` interne.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
data class Room(
    private val session: Session,
    val originalRoom: ApiRoom,
    val id: String
) {
    // Métadonnées (computed properties, déléguées à l'ApiRoom interne)
    val name: String
    val slug: String
    val accessLevel: RoomAccessLevel
    val accesses: List<Access>
    val isAdministrable: Boolean

    // État de connexion LiveKit (observable via StateFlow)
    val connectionState: StateFlow<ConnectionState>

    // Participants — types exposés par le module LiveKit interne
    val localParticipant: LocalParticipant                       // io.vopenia.livekit.participant.local.LocalParticipant
    val remoteParticipant: StateFlow<List<RemoteParticipant>>    // io.vopenia.livekit.participant.remote.RemoteParticipant

    // Nom d'utilisateur surchargé (défini lors du requestEntry)
    val overriddenUsername: String?
}
```

> **Note** — `remoteParticipant` est un **`StateFlow<List<RemoteParticipant>>`**, pas une simple `List`. Pour réagir aux arrivées/départs de participants, collecter ce flow (ex. via `collectAsState()` en Compose, ou `combine` / `flatMapLatest` côté Kotlin).

### Connexion à la visioconférence

```kotlin
// Se connecter à la salle (lance le flux audio/vidéo via LiveKit)
suspend fun connect(enableMicrophone: Boolean = true)

// Se déconnecter
fun disconnect()
```

**Prérequis** : des credentials LiveKit doivent être disponibles. Ils sont obtenus soit :
1. Via le champ `livekit` de l'`ApiRoom` (pour les salles publiques/trusted où l'utilisateur a déjà accès)
2. Via le mécanisme de `requestEntry` (salle d'attente) quand le statut passe à `Accepted`
3. Via les données push (`roomFromPushedValues`)

Si aucun credential n'est disponible, `connect()` **et** `disconnect()` lèvent une `IllegalStateException`.

### Machine à états de connexion

```kotlin
sealed class ConnectionState {
    data object Default         // État initial, pas de tentative de connexion
    data object Connecting      // Connexion en cours vers LiveKit
    data object Connected       // Connecté, flux audio/vidéo actifs
    data object Disconnected    // Déconnecté proprement
    class ConnectionError(      // Erreur de connexion
        val error: Throwable
    )
}
```

Observable via `room.connectionState` (Kotlin `StateFlow`), ce qui permet de réagir aux changements dans l'UI.

### Chat

```kotlin
val chatMessages: Flow<ChatMessage>
suspend fun sendChatMessage(text: String): ChatMessage
```

Messages échangés en temps réel dans la salle. Aucun stockage backend : le transport utilise le topic LiveKit réservé `lk-chat-topic`. Les consommateurs maintiennent leur propre historique. Voir le modèle `ChatMessage` dans la section « Modèles façade ».

### Réactions

```kotlin
val reactions: Flow<Reaction>
suspend fun sendReaction(emoji: MeetReactionEmoji)
suspend fun sendReaction(emoji: String)
```

Réactions éphémères (pas de persistance, pas de replay). Transport : enveloppe JSON `{"type":"reactionReceived","data":{"emoji":"..."}}` publiée sur le canal data LiveKit par défaut, sans topic. L'overload typé `MeetReactionEmoji` garantit que la valeur sera affichée par Meet Web ; un emoji passé en `String` qui n'appartient pas au catalogue `MeetReactionEmoji` est silencieusement ignoré côté Web.

### Raise hand (lever / baisser la main)

```kotlin
val handStates: StateFlow<Map<String, Boolean>>   // participantIdentity → main levée
suspend fun raiseHand(raised: Boolean)
```

État agrégé `identité de participant → main levée` (local + distants), dérivé de l'attribut LiveKit `handRaisedAt` (timestamp ISO-8601 quand la main est levée, chaîne vide quand baissée) — convention partagée avec le frontend Meet Web.

**Envoi** : `raiseHand(raised)` passe par l'endpoint backend `POST rooms/{id}/toggle-hand/`, authentifié par le **token LiveKit dans l'en-tête `Authorization: Bearer`** — et **non** par une écriture d'attribut côté client. Le backend Meet révoque `canUpdateOwnMetadata` sur le token (anti-spoofing) : une écriture directe `setAttributes("handRaisedAt", …)` serait rejetée (`NOT_ALLOWED`) et ne se propagerait jamais. Le backend estampille `handRaisedAt` et l'écrit via le SDK serveur ; le changement est ensuite diffusé à tous les participants, **y compris au participant local** (changement initié serveur → reçu via l'écho `AttributesChanged`, donc l'affichage local reflète l'état réel).

### Recording (enregistrement)

```kotlin
val recordingState: StateFlow<RecordingState>
suspend fun startRecording(
    mode: RecordingMode = RecordingMode.ScreenRecording
): RecordingHandle
suspend fun stopRecording()
```

Démarre / arrête un enregistrement Egress côté backend. **Admin/owner uniquement**. La machine à états `RecordingState` (voir section « Modèles façade ») reflète l'état en vol côté client. La complétion finale du fichier (`Saved`) est signalée côté backend par un webhook : pour récupérer l'asset, consulter périodiquement `Session.recordings()` ou s'abonner aux notifications push. Les modes disponibles sont :

- `RecordingMode.ScreenRecording` → MP4 composite vidéo
- `RecordingMode.Transcript` → audio pour transcription ASR offline

### Transcription (sous-titres temps réel)

```kotlin
val transcription: Flow<TranscriptionSegment>
suspend fun startTranscription(language: String): TranscriptionHandle
```

Démarre l'agent ASR backend pour la salle. **Admin/owner uniquement**. Les segments arrivent via le canal LiveKit dédié et sont agrégés sur l'ensemble des participants (local + distants). Notes :

- `TranscriptionSegment` provient du module LiveKit interne (`io.vopenia.livekit.participant.transcription`).
- Il n'existe **pas** d'endpoint de stop : l'agent ASR se termine automatiquement avec la salle. Le `TranscriptionHandle.stop()` est un no-op fourni pour la symétrie d'API.
- Le paramètre `language` est conservé dans le handle mais n'est pas encore propagé au backend (la langue de l'agent est configurée côté serveur).

### Actions admin sur participants

```kotlin
suspend fun muteParticipantTrack(participantIdentity: String, trackSid: String)
suspend fun updateParticipantAttributes(
    participantIdentity: String,
    attributes: Map<String, String>
)
suspend fun removeParticipant(participantIdentity: String)
```

Actions serveur sur les participants distants. **Admin/owner uniquement** ; le backend valide les droits avant d'appliquer.

- `muteParticipantTrack` : mute côté serveur un track audio/vidéo spécifique (identifié par son `trackSid`) d'un participant donné.
- `updateParticipantAttributes` : remplace / met à jour les attributs serveur d'un participant. Seules les clés fournies sont écrites.
- `removeParticipant` : déconnecte le participant de la room LiveKit (kick).

---

## 5. Salle d'attente (Waiting Room / Lobby)

### Concept métier

La salle d'attente est le mécanisme de **contrôle d'accès en temps réel**. Quand une salle est en accès `Restricted` (ou selon la configuration), un participant qui veut rejoindre la visio doit :

1. **Demander l'entrée** (`requestEntry`) en fournissant son nom d'affichage
2. **Attendre l'approbation** d'un administrateur (polling via `checkEntry`)
3. Une fois **accepté**, les credentials LiveKit sont automatiquement délivrés

Côté administrateur, le flux est :
1. **Consulter** la liste des participants en attente (`waitingParticipants`)
2. **Accepter ou refuser** chaque participant (`acceptWaitingParticipant`)

### API côté participant

```kotlin
// Demander à entrer dans la salle
suspend fun requestEntry(userName: String): RequestEntryManagement

class RequestEntryManagement {
    // État actuel de la demande
    val currentRequestEntryStatus: RequestEntryAnswer

    // Vérifier l'évolution de la demande (polling)
    suspend fun checkEntry(): RequestEntryAnswer
}
```

### API côté administrateur

```kotlin
// Lister les participants en attente
suspend fun waitingParticipants(): List<RequestEntryAnswer>

// Accepter ou refuser un participant
suspend fun acceptWaitingParticipant(participantId: String, accept: Boolean)
```

### Machine à états de la demande d'entrée

```kotlin
enum class RequestEntryStatus {
    Idle,      // Aucune demande en cours
    Waiting,   // En attente de validation par un admin
    Accepted,  // Accepté → credentials LiveKit disponibles, peut appeler connect()
    Denied,    // Refusé par un admin
    Timeout    // Délai d'attente dépassé, la demande a expiré
}
```

### Modèle de réponse

```kotlin
data class RequestEntryAnswer(
    val id: String,                  // ID du participant dans le lobby
    val username: String,            // Nom d'affichage choisi
    val status: RequestEntryStatus,  // État courant
    val color: String                // Couleur attribuée (pour l'avatar)
)
```

### Flux complet participant

```kotlin
// 1. Demander l'entrée
val entry = room.requestEntry("Jean Dupont")

// 2. Polling jusqu'à acceptation
while (entry.currentRequestEntryStatus.status == RequestEntryStatus.Waiting) {
    delay(2000)
    entry.checkEntry()
}

// 3. Si accepté, se connecter
if (entry.currentRequestEntryStatus.status == RequestEntryStatus.Accepted) {
    room.connect()
}
```

### Flux complet administrateur

```kotlin
// 1. Récupérer les participants en attente
val waiting = room.waitingParticipants()

// 2. Accepter un participant
room.acceptWaitingParticipant(
    participantId = waiting.first().id,
    accept = true
)
```

---

## 6. Niveaux d'accès des salles

### Concept métier

Chaque salle a un niveau d'accès qui détermine qui peut la rejoindre et sous quelles conditions.

```kotlin
enum class RoomAccessLevel {
    Public,      // Ouvert à tous, connexion directe sans approbation
    Trusted,     // Limité aux utilisateurs de confiance (authentifiés)
    Restricted   // Accès restreint, passage obligatoire par la salle d'attente
}
```

| Niveau | Qui peut rejoindre | Salle d'attente | Credentials LiveKit |
|--------|-------------------|-----------------|---------------------|
| `Public` | Tout le monde | Non | Fournis directement dans l'ApiRoom |
| `Trusted` | Utilisateurs authentifiés | Non | Fournis directement dans l'ApiRoom |
| `Restricted` | Sur approbation admin | Oui | Fournis après `requestEntry` → `Accepted` |

---

## 7. Gestion des accès (ACL)

### Concept métier

Chaque salle maintient une liste de contrôle d'accès (ACL) qui définit quels utilisateurs ont quel rôle sur la salle.

```kotlin
data class Access(
    val id: String,        // Identifiant de l'entrée d'accès
    val user: User,        // L'utilisateur concerné
    val resource: String,  // La ressource (identifiant de la salle)
    val role: String       // Le rôle (ex: "administrator", "member")
)
```

Le champ `isAdministrable` de la `Room` indique si l'utilisateur courant a les droits d'administration (accepter/refuser des participants, inviter, supprimer).

---

## 8. Invitations

### Concept métier

Un administrateur de salle peut inviter des participants par email. Le backend envoie alors un email d'invitation avec un lien pour rejoindre la salle.

### API (couche REST)

```kotlin
// Inviter par liste d'emails
suspend fun invite(id: String, emails: List<String>)

// Inviter par vararg
suspend fun invite(id: String, vararg emails: String)
```

**Endpoint REST** : `POST /rooms/{id}/invite/`

---

## 9. Devices (notifications push)

### Concept métier

Pour recevoir des notifications push (entrée en salle d'attente, invitations, etc.), l'application doit enregistrer le device courant auprès du backend. Trois canaux sont supportés :

- **APNS** pour iOS
- **WebPush / FCM** pour Android (transport WebPush avec `browser="android"`)
- **WebPush** pour les navigateurs (transport WebPush avec `browser="browser"`)

### API

```kotlin
class Devices {
    suspend fun registerAndroid(registrationId: String)
    suspend fun registeriOS(registrationId: String, deviceId: String, name: String)
    suspend fun registerBrowser(registrationId: String, p256dh: String, auth: String)
    suspend fun unregisterAndroid(registrationId: String)     // BUG : voir avertissement ci-dessous
    suspend fun unregisteriOS(registrationId: String)
    suspend fun unregisterBrowser(registrationId: String)
}

enum class Platform { Android, iOS, Web, JVM }
```

L'instance est exposée via `Session.devices`. L'enregistrement est idempotent côté backend : appeler `register*` plusieurs fois avec le même `registrationId` est un no-op.

> **Bug connu — `Devices.unregisterAndroid`** — au moment de cette version, `Devices.unregisterAndroid(...)` route en interne vers l'endpoint **APNS** (`POST /devices/apns/unregister/`) au lieu de l'endpoint WebPush. Tant que ce bug n'est pas corrigé côté SDK, ne pas utiliser cette méthode pour désenregistrer un device Android : appeler directement `Api.devices.unregisterBrowser(registrationId)` (couche REST) ou la méthode `unregisterBrowser` de la façade. Référence : `kotlin-meet-sdk/src/commonMain/kotlin/io/vopenia/sdk/devices/Devices.kt:32`.

---

## 10. Modèle utilisateur

```kotlin
data class User(
    val id: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String? = null,    // Nom complet (peut être null)
    @SerialName("short_name")
    val shortName: String? = null,   // Nom court / initiales
    val timezone: String,            // Fuseau horaire (ex: "Europe/Paris")
    val language: String             // Langue (ex: "fr")
)
```

### API utilisateurs

```kotlin
class ApiUsers {
    // Lister les utilisateurs
    suspend fun users(): Page<ApiUser>

    // Récupérer l'utilisateur courant
    suspend fun me(): ApiUser
}
```

---

## 11. Composants UI Compose

### `VideoView` - Rendu vidéo d'un participant

```kotlin
@Composable
fun VideoView(
    modifier: Modifier,
    room: Room,                // La salle de visio
    track: IVideoTrack,        // La piste vidéo à afficher
    scaleType: ScaleType,      // Mode de mise à l'échelle
    isMirror: Boolean = false  // Effet miroir (pour la caméra locale)
)
```

**Usage** : afficher le flux vidéo d'un participant (local ou distant). L'`IVideoTrack` est obtenu depuis `localParticipant` ou `remoteParticipant`.

### `CameraPreviewView` - Prévisualisation caméra

```kotlin
@Composable
fun CameraPreviewView(
    modifier: Modifier,
    scaleType: ScaleType,
    isMirror: Boolean = false
)
```

**Usage** : afficher un aperçu de la caméra **avant** de rejoindre une salle (écran de configuration pré-appel).

### `TranscriptionAnimated` - Transcription animée

```kotlin
@Composable
fun TranscriptionAnimated(
    modifier: Modifier,
    text: String,                  // Texte de transcription
    typingDelayInMs: Long = 50L,   // Délai entre chaque caractère (effet machine à écrire)
    content: @Composable (Modifier, String) -> Unit  // Composable de rendu
)
```

**Usage** : afficher la transcription temps réel de la parole avec un effet d'apparition progressive (typing animation).

### `ScaleType` - Mode de mise à l'échelle vidéo

```kotlin
enum class ScaleType {
    Fill,  // Remplit le conteneur (peut rogner)
    Fit    // S'adapte au conteneur (peut laisser des bandes)
}
```

---

## 12. Client REST bas niveau (`kotlin-meet-sdk-api`)

Ce module peut être utilisé **indépendamment** du SDK principal pour interagir directement avec l'API REST du backend Meet.

### Point d'entrée

```kotlin
class Api(
    prefix: String,                                    // URL de base
    enableHttpLogs: Boolean = false,
    getAuthent: suspend () -> AuthenticationInformation?
) {
    val users: ApiUsers              // Endpoints utilisateurs
    val rooms: ApiRooms              // Endpoints salles
    val devices: ApiDevices          // Endpoints d'enregistrement de devices push
    val recordings: ApiRecordings    // Endpoints sur les enregistrements passés
}
```

### Endpoints REST salles (`ApiRooms`)

| Méthode | Endpoint REST | Description |
|---------|--------------|-------------|
| `createRoom(param)` | `POST /rooms/` | Créer une salle |
| `room(slug)` | `GET /rooms/{slug}` | Récupérer une salle par slug |
| `createRoom(id, param)` | `PUT /rooms/{id}` | Créer une salle avec un ID spécifique |
| `updateRoom(id, param)` | `PATCH /rooms/{id}/` | Mettre à jour une salle |
| `deleteRoom(id)` | `DELETE /rooms/{id}/` | Supprimer une salle |
| `rooms()` | `GET /rooms/` | Lister les salles (paginé) |
| `rooms(page)` | `GET /rooms/?page={n}` | Lister les salles (page spécifique) |
| `invite(id, emails)` | `POST /rooms/{id}/invite/` | Inviter par email |
| `validateParticipantEntry(id, pid, allow)` | `POST /rooms/{id}/enter/` | Accepter/refuser un participant |
| `requestEntry(id, userName)` | `POST /rooms/{id}/request-entry/` | Demander l'entrée (première demande) |
| `requestEntry(id, previous)` | `POST /rooms/{id}/request-entry/` | Polling de la demande d'entrée |
| `waitingParticipants(id)` | `GET /rooms/{id}/waiting-participants/` | Liste des participants en attente |
| `startRecording(id, mode)` | `POST /rooms/{id}/start-recording/` | Démarrer un enregistrement Egress |
| `stopRecording(id)` | `POST /rooms/{id}/stop-recording/` | Arrêter l'enregistrement en cours |
| `startSubtitle(id)` | `POST /rooms/{id}/start-subtitle/` | Dispatcher l'agent ASR (transcription) |
| `muteParticipant(id, identity, trackSid)` | `POST /rooms/{id}/mute-participant/` | Mute serveur d'un track (admin) |
| `updateParticipant(id, param)` | `POST /rooms/{id}/update-participant/` | Mettre à jour attributs/metadata/name d'un participant (admin) |
| `removeParticipant(id, identity)` | `POST /rooms/{id}/remove-participant/` | Déconnecter un participant (admin) |

### Endpoints REST utilisateurs (`ApiUsers`)

| Méthode | Endpoint REST | Description |
|---------|--------------|-------------|
| `users()` | `GET /users` | Lister les utilisateurs |
| `me()` | `GET /users/me` | Utilisateur courant |

### Endpoints REST devices (`ApiDevices`)

| Méthode | Endpoint REST | Description |
|---------|--------------|-------------|
| `registeriOS(registrationId, deviceId, name)` | `POST /devices/apns/register/` | Enregistrer un device APNS |
| `registerAndroid(registrationId)` | `POST /devices/webpush/register/` | Enregistrer un device Android (WebPush, `browser="android"`) |
| `registerBrowser(registrationId, p256dh, auth)` | `POST /devices/webpush/register/` | Enregistrer un device navigateur (`browser="browser"`) |
| `unregisterBrowser(registrationId)` | `POST /devices/webpush/unregister/` | Désenregistrer un device WebPush |
| `unregisteriOS(registrationId)` | `POST /devices/apns/unregister/` | Désenregistrer un device APNS |

La couche REST `ApiDevices` n'expose **pas** de méthode `unregisterAndroid` dédiée — pour désenregistrer un device Android, utiliser `unregisterBrowser` (cf. avertissement dans la section « Devices »).

### Endpoints REST enregistrements (`ApiRecordings`)

| Méthode | Endpoint REST | Description |
|---------|--------------|-------------|
| `recordings()` | `GET /recordings/` | Première page, endpoint sans paramètre |
| `recordings(page)` | `GET /recordings/?page={n}` | Page spécifique |
| `recording(id)` | `GET /recordings/{id}/` | Récupérer un enregistrement (retourne `null` si 404/403) |
| `deleteRecording(id)` | `DELETE /recordings/{id}/` | Supprimer (statut final uniquement) |

### Pagination

```kotlin
data class Page<T>(
    val count: Int,           // Nombre total d'éléments
    val next: String?,        // URL de la page suivante (null si dernière)
    val previous: String?,    // URL de la page précédente (null si première)
    val results: List<T>      // Éléments de la page courante
)
```

La pagination est gérée automatiquement par `Session.rooms()` via la fonction utilitaire `getAllRooms` qui suit les liens `next` jusqu'à épuisement.

---

## 13. Modèles de données REST

### `ApiRoom`

```json
{
    "id": "uuid",
    "name": "Réunion équipe dev",
    "slug": "reunion-equipe-dev",
    "configuration": {},
    "access_level": "public" | "trusted" | "restricted",
    "accesses": [{ "id": "...", "user": {...}, "resource": "...", "role": "..." }],
    "livekit": { "url": "wss://...", "room": "...", "token": "jwt..." },
    "is_administrable": true
}
```

### `NewRoomParam` (création/mise à jour)

```json
{
    "name": "Ma salle",
    "configuration": "",
    "access_level": "public" | "trusted" | "restricted"
}
```

### `ApiRequestEntryAnswer`

```json
{
    "id": "participant-uuid",
    "username": "Jean Dupont",
    "status": "waiting" | "accepted" | "idle" | "denied" | "timeout",
    "color": "#FF5733",
    "livekit": { "url": "...", "room": "...", "token": "..." }
}
```

Le champ `livekit` n'est renseigné que lorsque `status` vaut `"accepted"`.

### `Livekit` (credentials de connexion)

```json
{
    "url": "wss://livekit.example.com",
    "room": "room-name",
    "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Modèles devices (`io.vopenia.api.devices.models`)

```kotlin
@Serializable
data class RegisterIOS(
    @SerialName("registration_id") val registrationId: String,
    @SerialName("device_id") val deviceId: String,
    val name: String,
)

@Serializable
data class RegisterFCM(
    @SerialName("registration_id") val registrationId: String,
    val p256dh: String = "",
    val auth: String = "",
    val browser: String,   // "android" | "browser"
)

@Serializable
data class UnregisterDevice(
    @SerialName("registration_id") val registrationId: String,
)
```

### Modèles enregistrements (`io.vopenia.api.recordings.models`)

L'énumération `ApiRecordingMode` est **partagée** avec les paramètres d'opérations sur les rooms ; pour éviter une dépendance cyclique entre modules, elle est déclarée dans `io.vopenia.api.rooms.models` (pas `recordings.models`).

```kotlin
// package io.vopenia.api.recordings.models
@Serializable
data class ApiRecording(
    val id: String,
    val room: ApiRecordingRoom,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val status: ApiRecordingStatus,
    val mode: ApiRecordingMode,                    // NB : déclaré dans io.vopenia.api.rooms.models
    val key: String? = null,
    @SerialName("is_expired") val isExpired: Boolean = false,
    @SerialName("expired_at") val expiredAt: String? = null
)

@Serializable
data class ApiRecordingRoom(
    val id: String,
    val name: String,
    val slug: String,
    @SerialName("access_level") val accessLevel: ApiRoomAccessLevel? = null
)

@Serializable
enum class ApiRecordingStatus {
    @SerialName("initiated")              INITIATED,
    @SerialName("active")                 ACTIVE,
    @SerialName("stopped")                STOPPED,
    @SerialName("saved")                  SAVED,
    @SerialName("aborted")                ABORTED,
    @SerialName("failed_to_start")        FAILED_TO_START,
    @SerialName("failed_to_stop")         FAILED_TO_STOP,
    @SerialName("notification_succeeded") NOTIFICATION_SUCCEEDED
}
```

### Modèles rooms — paramètres d'opérations (`io.vopenia.api.rooms.models`)

```kotlin
@Serializable
enum class ApiRecordingMode {
    @SerialName("screen_recording") SCREEN_RECORDING,
    @SerialName("transcript")       TRANSCRIPT
}

@Serializable
data class ApiOperationResponse(
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class ApiStartRecordingParam(
    val mode: ApiRecordingMode
)

@Serializable
data class ApiMuteParticipantParam(
    @SerialName("participant_identity") val participantIdentity: String,
    @SerialName("track_sid") val trackSid: String
)

@Serializable
data class ApiUpdateParticipantParam(
    @SerialName("participant_identity") val participantIdentity: String,
    val name: String? = null,
    val attributes: Map<String, String>? = null
)

@Serializable
data class ApiRemoveParticipantParam(
    @SerialName("participant_identity") val participantIdentity: String
)
```

`ApiOperationResponse` est la forme de retour commune aux actions admin (`startRecording`, `stopRecording`, `startSubtitle`, `muteParticipant`, `updateParticipant`, `removeParticipant`). Le backend renvoie soit `{"message": "..."}` soit `{"status": "..."}` selon l'endpoint ; les deux champs sont optionnels.

---

## 14. Modèles façade (features de `Room`)

Ces types vivent dans le module `kotlin-meet-sdk` (couche métier), au-dessus des modèles REST. Ils sont la surface publique observable par l'application cliente.

```kotlin
// package io.vopenia.sdk.room.chat
data class ChatMessage(
    val id: String,
    val author: String?,
    val text: String,
    val timestamp: Long,
    val editTimestamp: Long? = null,
    val deleted: Boolean = false,
    val generated: Boolean = false
)

// package io.vopenia.sdk.room.reactions
data class Reaction(
    val author: String?,
    val emoji: String,
    val timestamp: Long
) {
    val meetEmoji: MeetReactionEmoji?     // non-null si emoji ∈ catalogue Meet
}

enum class MeetReactionEmoji(val key: String) {
    ThumbsUp("thumbs-up"),
    ThumbsDown("thumbs-down"),
    ClappingHands("clapping-hands"),
    RedHeart("red-heart"),
    FaceWithTearsOfJoy("face-with-tears-of-joy"),
    FaceWithOpenMouth("face-with-open-mouth"),
    PartyPopper("party-popper"),
    FoldedHands("folded-hands");

    companion object {
        fun fromKey(key: String?): MeetReactionEmoji?
    }
}

// package io.vopenia.sdk.recording
data class Recording(
    val id: String,
    val roomId: String,
    val roomName: String,
    val roomSlug: String,
    val mode: RecordingMode,
    val status: RecordingStatus,
    val createdAt: String,
    val updatedAt: String,
    val key: String? = null,
    val isExpired: Boolean = false,
    val expiredAt: String? = null
)

enum class RecordingStatus {
    Initiated, Active, Stopped, Saved, Aborted,
    FailedToStart, FailedToStop, NotificationSucceeded;

    val isFinal: Boolean
    // true pour Stopped, Saved, Aborted, FailedToStart, FailedToStop
}

// package io.vopenia.sdk.room.recording
enum class RecordingMode {
    ScreenRecording,   // MP4 composite vidéo
    Transcript         // audio pour ASR offline
}

// constructeur `internal` — obtenu uniquement via Room.startRecording()
class RecordingHandle {
    val mode: RecordingMode
    val startedAt: Long

    suspend fun stop()    // équivalent à Room.stopRecording()
}

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val handle: RecordingHandle) : RecordingState()
    object Stopping : RecordingState()
    data class Failed(val error: Throwable) : RecordingState()
}

// package io.vopenia.sdk.room.transcription
// constructeur `internal` — obtenu uniquement via Room.startTranscription()
class TranscriptionHandle {
    val language: String

    suspend fun stop()    // no-op : l'agent se termine avec la room
}
```

**Notes** :

- `RecordingHandle` et `TranscriptionHandle` ont un **constructeur `internal`** : ils ne s'instancient pas côté client et ne s'obtiennent qu'en valeur de retour de `Room.startRecording()` et `Room.startTranscription()`. Les paramètres privés de leur constructeur ne sont volontairement pas exposés ici.
- Les sous-types `RecordingState.Idle` et `RecordingState.Stopping` sont des **`object`** Kotlin (singletons, à comparer avec `===`), pas des `data object`. `Recording` et `Failed` sont des `data class`.
- `RecordingStatus.isFinal` retourne `true` pour les statuts terminaux (succès comme échec) : utile pour décider si un enregistrement est supprimable.

---

## 15. Gestion des erreurs (`ApiException`)

Tous les appels REST exposés par `Api` valident le statut HTTP via `AbstractApi.ensureSuccess` et lèvent `ApiException` en cas de réponse non-2xx.

```kotlin
// package io.vopenia.api.utils
class ApiException(
    val status: HttpStatusCode,
    val endpoint: String
) : Exception("Issue with $endpoint, answer $status") {
    val isUnauthorized: Boolean    // status == 401
    val isForbidden: Boolean       // status == 403
}
```

**Exceptions à cette règle** — appels qui capturent silencieusement les erreurs et retournent une valeur « vide » :

- `ApiRooms.room(slug)` : retourne `null` en cas d'erreur.
- `ApiRecordings.recording(id)` : retourne `null` en cas d'erreur.
- `Room.waitingParticipants()` : retourne `emptyList()` en cas d'erreur.

Pour tous les autres appels, attraper `ApiException` au point d'invocation et utiliser `isUnauthorized` / `isForbidden` pour distinguer le besoin de re-authentification du refus d'accès.

---

## 16. Flux métier complets

### Flux 1 : Créer et rejoindre une salle publique

```
Application                    SDK                         Backend
    │                           │                             │
    ├── openSession() ─────────►│                             │
    │                           │                             │
    ├── createRoom("Daily",     │                             │
    │     Public) ─────────────►├── POST /rooms/ ────────────►│
    │                           │◄── ApiRoom (avec livekit) ──┤
    │◄── Room ──────────────────┤                             │
    │                           │                             │
    ├── room.connect() ────────►├── WebSocket LiveKit ───────►│
    │                           │◄── Flux audio/vidéo ────────┤
    │                           │                             │
    ├── room.disconnect() ─────►├── Fermeture WebSocket ─────►│
```

### Flux 2 : Rejoindre une salle restreinte (avec salle d'attente)

```
Participant                    SDK                         Backend              Admin
    │                           │                             │                   │
    ├── room("slug") ──────────►├── GET /rooms/{slug} ───────►│                   │
    │◄── Room (sans livekit) ───┤                             │                   │
    │                           │                             │                   │
    ├── requestEntry("Jean") ──►├── POST request-entry/ ─────►│                   │
    │◄── RequestEntryManagement │◄── status: "waiting" ───────┤                   │
    │                           │                             │                   │
    │  ┌─ polling ─────────────►├── POST request-entry/ ─────►│                   │
    │  │  checkEntry()          │◄── status: "waiting" ───────┤                   │
    │  │                        │                             │                   │
    │  │                        │                             │◄── accepter ──────┤
    │  │                        │                             │                   │
    │  └─ checkEntry() ────────►├── POST request-entry/ ─────►│                   │
    │◄── status: "accepted"     │◄── status: "accepted"       │                   │
    │    + livekit credentials  │    + livekit credentials    │                   │
    │                           │                             │                   │
    ├── room.connect() ────────►├── WebSocket LiveKit ───────►│                   │
```

### Flux 3 : Notification push

```
Serveur Push ──► Application ──► roomFromPushedValues(id, name, slug, ..., livekitUrl, livekitRoom, livekitToken)
                                          │
                                          ▼
                                   Room (avec credentials)
                                          │
                                   room.connect() ──► LiveKit
```

---

## 17. Gestion des permissions Android

Sur Android, le SDK nécessite les permissions caméra et microphone. Un contrôleur dédié gère la demande de permissions (package `io.vopenia.sdk.permissions`, source-set `androidMain`) :

```kotlin
package io.vopenia.sdk.permissions

object PermissionsActivityController {
    fun setActivity(activity: FragmentActivity)
}
```

L'application doit appeler `setActivity()` avec la `FragmentActivity` courante pour que le SDK puisse déclencher les dialogues de permission système.

---

## 18. Résumé des packages

| Package | Contenu |
|---------|---------|
| `io.vopenia.sdk` | `VisioSdk`, `Session` — Points d'entrée |
| `io.vopenia.sdk.room` | `Room`, `ConnectionState`, `RoomAccessLevel`, `RequestEntryStatus`, `RequestEntryAnswer`, `Access` |
| `io.vopenia.sdk.room.chat` | `ChatMessage` |
| `io.vopenia.sdk.room.reactions` | `Reaction`, `MeetReactionEmoji` |
| `io.vopenia.sdk.room.recording` | `RecordingHandle`, `RecordingMode`, `RecordingState` |
| `io.vopenia.sdk.room.transcription` | `TranscriptionHandle` |
| `io.vopenia.sdk.recording` | `Recording`, `RecordingStatus` |
| `io.vopenia.sdk.devices` | `Devices`, `Platform` |
| `io.vopenia.sdk.permissions` (androidMain) | `PermissionsActivityController` |
| `io.vopenia.sdk.user` | `User` |
| `io.vopenia.sdk.utils` | `AuthenticationInformation`, utilitaires internes |
| `io.vopenia.sdk.compose` | `VideoView`, `CameraPreviewView`, `ScaleType` |
| `io.vopenia.sdk.compose.transcription` | `TranscriptionAnimated` |
| `io.vopenia.api` | `Api`, `AuthenticationInformation` (couche REST) |
| `io.vopenia.api.rooms` | `ApiRooms` |
| `io.vopenia.api.rooms.models` | Modèles sérialisables REST — inclut `ApiRecordingMode`, `ApiOperationResponse`, `ApiStartRecordingParam`, `ApiMuteParticipantParam`, `ApiUpdateParticipantParam`, `ApiRemoveParticipantParam` |
| `io.vopenia.api.users` | `ApiUsers` |
| `io.vopenia.api.users.models` | `ApiUser` |
| `io.vopenia.api.devices` | `ApiDevices` |
| `io.vopenia.api.devices.models` | `RegisterIOS`, `RegisterFCM`, `UnregisterDevice` |
| `io.vopenia.api.recordings` | `ApiRecordings` |
| `io.vopenia.api.recordings.models` | `ApiRecording`, `ApiRecordingRoom`, `ApiRecordingStatus` |
| `io.vopenia.api.utils` | `AbstractApi`, `Page`, `ApiException` |
