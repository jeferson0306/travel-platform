import { useEffect, useRef, useState } from 'react';

/**
 * Hotel city search requires an exact match against what's stored in our own inventory (see
 * hotel-service's SearchHotelsQuery), so suggestions have to come from data we control - unlike
 * flight airports, a public API's spelling of a city name can't be guaranteed to match ours. This
 * list mirrors scripts/seed-demo-data.sh; update both together.
 */
const KNOWN_CITIES = ['Lisbon', 'Porto', 'Sao Paulo', 'New York', 'Madrid'];

interface CityAutocompleteProps {
  label: string;
  value: string;
  onChange: (city: string) => void;
}

export function CityAutocomplete({ label, value, onChange }: CityAutocompleteProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const matches = KNOWN_CITIES.filter((city) => city.toLowerCase().includes(value.toLowerCase()));

  return (
    <div ref={containerRef} className="relative">
      <label className="block text-sm font-medium text-ink-800">
        {label}
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setOpen(true)}
          placeholder="City"
          autoComplete="off"
          required
          className="mt-1 w-full rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20"
        />
      </label>
      {open && matches.length > 0 && (
        <ul className="absolute z-20 mt-1 w-48 overflow-hidden rounded-lg border border-ink-950/10 bg-white shadow-lg">
          {matches.map((city) => (
            <li key={city}>
              <button
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  onChange(city);
                  setOpen(false);
                }}
                className="w-full px-3 py-2 text-left text-sm text-ink-900 hover:bg-ink-950/5"
              >
                {city}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
