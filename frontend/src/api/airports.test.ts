import { afterEach, describe, expect, it, vi } from 'vitest';
import { searchAirports } from './airports';

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

    const result = await searchAirports('lisbon');

    expect(result).toEqual([
      { iataCode: 'BIG', name: 'Big International', type: 'large_airport' },
      { iataCode: 'SML', name: 'Small Regional', type: 'small_airport' },
    ]);
  });

  it('returns an empty list when the API call fails', async () => {
    mockFetchOnce({}, false);

    const result = await searchAirports('lisbon');

    expect(result).toEqual([]);
  });
});
