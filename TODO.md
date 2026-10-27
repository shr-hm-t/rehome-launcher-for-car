# TODO and hand-over notes

The project was paused after **Go 2.4.4** (latest Go release) and **Rehome: Scroll 1.7** (release `main`). Nothing below is started unless stated. Anyone is welcome to continue.

## Small things left for Go

- README: say clearly that **Rehome: Boot Pack may need Go force-stopped first** (Go's overlays can sit over Boot Pack's screen; `GoService.suspend()` only partly fixes it), and that **holding the radio button opens YouTube (Brave)**.
- Change the Boot Pack app icon (`ic_boot`) to a download-style logo.
- Optional "option box" (no outline, vignette-like) behind the three buttons.
- Half-screen keyboard with an undo button (planned for both apps; `keyboard.png` is already made by Boot Pack as its theme picture).
- Dynamic island for turn-by-turn and city information. Must be switchable off and hidden on Maps.
- Rename `keyboard.png` to `theme.png` if wanted (the owner once called it that).
- Verify on the car everything since Go 2.1 (only partly tested).

## Planned "2.0" (not started)

Two APKs: **Go** and **main + Go**.

- Replace the launcher's boot pack with Go's; keep old and new start-up macros selectable; choose launcher vs Go as Home.
- Screensaver start: logo on the right, analog clock on the left, switching to the map after 10 s (default). Remove the map start-up, keep the radio start-up.
- Contact bar: 6 buttons become 5 (radio, 360, Maps, YouTube, Settings). No call buttons, no hold-to-open-settings, holding Maps does nothing, holding YouTube opens Brave. Default bar height 18%.
- Settings window inside the widget area.
- Idle logo screen on black when Maps is selected and nothing is navigating (default delay 10 s) with a red (changeable) inverse glow.
- Offline navigation with 3D maps: not OsmAnd. Idea: GraphHopper (Apache 2.0), graph built in GitHub Actions like the Sri Lanka map (release `map-sri-lanka`), a places search database, spoken guidance. Do a feasibility spike first. Other ideas the owner mentioned: free Google Maps navigation as seen in other car launchers, and Mapbox. Not researched.
- Possibly integrate the 360 viewer [car360-player](https://github.com/bv-ack/car360-player).

## Notes for the next Claude (or person)

- **Layout:** the launcher is in `app/` (about 4,300 lines in `MainActivity`). Go is separate in `go/` (package `com.rehome.go`, Boot Pack is a second launcher icon of the same app). Go classes: `GoActivity` (Home), `GoService` (all overlays), `GoPrefs`, `AppWatcher`, `BootPackActivity`, `NullPack` (picture and file maker).
- **Builds:** only through GitHub Actions. Go: workflow `Build Rehome Go` (`build-go.yml`), optional input `release_tag` such as `go-2.5` publishes a release. Compile errors show as annotations. There is no local Android SDK.
- **Versions:** Go `go/app/build.gradle` (versionCode 29, versionName 2.4.4). Add a section to `RELEASE_NOTES.md` for each release.
- **Rules from the owner:** no brand or trademark names in repository files; screenshots have the emblem painted out. Ask before big changes to the launcher (it is tested and loved).
- **Android facts learned:** newer Android drops touches through a full-screen overlay window, so the frame is built from edge strips and corner windows. The head unit auto-grants usage access and reports `MODE_DEFAULT`; see `AppWatcher.granted()`. Direct boot: prefs and files live in device-protected storage. `Bitmap.createBitmap(int[]...)` is immutable, so draw on a mutable bitmap.
- **Boot Pack picture:** full colour background, black laid over it (full in the middle, fading linearly to nothing at "black spread"), emblem placement x, y and size are settings, rounded black corners on all outputs. Files go to `Download/Rehome`: `bkg.png`, `logo.png`, `logo.bmp` (turned 90 degrees clockwise, 720x1280), `bootanimation.zip` (one frame), `keyboard.png`.
- **Go behaviour:** one black pill (360, Maps, radio or YouTube). Hold 360 opens the 360 player (`bvack.*` package). Hold Maps to move, resize and set transparency. Location and usage access are optional. Overlays show even when Go is not Home unless the test switch in Boot Pack is turned off.
