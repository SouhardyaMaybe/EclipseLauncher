# WIKI

## Build facts (verified against artifact POMs/sources)
- M3 1.2.1: `Surface` has `shadowElevation`, no `elevation`; clickable overload requires non-null `onClick`.
- M3 1.2.1: `TextFieldDefaults.textFieldColors` uses `focusedTextColor`/`unfocusedTextColor`/`focusedPlaceholderColor`/`unfocusedPlaceholderColor`.
- M3 1.2.1: `Slider` parameter is `valueRange`, not `range`.
- M3 1.2.1: `lightColorScheme`/`darkColorScheme` have no `shadow` parameter.
- `MaterialTheme` (M3 object) has no `typography` custom property; use `MaterialTheme.typography`.
- `Icon` (M3) has no `onClick`; use `Modifier.clickable` or `IconButton`.
- `Modifier.weight` is a `RowScope`/`ColumnScope` extension: never import it; top-level composables using it on their root Column must be scope extensions.
- `collectAsStateWithLifecycle` requires lifecycle-runtime-compose; app uses `collectAsState` instead.
- `Text(text = nullable)` fails; use `.orEmpty()`.
- Icons: `Icons.Default.*` need `import androidx.compose.material.icons.filled.*` (extended artifact).

## CI
- Only verification path: GitHub Actions (`gradle lint`, `gradle testDebugUnitTest`, `gradle assembleDebug`, `gradle bundleRelease`).
- Inspect failures: `gh run view <id> --log-failed`.
- AGP lint DSL: `textReport = true` with `textOutput` unset prints the full lint report to stdout (verified against AGP 8.9 Lint DSL docs).
- AGP writes lint HTML as `app/build/reports/lint-results-debug.html`; text report at `app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt`.
- `tools:targetApi="27"` on an XML `<item>` silences NewApi for API-gated theme attributes with minSdk 24.
