# Read this first

Both apps are made mainly for **cars with a 360 camera**, and were tested on one unit only. Older versions were removed, so this page is a list of what not to miss, not a version history.

## Rehome 1.7 (full launcher)

- **Hold the 360 button to open the settings.**
- It leaves room for your unit's own 360 picture-in-picture. It does not draw the camera itself.
- Use only one of Rehome 1.7 and Rehome: Go as the Home app. After switching, restart the unit.

## Rehome: Go (Home app with buttons)

- **Install Google Maps and Brave.** Pressing Home opens Google Maps. The YouTube button opens Brave: set `https://m.youtube.com` as Brave's homepage so it lands on YouTube.
- **Hold the Maps button** to move and resize the buttons and set their transparency.
- **Hold the 360 button** to open the 360 player ([car360-player](https://github.com/bv-ack/car360-player)). Tap opens the unit's own 360 camera.
- **Hold the radio button** to open YouTube. Tap opens the radio (without usage access it never switches by itself).
- **Start-up cover:** Go covers the unit's start-up screens with your picture (12 s by default). If your unit has no ugly start-up screens, set the cover time to 0 or a low value in Rehome: Boot Pack.
- **Boot Pack:** force-stop Rehome: Go in Android settings before opening Rehome: Boot Pack, or it may not work. It only makes the files (in `Download/Rehome`). Installing the boot logo and animation on the unit needs your own tools.
- **Optional permissions:** location (speed light, and the 360 opening by itself when you creep along slowly) and usage access (the third button switching between radio and YouTube). Without them those features do nothing.

## Known problems we did not finish

- Boot Pack still needs Go force-stopped first (Go's overlays sit over its screen). We could not fix it in time.
- Only tested on one head unit and one tablet. Other units, screen sizes or Android versions may behave differently. The first Go versions did not receive touches on the tablet (fixed since, but another unit might differ).
- Everything made after Go 2.1 was only partly tested in the car.
- No navigation of its own, no offline routing, and no half-screen keyboard. These were planned. See [TODO.md](TODO.md).
