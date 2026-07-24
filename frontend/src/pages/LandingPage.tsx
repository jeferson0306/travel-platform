import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { SiteHeader } from '../components/SiteHeader';
import { api, ApiError, type Flight } from '../api/client';

const FEATURED_DESTINATIONS = [
  { city: 'Lisbon', country: 'Portugal', blurb: 'Pastel facades, river light, tram bells.' },
  { city: 'Porto', country: 'Portugal', blurb: 'Port wine cellars and a bridge by Eiffel.' },
  { city: 'Sao Paulo', country: 'Brazil', blurb: 'Skyline, food scene, non-stop energy.' },
  { city: 'New York', country: 'USA', blurb: 'The city that never sits down.' },
  { city: 'Madrid', country: 'Spain', blurb: 'Late dinners, wide boulevards, Prado art.' },
];

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0 },
};

export function LandingPage() {
  const [origin, setOrigin] = useState('LIS');
  const [destination, setDestination] = useState('GRU');
  const [flights, setFlights] = useState<Flight[]>([]);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      setFlights(await api.searchFlights(origin, destination));
      setSearched(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Search failed');
    }
  };

  return (
    <div className="min-h-screen">
      <SiteHeader />

      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full bg-sunset-400/20 blur-3xl" />
        <div className="pointer-events-none absolute -left-24 top-40 h-72 w-72 rounded-full bg-pine-400/20 blur-3xl" />
        <div className="relative mx-auto max-w-6xl px-6 py-20 sm:py-28">
          <motion.p
            initial="hidden"
            animate="visible"
            variants={fadeUp}
            transition={{ duration: 0.5 }}
            className="mb-4 inline-block rounded-full bg-pine-600/10 px-4 py-1.5 text-sm font-medium text-pine-600"
          >
            One route, right now: Lisbon to Sao Paulo from 589 EUR
          </motion.p>
          <motion.h1
            initial="hidden"
            animate="visible"
            variants={fadeUp}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="max-w-3xl text-5xl font-medium leading-[1.05] text-ink-950 sm:text-6xl"
          >
            Book flights and hotels without the runaround.
          </motion.h1>
          <motion.p
            initial="hidden"
            animate="visible"
            variants={fadeUp}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="mt-6 max-w-xl text-lg text-ink-800"
          >
            Search real inventory, book in a couple of clicks, get a confirmation the moment
            payment clears. No hidden steps, no dark patterns.
          </motion.p>

          {/* Mini search widget - hits the real, public search API */}
          <motion.form
            initial="hidden"
            animate="visible"
            variants={fadeUp}
            transition={{ duration: 0.5, delay: 0.3 }}
            onSubmit={handleSearch}
            className="mt-10 flex max-w-xl flex-col gap-3 rounded-2xl border border-ink-950/10 bg-white/80 p-3 shadow-lg shadow-ink-950/5 sm:flex-row sm:items-center"
          >
            <div className="flex flex-1 items-center gap-2 rounded-xl px-3 py-2">
              <label className="flex-1">
                <span className="block text-xs font-medium uppercase tracking-wide text-ink-800/60">
                  From
                </span>
                <input
                  value={origin}
                  onChange={(e) => setOrigin(e.target.value.toUpperCase())}
                  maxLength={3}
                  placeholder="LIS"
                  className="w-full bg-transparent font-display text-lg text-ink-950 outline-none placeholder:text-ink-950/30"
                />
              </label>
              <span className="text-ink-950/20">&rarr;</span>
              <label className="flex-1">
                <span className="block text-xs font-medium uppercase tracking-wide text-ink-800/60">
                  To
                </span>
                <input
                  value={destination}
                  onChange={(e) => setDestination(e.target.value.toUpperCase())}
                  maxLength={3}
                  placeholder="GRU"
                  className="w-full bg-transparent font-display text-lg text-ink-950 outline-none placeholder:text-ink-950/30"
                />
              </label>
            </div>
            <button
              type="submit"
              className="rounded-xl bg-sunset-500 px-6 py-3 text-sm font-semibold text-white transition hover:bg-sunset-600 active:scale-[0.98]"
            >
              Search flights
            </button>
          </motion.form>

          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

          {searched && (
            <motion.ul
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="mt-6 flex max-w-xl flex-col gap-2"
            >
              {flights.length === 0 && (
                <li className="rounded-xl border border-ink-950/10 bg-white/60 px-4 py-3 text-sm text-ink-800">
                  No flights found for that route right now - try LIS &rarr; GRU or explore below.
                </li>
              )}
              {flights.map((flight) => (
                <li
                  key={flight.id}
                  className="flex items-center justify-between rounded-xl border border-ink-950/10 bg-white/80 px-4 py-3"
                >
                  <span className="text-sm text-ink-900">
                    {flight.origin} &rarr; {flight.destination} &middot; {flight.availableSeats}{' '}
                    seats left
                  </span>
                  <span className="font-display text-lg text-ink-950">
                    {flight.priceAmount} {flight.priceCurrency}
                  </span>
                </li>
              ))}
              {flights.length > 0 && (
                <Link
                  to="/search"
                  className="mt-2 self-start text-sm font-semibold text-pine-600 underline decoration-pine-600/40 underline-offset-4 hover:text-pine-500"
                >
                  Log in to book this flight &rarr;
                </Link>
              )}
            </motion.ul>
          )}
        </div>
      </section>

      {/* How it works */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <h2 className="text-3xl font-medium text-ink-950">How it works</h2>
        <div className="mt-10 grid gap-8 sm:grid-cols-3">
          {[
            {
              step: '01',
              title: 'Search',
              body: 'Real-time flight and hotel availability, no fake "3 people are looking at this" pressure tactics.',
            },
            {
              step: '02',
              title: 'Book',
              body: 'One click reserves it. Payment authorization runs automatically behind the scenes.',
            },
            {
              step: '03',
              title: 'Fly',
              body: 'A confirmation email lands the moment the booking clears - no polling, no waiting around.',
            },
          ].map((item) => (
            <div key={item.step}>
              <span className="font-display text-sm text-sunset-500">{item.step}</span>
              <h3 className="mt-2 text-xl font-medium text-ink-950">{item.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-800">{item.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Featured destinations */}
      <section className="bg-ink-950 py-20">
        <div className="mx-auto max-w-6xl px-6">
          <h2 className="text-3xl font-medium text-sand-50">Popular right now</h2>
          <p className="mt-2 text-sm text-sand-50/60">
            Illustrative destinations from this demo's seeded inventory.
          </p>
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURED_DESTINATIONS.map((dest, i) => (
              <div
                key={dest.city}
                className="group relative overflow-hidden rounded-2xl border border-sand-50/10 p-6 transition hover:border-sand-50/30"
                style={{
                  background: `linear-gradient(135deg, hsl(${170 + i * 24} 35% 16%), hsl(${170 + i * 24} 45% 10%))`,
                }}
              >
                <h3 className="font-display text-2xl text-sand-50">{dest.city}</h3>
                <p className="text-sm text-sand-50/50">{dest.country}</p>
                <p className="mt-3 text-sm text-sand-50/70">{dest.blurb}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-6xl px-6 py-24 text-center">
        <h2 className="mx-auto max-w-2xl text-4xl font-medium text-ink-950">
          Ready to see it work end to end?
        </h2>
        <p className="mx-auto mt-4 max-w-lg text-ink-800">
          Create a free account and book a flight or hotel - the whole saga runs for real behind
          the scenes.
        </p>
        <Link
          to="/register"
          className="mt-8 inline-block rounded-full bg-sunset-500 px-8 py-3.5 text-sm font-semibold text-white shadow-md shadow-sunset-500/20 transition hover:bg-sunset-600"
        >
          Create your account
        </Link>
      </section>

      <footer className="border-t border-ink-950/10 py-10">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-6 text-sm text-ink-800 sm:flex-row sm:items-center sm:justify-between">
          <p>
            Aerostay is a portfolio project by Jeferson Siqueira - nine Java/Quarkus
            microservices, Kafka, MongoDB, Kubernetes.{' '}
            <a
              href="https://github.com/jeferson0306/travel-platform"
              className="font-medium text-pine-600 underline decoration-pine-600/30 underline-offset-4 hover:text-pine-500"
            >
              See the source & architecture docs
            </a>
            .
          </p>
        </div>
      </footer>
    </div>
  );
}
