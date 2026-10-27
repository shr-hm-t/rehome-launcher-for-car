# Release notes

## Go 2.3

- Boot Pack: colour vignette now centred on the screen (black in the middle, colour towards the edges), rounded black corners drawn on every output, emblem on the right.
- One black pill holds the three buttons; only the top two corners are rounded. It can be moved a little outside the screen.
- Icons normalised to the same visual size.
- Optional location and usage access, hold 360 for the 360 player, cover 12 s, new README user guide.

## Go 2.2

- New icons for the buttons and the app icons.
- Fixed "immutable bitmap" when choosing an emblem; the black-background cut is now an option (on by default) that keeps your original picture.
- Rehome: Boot Pack no longer sits under Go's frame and buttons: Go hides its overlays while Boot Pack is open, so Go no longer needs to be force stopped.
- The black frame is now four edge strips and four corner pieces, so nothing covers the middle of the screen. Newer Android versions (for example on a tablet) drop touches that pass through a full-screen overlay; this keeps touches working there. The speed light is added only while speeding.
- A message is shown if Google Maps is not found.

## Go 2.1

A redesign of Rehome: Go. It is now very small and has no screens of its own.

- **Three buttons, bottom right:** bare icons on separate black discs: 360, Maps, and a third one that shows the radio when you are in the 360 or YouTube, and YouTube when you are in Maps or the radio. Hold them to move and resize them (corner handles, fixed ratio) and to set the transparency; otherwise they stay fixed.
- **Black corner frame:** the launcher's frame with rounded corners, fully opaque, always on.
- **Home opens Maps** (also when opened from the app list). At boot nothing is opened: the start-up cover (your emblem on black with a smooth round glow, red by default) stays for a few seconds above everything.
- **Speed light and the slow-speed 360** are kept. The slow-speed 360 now defaults to 5 to 20 km/h.
- **Rehome: Boot Pack** is a separate app icon: it makes the null boot logo (turned 90 degrees clockwise), the null boot animation (one frame, 1 fps, looping) and the wallpaper from your emblem (60% of the height, a little above the middle; a black background is made transparent automatically). Cover time and the driving settings are there too.
- **No usage access needed any more.** Apps are found by name and package automatically. Only "display over other apps" and location are asked.
- Removed: the fallback screen, the welcome, the vignette, the top frame, the logo overlay and the start-up macro.

## Go 2.0.1

- Usage access is now recognised on units that grant it automatically; Go keeps checking until it works.
- The mini bar now sits lower and to the right by default (below the unit's status bar), and the logo takes the top-centre place where the bar was. Both are still adjustable.

## Go 2.0 (new, separate app)

- **Rehome: Go**: a second app (`Rehome-Go.apk`) that can be set as Home instead of Rehome: Scroll. Rehome: Scroll is unchanged.
- **Start-up:** a wallpaper cover goes up, Maps opens at once, then the radio, then 360 (delays in 100 ms steps, cover time after 360 is configurable). Direct boot aware.
- **Overlays over Maps, radio, 360 and YouTube only:** star vignette (solid black, not see-through), your logo with sparkle, a top frame (left and right pieces, not over the 360) joined to the mini bar, and the speed light (amber from 80 km/h, red from 120 km/h).
- **Mini bar:** shows the other three apps, hold any button to open the settings and app chooser. It can sit at the top (joined to the frame) or at the bottom (own rounded bar), with width, height and position settings.
- **Slow-speed 360:** shows the 360 when driving slowly, upper limit 20 km/h.
- **Wallpaper maker:** black wallpaper with a horizon and a bell-shaped glow, with your logo and the frame painted in; or use your own image.
- **Fallback screen and welcome:** first-run setup (permissions, logo, apps, wallpaper) and a settings screen with calibration for every size. Home can return to the last app (default) or show this screen.
- Apps are picked by name and package automatically, and can be changed.

## 1.7 (compared with 1.6)

- **Start-up cover:** at boot (and, if you choose, each time the 360 app is opened from Rehome) a dark screen with a slowly breathing amber and blue glow in the middle and your logo fades in over the unit's start-up screens, then fades out. Time and mode are in the 360 camera and start-up tab. This replaces the plain black cover.
- **Vignette shapes:** oval, circle, rounded rectangle, and a four-point star (dark bands along the edges, thinnest at the corners).
- **Logo sparkle** (can be switched off).
- **Emergency menu:** hold four fingers on the launcher for five seconds to open the settings or reset everything.
- **Boot pack tab:** makes the boot logo and boot animation on the device from your own badge PNG (optional transparent background) and your own reminder text, and saves the files for you to install with your own tools.
- A neutral boot logo and boot animation are in the boot folder, and showcase and icon prompts in the docs folder.

## 1.6 (compared with 1.5)

Fixes and changes from the 1.5 car test. 

- **Settings always reachable:** opening Rehome from the app list opens the settings; holding the clock, the logo, any bar icon or the bar itself does too.
- **Floating button:** only the Home button, only over the 360 app, and not while the extended frame is on (the extended bar's 360 button turns into Home while the 360 app is shown). Its position is remembered separately for each driver side.
- **Frame:** the status bar frame is now two glass slabs like the contacts bar (height adjustable, can be switched off, can skip the home screen). The corner frame starts at the bottom of the status bar frame. The extended frame settings are hidden until you hold the About tab.
- **Vignette:** two styles (surround or a soft bottom fade; automatic uses the fade except in always-dark mode), surround shapes, spread, a separate strength over the YouTube widget, smoother edge. Default 200.
- **Contact bar:** one rounded bezel with a thin divider between the icons, corner 60 by default; the icons no longer sit on separate glass.
- **Navigation and city text** are right-aligned without boxes; navigation shows the extra line (destination or arrival time) when the navigation app provides it. The play button sits above the bars.
- **Logo** moves to the top corner (right when the driver sits on the left); default size 160. Hold it for settings.
- **Bottom bar:** all buttons shown, order set in settings; two new buttons (open YouTube in the browser). With the 360 window layout, tapping the map/video button switches the widget. The app list is now a large icon grid.
- **Defaults:** no 360 window, UI size 100, map always line art, call-and-wheel and extended-frame settings hidden until About is held, no icon glow, mute removed.
- **Cover:** a black screen fades out while the 360 app starts.
- Names: the main app is **Rehome: Scroll**, the installer **Rehome: Installer**, the helper **Rehome: Call Plugin**.
- Settings tab list shows a small arrow with a fade when there are more tabs below.

Not done yet: flat redesigned icons, a new theme, navigation as an overlay, the extended-frame fixes, 3D map setup help.

## 1.5 (compared with 1.4)

Renamed to **Rehome** (the repository is rehome-launcher-for-car). The package is now com.rehome.launcher, so this installs as a new app next to the old one: set Rehome as the home app again and go through the welcome screen. Three icons and names: the app itself "Rehome Installer" (what the installer shows), the home screen "Rehome: for you", the call helper "Rehome: Call Plugin".

- Layout: the map runs under one rounded, glass contacts bar (no black box behind it); the bar stays inside the frame margin; the bottom dock is a plain black bar. The 360 window and the bars mirror when the driver sits on the left.
- Navigation and city info now sit on the side away from the 360 window, in glass boxes. Icons on the contacts bar sit on glowing red/blue glass; the dock has no glow.
- Simple layout bar order: radio, 360, map/media switch, three contacts. The switch shows a map pin while the map is shown; tap opens Google Maps (or Brave while the media widget is shown), hold changes the widget. The Navigate button is gone.
- The YouTube embed is gone; the widget shows what is playing (from the notification) with a play/pause button.
- Frame: one adjustable frame (margin, rounded corners) that blocks touches on its black parts; optional extended bottom bar with 360 and the primary contact (simple layout); optional split status-bar slabs; a setting to leave Android system apps uncovered.
- Floating buttons: choose which ones (Home by default; Back, Mute, Radio, 360 optional; Back and Mute may not work on every unit); they appear only in the apps you choose (default: the 360 app).
- Speed glow: top third of the screen only, yellow from 80 km/h and red from 120 km/h, nothing below that. No orange mist.
- Vignette up to 200. Map text 300% and UI size 130% by default. Dark theme by default.
- Start-up: radio and 360 start first; the interface, frame and floating buttons appear a few seconds later (setting). Black cover that fades out after the map has drawn.
- Welcome screen simplified (look, driver side, 360 window); then Android asks for home app, display over other apps and notification access.
- New Guide tab (first): how to open settings, Brave, map downloads, 3D map files. Settings tab list shows arrows when there are more tabs.
- Optional centre logo (your own PNG). Call settings reduced to the call account; advanced call and wheel-button settings appear after holding the About tab.
- Removed: Navigate button, navigation test, backup/restore, per-app exclusion lists, status bar looks, hold-to-open-Brave, YouTube embed.

## 1.4 (compared with 1.3)

- Package name is now a different package name from before (a new app: it does not update over the old one; set Rehome as the home app again and redo the permissions). The APK is called Rehome.apk.
- New app icon.
- The welcome screen asks whether your unit has a 360 window (PIP).
- Wider speed glow along the top and sides.
- Optional 3D map (MapLibre): choose it in Settings > Map and pick an offline .mbtiles vector file (OpenMapTiles layout). Tilted view with 3D buildings; no place names yet. Mapsforge flat map stays the default.

## 1.3 (compared with 1.1)

Note: the 1.1 release was built from a commit whose app version already said 1.2 inside, so the installed 1.1 APK shows "1.2" in Android's app info. This release is 1.3 (version code 8).

- Start-up: black screen first, then the interface, then a fade-in once the map has had time to draw (no white tiles). Map background no longer flashes white in the dark theme.
- Speed glow: a wider, brighter light along the top and both sides of the map (accent colour normally, amber from 80 km/h, red from 120 km/h).
- The bottom bar and the contacts bar are each one rounded bar (no separate tiles, no tile gap).
- The play button in the YouTube widget is no longer hidden behind the vignette.
- New shield bar: a black bar at the bottom of other apps with the 360 and primary-contact buttons and an adjustable angle; it blocks touches. The frame, shield bar and floating buttons each have a NetGuard-style exclusion list (Settings > Per-app rules). Needs Usage access.

## 1.1 (compared with 1.0)

- Offline Mapsforge map only (online maps removed), new thin "line art" map style, stronger vignette, tile gaps and icon glow (all adjustable in the theme).
- Configurable bottom bar (radio, map/YouTube, navigation, music, dialer, car app, day/night, apps, settings).
- PIP off = simple full-width layout: no 360 window, no black dock; radio and map/YouTube switch move into the contacts bar.
- Faster start: the launcher shows its background first, then builds the screen and loads the map.
- Renamed Rehome with a new app icon; holding the play button in the YouTube widget opens Brave; floating buttons start on the left.
- Floating second button can be Back (default; falls back to Home where the unit does not allow it) or Home.
- Per-app rules using Usage access (no accessibility service).
- Welcome guide: overlay permission, notification access, usage access.
- Navigation info in readable dark boxes; Navigate button; settings backup/restore; status bar looks; YouTube embed fixes (no more intent: errors); call hang-up; sea-level altitude.
- New defaults: 360 window 39.8 x 51.5, black bar 6.5, margin 8, corner 60, auto-360 speed 5-10 km/h, 360 retry 4 s, radio 1 s.

## 1.0 (compared with 0.2)

**Layout**
- New home screen: map / widget area on top, a 15% bar with the 360 camera and three contacts (right of the 360 window), and a black bottom dock (clock under the 360 window, icons: Radio, map/YouTube switch, theme sun/moon, app drawer, settings).
- The play button moved from the icon grid into the YouTube widget.
- The 360 camera window slot now shows a summary behind it: date, heading, altitude, GPS accuracy.
- Calibration screen with live guides: 360 window width and height, black bar height, bar height, frame margin, corner size.

**Screen frame and floating buttons**
- The black frame is now a real 4-corner overlay above every app (touches pass through).
- Bigger neon Mute and Home buttons. Default side follows the driver seat, optional lock, colours, size.
- Mute now has five methods; the default is "like the system mute key".

**Map**
- Engines: OpenStreetMap, offline Mapsforge, Google Maps web page, none.
- Map zoom, map text size, and a vignette with adjustable strength.
- Navigation info test (Settings > Map) shows the raw notification data for troubleshooting.

**YouTube widget**
- Choose notification mode (Open YouTube button) or embedded page. Thumbnail only, play/pause on the thumbnail, the thumbnail stays.

**360 camera**
- Opens automatically when you drive slowly (GPS speed), with time-gap or re-arm options and a touch pause. Optional second try after boot.

**Calling**
- Choose the account (Bluetooth or SIM) for calls.
- Steering-wheel voice button: pick "Call Plugin" in the unit's voice list to call the primary contact.

**Look**
- Radio-style look: gradient, soft colour blobs, vignette, dark translucent tiles, orange accent.
- Settings menus are now side-tab panels at the top right.
- Contacts show a phone icon with the letter; the primary is in the accent colour, the others grey.
- Everything is themable with one JSON file: export, edit, import (see THEMING.md).
- Settings > About with "Made with Claude".

**Start-up**
- The launcher is now direct-boot aware: at a cold start it shows a black screen straight away (instead of "Android is starting") and loads the full home screen as soon as the unit unlocks.

**Removed**: fancy look (replaced by the theme file), three-corner screen rounding.

Version is now 1.0 (versionCode 4).
