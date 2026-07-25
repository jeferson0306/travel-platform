import { useEffect, useRef, useState } from 'react';
import { searchAirports, type AirportSuggestion } from '../api/airports';

interface AirportAutocompleteProps {
  label: string;
  value: string;
  onChange: (iataCode: string) => void;
  placeholder?: string;
}

export function AirportAutocomplete({ label, value, onChange, placeholder }: AirportAutocompleteProps) {
  const [query, setQuery] = useState(value);
  const [suggestions, setSuggestions] = useState<AirportSuggestion[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [highlighted, setHighlighted] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setQuery(value);
  }, [value]);

  useEffect(() => {
    if (!open) return;
    const handle = setTimeout(() => {
      setLoading(true);
      searchAirports(query)
        .then((results) => {
          setSuggestions(results);
          setHighlighted(0);
        })
        .finally(() => setLoading(false));
    }, 300);
    return () => clearTimeout(handle);
  }, [query, open]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const select = (suggestion: AirportSuggestion) => {
    onChange(suggestion.iataCode);
    setQuery(suggestion.iataCode);
    setOpen(false);
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (!open || suggestions.length === 0) return;
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setHighlighted((i) => Math.min(i + 1, suggestions.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlighted((i) => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      select(suggestions[highlighted]);
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  };

  return (
    <div ref={containerRef} className="relative">
      <label className="block text-sm font-medium text-ink-800">
        {label}
        <input
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          autoComplete="off"
          required
          className="mt-1 w-full rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20"
        />
      </label>
      {open && query.trim().length >= 2 && (
        <ul className="absolute z-20 mt-1 max-h-64 w-72 overflow-auto rounded-lg border border-ink-950/10 bg-white shadow-lg">
          {loading && <li className="px-3 py-2 text-sm text-ink-800/50">Searching...</li>}
          {!loading && suggestions.length === 0 && (
            <li className="px-3 py-2 text-sm text-ink-800/50">
              No airports found for "{query}" - try the IATA code directly (e.g. LIS).
            </li>
          )}
          {!loading &&
            suggestions.map((suggestion, index) => (
              <li key={suggestion.iataCode + suggestion.name}>
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => select(suggestion)}
                  className={`flex w-full items-center justify-between px-3 py-2 text-left text-sm ${
                    index === highlighted ? 'bg-pine-600/10' : 'hover:bg-ink-950/5'
                  }`}
                >
                  <span className="text-ink-900">{suggestion.name}</span>
                  <span className="ml-2 font-mono text-xs text-ink-800/60">{suggestion.iataCode}</span>
                </button>
              </li>
            ))}
        </ul>
      )}
    </div>
  );
}
