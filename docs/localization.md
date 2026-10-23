# App languages

The settings spinner supports English (`en`), Portuguese (`pt`),
Spanish (`es`), Italian (`it`), Ryukyuan/Okinawan (`ryu`) and Javanese (`jv`).
Its single, non-translatable `languages` array follows the indexes in `Language`.
The shared Portuguese translation in `values-pt` serves all Portuguese speakers,
using the language code `pt` without a region. Unsupported device languages
fall back to English.

`jv` now selects Javanese. There is intentionally no migration from the previous
use of `jv` for Ryukyuan; select Ryukyuan again in Settings when needed.

## Ryukyuan maintenance

The original translated strings contributed by kuragehime (GitHub
kuragehimekurara1), through commit `7d18c1d`, retain their wording from the
historical `values-ja-ryu/strings.xml`. Only subsequent additions have their wording revised.
Common app names and non-translatable metadata are inherited from default resources.
Only the format markers in the two original multi-argument messages are
normalized to positional indexes (`%1$s`, `%2$d`, etc.) for AAPT and lint; their
wording, argument order and line breaks remain unchanged.

`values-ryu` holds the Ryukyuan translation. Android API 14–20 cannot resolve
three-letter language resource codes, so `LocaleHelper` uses `ja` internally
on those versions when Ryukyuan is selected. `values-ja` is an identical copy
of the Ryukyuan strings for this compatibility path, not a separate Japanese
translation. Keep both files synchronized. The stored language remains `ryu`.

The new Ryukyuan wording follows the existing contributor's conventions; the
Javanese translation uses informal Javanese. Native-speaker review of the new
wording is still welcome.

## Validation

`LanguageTest` checks distinct codes, Portuguese region normalization, spinner indexes
and unsupported-language fallback. `LocalizationTest` checks actual Android
resource selection and formatted server messages for all six languages. Run:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

Run the device tests on API 14 or 19 and a recent API to cover both Ryukyuan
resource paths.

References: [Android resource localization](https://developer.android.com/guide/topics/resources/localization),
[pre-Lollipop language-code limitation in Chromium](https://chromium.googlesource.com/chromium/src/+/19933cdbe5cae51d150fccb62883774edb543b51/build/android/gyp/util/resource_utils.py),
[Okinawan language code](https://glottolog.org/resource/languoid/id/cent2126),
[Javanese locale](https://unicode.org/cldr/charts/44/summary/jv.html).
