/**
 * Airport autocomplete backed by airportsapi.com - a free, keyless public API (confirmed CORS-open,
 * no signup required: https://airportsapi.com). Called directly from the browser rather than
 * proxied through our own gateway since it's read-only public reference data, not something this
 * platform owns or needs to protect.
 *
 * Known limitation: matching is a substring search on the airport's official name, which doesn't
 * always contain its city (e.g. Porto's "Francisco Sá Carneiro Airport" won't match "Porto") - a
 * real trade-off of using a live public dataset instead of a hand-curated one, not hidden here.
 */

export interface AirportSuggestion {
  iataCode: string;
  name: string;
  type: string;
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

export async function searchAirports(query: string): Promise<AirportSuggestion[]> {
  if (query.trim().length < 2) return [];

  const url = new URL('https://airportsapi.com/api/airports');
  url.searchParams.set('filter[name]', query.trim());

  const response = await fetch(url.toString());
  if (!response.ok) return [];

  const body = (await response.json()) as AirportsApiResponse;
  return body.data
    .map((entry) => entry.attributes)
    .filter((attrs): attrs is typeof attrs & { iata_code: string } => Boolean(attrs.iata_code))
    .sort((a, b) => (TYPE_RANK[a.type] ?? 9) - (TYPE_RANK[b.type] ?? 9))
    .slice(0, 8)
    .map((attrs) => ({ iataCode: attrs.iata_code, name: attrs.name, type: attrs.type }));
}
