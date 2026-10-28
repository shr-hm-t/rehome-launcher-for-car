# TODO and hand-over notes

The owner has paused the project. Latest: **Rehome: Go 2.6.1** and **Rehome 1.7**. Everything below is undone unless it says otherwise. Anyone is welcome to continue.

## Next small changes for Go (asked for, not built)

- **Speed glow bigger:** the light along the top should cover more of the screen, about 30-40% of the height or more, with a setting for the size.
- **Darker right side of the picture:** a setting for how dark the right side of the Boot Pack picture gets (the look is fixed now, like the unit's own theme).
- Colour presets in Boot Pack (red, blue, green like the unit's own themes) and optional strength of the fine horizontal lines.
- Boot Pack app icon (`ic_boot`) as a download-style logo.
- Rename `keyboard.png` to `theme.png` if wanted.
- **Black screen when opening Boot Pack while Go runs:** the cause was never confirmed (the start-up cover, the overlays, or the shared Home task were suspects). The gear now hides Go's windows first and opens settings in its own task. Check on a car. If it still happens, look at the logs.
- **Size of the app (about 175 KB):** turn on R8 shrinking (`minifyEnabled true`, `shrinkResources true`) and use fewer or smaller launcher icon files (they are about a third of the file). Realistic result: roughly 110-130 KB. Shorter names or other character sets do not help, R8 already renames everything.
- Test everything since Go 2.1 on a car (only partly tested). The 4-button pill is wider, so saved positions may need re-placing.
- Keep the old Boot Pack launcher icon or remove it once the gear is proven.
- Put the GitHub topics on the repository (only the owner can).
- Publish a release and a description on the [car360-player](https://github.com/bv-ack/car360-player) repo, so the "hold 360" player is downloadable.

## Bigger ideas (both apps)

- **Option box** (no outline, vignette-like) behind the Go buttons.
- **Half-screen keyboard with an undo button** (`keyboard.png` from Boot Pack is its theme picture).
- **Dynamic island** for turn-by-turn and city information. Must be switchable off and hidden on Maps.
- **Offline navigation** with 3D maps: not OsmAnd. Idea: GraphHopper (Apache 2.0), graph built in GitHub Actions like the Sri Lanka map (release `map-sri-lanka`), a places search database, spoken guidance. Do a feasibility spike first. Other ideas: free Google Maps navigation as seen in other car launchers, and Mapbox. Not researched.
- In-app map downloads (Mapsforge) for the launcher.
- Integrate the 360 viewer [car360-player](https://github.com/bv-ack/car360-player) into the apps, or keep it separate.

## Planned "2.0" for the launcher (not started)

Two APKs: **Go** and **main + Go**.

- Replace the launcher's boot pack with Go's; keep old and new start-up macros selectable; choose launcher vs Go as Home.
- Screensaver start: logo on the right, analog clock on the left, switching to the map after 10 s (default). Remove the map start-up, keep the radio start-up.
- Contact bar: 6 buttons become 5 (radio, 360, Maps, YouTube, Settings). No call buttons, no hold-to-open-settings, holding Maps does nothing, holding YouTube opens Brave. Google AI icons in the original radio-style look. Default bar height 18%.
- Settings window inside the widget area.
- Idle logo screen on black when Maps is selected and nothing is navigating (default delay 10 s), same size and position as Go's logo, with a red (changeable) inverse glow behind it.
- Dynamic island and the keyboard in the launcher too.

## Notes for the next Claude (or person)

- **Layout:** the launcher is in `app/` (about 4,300 lines in `MainActivity`). Go is separate in `go/` (package `com.rehome.go`; settings and the Boot Pack maker are one screen, opened by the gear on the pill, hold Maps then Settings, or the old second launcher icon). Go classes: `GoActivity` (Home), `GoService` (all overlays), `GoPrefs`, `AppWatcher`, `BootPackActivity`, `NullPack` (picture and file maker).
- **Builds:** only through GitHub Actions. Go: workflow `Build Rehome Go` (`build-go.yml`), optional input `release_tag` such as `go-2.5` publishes a release. Compile errors show as annotations. There is no local Android SDK.
- **Versions:** Go `go/app/build.gradle` (versionCode 30, versionName 2.4.5). Add a section to `RELEASE_NOTES.md` for each release.
- **Rules from the owner:** no brand or trademark names in repository files; screenshots have the emblem painted out. Ask before big changes to the launcher (it is tested and loved).
- **Android facts learned:** newer Android drops touches through a full-screen overlay window, so the frame is built from edge strips and corner windows. The head unit auto-grants usage access and reports `MODE_DEFAULT`; see `AppWatcher.granted()`. Direct boot: prefs and files live in device-protected storage. `Bitmap.createBitmap(int[]...)` is immutable, so draw on a mutable bitmap.
- **Boot Pack picture:** full colour background, black laid over it (full in the middle, fading linearly to nothing at "black spread"), emblem placement x, y and size are settings, rounded black corners on all outputs. Files go to `Download/Rehome`: `bkg.png`, `logo.png`, `logo.bmp` (turned 90 degrees clockwise, 720x1280), `bootanimation.zip` (one frame), `keyboard.png`.
- **Go behaviour:** one black pill (360, Maps, radio or YouTube). Hold 360 opens the 360 player (`bvack.*` package). Hold Maps to move, resize and set transparency. Location and usage access are optional. Overlays show even when Go is not Home unless the test switch in Boot Pack is turned off.
