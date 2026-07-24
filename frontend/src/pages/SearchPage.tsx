import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, ApiError, type Flight, type Hotel } from '../api/client';
import { useAuth } from '../context/AuthContext';

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
  const { token, userId, email, logout } = useAuth();
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
    <div className="search-page">
      <header>
        <h1>Travel Platform</h1>
        <div>
          <span>{email}</span>
          <button onClick={logout}>Log out</button>
        </div>
      </header>

      {error && <p className="error">{error}</p>}

      <section>
        <h2>Search flights</h2>
        <form onSubmit={searchFlights}>
          <input value={origin} onChange={(e) => setOrigin(e.target.value)} placeholder="Origin (e.g. LIS)" required />
          <input
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
            placeholder="Destination (e.g. GRU)"
            required
          />
          <button type="submit">Search</button>
        </form>
        <ul className="results">
          {flights.map((flight) => (
            <li key={flight.id}>
              <span>
                {flight.origin} -&gt; {flight.destination} - {flight.priceAmount} {flight.priceCurrency} (
                {flight.availableSeats} seats left)
              </span>
              <button onClick={() => bookFlight(flight)} disabled={booking === flight.id}>
                {booking === flight.id ? 'Booking...' : 'Book'}
              </button>
            </li>
          ))}
          {flights.length === 0 && !flightsSearched && (
            <li className="empty">Search above to see available flights.</li>
          )}
          {flights.length === 0 && flightsSearched && (
            <li className="empty">No flights found for that route. Try a different origin/destination.</li>
          )}
        </ul>
      </section>

      <section>
        <h2>Search hotels</h2>
        <form onSubmit={searchHotels}>
          <input value={city} onChange={(e) => setCity(e.target.value)} placeholder="City" required />
          <button type="submit">Search</button>
        </form>
        <ul className="results">
          {hotels.map((hotel) => (
            <li key={hotel.id}>
              <span>
                {hotel.name} ({hotel.city}) - {hotel.pricePerNightAmount} {hotel.pricePerNightCurrency}/night (
                {hotel.availableRooms} rooms left)
              </span>
              <button onClick={() => bookHotel(hotel)} disabled={booking === hotel.id}>
                {booking === hotel.id ? 'Booking...' : 'Book'}
              </button>
            </li>
          ))}
          {hotels.length === 0 && !hotelsSearched && (
            <li className="empty">Search above to see available hotels.</li>
          )}
          {hotels.length === 0 && hotelsSearched && (
            <li className="empty">No hotels found in that city. Try a different city.</li>
          )}
        </ul>
      </section>
    </div>
  );
}
