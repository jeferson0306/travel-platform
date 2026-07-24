import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { toast } from 'sonner';
import { PlaneTakeoff, BedDouble, SearchX, CalendarSearch } from 'lucide-react';
import { api, friendlyErrorMessage, type Flight, type Hotel } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { SiteHeader } from '../components/SiteHeader';
import { CheckoutModal, type CheckoutSummary } from '../components/CheckoutModal';
import { AirportAutocomplete } from '../components/AirportAutocomplete';
import { CityAutocomplete } from '../components/CityAutocomplete';

const dateInputClass =
  'mt-1 w-full rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20';

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
  const { token, userId, email } = useAuth();
  const navigate = useNavigate();

  const searchFlights = async (event: FormEvent) => {
    event.preventDefault();
    setSearchingFlights(true);
    try {
      setFlights(await api.searchFlights(origin, destination, departureDate || undefined));
      setFlightsSearched(true);
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
    if (!pending || !token || !userId || !email) return;
    setSubmitting(true);
    try {
      // A short pause makes the "processing" state read as real rather than instant - the
      // booking itself still triggers the real choreography saga once submitted.
      await new Promise((resolve) => setTimeout(resolve, 600));
      const isFlight = pending.kind === 'FLIGHT';
      const { bookingId } = await api.createBooking(
        {
          travelerId: userId,
          travelerEmail: email,
          itemType: pending.kind,
          itemId: pending.item.id,
          quantity: 1,
          amount: isFlight ? pending.item.priceAmount : pending.item.pricePerNightAmount,
          currency: isFlight ? pending.item.priceCurrency : pending.item.pricePerNightCurrency,
        },
        token,
      );
      setPending(null);
      toast.success('Booking confirmed!');
      navigate(`/booking/${bookingId}`);
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
          subtitle: 'Flight',
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
            <label className="text-sm font-medium text-ink-800">
              Departure date
              <input
                type="date"
                value={departureDate}
                onChange={(e) => setDepartureDate(e.target.value)}
                className={`${dateInputClass} w-44`}
              />
            </label>
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
          {!searchingFlights && (
            <ul className="mt-5 flex flex-col gap-2">
              {flights.map((flight, i) => (
                <motion.li
                  key={flight.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className="flex items-center justify-between rounded-xl border border-ink-950/10 bg-white px-4 py-3"
                >
                  <span className="text-sm text-ink-900">
                    {flight.origin} &rarr; {flight.destination} &middot;{' '}
                    {new Date(flight.departureAt).toLocaleDateString()} &middot; {flight.priceAmount}{' '}
                    {flight.priceCurrency} &middot; {flight.availableSeats} seats left
                  </span>
                  <motion.button
                    {...tap}
                    transition={spring}
                    onClick={() => setPending({ kind: 'FLIGHT', item: flight })}
                    className="rounded-lg bg-sunset-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sunset-600"
                  >
                    Book
                  </motion.button>
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
              {hotels.map((hotel, i) => (
                <motion.li
                  key={hotel.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className="flex items-center justify-between rounded-xl border border-ink-950/10 bg-white px-4 py-3"
                >
                  <span className="text-sm text-ink-900">
                    {hotel.name} ({hotel.city}) &middot; {hotel.pricePerNightAmount}{' '}
                    {hotel.pricePerNightCurrency}/night &middot; {hotel.availableRooms} rooms left
                  </span>
                  <motion.button
                    {...tap}
                    transition={spring}
                    onClick={() => setPending({ kind: 'HOTEL', item: hotel })}
                    className="rounded-lg bg-sunset-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-sunset-600"
                  >
                    Book
                  </motion.button>
                </motion.li>
              ))}
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
