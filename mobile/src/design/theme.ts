// Classical design system — tokens ported from the design's styles.css.
// Editorial, book-like: soft near-white ground, brass-gold accent applied as
// stroke (borders/underlines), hairline dividers, Cormorant Garamond over Lora.
// The mobile app uses the "soft" variant (rounded cards, pill buttons).

export const colors = {
  bg: '#f3f2f2', // --color-bg (the screen ground)
  surface: '#eae9e9', // --color-surface (dialogs, mats)
  appBg: '#e6e4e0', // ground behind the device in the prototype
  text: '#201f1d', // --color-text
  accent: '#b68235', // --color-accent (brass gold)
  transition: '#D97757', // the bubble transition's fill
  divider: 'rgba(32,31,29,0.16)', // --color-divider (ink 16%)

  // Neutral tonal ramp (100 light → 900 dark)
  neutral: {
    100: '#f8f4f4', 200: '#eae7e7', 300: '#d7d3d3', 400: '#bab6b6',
    500: '#9b9797', 600: '#7d7979', 700: '#605d5d', 800: '#444141', 900: '#2d2b2b',
  },
  // Accent tonal ramp
  accentRamp: {
    100: '#fff3e4', 200: '#ffe3bf', 300: '#facb8d', 400: '#e1ad66',
    500: '#c28d41', 600: '#a06f24', 700: '#7d5411', 800: '#5a3b0a', 900: '#3a270d',
  },
} as const;

// Muted ink — RN has no color-mix(), so this returns an rgba of the ink color.
// ink(0.55) === "text at 55%", matching the design's color-mix(ink X%, transparent).
export const ink = (alpha: number) => `rgba(32,31,29,${alpha})`;

// Font family names as registered by @expo-google-fonts once loaded.
export const fonts = {
  displayLight: 'CormorantGaramond_300Light', // big display numerals/headings
  displayRegular: 'CormorantGaramond_400Regular',
  displayMedium: 'CormorantGaramond_500Medium',
  heading: 'CormorantGaramond_600SemiBold', // interface headings' semibold ceiling
  body: 'Lora_400Regular',
  bodyMedium: 'Lora_500Medium',
  bodySemi: 'Lora_600SemiBold',
} as const;

// Radii — the "soft" app variant (dk-soft in the prototype).
export const radius = { sm: 12, md: 22, lg: 30, pill: 999 } as const;

// Spacing scale (density 1.15×) from --space-*.
export const space = { xs: 5, sm: 9, md: 14, lg: 18, xl: 28, xxl: 37 } as const;
