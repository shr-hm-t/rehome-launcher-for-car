# Rehome: Launcher for Car

<p align="center"><img src="assets/screenshot-home.png" alt="Rehome home screen" width="49%"> <img src="assets/screenshot-bar.png" alt="Rehome map and contacts bar" width="49%"></p>

A user guide for two apps for Android car head units. Built and tested on one car with one aftermarket Android unit. Other landscape units may behave differently.

| App | What it is | Choose it if |
| --- | --- | --- |
| **Rehome: Scroll** | A full home screen: offline map, 360 camera window, radio and media widget, one-tap contacts. | You want a launcher of its own. |
| **Rehome: Go** | A tiny Home app that opens Google Maps and puts three buttons, a black corner frame and a speed light over everything. Comes with **Rehome: Boot Pack**. | You already use Google Maps, the radio and YouTube and want them one tap apart. |

Download the APKs from the [releases](../../releases). You can have both installed. Only one can be the Home app at a time.

## 1. Install

1. Download the APK.
2. Allow installing from unknown sources, open the APK and choose **Install anyway** if Play Protect warns.
3. If an update is refused, uninstall the old version first (settings will reset).

## 2. Rehome: Go

### First start

1. Open **Rehome: Go**. Android asks to **display over other apps**. Allow it (Go cannot work without it).
2. Choose **Rehome: Go** as the Home app: Android settings, Apps, Default apps, Home app (or press Home and pick it, then choose "Always").
3. Restart the unit once. This also clears any overlays of Rehome: Scroll if you used it before.

### Using it

Pressing Home (or opening Rehome: Go from the app list) opens Google Maps. At boot nothing is opened: a cover with your emblem stays over the unit's start-up screens (12 seconds by default) and then fades away.

The three buttons sit in the bottom right corner on one black pill:

| Button | Tap | Hold |
| --- | --- | --- |
| 360 | Opens the unit's 360 camera | Opens the 360 player (an app whose package starts with `bvack.`, for example [car360-player](https://github.com/bv-ack/car360-player)) |
| Map | Opens Google Maps | **Move and resize** the pill. Drag it, drag a corner handle to resize, use − and + for the transparency, then press Done. The pill may sit a little outside the screen. |
| Radio / YouTube | Opens the radio, or YouTube in Brave (see below) | Opens the other one |

The black corner frame is always on and is fully black. A coloured light along the top warns when you go fast (amber from 80 km/h, red from 120 km/h). The 360 camera opens by itself when you creep along between 5 and 20 km/h.

### Optional permissions (set them in Rehome: Boot Pack)

Go asks for nothing else. These two are your choice, and Go works without them:

- **Location** gives Go your speed. It is used only for the speed light and for opening the 360 when you creep along. Without it, those two features do nothing.
- **Usage access** lets Go see *which app is in front* (only the app name, once a second, nothing is stored or sent). It is used so that the third button can switch between the radio and YouTube depending on where you are. Without it, the third button is the radio on a tap and YouTube on a hold, and it never switches.

### YouTube through Brave

The YouTube button opens the **Brave** browser. To make it land on YouTube:

1. Install Brave (see [brave.com/download](https://brave.com/download/)).
2. Open Brave and go to `m.youtube.com`.
3. Open Brave's menu, then **Settings**, then **Homepage**. Switch it on and enter `https://m.youtube.com` (the menu wording differs a little between versions).
4. Tap the YouTube button in Go. Brave now opens on YouTube.

### Rehome: Boot Pack

A second icon of the Go app. It makes the pictures for the unit's start-up from your emblem:

1. Open **Rehome: Boot Pack** and choose your emblem (PNG). A black background is made transparent for you; switch that off if your emblem has black parts you want to keep.
2. Pick the glow colour (red by default). The preview shows the result.
3. Press **Make the boot pack**. The files are saved in the `Download/Rehome` folder: `bootanimation.zip`, `logo.png`, `logo.bmp` (turned 90 degrees clockwise), `bkg.png` (wallpaper) and `keyboard.png` (a keyboard background without the emblem).
4. **Set the wallpaper** puts the wallpaper on the device. The same picture is the start-up cover of Go.

The app cannot install the boot logo or the boot animation itself: they live in the unit's system and need your own flashing tools. Without an emblem everything is plain black.

Cover time and the slow-speed 360 and speed-light numbers are also in this app.

## 3. Rehome: Scroll

1. Press Home and choose **Rehome**, then follow the welcome screen (home app, display over other apps, notification access).
2. Open settings (gear icon). The **Guide** tab explains the rest.

What it shows: offline map (Mapsforge, optional 3D with MapLibre `.mbtiles`), 360 camera window, radio and media widget, one-tap contacts, steering-wheel voice button through **Rehome: Call Plugin**, a floating Home button and a black frame over other apps, a speed glow along the top, and a dark or light theme (theme files: see [THEMING.md](THEMING.md)).

Emergency way into the settings: open **Rehome: Scroll** from the app list, or hold four fingers on the launcher for five seconds.

## 4. Troubleshooting

| Problem | Try |
| --- | --- |
| Buttons or frame do not show | Allow *display over other apps* for Rehome: Go. Make sure Go is the Home app. |
| Both Go and Scroll draw overlays | Restart the unit after changing the Home app. |
| The third button never switches | That is normal without *usage access*. Allow it in Rehome: Boot Pack if you want it. |
| Speed light or slow-speed 360 do nothing | They need *location*. Allow it in Rehome: Boot Pack and make sure the unit has a GPS fix. |
| Touches do not work on a tablet | Update to the latest Go. Newer Android versions drop touches through full-screen overlays; Go avoids them. |
| Maps opens but the buttons are in the way | Hold the Map button and move or shrink the pill. |

## Related apps

- [car360-player](https://github.com/bv-ack/car360-player): a 360 camera player by the same author. Go opens it when you hold the 360 button.

## Extras

- [`boot`](boot): a ready-made neutral boot logo and boot animation (download from there)
- [`docs/ASSET_PROMPTS.md`](docs/ASSET_PROMPTS.md) and [`docs/ASSET_PROMPT_ONE_SHEET.md`](docs/ASSET_PROMPT_ONE_SHEET.md): prompts for making icons and UI art
- [`docs/SHOWCASE_PROMPTS.md`](docs/SHOWCASE_PROMPTS.md): prompts for showcase pictures

## Build

Java 17, Gradle 8.9. Rehome: Scroll: `gradle assembleRelease` or the **Build APK** workflow in GitHub Actions. Rehome: Go lives in the `go` folder and has its own workflow, **Build Rehome Go**.

## Privacy

Everything stays on the device. Rehome: Scroll makes one network request: the area-name lookup (OpenStreetMap Nominatim). Rehome: Go makes none. No analytics, no account.

## Credits

Claude (Anthropic) is the lead builder and designer. Google AI and ChatGPT create images and media. Grok helped in a minor way. Idea, direction and testing: the project owner. Map rendering by [Mapsforge](https://github.com); map data © OpenStreetMap contributors.

An independent hobby project, not affiliated with any brand named here. Released under the [MIT licence](LICENSE).
