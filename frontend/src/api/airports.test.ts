import { afterEach, describe, expect, it, vi } from 'vitest';
import { searchAirports, formatAirportLabel } from './airports';

function mockFetchOnce(body: unknown, ok = true) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok,
      json: () => Promise.resolve(body),
    }),
  );
}

describe('searchAirports', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns an empty list without calling the API for a too-short query', async () => {
    const fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);

    const result = await searchAirports('L');

    expect(result).toEqual([]);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('filters out entries with no IATA code and sorts large airports first', async () => {
    mockFetchOnce({
      data: [
        { attributes: { name: 'Some Small Field', type: 'small_airport', iata_code: null } },
        { attributes: { name: 'Small Regional', type: 'small_airport', iata_code: 'SML' } },
        { attributes: { name: 'Big International', type: 'large_airport', iata_code: 'BIG' } },
      ],
    });

    const result = await searchAirports('international');

    expect(result).toEqual([
      { iataCode: 'BIG', name: 'Big International', type: 'large_airport' },
      { iataCode: 'SML', name: 'Small Regional', type: 'small_airport' },
    ]);
  });

  it('returns an empty list when the API call fails', async () => {
    mockFetchOnce({}, false);

    const result = await searchAirports('international');

    expect(result).toEqual([]);
  });

  it('matches known cities whose official airport name does not contain the city (e.g. Porto)', async () => {
    // airportsapi.com has no city field, only "name" - Porto's real airport is "Francisco de
    // Sá Carneiro Airport", which the live API can never match on a "Porto" query. The curated
    // KNOWN_AIRPORTS list patches exactly this gap.
    mockFetchOnce({ data: [] });

    const result = await searchAirports('Porto');

    expect(result).toEqual([
      { iataCode: 'OPO', name: 'Francisco de Sá Carneiro Airport', type: 'large_airport', city: 'Porto', country: 'Portugal' },
    ]);
  });

  it('matches a curated airport by country name, not just city', async () => {
    mockFetchOnce({ data: [] });

    const result = await searchAirports('Brazil');

    expect(result.map((r) => r.iataCode)).toEqual(expect.arrayContaining(['GRU', 'GIG', 'BSB']));
  });

  it('does not duplicate an airport already suggested by the curated city list', async () => {
    mockFetchOnce({
      data: [{ attributes: { name: 'Lisbon Humberto Delgado Airport', type: 'large_airport', iata_code: 'LIS' } }],
    });

    const result = await searchAirports('Lisbon');

    expect(result.filter((r) => r.iataCode === 'LIS')).toHaveLength(1);
  });
});

describe('formatAirportLabel', () => {
  it('formats a curated airport as "Country - City (CODE)"', () => {
    expect(
      formatAirportLabel({ iataCode: 'OPO', name: 'Francisco de Sá Carneiro Airport', type: 'large_airport', city: 'Porto', country: 'Portugal' }),
    ).toBe('Portugal - Porto (OPO)');
  });

  it('falls back to "name (CODE)" when city/country are unknown (live API result)', () => {
    expect(formatAirportLabel({ iataCode: 'BIG', name: 'Big International', type: 'large_airport' })).toBe(
      'Big International (BIG)',
    );
  });
});
