/**
 * Purely decorative per-city theming for hotel cards - this demo has no real photo storage
 * (no CDN/upload pipeline, see hotel-service's Hotel aggregate), so instead of faking photo
 * URLs that look server-sourced, each known city gets a gradient + accent color. Mirrors the
 * "Illustrative destinations" framing already used on the landing page. Keep in sync with
 * CityAutocomplete's KNOWN_CITIES and scripts/seed-demo-data.sh.
 */
const CITY_THEMES: Record<string, { gradient: string; accent: string }> = {
  Lisbon: { gradient: 'from-sunset-400 to-pine-500', accent: 'text-sunset-600' },
  Porto: { gradient: 'from-pine-500 to-ink-800', accent: 'text-pine-600' },
  'Sao Paulo': { gradient: 'from-ink-700 to-sunset-500', accent: 'text-ink-800' },
  'New York': { gradient: 'from-ink-900 to-pine-700', accent: 'text-ink-900' },
  Madrid: { gradient: 'from-sunset-500 to-ink-900', accent: 'text-sunset-700' },
};

const DEFAULT_THEME = { gradient: 'from-pine-500 to-ink-800', accent: 'text-pine-600' };

export function destinationTheme(city: string): { gradient: string; accent: string } {
  return CITY_THEMES[city] ?? DEFAULT_THEME;
}
