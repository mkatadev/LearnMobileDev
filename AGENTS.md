# AGENTS.md — working rules for this repository

Learn Mobile Development is a Kotlin Multiplatform teaching app (Android + iOS, Compose
Multiplatform). The codebase is itself the teaching material, so consistency matters more
here than in an ordinary product.

Read this file before making changes. Every rule below is enforced either by a test or by
review; where a test exists, it is named.

---

## 1. Language policy — the rule that is broken most often

| What | Language | Why |
|---|---|---|
| Code: identifiers, comments, KDoc | **English, always** | Code is read by tooling and by non-Polish speakers |
| Test names and assertion messages | **English, always** | They are output, and output ends up in CI logs |
| Exception messages | **English, always** | They go to logs and crash reporting, never to a screen |
| Commit messages, this file, README | **English** | Same reason |
| UI strings | **String resources only** | `composeResources/values/strings.xml`, `values-pl/strings.xml` |
| Course content (lessons, quiz, scenarios) | **Served by `:fakeApi`** | `fakeApi/…/composeResources/files/<lang>/` |
| Demo users (fictional personal data) | **Served by `:fakeApi`** | `fakeApi/…/composeResources/files/users.json` |

Two tests enforce this rather than trusting discipline: `CodeLanguagePolicyTest` rejects
Polish characters and hard-coded `Text("…")` literals in Kotlin sources, and
`StringResourcesTest` rejects an incomplete set of translations.

**No Polish anywhere in `.kt` files.** Not in comments, not in KDoc, not in test names,
not in string literals — with no exceptions. Fictional personal data such as
`Celina Wójcik` is data, not code, and lives in the user table the fake backend serves
(`fakeApi/…/composeResources/files/users.json`).

Quick audit:

```bash
grep -rn '[ąćęłńóśźż]' shared/src androidApp/src --include=*.kt
```

Anything this prints, other than the demo fixture, is a defect.

### No user-facing text in code

A screen must never contain a hard-coded sentence, and neither must a domain model.

This covers more than prose. **Symbols, markers, separators and counter formats are text
too**, and every one of them belongs to the catalogue:

```kotlin
// WRONG — a locale may want a different glyph, and a translator cannot reorder this
Text("• $line")
Text("✓ ${state.correctCount}")
Text("${currentIndex + 1}/${questions.size}")

// RIGHT — the catalogue owns both the glyph and the ordering
Text(localized(AppString.MarkerLogLine, line))
Text(localized(AppString.QuizCorrectCount, state.correctCount))
Text(localized(AppString.QuizProgress, state.questionNumber, state.questions.size))
```

The same applies to state: expose the *numbers* and let the UI format them.

```kotlin
// WRONG — state assembling display text, impossible to translate
val progressLabel: String get() = "${currentIndex + 1}/${questions.size}"

// RIGHT
val questionNumber: Int get() = currentIndex + 1
```

`CodeLanguagePolicyTest` fails the build on any `Text("…")` or `text = "…"` literal in
`commonMain`.

```kotlin
// WRONG — the enum dictates the wording, so it can never be translated
enum class QuizCategory(val label: String) { Coroutines("Korutyny") }

// RIGHT — the domain names the concept, the presentation layer names the text
enum class QuizCategory { Coroutines }

val QuizCategory.labelRes: AppString
    get() = when (this) { QuizCategory.Coroutines -> AppString.CategoryCoroutines }
```

### Errors: exception type decides, message does not

`error.message` is technical English for logs. What the user reads is chosen in the
presentation layer, from the exception **type**.

```kotlin
// domain
sealed class UserSyncException(message: String) : Exception(message) {
    class NetworkUnavailable : UserSyncException("Users endpoint unavailable (HTTP 503)")
    class FavoriteRejected(userId: String) : UserSyncException("Server rejected ... $userId")
}

// presentation
private fun Exception.toUiText(): UiText = when (this) {
    is UserSyncException.NetworkUnavailable -> AppString.ErrorNetworkUnavailable.asUiText()
    is UserSyncException.FavoriteRejected -> AppString.ErrorFavoriteRejected.asUiText()
    else -> AppString.ErrorUnknown.asUiText()
}
```

State holds `UiText`, never `String`. A store cannot call `stringResource` — it is
`@Composable` — and resolving text inside a store would also make messages impossible to
assert on independently of the current language.

---

## 2. Localization

### UI strings

Strings are ordinary Android string resources, looked up through `AppString` — an enum of
keys — and resolved by `localized()`:

```
shared/src/commonMain/composeResources/
├─ values/strings.xml      <- default (en)
└─ values-pl/strings.xml   <- Polish
```

```kotlin
Text(localized(AppString.QuizStart))
Text(localized(AppString.QuizScore, correct, total))
```

`AppString` maps a key onto a `StringResource` through the generated `Res.allStringResources`
rather than a 128-branch `when`. A mapping that large is a place for a copy-paste mistake to
hide; `StringResourcesTest` proves every key resolves, which a hand-written `when` could not.

Consequences worth knowing:

- Adding a string means one entry in `AppString` plus one `<string>` in **every**
  `strings.xml`. `StringResourcesTest` fails the build on a missing key, an unused key, a
  blank value, a duplicate, or a translation whose `%1$s` placeholders do not match the
  default.
- A key with no resource behind it throws when that screen opens. That is why the test is
  not optional.

### Course content and demo users

Lessons, questions, scenarios and the user directory are **not** app resources. They are
the backend's data and live in `:fakeApi`:

```
fakeApi/src/commonMain/composeResources/files/
├─ en/{lessons,questions,scenarios}.json   <- default
├─ pl/{lessons,questions,scenarios}.json
├─ users.json                              <- the user table, not localized
├─ roles.json                              <- the roles a user may hold, not localized
├─ infographics.json                       <- the infographic catalogue
└─ images/                                 <- the pictures themselves
```

Nothing above the HTTP boundary can reach them: the app has no path to another module's
resources and must go through `GET /api/v1/content/{resource}?lang=xx`,
`GET /api/v1/users?q=`, `GET /api/v1/roles` or `GET /api/v1/infographics`. Language
negotiation happens on the server (`LanguageCatalog`), which answers with the translation
it has and reports it in `Content-Language`; an unsupported language falls back to `en`.

**Roles are data, not an enum and not a translation.** A role is a string stored on a user
row, so the set of legal values is the server's to define: it publishes them, the app
offers exactly those in a picker, and the server refuses anything else with `422`. An app
that shipped its own copy would offer values the backend rejects and need a release to add
one. Translating them would be worse still — the stored value would then depend on the
language the author happened to be using.

**Infographics are pictures, and pictures are not localized.** The text is baked into the
pixels, so the catalogue states which language each image is *drawn in* rather than
pretending a translated caption makes it readable. `ApiResponse` therefore carries a
`ByteArray`, not a `String`: a picture decoded to text and back is a corrupt picture, and
that is the one failure a JSON-shaped fake backend would have hidden until a device tried
to render it.

Images ship as **WebP**, and the route derives its `Content-Type` from the stored file
rather than hardcoding one. A text-heavy infographic went from 1.7 MB as PNG to 233 KB at
`cwebp -q 88`, with a mean per-channel error of about 1% — measured on the densest code
block rather than eyeballed, because the whole point of the tab is reading that code at 6x
zoom. Both platforms decode it through `decodeToImageBitmap`, which is Skia on iOS and
`BitmapFactory` on Android; that was verified by running the app on each, since a JVM unit
test has only a stubbed decoder and would have passed regardless.

`BundledContentTest` guards the data set — every published resource, in every served
language, the user table, the role catalogue, that every seed user holds a role the service
actually publishes, that every listed infographic has its image and dimensions, and nothing
the service does not publish.

### The user service is a full CRUD

| Method | Path | Answers |
|---|---|---|
| `GET` | `/api/v1/users?q=` | the directory, filtered server-side |
| `POST` | `/api/v1/users` | `201` and the stored row, id assigned by the server |
| `PUT` | `/api/v1/users/{id}` | `200` and the stored row |
| `DELETE` | `/api/v1/users/{id}` | `204`, or `404` if there was no such row |
| `PUT` | `/api/v1/users/{id}/favorite` | `200`, or `409` for the locked demo user |
| `GET` | `/api/v1/roles` | the roles a user may hold |

`POST` rather than `PUT` for creation is not cosmetic: the server assigns the id, so the
client cannot name the resource it is asking for and a repeated call creates a second
person. That is exactly why a create must not be retried the way a favorite may be.

Endpoints are **typed resources**, not interpolated strings (`ApiRoutes`, Ktor Resources).
`"$USERS_PATH/$userId/favorite"` compiles whatever is put into it and fails on a device;
a resource is checked by the compiler and escaped by the client, so an id containing a
slash cannot break the request.

### The infographic service

| Method | Path | Answers |
|---|---|---|
| `GET` | `/api/v1/infographics` | the catalogue, as JSON metadata |
| `GET` | `/api/v1/infographics/{id}/image` | the picture, typed by its stored format |

Two endpoints rather than one: metadata is small and always needed, a picture is about a
megabyte and needed only when it is shown. Inlining the bytes would make the app download
every image to draw a list of titles, and base64 would add a third again for the privilege.
The `path` in the metadata is the *storage's* path and is deliberately not mapped into the
domain — the app addresses an image by id, and where the service keeps its files is its own
business.

Images are fetched concurrently by `ApiInfographicRepository` and cached. Unlike the course
content the cache is **not** keyed by language: the text is in the pixels, so switching the
app's language does not change which bytes are correct.

**Zoom state is the exception to the "UI state lives in the store" rule.** `ZoomableImage`
keeps scale and offset in `remember`, because losing them costs nothing a user would call a
bug — the picture is still open, merely un-zoomed. What *would* be a bug is the viewer
closing itself on rotation, which is why `openedId` lives in `InfographicsState`. That is
the test to apply: not "is it UI state?" but "would losing it be a defect?".

Panning is bounded to the drawn size after `ContentScale.Fit`, not to the bitmap's pixel
size — for a tall infographic on a phone those differ by most of the picture, and limits
computed from the wrong one let the image be flung off-screen with no way back. Pinch and
double tap both anchor on the touch point rather than the centre, because centre-anchored
zoom slides the detail away exactly when the user is trying to look at it.

### In-app language switching

**A language change takes effect on the next launch, and the app says so.**

The header opens a picker listing every `AppLanguage`, each written in its own name
(`English`, `Polski`) in *every* locale — a picker that translates its options hides the one
entry a user who cannot read the current language is looking for. The restart line appears
only once a change is actually pending; a warning that is always on screen stops being read.

There is no "close the app" button: Android could finish the activity, but no supported API
relaunches an iOS app, and a button that worked on one platform only would be worse than
none.

Resources follow the *platform* locale, and Compose Multiplatform 1.11 seals that off:
`ResourceEnvironment` has an internal constructor and `LocalComposeEnvironment` is internal,
so there is no supported way to re-resolve resources in-process. The switch therefore writes
the choice to the platform and asks the user to restart:

| Platform | Mechanism | Read back at launch by |
|---|---|---|
| Android | SharedPreferences + `Locale.setDefault` in `MainActivity.onCreate` | `Locale.getDefault()` |
| iOS | `AppleLanguages` in `NSUserDefaults` | `NSLocale.preferredLanguages` |

`AppCompatDelegate.setApplicationLocales` would avoid the restart on Android, but it drags
in AppCompat, exists only there, and would leave the same switch behaving differently on
each platform. No API restarts an iOS app at all — `exit(0)` is indistinguishable from a
crash and is grounds for App Store rejection. Both platforms therefore ask.

`PlatformLocale` is the single source of language: `LanguageProvider` delegates to it, so
content and UI strings can never disagree about which language is on screen.
`AppShellState.language` is what the platform reported at launch — never the stored
preference, which may already point at a language the UI is demonstrably not rendering in.
`pendingLanguage` carries the scheduled choice so the UI can explain itself rather than
appear to have ignored the tap.

Because the language cannot change mid-session, content caches cannot go stale and screens
need no reload machinery.

---

## 3. Architecture

Dependencies point inward: **presentation → domain ← data**.

```
shared/src/commonMain/kotlin/pl/prodevcode/learnmobiledev/
├─ core/mvi/        MVI framework, feature-agnostic
├─ core/ui/         AppString, UiText — text as a key, not a resolved string
├─ domain/
│  ├─ model/        plain Kotlin, no framework annotations
│  ├─ repository/   PORTS (interfaces) + domain exceptions
│  └─ usecase/      one business rule per class
├─ data/            ADAPTERS: sources, parsers, DTOs, repositories
├─ presentation/    Contract + Reducer + ViewModel + Screen, per feature
└─ di/              Koin modules, one per layer
```

Non-negotiable:

- A repository **interface** lives in `domain`, its implementation in `data`. Reversing
  this reverses the dependency arrow.
- `domain` imports nothing from `data` or `presentation`, and no framework annotations
  (`@Serializable`, `@Entity`, Android classes). In KMP the last one would also break the
  iOS build.
- DTOs never leave `data`. Mapping DTO → domain happens at that boundary.
- A business rule belongs in a use case, not in a store. Test: would a widget or the iOS
  app need the same rule? If yes, it is domain.

Clean architecture is about the **direction of the arrows**, not the number of folders.

### Platform code: port or expect/actual?

The codebase uses both, and picking the wrong one is easy. The question is whether the
thing has behaviour worth substituting.

**Default to an interface in `commonMain` plus a per-platform implementation wired through
DI.** `KeyValueStore` and `PlatformLocale` work that way: tests supply their own
implementation, previews get an in-memory one, and the dependency stays visible in the
constructor signature.

**Reach for `expect/actual` only when there is nothing to substitute** — a pure platform
side effect with no logic and no meaningful fake. `SystemBarsAppearance` qualifies: it
needs the platform window handle, does nothing on iOS, and a test double would assert
nothing. Note that `actual` cannot be faked, which is exactly why it is the exception
rather than the rule.

Lesson 13 in the app walks through this decision with both patterns side by side.

---

## 4. MVI

The three rules that make the pattern worth its boilerplate:

1. **One state object per screen.** Derived state is a computed property on `State`, never
   a calculation inside a `@Composable`.
2. **One entry point (`dispatch`) and one mutation point (the reducer).** No
   `_state.value = ...` scattered around a store.
3. **The reducer is pure.** No I/O, no clock, no randomness. I/O results re-enter the loop
   as `Intent.Internal`.

Intents are split deliberately:

- `Intent.Ui` — what the user wants (`QueryChanged`, `FavoriteToggled`)
- `Intent.Internal` — what has already happened (`LoadSucceeded`, `LoadFailed`)

Both go through the same reducer, so every state change has a name and appears on the
timeline.

More conventions:

- **The reducer knows WHAT happened, the store knows WHAT TO DO next.** Side effects run
  in `onIntentProcessed`, after reduction, in their own coroutines — never blocking the
  intent loop.
- **One-off events are effects, not state.** State is replayable; a snackbar kept in state
  fires again after a configuration change. Effects use a `Channel`, not a `SharedFlow`
  without replay, so nothing is dropped and nothing is duplicated.
- **Guard clauses belong in the reducer.** Double-tap protection is `if (id in saving)
  return state`, not `enabled = false` in Compose. That way it is testable and cannot be
  bypassed by a programmatic dispatch.
- **UI-only state still belongs in the state** when losing it would be a bug: an expanded
  lesson, a visible confirmation dialog, the selected tab. `var by remember` does not
  survive a configuration change.
- **State that outlives its screen has to be cleared when the screen is left.** A draft in
  the store outlives the composition that shows it, so leaving the details screen
  dispatches `EditCancelled` — otherwise an edit the user never saved is still there on the
  next visit, and since a present draft is what makes that screen render the form, it opens
  straight into a stale form. Tie this to the deliberate act (back), not to `onDispose`: a
  rotation also disposes the composition and must *keep* the draft.
- **Navigation state is hoisted above the tab switch** (`openUserId` in `AppContent`,
  `rememberSaveable`). Remembered inside a tab it would be discarded whenever another tab
  was selected, so the user would return to the list instead of the screen they left —
  navigation forgetting faster than the store is what makes the two disagree.
- **Destructive actions are confirmed, and a gesture only asks.** A swipe dispatches
  `DeleteClicked`, which opens a dialog; the row snaps back and leaves the list only on
  `DeleteSucceeded`. A swipe is easy to perform while scrolling, and there is no flag to
  flip back the way there is for a favorite — restoring a row means putting it back in the
  right place in a sorted list.
- **Optimistic updates are for reversible writes.** The favorite flag is applied
  immediately and rolled back on failure. A create and a delete are not: the list adopts
  what the server answered, because a create has no id until it replies and a delete has
  nothing to put back.
- `CancellationException` is always rethrown; it is a control signal, not a domain failure.

---

## 5. Testing

```bash
./gradlew :shared:testAndroidHostTest      # unit + integration + smoke (JVM)
./gradlew :shared:iosSimulatorArm64Test    # the same tests on iOS
./gradlew :androidApp:assembleDebug        # app build
```

Rules:

- **Reducer tests carry the weight.** Pure functions, no coroutines, no mocks, no UI.
- **Store tests run on virtual time.** `runTest` plus `advanceTimeBy`, so a 350 ms debounce
  and a 700 ms delay cost milliseconds. Never shorten production constants for tests.
- **Effects are collected on `UnconfinedTestDispatcher(testScheduler)`.** With
  `StandardTestDispatcher` delivery depends on task ordering and the test flakes. This bit
  us once already.
- **Fakes, not mocking libraries.** A fake asserts on behaviour rather than on call
  sequences, and most mocking libraries rely on JVM reflection and do not work in
  `commonTest`.
- **Assert on message identity, not wording.** `assertEquals(AppString.X.asUiText(),
  state.error)` survives copy changes and new translations.
- **Content is data, so validate it like data.** `LessonsContentTest`,
  `QuestionsContentTest`, `StringResourcesTest` and `BundledContentTest` parse the real
  shipped files —
  every locale, not just the default — and check unique ids, non-empty blocks, table column
  counts, explanation length, matching `%1$s` placeholders and, for the quiz, that correct
  answers are not always at the same index. The authored index is only a fallback anyway:
  `GetQuizQuestionsUseCase` shuffles the options of every served question (and moves
  `correctIndex` with the answer), so a session can never be passed by picking one slot.
  `GetQuizQuestionsUseCaseTest` covers that, seeded so it stays repeatable.
- **Which locales a content test checks comes from `AppLanguage`, not from `listFiles()`.**
  Scanning the resource directory made every new subdirectory look like a locale, so adding
  `files/images/` for the infographics broke six unrelated tests looking for
  `images/lessons.json`. Driving the loop off `AppLanguage.SUPPORTED_TAGS` also keeps the
  promise this file makes: adding a language is one enum entry plus one directory, and the
  test starts demanding the directory the moment the entry appears.
- **Guard the promises made in this file.** Where a rule claims something is easy ("adding
  a language is one enum entry plus one directory"), a test enforces it. Rules that only
  live in prose rot; `CodeLanguagePolicyTest` and `StringResourcesTest` exist for
  exactly that reason.
- **A demonstration of a bug must fail.** `CoroutineConcurrencyLabTest` asserts
  `assertFalse(result.passed)` for buggy scenarios. If someone "fixes" the demo, the test
  catches it, because otherwise the lesson would lie.
- **Tests live where their dependencies allow.** Logic in `commonTest` runs on both
  platforms; anything touching the filesystem goes to `androidHostTest`.

Races in the lab are forced with an explicit `yield()`. Never write a test that depends on
an accidental interleaving.

---

## 6. Compose

- Screens are stateless: they take `state` and emit intents. That also makes golden tests
  trivial, because any state can be rendered directly.
- `LazyColumn` items always have a stable `key`.
- Spacing comes from `Spacing`, never from inline literals. Watch for **accumulation**:
  screen margin plus card padding is what the user actually sees.
- Insets are applied selectively (`statusBarsPadding`, `displayCutoutPadding`). Never
  `safeContentPadding()` on a whole screen — it also pads horizontally and pushes the
  bottom bar off the edge.
- The theme wraps content in a `Surface` with the `background` colour; without it the area
  behind system bars stays white in dark mode.
- Content is rendered from a model (`Block`), never from strings assembled in the UI.
- System bar icons follow the app theme, not the system one. `enableEdgeToEdge()` picks
  their tone once, so after an in-app switch to dark mode they would vanish against the
  dark background; `SystemBarsAppearance` corrects that on every composition.

---

## 7. Branches and releases

`main` is protected: no direct pushes, both CI jobs must pass, history stays linear, and
the rules apply to administrators as well.

- Branch from `main`, open a pull request, merge with squash.
- `ci.yml` gates the merge; `release.yml` builds and publishes after it.
- Bump `app.versionName` in `gradle.properties` in the same PR as the change that warrants
  it. `versionCode` is the CI run number and is never set by hand.
- Never commit a keystore, a certificate or a token. Signing material belongs in repository
  secrets.

---

## 8. Definition of done

Before calling a change complete:

1. `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug`
   passes.
2. `CodeLanguagePolicyTest` passes — it covers both the Polish check and hard-coded UI
   text, so there is no need to run the grep by hand.
3. New user-facing text has an `AppString` entry and a key in **both**
   `values/strings.xml` and `values-pl/strings.xml`.
4. New course content exists in **both** `files/en/` and `files/pl/`.
5. New logic has a test; new content is covered by the content-validation tests.
6. README is updated when behaviour or structure changed, and **this file** is updated
   when a rule, a path or a class name changed. Stale guidance is worse than none: it gets
   followed.

When adding a lesson or a quiz question, remember the content tests: unique id, non-empty
fields, explanation of at least 80 characters, distinct options and a `correctIndex` that
is in range.
