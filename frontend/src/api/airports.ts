/**
 * Airport autocomplete backed by airportsapi.com - a free, keyless public API (confirmed CORS-open,
 * no signup required: https://airportsapi.com). Called directly from the browser rather than
 * proxied through our own gateway since it's read-only public reference data, not something this
 * platform owns or needs to protect.
 *
 * Structural limitation of this API (not a bug): its records have no city/country field at all -
 * `filter[name]` only substring-matches the airport's official name, e.g. "Francisco de Sá
 * Carneiro Airport" for Porto, "John F. Kennedy International Airport" for New York - neither
 * contains the city, so searching "Porto" or "New York" finds nothing from the live API alone.
 * KNOWN_CITY_AIRPORTS below patches exactly that gap for this demo's own seeded routes (kept in
 * sync with scripts/seed-demo-data.sh) by matching city name first and injecting the correct
 * airport; the live API still runs for everything else (IATA codes, less common cities, airport
 * names).
 */

export interface AirportSuggestion {
  iataCode: string;
  name: string;
  type: string;
}

const KNOWN_CITY_AIRPORTS: Array<{ city: string; airport: AirportSuggestion }> = [
  { city: 'Lisbon', airport: { iataCode: 'LIS', name: 'Lisbon Humberto Delgado Airport', type: 'large_airport' } },
  { city: 'Porto', airport: { iataCode: 'OPO', name: 'Francisco de Sá Carneiro Airport (Porto)', type: 'large_airport' } },
  {
    city: 'Sao Paulo',
    airport: { iataCode: 'GRU', name: 'São Paulo/Guarulhos International Airport', type: 'large_airport' },
  },
  { city: 'New York', airport: { iataCode: 'JFK', name: 'John F. Kennedy International Airport (New York)', type: 'large_airport' } },
  { city: 'Madrid', airport: { iataCode: 'MAD', name: 'Adolfo Suárez Madrid–Barajas Airport', type: 'large_airport' } },
];

function matchKnownCityAirports(query: string): AirportSuggestion[] {
  const q = query.trim().toLowerCase();
  return KNOWN_CITY_AIRPORTS.filter(({ city }) => city.toLowerCase().includes(q)).map(({ airport }) => airport);
}

interface AirportsApiResponse {
  data: Array<{
    attributes: {
      name: string;
      type: string;
      iata_code: string | null;
    };
  }>;
}

const TYPE_RANK: Record<string, number> = { large_airport: 0, medium_airport: 1, small_airport: 2 };

async function searchLiveApi(query: string): Promise<AirportSuggestion[]> {
  const url = new URL('https://airportsapi.com/api/airports');
  url.searchParams.set('filter[name]', query);

  let response: Response;
  try {
    response = await fetch(url.toString());
  } catch {
    return [];
  }
  if (!response.ok) return [];

  const body = (await response.json()) as AirportsApiResponse;
  return body.data
    .map((entry) => entry.attributes)
    .filter((attrs): attrs is typeof attrs & { iata_code: string } => Boolean(attrs.iata_code))
    .sort((a, b) => (TYPE_RANK[a.type] ?? 9) - (TYPE_RANK[b.type] ?? 9))
    .map((attrs) => ({ iataCode: attrs.iata_code, name: attrs.name, type: attrs.type }));
}

export async function searchAirports(query: string): Promise<AirportSuggestion[]> {
  if (query.trim().length < 2) return [];

  const curated = matchKnownCityAirports(query);
  const live = await searchLiveApi(query.trim());

  const seen = new Set(curated.map((a) => a.iataCode));
  const merged = [...curated, ...live.filter((a) => !seen.has(a.iataCode))];
  return merged.slice(0, 8);
}
