# Learn Mobile Development

A Kotlin Multiplatform app that teaches **MVI and Clean Architecture** — where the codebase
itself is the teaching material.

Most architecture tutorials stop at a counter. This one is a working app whose own source
is the worked example: every pattern explained in the lessons is used, tested and
cross-referenced in the code you are reading.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS-lightgrey)](#running-it)
[![Tests](https://img.shields.io/badge/tests-111%20passing-success)](#testing)
[![CI](https://github.com/mkatadev/LearnMobileDev/actions/workflows/ci.yml/badge.svg)](https://github.com/mkatadev/LearnMobileDev/actions/workflows/ci.yml)

---

## What is inside

Four tabs, each one a separate MVI feature built on the same core.

| Tab | What it does |
|---|---|
| 📘 **Learn** | 16 lessons: MVI, MVP/MVVM, Clean Architecture, Kotlin type declarations, abstract class vs interface, coroutines & Flow, RxJava, SOLID, DI, Kotlin Multiplatform, testing |
| ⚙ **Demo** | A live users screen with debounce, request cancellation, optimistic updates with rollback and a time-travel debugger |
| ✓ **Quiz** | 178 senior-level questions across 12 categories, each answer followed by an explanation |
| ⇄ **Sync** | A concurrency lab: 7 deterministic scenarios with live PASS/FAIL results |

Available in **English and Polish**, switchable inside the app.

---

## Why it might be worth your time

**The demo screen covers the cases tutorials skip.** Debounced search that cancels stale
requests, an optimistic favourite toggle that rolls back when the server rejects it,
one-off effects that survive a configuration change, and a timeline you can rewind:

```kotlin
// Every state change has a name and lands on the timeline
is UsersIntent.Internal.LoadSucceeded -> state.copy(isLoading = false, users = intent.users)
```

**The concurrency lab proves things instead of asserting them.** Seven scenarios run on
demand and report PASS/FAIL. The ones marked 🐞 are *supposed* to fail — a red result is
the evidence:

```kotlin
var counter = 0
List(workers) {
    async {
        val read = counter
        yield()              // the gap other coroutines slip into
        counter = read + 1
    }
}.awaitAll()
// 100 coroutines, one thread, fewer than 100 increments
```

Races are forced with an explicit `yield()`, never left to chance, so the demonstration
behaves identically on every device — and a test asserts that the buggy scenarios still
fail, because otherwise the lesson would be lying.

**The quiz explains, it does not just grade.** Every question carries a multi-sentence
explanation shown after each answer, and a test enforces that none of them is a one-liner.

---

## Architecture

Dependencies point inward: **presentation → domain ← data**.

```
shared/src/commonMain/kotlin/pl/prodevcode/learnmobiledev/
├─ core/mvi/        MVI framework: state, intents, reducer, store, timeline
├─ core/ui/         AppString and UiText — text as a key, not a resolved string
├─ domain/
│  ├─ model/        plain Kotlin, no framework annotations
│  ├─ repository/   ports (interfaces) + domain exceptions
│  └─ usecase/      one business rule per class
├─ data/            adapters: sources, parsers, DTOs, repositories
├─ presentation/    Contract + Reducer + ViewModel + Screen, per feature
└─ di/              Koin modules, one per layer

fakeApi/src/commonMain/
├─ kotlin/.../fakeapi/
│  ├─ http/         request, response, router with path templates
│  ├─ routes/       the endpoints this service publishes
│  └─ ...           storage, user directory, language catalogue, client factory
└─ composeResources/files/
   ├─ <lang>/       lessons, questions, scenarios — the content, per language
   └─ users.json    the user table — the same in every language
```

### The backend

Neither the course content nor the user directory is held by the app; both are fetched over
HTTP from `:fakeApi`, an in-process stand-in for a real service:

```
GET /api/v1/content/{resource}?lang=xx   ->  200 + Content-Language
                                             404 unknown resource / no such document
                                             503 outage switch

GET /api/v1/users?q=                     ->  200 the directory, filtered server-side
PUT /api/v1/users/{id}                   ->  200 the row as stored
                                             404 unknown user
                                             422 values the server will not store
PUT /api/v1/users/{id}/favorite          ->  200 the flag as stored
                                             404 unknown user
                                             409 the server refuses this write
```

Searching is a query parameter rather than a full download filtered on the phone, and a
favorite is only true once the server has said so — the users screen's optimistic update is
rolled back by a genuine `409`. An edit is the mirror image: no optimistic update at all,
because several fields may be normalized or rejected at once, so the list adopts the row
the server answers with and nothing before that. Validation lives on the server and comes
back as `422`; the form only disables Save while a field is empty.

The demo's "simulate a network error" switch sends
`X-Fake-Fault: unavailable`, so the failure is a real `503` on that call and does not knock
the content service over.

Only the *transport* is fake. Above it there is a genuine HTTP surface — paths, query
parameters, status codes, headers — and below it genuine routing and storage, so the app
uses an ordinary Ktor client and cannot tell the difference. Swapping in a deployed server
means changing an engine and a base URL, nothing else. A Ktor `MockEngine` rather than an
embedded server, because a real server would not run on iOS.

The content documents and the user table live in `:fakeApi`, not in the app. They are the
service's database, and keeping them there means nothing above the HTTP boundary can reach
them: the app has no path to another module's resources and must go through the API,
exactly as it would against a real server. UI strings are a separate matter: they stay in
`:shared` as ordinary string resources, because they are the app's own text rather than
content served to it.

Language negotiation lives on the server, where it belongs: the client states a preference,
the backend answers with the translation it actually has and reports it in
`Content-Language`. `FakeBackendConfig` adds latency and an outage switch, so loading and
failure states can be exercised on demand.

### The MVI core

One store serves all four features. `dispatch` only queues an intent; a single loop
consumes the queue, so reduction is never concurrent and the reducer needs no locks:

```kotlin
abstract class MviStore<S : MviState, I : MviIntent, E : MviEffect>(
    initialState: S,
    private val reducer: Reducer<S, I>,
) : ViewModel() {

    fun dispatch(intent: I) { inbox.trySend(intent) }

    /** Where I/O belongs. Results come back through dispatch as Intent.Internal. */
    protected open fun onIntentProcessed(intent: I, before: S, after: S) = Unit
}
```

Intents are split on purpose: `Intent.Ui` is what the user wants, `Intent.Internal` is what
already happened. Both pass through the same reducer, which is what makes the timeline and
time travel possible at all.

### Decisions worth a look

- **Errors carry a type, not a message.** `error.message` is technical English for logs;
  the presentation layer maps the exception *type* to a localized string. State holds
  `UiText`, never `String`.
- **Content and users are data.** Lessons, questions, scenarios and the user directory are
  JSON documents owned by the backend, validated by tests rather than trusted. The app
  keeps no copy of either.
- **UI strings are ordinary string resources** (`values/`, `values-pl/`), resolved through
  the `AppString` key enum. Because Compose Resources follows the *system* locale and 1.11
  offers no supported way to override it, a language change applies on the next launch. The
  picker says so once a change is pending, rather than pretending the tap did nothing.
- **One source of language.** `PlatformLocale` decides it for both the UI and the content,
  so the two can never disagree about what is on screen.

---

## Testing

```bash
./gradlew :shared:testAndroidHostTest      # unit + integration + smoke (JVM)
./gradlew :fakeApi:testAndroidHostTest     # the fake backend
./gradlew :shared:iosSimulatorArm64Test    # the same tests on iOS
```

**148 tests**, all green on both platforms. The interesting ones are not the reducer tests:

| Test | What it protects |
|---|---|
| `CoroutineConcurrencyLabTest` | That a buggy demo still fails — if someone "fixes" it, the lesson becomes a lie |
| `CodeLanguagePolicyTest` | No Polish and no hard-coded `Text("…")` anywhere in Kotlin sources |
| `StringResourcesTest` | Both locales define every key the code uses, with matching `%1$s` placeholders and no unused or blank entries |
| `BundledContentTest` | The backend ships every document in every language it serves, plus the user table, and nothing it does not publish |
| `UserRoutesTest` | The user endpoints answer with the right status: server-side search, a stored favorite, a stored edit, `404`, `409`, `422` and injected faults |
| `ApiUserRepositoryTest` | The users screen really goes over HTTP — search, favorite and edit, from repository to user table, with each status mapped to its domain failure |
| `QuestionsContentTest` | Unique ids, distinct options, and correct answers not always at the same index |
| `KoinModulesTest` | The DI graph resolves — a missing definition fails the build, not the user |

Async behaviour is tested on virtual time, so a 350 ms debounce and a 500 ms network delay
cost milliseconds:

```kotlin
vm.dispatch(UsersIntent.Ui.QueryChanged("A"));   advanceTimeBy(100)
vm.dispatch(UsersIntent.Ui.QueryChanged("An"));  advanceTimeBy(100)
vm.dispatch(UsersIntent.Ui.QueryChanged("Ann")); advanceUntilIdle()

assertEquals(listOf("Ann"), repository.loadedQueries)   // exactly one request
```

---

## Running it

**Requirements:** JDK 21 (declared in `gradle/gradle-daemon-jvm.properties`, auto-provisioned
by the Gradle wrapper), Android Studio. For iOS: macOS with Xcode.

```bash
git clone https://github.com/mkatadev/LearnMobileDev.git
cd LearnMobileDev

./gradlew :androidApp:assembleDebug     # Android
```

For iOS, open `iosApp/iosApp.xcodeproj` in Xcode and run.

Watch Logcat while using the app: `LoggingMiddleware` prints every reduction as
`intent → state before → state after`, which makes unidirectional data flow visible.
Ktor's `Logging` plugin prints every exchange with the backend under the
`ContentHttp` tag (`Log.d` on Android, `NSLog` on iOS); switch it off with
`logTraffic = false` in `dataModule`.

---

## CI/CD

Three workflows, split by purpose.

**`ci.yml`** runs on every pull request, as two jobs in parallel:

| Job | Runner | Does |
|---|---|---|
| `Android` | Ubuntu | Lint, shared tests on the JVM, release APK |
| `iOS` | macOS | The same shared tests on the simulator, release archive |

The split is by platform rather than by activity: lint, tests and the APK share one Gradle
cache and one checkout, so separate jobs would pay the setup cost three times for no extra
parallelism. Android and iOS genuinely need different runners, and the slow macOS job
starts immediately instead of queueing.

**`fake-api.yml`** runs only when the fake backend changes — `fakeApi/**`, the version
catalogue, `settings.gradle.kts` or the workflow itself. Most pull requests touch lessons or
UI and have no reason to pay for a backend run. The catalogue and settings file are in the
trigger because the module's Ktor version and its presence in the build are declared there,
so a change to either can break it without a single file under `fakeApi/` being touched.

It runs the backend's own tests, the end-to-end contract test from `:shared`, and a
Kotlin/Native compile. Compiling is enough for iOS: `ci.yml` already links the module into
the framework and runs the shared suite on the simulator, so a second macOS runner would buy
very little for what it costs.

> **This job must not be a required status check.** A required check that is path-filtered
> never reports on the pull requests that skip it, and GitHub waits for it forever — the PR
> becomes unmergeable. The jobs in `ci.yml` are unfiltered, which is exactly why they are the
> required ones.

**`release.yml`** runs on every merge into `main` and publishes a GitHub Release with both
artifacts attached. Since `main` can only be reached through a passing PR, "merged" already
implies "verified", so the release workflow builds rather than re-checking.

### Versioning

`versionName` lives in `gradle.properties`, so bumping a release is a reviewed change in a
PR. `versionCode` is the CI run number: monotonic, unique and never reused. Both are passed
in at build time:

```bash
./gradlew :androidApp:assembleRelease \
  -Papp.versionName=1.0.0 -Papp.versionCode=42
```

Artifacts are named `LearnMobileDev-<version>-<code>.apk` / `.ipa`, so a downloaded file
still says what it is. The tag (`v1.0.0+1`) is created by the workflow, which means a tag
can only exist for a commit that actually produced artifacts.

> **Signing.** The Android release is signed with the debug key, because this project ships
> no keystore and an unsigned APK cannot be installed at all. The IPA is unsigned and needs
> re-signing before it will run on a device. Both are deliberate for a teaching repository —
> replace them with real credentials stored as repository secrets before shipping anywhere.

### Branch protection

`main` accepts no direct pushes. A change has to go through a pull request with both `ci.yml`
jobs green — and only those two, for the reason given above; force pushes and branch deletion are disabled, history stays linear, and the rules
apply to administrators too.

---

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4.10 (Multiplatform) |
| UI | Compose Multiplatform 1.11.1, Material 3 |
| Async | Coroutines 1.10.2, Flow |
| DI | Koin 4.1.0 |
| Serialization | kotlinx.serialization 1.9.0 |
| Build | Gradle 9.1, AGP 9.0.1, JVM target 11 |
| Platforms | Android (minSdk 31, compileSdk 36), iOS (arm64, simulator arm64) |

No networking library on purpose: the "backend" is an in-memory fake with controllable
delays and failures, so the demo is repeatable and the tests never flake.

---

## Contributing

[`AGENTS.md`](AGENTS.md) holds the working rules — language policy, architecture
constraints, MVI conventions, testing rules and the definition of done. It is written for
both humans and coding agents, and most of it is enforced by tests.

The short version:

- Kotlin sources are **English only**, including comments and test names.
- User-facing text lives in `strings.xml`; symbols and counter formats count as text.
- A repository interface belongs to `domain`, its implementation to `data`.
- The reducer stays pure: no I/O, no clock, no randomness.

Adding a lesson or a quiz question means editing JSON in **both** `files/en/` and
`files/pl/` — the content tests will tell you if you missed something.

---

## License

[MIT](LICENSE) — use the code, copy the patterns, teach from it.
