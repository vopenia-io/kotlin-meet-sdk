# Kotlin Visio Solution

A modern video conferencing solution developed in Kotlin, designed to offer seamless integration between web services, video communication, and user API.

## ✨ Main Features

- 📹 Real-time video conferencing
- 👥 User management
- ...

## 📦 User API

Instantiate the API to access the sub-objects

```kotlin
val api = Api(
  "http://localhost:8071/api/v1.0",
  enableHttpLogs = true
) { "my meet_sessionid session cookie" }
```

From there, access the various ressources via the getters and their methods

## 🔐 Test Configuration

To enable the tests, you will need to do the following steps

### Étapes pour lancer les tests :

1. Log in to the platform's web interface.
2. Open developer tools (F12) > "Storage" tab > "Cookies".
3. Copy the value of the `meet_sessionid` cookie.
4. Add this value to your `~/.gradle/gradle.properties` file under the following key:

```properties
VOPENIA_MEET_TESTS_COOKIE_TOKEN_VALUE=your_meet_sessionid_cookie_value
```

5. Run the tests with Gradle using the JVM platform: _currently the other platforms will likely fail given the backend's certificate_

```bash
./gradlew test
```

## 🚀 Install the Project

To run the application locally:

```bash
./gradlew publishToMavenLocal
```

## 📄 Licence

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

---

Developed with ❤️ in Kotlin
