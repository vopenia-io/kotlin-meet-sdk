# Kotlin Meet SDK

A Kotlin Multiplatform SDK for video conferencing, providing a REST API client, Compose UI components, session management, and room management. Depends on [client-sdk-kotlin-multiplatform](../client-sdk-kotlin-multiplatform) (Vopenia SDK).

## Features

- Real-time video conferencing
- REST API client for room and user management
- Compose Multiplatform UI components
- Session and cookie-based authentication
- HTTP tunneling for mobile device testing
- Configuration management

## Modules

| Module | Description |
|---|---|
| `:kotlin-meet-sdk` | Core SDK (session and room management) |
| `:kotlin-meet-sdk-api` | REST API client, room/user models |
| `:kotlin-meet-sdk-compose` | Compose UI components |
| `:konfig` | Configuration module |
| `:tunnel-http` | HTTP tunnel utility for mobile testing |
| `:shared` | Shared KMP module |
| `:sampleAndroid` | Android sample app |

## Platform Targets

- Android (`androidTarget`)
- iOS (`iosArm64`, `iosSimulatorArm64`, `iosX64`)
- JVM/Desktop (`jvm()`)

## Prerequisites

- JDK (bundled with Android Studio recommended)
- Android SDK
- CocoaPods (`brew install cocoapods`)
- Sibling repo `client-sdk-kotlin-multiplatform` cloned alongside this repo (consumed via composite build)

## Build Commands

| Command | Description |
|---|---|
| `./gradlew build` | Full build (all targets) |
| `./gradlew :sampleAndroid:installDebug` | Build and install Android sample app |
| `./gradlew check` | Run all checks and tests |
| `./gradlew publishToMavenLocal` | Publish SDK to local Maven repository |

## API Usage

Instantiate the API to access sub-objects:

```kotlin
val api = Api(
  "http://localhost:8071/api/v1.0",
  enableHttpLogs = true
) { "my meet_sessionid session cookie" }
```

The `localhost` URL is for local development. For production or mobile testing, use the appropriate endpoint (see tunnel configuration below).

From there, access the various resources via the getters and their methods.

## Test Configuration

To enable tests, follow these steps:

1. Log in to the platform's web interface.
2. Open developer tools (F12) > "Storage" tab > "Cookies".
3. Copy the value of the `sessionid` cookie.
4. Add this value to your `~/.gradle/gradle.properties`:

```properties
VOPENIA_MEET_TESTS_COOKIE_TOKEN_VALUE=your_meet_sessionid_cookie_value
```

5. Run the tests with Gradle using the JVM platform (other platforms may fail due to backend certificate issues):

```bash
./gradlew test
```

## Mobile Testing

To test on mobile devices, set up tunnels to your local backend:

- Create a tunnel to the `:3071` endpoint
- Create a tunnel to port `8181` (used for credential exchange)
- Add the tunnel endpoints to your `~/.gradle/gradle.properties`:

```properties
VOPENIA_MEET_TESTS_TUNNEL_ENDPOINT=https://ngrok.endpoint.for.8181
VOPENIA_MEET_TESTS_TUNNEL_API=https://ngrok.endpoint.for.3071
```

### Testing on iOS

Add the iOS simulator UUID to your `gradle.properties`:

```properties
iOSSimulatorUuid=4BB42133-F542...
```

You can get the list of devices via `xcrun xctrace list devices`.

Running `iosSimulatorArm64Test` will open the simulator and run the tests:

```bash
./gradlew :kotlin-meet-sdk-api:iosSimulatorArm64Test
```

iOS deployment target: **16.0**

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
