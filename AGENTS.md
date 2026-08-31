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
| UI strings | **JSON catalogue only** | `files/en/strings.json`, `files/pl/strings.json` |
| Course content (lessons, quiz, scenarios) | **JSON assets only** | `files/en/`, `files/pl/` |

Two tests enforce this rather than trusting discipline: `CodeLanguagePolicyTest` rejects
Polish characters and hard-coded `Text("…")` literals in Kotlin sources, and
`StringCatalogContentTest` rejects an incomplete catalogue.

**No Polish anywhere in `.kt` files.** Not in comments, not in KDoc, not in test names,
not in string literals. The only tolerated exception is fictional personal data in demo
fixtures (`InMemoryUserRepository` contains names such as `Celina Wójcik`) — that is data,
not code.

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

Strings live in `composeResources/files/<lang>/strings.json` and are looked up through
`AppString` (a generated-style enum of keys) and `LocalStrings`.

```kotlin
Text(localized(AppString.QuizStart))
Text(localized(AppString.QuizScore, correct, total))
```

**Why not Compose Resources `values-pl/`?** Because `Res.string` resolves the locale from
the **system** setting, and in Compose Multiplatform 1.11 that cannot be overridden:
`ResourceEnvironment` has an internal constructor and `LocalComposeEnvironment` is
internal. With an in-app language switch the course content would change while tab labels
and buttons stayed in the system language.

Consequences worth knowing:

- Adding a string means one entry in `AppString` plus one key in **every**
  `strings.json`. `StringCatalogContentTest` fails the build on a missing key, an unused
  key, a blank value, or a translation whose `%1$s` placeholders do not match the default.
- A missing key at runtime renders the key itself rather than throwing — a defect should
  be visible during development, never a crash in a user's hands.
- The catalogue is gated at startup: `AppContent` renders a brand-coloured screen until
  both the theme and the strings are loaded, so nothing ever flashes raw keys.

### Course content

Compose Resources applies qualifiers to `values/` and `drawable/`, but **not** to `files/`.
Localized content therefore lives in one directory per language:

```
composeResources/files/
├─ en/{strings,lessons,questions,scenarios}.json   <- default
└─ pl/{strings,lessons,questions,scenarios}.json
```

`LocalizedAsset` resolves the path from `LanguageProvider`; an unsupported language falls
back to `en`. Adding a language means adding one directory and one enum entry — no other
code changes.

`LanguageProvider` is a port with per-platform implementations (`Locale.getDefault()` on
Android, `NSLocale.preferredLanguages` on iOS). `EffectiveLanguage` decorates it with the
user's choice and is the single source of truth for every data source.

### In-app language switching

The device language decides only the **initial** value; after that the choice is explicit
and persisted. There is deliberately no "system" variant of either `AppLanguage` or
`ThemeMode` — a third option that renders identically to one of the other two is a state
nobody can reason about.

Because repositories cache parsed JSON, **every content cache is keyed by language**:

```kotlin
private var cached: Pair<String, List<Lesson>>? = null
```

A plain field would keep serving the previous locale after a switch. On top of that,
`AppShellState.contentRevision` is bumped only when the *effective* language really
changed, and screens key their reload on it — so switching to a language that resolves to
the same tag does not trigger a pointless reload.

---

## 3. Architecture

Dependencies point inward: **presentation → domain ← data**.

```
shared/src/commonMain/kotlin/pl/prodevcode/learnmobiledev/
├─ core/mvi/        MVI framework, feature-agnostic
├─ core/ui/         AppString, StringCatalog, UiText — text as a key, not a resolved string
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
DI.** `KeyValueStore` and `LanguageProvider` work that way: tests supply their own
implementation, previews get an in-memory one, and the dependency stays visible in the
constructor signature.

**Reach for `expect/actual` only when there is nothing to substitute** — a pure platform
side effect with no logic and no meaningful fake. `SystemBarsAppearance` qualifies: it
needs the platform window handle, does nothing on iOS, and a test double would assert
nothing. Note that `actual` cannot be faked, which is exactly why it is the exception
rather than the rule.

Lesson 10 in the app walks through this decision with both patterns side by side.

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
  `QuestionsContentTest` and `StringCatalogContentTest` parse the real shipped files —
  every locale, not just the default — and check unique ids, non-empty blocks, table column
  counts, explanation length, matching `%1$s` placeholders and, for the quiz, that correct
  answers are not always at the same index.
- **Guard the promises made in this file.** Where a rule claims something is easy ("adding
  a language is one enum entry plus one directory"), a test enforces it. Rules that only
  live in prose rot; `CodeLanguagePolicyTest` and `StringCatalogContentTest` exist for
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
   `files/en/strings.json` and `files/pl/strings.json`.
4. New course content exists in **both** `files/en/` and `files/pl/`.
5. New logic has a test; new content is covered by the content-validation tests.
6. README is updated when behaviour or structure changed, and **this file** is updated
   when a rule, a path or a class name changed. Stale guidance is worse than none: it gets
   followed.

When adding a lesson or a quiz question, remember the content tests: unique id, non-empty
fields, explanation of at least 80 characters, distinct options and a `correctIndex` that
is in range.
