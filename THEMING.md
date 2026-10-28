# Theming

Settings > Look > Export theme writes `rehome-theme.json` to the app's files folder (Android/data/com.rehome.launcher/files/theme). Edit it, then Settings > Look > Import theme. Keys you leave out keep their default. `themes/rehome-default-theme.json` is the full default.

Colours are `#RRGGBB` or `#AARRGGBB`.

- `name`, `version`: Label only
- `dark`, `light`: Palettes: `bgTop`, `bgBottom` (background gradient), `accent`, `text`, `sub` (secondary text), `glyph` (icons), `tileFill`, `tileStroke`, `vignetteColor`
- `menu`: Settings panels: `bgTop`, `bgBottom`, `stroke`, `accent`, `text`, `sub`
- `vignette`: Strength 0-100
- `tileRadius`: Tile corner roundness in dp
- `layout`: Fractions of the screen: `pipW`, `pipH`, `barH`, `dockH`
- `frame`: `margin`, `corner` in dp
- `icons`: Icon overrides, see below

## Icons

`icons` maps an icon id to a stroked path on a 24x24 canvas, using only `M`, `L`, `C`, `Z` commands (absolute, space separated), for example `"home": "M 4 11 L 12 4 L 20 11 L 20 20 L 4 20 Z"`.

Ids: `radio`, `previous`, `play`, `next`, `mute`, `home`, `settings`, `app-grid`, `map-pin`, `youtube`, `pause`, `theme-auto`, `theme-dark`, `360`, `phone`.

## Prompt for a designer or an AI

"Here is the theme file of an Android car launcher (landscape, 1280x720, dark by default, inspired by a car radio UI: gradient background, soft colour blobs, vignette, translucent dark tiles, orange accent). Return a complete JSON in the same format with a new look. Keep every key. Icons must be 24x24 stroked paths using only M, L, C, Z."
