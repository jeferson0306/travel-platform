/**
 * Airport autocomplete backed by airportsapi.com - a free, keyless public API (confirmed CORS-open,
 * no signup required: https://airportsapi.com). Called directly from the browser rather than
 * proxied through our own gateway since it's read-only public reference data, not something this
 * platform owns or needs to protect.
 *
 * Structural limitation of this API (not a bug): its records have no city/country field at all -
 * `filter[name]` only substring-matches the airport's official name, e.g. "Francisco de Sá
 * Carneiro Airport" for Porto, "John F. Kennedy International Airport" for New York - neither
 * contains the city, so searching "Porto", "New York", or any country name finds nothing from the
 * live API alone. KNOWN_AIRPORTS below patches that gap with a curated country/city/code table
 * (matched first, by city or country) covering this demo's own seeded routes (kept in sync with
 * scripts/seed-demo-data.sh) plus a broad set of major world airports; the live API still runs
 * for everything else (IATA codes, less common cities, airport names).
 */

export interface AirportSuggestion {
  iataCode: string;
  name: string;
  type: string;
  city?: string;
  country?: string;
}

/** "Country - City (CODE)" when we know both, else falls back to "name (CODE)". */
export function formatAirportLabel(airport: AirportSuggestion): string {
  if (airport.country && airport.city) {
    return `${airport.country} - ${airport.city} (${airport.iataCode})`;
  }
  return `${airport.name} (${airport.iataCode})`;
}

interface KnownAirport {
  city: string;
  country: string;
  iataCode: string;
  name: string;
}

// prettier-ignore
const KNOWN_AIRPORTS: KnownAirport[] = [
  { city: 'Lisbon', country: 'Portugal', iataCode: 'LIS', name: 'Humberto Delgado Airport' },
  { city: 'Porto', country: 'Portugal', iataCode: 'OPO', name: 'Francisco de Sá Carneiro Airport' },
  { city: 'Sao Paulo', country: 'Brazil', iataCode: 'GRU', name: 'São Paulo/Guarulhos International Airport' },
  { city: 'Rio de Janeiro', country: 'Brazil', iataCode: 'GIG', name: 'Rio de Janeiro/Galeão International Airport' },
  { city: 'Brasilia', country: 'Brazil', iataCode: 'BSB', name: 'Presidente Juscelino Kubitschek International Airport' },
  { city: 'New York', country: 'United States', iataCode: 'JFK', name: 'John F. Kennedy International Airport' },
  { city: 'Los Angeles', country: 'United States', iataCode: 'LAX', name: 'Los Angeles International Airport' },
  { city: 'Chicago', country: 'United States', iataCode: 'ORD', name: "O'Hare International Airport" },
  { city: 'Miami', country: 'United States', iataCode: 'MIA', name: 'Miami International Airport' },
  { city: 'Madrid', country: 'Spain', iataCode: 'MAD', name: 'Adolfo Suárez Madrid–Barajas Airport' },
  { city: 'Barcelona', country: 'Spain', iataCode: 'BCN', name: 'Josep Tarradellas Barcelona-El Prat Airport' },
  { city: 'London', country: 'United Kingdom', iataCode: 'LHR', name: 'Heathrow Airport' },
  { city: 'Paris', country: 'France', iataCode: 'CDG', name: 'Charles de Gaulle Airport' },
  { city: 'Rome', country: 'Italy', iataCode: 'FCO', name: 'Leonardo da Vinci–Fiumicino Airport' },
  { city: 'Berlin', country: 'Germany', iataCode: 'BER', name: 'Berlin Brandenburg Airport' },
  { city: 'Frankfurt', country: 'Germany', iataCode: 'FRA', name: 'Frankfurt Airport' },
  { city: 'Amsterdam', country: 'Netherlands', iataCode: 'AMS', name: 'Amsterdam Airport Schiphol' },
  { city: 'Dublin', country: 'Ireland', iataCode: 'DUB', name: 'Dublin Airport' },
  { city: 'Zurich', country: 'Switzerland', iataCode: 'ZRH', name: 'Zurich Airport' },
  { city: 'Vienna', country: 'Austria', iataCode: 'VIE', name: 'Vienna International Airport' },
  { city: 'Copenhagen', country: 'Denmark', iataCode: 'CPH', name: 'Copenhagen Airport' },
  { city: 'Stockholm', country: 'Sweden', iataCode: 'ARN', name: 'Stockholm Arlanda Airport' },
  { city: 'Oslo', country: 'Norway', iataCode: 'OSL', name: 'Oslo Airport' },
  { city: 'Helsinki', country: 'Finland', iataCode: 'HEL', name: 'Helsinki Airport' },
  { city: 'Warsaw', country: 'Poland', iataCode: 'WAW', name: 'Warsaw Chopin Airport' },
  { city: 'Prague', country: 'Czechia', iataCode: 'PRG', name: 'Václav Havel Airport Prague' },
  { city: 'Athens', country: 'Greece', iataCode: 'ATH', name: 'Athens International Airport' },
  { city: 'Istanbul', country: 'Turkey', iataCode: 'IST', name: 'Istanbul Airport' },
  { city: 'Moscow', country: 'Russia', iataCode: 'SVO', name: 'Sheremetyevo International Airport' },
  { city: 'Dubai', country: 'United Arab Emirates', iataCode: 'DXB', name: 'Dubai International Airport' },
  { city: 'Doha', country: 'Qatar', iataCode: 'DOH', name: 'Hamad International Airport' },
  { city: 'Cairo', country: 'Egypt', iataCode: 'CAI', name: 'Cairo International Airport' },
  { city: 'Johannesburg', country: 'South Africa', iataCode: 'JNB', name: 'O. R. Tambo International Airport' },
  { city: 'Tokyo', country: 'Japan', iataCode: 'HND', name: 'Haneda Airport' },
  { city: 'Beijing', country: 'China', iataCode: 'PEK', name: 'Beijing Capital International Airport' },
  { city: 'Shanghai', country: 'China', iataCode: 'PVG', name: 'Shanghai Pudong International Airport' },
  { city: 'Hong Kong', country: 'Hong Kong', iataCode: 'HKG', name: 'Hong Kong International Airport' },
  { city: 'Seoul', country: 'South Korea', iataCode: 'ICN', name: 'Incheon International Airport' },
  { city: 'Singapore', country: 'Singapore', iataCode: 'SIN', name: 'Singapore Changi Airport' },
  { city: 'Bangkok', country: 'Thailand', iataCode: 'BKK', name: 'Suvarnabhumi Airport' },
  { city: 'Sydney', country: 'Australia', iataCode: 'SYD', name: 'Sydney Kingsford Smith Airport' },
  { city: 'Toronto', country: 'Canada', iataCode: 'YYZ', name: 'Toronto Pearson International Airport' },
  { city: 'Mexico City', country: 'Mexico', iataCode: 'MEX', name: 'Mexico City International Airport' },
  { city: 'Buenos Aires', country: 'Argentina', iataCode: 'EZE', name: 'Ministro Pistarini International Airport' },
  { city: 'Santiago', country: 'Chile', iataCode: 'SCL', name: 'Arturo Merino Benítez International Airport' },
  { city: 'Bogota', country: 'Colombia', iataCode: 'BOG', name: 'El Dorado International Airport' },
  { city: 'Lima', country: 'Peru', iataCode: 'LIM', name: 'Jorge Chávez International Airport' },
];

function matchKnownAirports(query: string): AirportSuggestion[] {
  const q = query.trim().toLowerCase();
  return KNOWN_AIRPORTS.filter(
    (a) => a.city.toLowerCase().includes(q) || a.country.toLowerCase().includes(q) || a.iataCode.toLowerCase() === q,
  ).map((a) => ({ iataCode: a.iataCode, name: a.name, type: 'large_airport', city: a.city, country: a.country }));
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

  const curated = matchKnownAirports(query);
  const live = await searchLiveApi(query.trim());

  const seen = new Set(curated.map((a) => a.iataCode));
  const merged = [...curated, ...live.filter((a) => !seen.has(a.iataCode))];
  return merged.slice(0, 8);
}
