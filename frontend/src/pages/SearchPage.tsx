import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { api, ApiError, type Flight, type Hotel } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { SiteHeader } from '../components/SiteHeader';

const inputClass =
  'w-full rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20';

export function SearchPage() {
  const [origin, setOrigin] = useState('LIS');
  const [destination, setDestination] = useState('GRU');
  const [city, setCity] = useState('Lisbon');
  const [flights, setFlights] = useState<Flight[]>([]);
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [flightsSearched, setFlightsSearched] = useState(false);
  const [hotelsSearched, setHotelsSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [booking, setBooking] = useState<string | null>(null);
  const { token, userId, email } = useAuth();
  const navigate = useNavigate();

  const searchFlights = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      setFlights(await api.searchFlights(origin, destination));
      setFlightsSearched(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Search failed');
    }
  };

  const searchHotels = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      setHotels(await api.searchHotels(city));
      setHotelsSearched(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Search failed');
    }
  };

  const bookFlight = async (flight: Flight) => {
    if (!token || !userId || !email) return;
    setError(null);
    setBooking(flight.id);
    try {
      const { bookingId } = await api.createBooking(
        {
          travelerId: userId,
          travelerEmail: email,
          itemType: 'FLIGHT',
          itemId: flight.id,
          quantity: 1,
          amount: flight.priceAmount,
          currency: flight.priceCurrency,
        },
        token,
      );
      navigate(`/booking/${bookingId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Booking failed');
    } finally {
      setBooking(null);
    }
  };

  const bookHotel = async (hotel: Hotel) => {
    if (!token || !userId || !email) return;
    setError(null);
    setBooking(hotel.id);
    try {
      const { bookingId } = await api.createBooking(
        {
          travelerId: userId,
          travelerEmail: email,
          itemType: 'HOTEL',
          itemId: hotel.id,
          quantity: 1,
          amount: hotel.pricePerNightAmount,
          currency: hotel.pricePerNightCurrency,
        },
        token,
      );
      navigate(`/booking/${bookingId}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Booking failed');
    } finally {
      setBooking(null);
    }
  };

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <div className="mx-auto max-w-4xl px-6 py-12">
        <h1 className="text-3xl font-medium text-ink-950">Find your next trip</h1>

        <AnimatePresence>
          {error && (
            <motion.p
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="mt-4 rounded-lg bg-red-50 px-4 py-2 text-sm text-red-600"
            >
              {error}
            </motion.p>
          )}
        </AnimatePresence>

        <section className="mt-10 rounded-2xl border border-ink-950/10 bg-white/60 p-6">
          <h2 className="text-xl font-medium text-ink-950">Flights</h2>
          <form onSubmit={searchFlights} className="mt-4 flex flex-wrap items-end gap-3">
            <label className="text-sm font-medium text-ink-800">
              Origin
              <input
                value={origin}
                onChange={(e) => setOrigin(e.target.value.toUpperCase())}
                placeholder="e.g. LIS"
                required
                className={`${inputClass} mt-1 w-32`}
              />
            </label>
            <label className="text-sm font-medium text-ink-800">
              Destination
              <input
                value={destination}
                onChange={(e) => setDestination(e.target.value.toUpperCase())}
                placeholder="e.g. GRU"
                required
                className={`${inputClass} mt-1 w-32`}
              />
            </label>
            <button
              type="submit"
              className="rounded-lg bg-ink-950 px-5 py-2 text-sm font-medium text-white transition hover:bg-ink-800"
            >
              Search
            </button>
          </form>
          <ul className="mt-5 flex flex-col gap-2">
            {flights.map((flight) => (
              <motion.li
                key={flight.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center justify-between rounded-xl border border-ink-950/10 bg-white px-4 py-3"
              >
                <span className="text-sm text-ink-900">
                  {flight.origin} &rarr; {flight.destination} &middot; {flight.priceAmount}{' '}
                  {flight.priceCurrency} &middot; {flight.availableSeats} seats left
                </span>
                <button
                  onClick={() => bookFlight(flight)}
                  disabled={booking === flight.id}
                  className="rounded-lg bg-sunset-500 px-4 py-1.5 text-sm font-medium text-white transition hover:bg-sunset-600 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {booking === flight.id ? 'Booking...' : 'Book'}
                </button>
              </motion.li>
            ))}
            {flights.length === 0 && !flightsSearched && (
              <li className="rounded-xl bg-ink-950/[0.03] px-4 py-3 text-center text-sm text-ink-800/60">
                Search above to see available flights.
              </li>
            )}
            {flights.length === 0 && flightsSearched && (
              <li className="rounded-xl bg-ink-950/[0.03] px-4 py-3 text-center text-sm text-ink-800/60">
                No flights found for that route. Try a different origin/destination.
              </li>
            )}
          </ul>
        </section>

        <section className="mt-6 rounded-2xl border border-ink-950/10 bg-white/60 p-6">
          <h2 className="text-xl font-medium text-ink-950">Hotels</h2>
          <form onSubmit={searchHotels} className="mt-4 flex flex-wrap items-end gap-3">
            <label className="text-sm font-medium text-ink-800">
              City
              <input
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="City"
                required
                className={`${inputClass} mt-1 w-48`}
              />
            </label>
            <button
              type="submit"
              className="rounded-lg bg-ink-950 px-5 py-2 text-sm font-medium text-white transition hover:bg-ink-800"
            >
              Search
            </button>
          </form>
          <ul className="mt-5 flex flex-col gap-2">
            {hotels.map((hotel) => (
              <motion.li
                key={hotel.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex items-center justify-between rounded-xl border border-ink-950/10 bg-white px-4 py-3"
              >
                <span className="text-sm text-ink-900">
                  {hotel.name} ({hotel.city}) &middot; {hotel.pricePerNightAmount}{' '}
                  {hotel.pricePerNightCurrency}/night &middot; {hotel.availableRooms} rooms left
                </span>
                <button
                  onClick={() => bookHotel(hotel)}
                  disabled={booking === hotel.id}
                  className="rounded-lg bg-sunset-500 px-4 py-1.5 text-sm font-medium text-white transition hover:bg-sunset-600 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {booking === hotel.id ? 'Booking...' : 'Book'}
                </button>
              </motion.li>
            ))}
            {hotels.length === 0 && !hotelsSearched && (
              <li className="rounded-xl bg-ink-950/[0.03] px-4 py-3 text-center text-sm text-ink-800/60">
                Search above to see available hotels.
              </li>
            )}
            {hotels.length === 0 && hotelsSearched && (
              <li className="rounded-xl bg-ink-950/[0.03] px-4 py-3 text-center text-sm text-ink-800/60">
                No hotels found in that city. Try a different city.
              </li>
            )}
          </ul>
        </section>
      </div>
    </div>
  );
}
