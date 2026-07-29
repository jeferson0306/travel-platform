import { useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { toast } from 'sonner';
import {
  PlaneTakeoff,
  BedDouble,
  SearchX,
  CalendarSearch,
  ArrowUpDown,
  SlidersHorizontal,
  Star,
  MapPin,
} from 'lucide-react';
import { api, friendlyErrorMessage, type Flight, type Hotel } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { SiteHeader } from '../components/SiteHeader';
import { CheckoutModal, type CheckoutSummary } from '../components/CheckoutModal';
import { AirportAutocomplete } from '../components/AirportAutocomplete';
import { CityAutocomplete } from '../components/CityAutocomplete';
import { DatePicker } from '../components/DatePicker';
import { formatMoney, formatDate, formatTime, formatDuration } from '../lib/format';
import { destinationTheme } from '../lib/destinationTheme';

type FlightSort = 'price' | 'duration';

function flightDurationMinutes(flight: Flight): number {
  return (new Date(flight.arrivalAt).getTime() - new Date(flight.departureAt).getTime()) / 60000;
}

/** What the frontend sends as CreateBookingRequest.itemSummary - it already has the full
 * Flight/Hotel object from the search step, so it builds a short human-readable description
 * here rather than booking-service looking it up (no synchronous cross-service calls, ADR 0004). */
function flightSummary(flight: Flight): string {
  return `${flight.origin} → ${flight.destination} · ${flight.flightNumber} · ${flight.airline}`;
}

function hotelSummary(hotel: Hotel): string {
  return `${hotel.name}, ${hotel.city}`;
}

function stopsLabel(stops: number): string {
  if (stops === 0) return 'Nonstop';
  if (stops === 1) return '1 stop';
  return `${stops} stops`;
}

const tap = { whileHover: { scale: 1.03, y: -1 }, whileTap: { scale: 0.96 } };
const spring = { type: 'spring' as const, stiffness: 400, damping: 17 };

type PendingBooking =
  | { kind: 'FLIGHT'; item: Flight }
  | { kind: 'HOTEL'; item: Hotel };

function ResultsSkeleton() {
  return (
    <div className="mt-5 flex flex-col gap-2">
      {[0, 1, 2].map((i) => (
        <div key={i} className="h-14 animate-pulse rounded-xl bg-ink-950/[0.05]" />
      ))}
    </div>
  );
}

function EmptyState({ icon: Icon, text }: { icon: typeof SearchX; text: string }) {
  return (
    <li className="flex flex-col items-center gap-2 rounded-xl bg-ink-950/[0.03] px-4 py-8 text-center text-sm text-ink-800/60">
      <Icon size={22} className="text-ink-950/25" />
      {text}
    </li>
  );
}

export function SearchPage() {
  const [origin, setOrigin] = useState('LIS');
  const [destination, setDestination] = useState('GRU');
  const [departureDate, setDepartureDate] = useState('');
  const [city, setCity] = useState('Lisbon');
  const [flights, setFlights] = useState<Flight[]>([]);
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [flightsSearched, setFlightsSearched] = useState(false);
  const [hotelsSearched, setHotelsSearched] = useState(false);
  const [searchingFlights, setSearchingFlights] = useState(false);
  const [searchingHotels, setSearchingHotels] = useState(false);
  const [pending, setPending] = useState<PendingBooking | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [flightSort, setFlightSort] = useState<FlightSort>('price');
  const [nonstopOnly, setNonstopOnly] = useState(false);
  const [airlineFilter, setAirlineFilter] = useState('ALL');
  const { token, email } = useAuth();
  const navigate = useNavigate();

  const airlineOptions = useMemo(
    () => Array.from(new Set(flights.map((f) => f.airline))).sort(),
    [flights],
  );

  const visibleFlights = useMemo(() => {
    return flights
      .filter((f) => !nonstopOnly || f.stops === 0)
      .filter((f) => airlineFilter === 'ALL' || f.airline === airlineFilter)
      .sort((a, b) =>
        flightSort === 'price' ? a.priceAmount - b.priceAmount : flightDurationMinutes(a) - flightDurationMinutes(b),
      );
  }, [flights, nonstopOnly, airlineFilter, flightSort]);

  const searchFlights = async (event: FormEvent) => {
    event.preventDefault();
    setSearchingFlights(true);
    try {
      setFlights(await api.searchFlights(origin, destination, departureDate || undefined));
      setFlightsSearched(true);
      setNonstopOnly(false);
      setAirlineFilter('ALL');
    } catch (err) {
      toast.error(friendlyErrorMessage(err));
    } finally {
      setSearchingFlights(false);
    }
  };

  const searchHotels = async (event: FormEvent) => {
    event.preventDefault();
    setSearchingHotels(true);
    try {
      setHotels(await api.searchHotels(city));
      setHotelsSearched(true);
    } catch (err) {
      toast.error(friendlyErrorMessage(err));
    } finally {
      setSearchingHotels(false);
    }
  };

  const confirmBooking = async () => {
    if (!pending || !token || !email) return;
    setSubmitting(true);
    try {
      // A short pause makes the "processing" state read as real rather than instant - the
      // booking itself still triggers the real choreography saga once submitted.
      await new Promise((resolve) => setTimeout(resolve, 600));
      const isFlight = pending.kind === 'FLIGHT';
      const itemSummary = isFlight ? flightSummary(pending.item) : hotelSummary(pending.item);
      const { bookingId } = await api.createBooking(
        {
          travelerEmail: email,
          itemType: pending.kind,
          itemId: pending.item.id,
          quantity: 1,
          amount: isFlight ? pending.item.priceAmount : pending.item.pricePerNightAmount,
          currency: isFlight ? pending.item.priceCurrency : pending.item.pricePerNightCurrency,
          itemSummary,
        },
        token,
      );
      setPending(null);
      toast.success('Booking confirmed!');
      navigate(`/booking/${bookingId}`, { state: { itemSummary } });
    } catch (err) {
      toast.error(friendlyErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const checkoutSummary: CheckoutSummary | null = pending
    ? pending.kind === 'FLIGHT'
      ? {
          title: `${pending.item.origin} → ${pending.item.destination}`,
          subtitle: `${pending.item.airline} ${pending.item.flightNumber} · ${stopsLabel(pending.item.stops)}`,
          amount: pending.item.priceAmount,
          currency: pending.item.priceCurrency,
        }
      : {
          title: pending.item.name,
          subtitle: `${pending.item.city} · 1 night`,
          amount: pending.item.pricePerNightAmount,
          currency: pending.item.pricePerNightCurrency,
        }
    : null;

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <div className="mx-auto max-w-4xl px-6 py-12">
        <h1 className="text-3xl font-medium text-ink-950">Find your next trip</h1>

        <section className="shadow-elevated mt-10 rounded-2xl border border-ink-950/10 bg-white/70 p-6">
          <h2 className="flex items-center gap-2 text-xl font-medium text-ink-950">
            <PlaneTakeoff size={19} className="text-sunset-600" />
            Flights
          </h2>
          <form onSubmit={searchFlights} className="mt-4 flex flex-wrap items-end gap-3">
            <AirportAutocomplete label="Origin" value={origin} onChange={setOrigin} placeholder="City or airport" />
            <AirportAutocomplete
              label="Destination"
              value={destination}
              onChange={setDestination}
              placeholder="City or airport"
            />
            <DatePicker label="Departure date" value={departureDate} onChange={setDepartureDate} className="w-44" />
            <motion.button
              {...tap}
              transition={spring}
              type="submit"
              disabled={searchingFlights}
              className="rounded-lg bg-ink-950 px-5 py-2 text-sm font-medium text-white hover:bg-ink-800 disabled:opacity-60"
            >
              {searchingFlights ? 'Searching...' : 'Search'}
            </motion.button>
          </form>
          <p className="mt-2 text-xs text-ink-800/50">
            Origin/destination suggestions come from a public airports directory - if your city
            doesn't show up, try the 3-letter airport code directly.
          </p>

          {searchingFlights && <ResultsSkeleton />}
          {!searchingFlights && flights.length > 0 && (
            <div className="mt-5 flex flex-wrap items-center gap-3 text-xs text-ink-800/70">
              <span className="flex items-center gap-1.5">
                <ArrowUpDown size={13} className="text-ink-800/40" />
                Sort
                <select
                  value={flightSort}
                  onChange={(e) => setFlightSort(e.target.value as FlightSort)}
                  className="rounded-md border border-ink-950/15 bg-white px-2 py-1 text-ink-900 outline-none focus:border-pine-500"
                >
                  <option value="price">Price</option>
                  <option value="duration">Duration</option>
                </select>
              </span>
              <span className="flex items-center gap-1.5">
                <SlidersHorizontal size={13} className="text-ink-800/40" />
                <label className="flex items-center gap-1.5">
                  <input
                    type="checkbox"
                    checked={nonstopOnly}
                    onChange={(e) => setNonstopOnly(e.target.checked)}
                    className="accent-pine-600"
                  />
                  Nonstop only
                </label>
              </span>
              {airlineOptions.length > 1 && (
                <select
                  value={airlineFilter}
                  onChange={(e) => setAirlineFilter(e.target.value)}
                  className="rounded-md border border-ink-950/15 bg-white px-2 py-1 text-ink-900 outline-none focus:border-pine-500"
                >
                  <option value="ALL">All airlines</option>
                  {airlineOptions.map((a) => (
                    <option key={a} value={a}>
                      {a}
                    </option>
                  ))}
                </select>
              )}
              <span className="text-ink-800/40">
                {visibleFlights.length} of {flights.length} flights
              </span>
            </div>
          )}
          {!searchingFlights && (
            <ul className="mt-3 flex flex-col gap-2.5">
              {visibleFlights.map((flight, i) => (
                <motion.li
                  key={flight.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  whileHover={{ y: -2 }}
                  transition={{ delay: i * 0.05 }}
                  className="shadow-elevated flex flex-col gap-3 rounded-xl border border-ink-950/10 bg-white px-4 py-3 transition-shadow hover:border-ink-950/20 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="flex items-start gap-3">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-ink-950 text-[11px] font-semibold text-white">
                      {flight.airlineCode}
                    </span>
                    <div>
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-ink-800/60">
                        <span>{flight.airline}</span>
                        <span>&middot;</span>
                        <span>{flight.flightNumber}</span>
                        {flight.cabinClass && (
                          <>
                            <span>&middot;</span>
                            <span>{flight.cabinClass}</span>
                          </>
                        )}
                      </div>
                      <p className="mt-1 flex items-center gap-2 text-sm font-medium text-ink-950">
                        <span>{formatTime(flight.departureAt)}</span>
                        <span className="text-ink-800/40">{flight.origin}</span>
                        <span className="text-ink-800/40">
                          &mdash; {formatDuration(flight.departureAt, flight.arrivalAt)} &mdash;
                        </span>
                        <span className="text-ink-800/40">{flight.destination}</span>
                        <span>{formatTime(flight.arrivalAt)}</span>
                      </p>
                      <p className="mt-1 text-xs text-ink-800/60">
                        {formatDate(flight.departureAt)} &middot; {stopsLabel(flight.stops)} &middot;{' '}
                        {flight.availableSeats} seats left
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 self-end sm:self-auto">
                    <span className="font-display text-lg text-ink-950">
                      {formatMoney(flight.priceAmount, flight.priceCurrency)}
                    </span>
                    <motion.button
                      {...tap}
                      transition={spring}
                      onClick={() => setPending({ kind: 'FLIGHT', item: flight })}
                      className="rounded-lg bg-sunset-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sunset-600"
                    >
                      Book
                    </motion.button>
                  </div>
                </motion.li>
              ))}
              {flights.length === 0 && !flightsSearched && (
                <EmptyState icon={PlaneTakeoff} text="Search above to see available flights." />
              )}
              {flights.length === 0 && flightsSearched && (
                <EmptyState
                  icon={SearchX}
                  text={`No flights found for that route${departureDate ? ' on that date' : ''}. Try a different ${departureDate ? 'date, ' : ''}origin, or destination.`}
                />
              )}
              {flights.length > 0 && visibleFlights.length === 0 && (
                <EmptyState icon={SearchX} text="No flights match these filters. Try widening your search." />
              )}
            </ul>
          )}
        </section>

        <section className="shadow-elevated mt-6 rounded-2xl border border-ink-950/10 bg-white/70 p-6">
          <h2 className="flex items-center gap-2 text-xl font-medium text-ink-950">
            <BedDouble size={19} className="text-pine-600" />
            Hotels
          </h2>
          <form onSubmit={searchHotels} className="mt-4 flex flex-wrap items-end gap-3">
            <CityAutocomplete label="City" value={city} onChange={setCity} />
            <motion.button
              {...tap}
              transition={spring}
              type="submit"
              disabled={searchingHotels}
              className="rounded-lg bg-ink-950 px-5 py-2 text-sm font-medium text-white hover:bg-ink-800 disabled:opacity-60"
            >
              {searchingHotels ? 'Searching...' : 'Search'}
            </motion.button>
          </form>
          <p className="mt-2 flex items-center gap-1.5 text-xs text-ink-800/50">
            <CalendarSearch size={13} />
            Date-based availability isn't modeled yet for hotels (no per-night inventory in this
            demo) - results show current room counts only.
          </p>

          {searchingHotels && <ResultsSkeleton />}
          {!searchingHotels && (
            <ul className="mt-5 flex flex-col gap-2">
              {hotels.map((hotel, i) => {
                const theme = destinationTheme(hotel.city);
                return (
                  <motion.li
                    key={hotel.id}
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    whileHover={{ y: -2 }}
                    transition={{ delay: i * 0.05 }}
                    className="shadow-elevated flex flex-col gap-3 rounded-xl border border-ink-950/10 bg-white px-4 py-3 transition-shadow hover:border-ink-950/20 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div className="flex items-start gap-3">
                      <span
                        className={`hidden h-14 w-14 shrink-0 rounded-lg bg-gradient-to-br sm:block ${theme.gradient}`}
                        aria-hidden
                      />
                      <div>
                        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                          <span className="text-sm font-medium text-ink-950">{hotel.name}</span>
                          <span className="flex items-center gap-0.5 text-xs text-amber-500">
                            {Array.from({ length: hotel.starRating }).map((_, star) => (
                              <Star key={star} size={11} fill="currentColor" strokeWidth={0} />
                            ))}
                          </span>
                          {hotel.reviewScore != null && (
                            <span className="text-xs text-ink-800/60">
                              {hotel.reviewScore.toFixed(1)}
                              {hotel.reviewCount > 0 && ` (${hotel.reviewCount})`}
                            </span>
                          )}
                        </div>
                        <p className="mt-0.5 flex items-center gap-1 text-xs text-ink-800/60">
                          <MapPin size={11} />
                          {hotel.address ? `${hotel.address}, ${hotel.city}` : hotel.city}
                        </p>
                        {hotel.amenities.length > 0 && (
                          <p className="mt-1.5 flex flex-wrap gap-1">
                            {hotel.amenities.slice(0, 4).map((amenity) => (
                              <span
                                key={amenity}
                                className={`rounded-full bg-ink-950/[0.05] px-2 py-0.5 text-[11px] ${theme.accent}`}
                              >
                                {amenity}
                              </span>
                            ))}
                          </p>
                        )}
                        <p className="mt-1 text-xs text-ink-800/60">{hotel.availableRooms} rooms left</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3 self-end sm:self-auto">
                      <div className="text-right">
                        <span className="font-display block text-lg text-ink-950">
                          {formatMoney(hotel.pricePerNightAmount, hotel.pricePerNightCurrency)}
                        </span>
                        <span className="text-[11px] text-ink-800/50">/night</span>
                      </div>
                      <motion.button
                        {...tap}
                        transition={spring}
                        onClick={() => setPending({ kind: 'HOTEL', item: hotel })}
                        className="rounded-lg bg-sunset-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sunset-600"
                      >
                        Book
                      </motion.button>
                    </div>
                  </motion.li>
                );
              })}
              {hotels.length === 0 && !hotelsSearched && (
                <EmptyState icon={BedDouble} text="Search above to see available hotels." />
              )}
              {hotels.length === 0 && hotelsSearched && (
                <EmptyState icon={SearchX} text="No hotels found in that city. Try a different city." />
              )}
            </ul>
          )}
        </section>
      </div>

      <CheckoutModal
        summary={checkoutSummary}
        submitting={submitting}
        onCancel={() => setPending(null)}
        onConfirm={confirmBooking}
      />
    </div>
  );
}
