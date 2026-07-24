import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Search, CreditCard, MailCheck, MapPin, ArrowRight } from 'lucide-react';
import { SiteHeader } from '../components/SiteHeader';
import { ScrollReveal } from '../components/ScrollReveal';
import { CountUp } from '../components/CountUp';
import { api, friendlyErrorMessage, type Flight } from '../api/client';
import { gsap } from '../lib/gsap';

const FEATURED_DESTINATIONS = [
  { city: 'Lisbon', country: 'Portugal', blurb: 'Pastel facades, river light, tram bells.' },
  { city: 'Porto', country: 'Portugal', blurb: 'Port wine cellars and a bridge by Eiffel.' },
  { city: 'Sao Paulo', country: 'Brazil', blurb: 'Skyline, food scene, non-stop energy.' },
  { city: 'New York', country: 'USA', blurb: 'The city that never sits down.' },
  { city: 'Madrid', country: 'Spain', blurb: 'Late dinners, wide boulevards, Prado art.' },
];

const HOW_IT_WORKS = [
  {
    icon: Search,
    step: '01',
    title: 'Search',
    body: 'Real-time flight and hotel availability, no fake "3 people are looking at this" pressure tactics.',
  },
  {
    icon: CreditCard,
    step: '02',
    title: 'Book',
    body: 'One click reserves it. Payment authorization runs automatically behind the scenes.',
  },
  {
    icon: MailCheck,
    step: '03',
    title: 'Fly',
    body: 'A confirmation email lands the moment the booking clears - no polling, no waiting around.',
  },
];

const STATS = [
  { value: 9, suffix: '', label: 'independent microservices' },
  { value: 20, suffix: '', label: 'milestones shipped' },
  { value: 100, suffix: '%', label: 'real saga, zero mocked data' },
];

export function LandingPage() {
  const [origin, setOrigin] = useState('LIS');
  const [destination, setDestination] = useState('GRU');
  const [flights, setFlights] = useState<Flight[]>([]);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const heroRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!heroRef.current) return;
    const ctx = gsap.context(() => {
      const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });
      tl.from('.hero-badge', { y: 20, opacity: 0, duration: 0.5 })
        .from('.hero-title', { y: 36, opacity: 0, duration: 0.7 }, '-=0.3')
        .from('.hero-subtitle', { y: 24, opacity: 0, duration: 0.6 }, '-=0.4')
        .from('.hero-form', { y: 20, opacity: 0, duration: 0.6 }, '-=0.35')
        .from(
          '.hero-glow',
          { opacity: 0, scale: 0.8, duration: 1.2, ease: 'power2.out' },
          '-=1',
        );
    }, heroRef);
    return () => ctx.revert();
  }, []);

  const handleSearch = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    try {
      setFlights(await api.searchFlights(origin, destination));
      setSearched(true);
    } catch (err) {
      setError(friendlyErrorMessage(err));
    }
  };

  return (
    <div className="min-h-screen overflow-x-hidden">
      <SiteHeader />

      {/* Hero */}
      <section ref={heroRef} className="relative overflow-hidden">
        <div className="hero-glow pointer-events-none absolute -right-24 -top-24 h-[28rem] w-[28rem] rounded-full bg-sunset-400/25 blur-3xl" />
        <div className="hero-glow pointer-events-none absolute -left-32 top-52 h-80 w-80 rounded-full bg-pine-400/25 blur-3xl" />
        <div className="bg-grain relative mx-auto max-w-6xl px-6 py-20 sm:py-28">
          <p className="hero-badge mb-4 inline-flex items-center gap-2 rounded-full bg-pine-600/10 px-4 py-1.5 text-sm font-medium text-pine-600">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-pine-500" />
            One route, right now: Lisbon to Sao Paulo from 589 EUR
          </p>
          <h1 className="hero-title max-w-3xl text-5xl font-medium leading-[1.05] text-ink-950 sm:text-6xl">
            Book flights and hotels without the runaround.
          </h1>
          <p className="hero-subtitle mt-6 max-w-xl text-lg text-ink-800">
            Search real inventory, book in a couple of clicks, get a confirmation the moment
            payment clears. No hidden steps, no dark patterns.
          </p>

          {/* Mini search widget - hits the real, public search API */}
          <form
            onSubmit={handleSearch}
            className="hero-form shadow-elevated mt-10 flex max-w-xl flex-col gap-3 rounded-2xl border border-ink-950/10 bg-white/85 p-3 backdrop-blur sm:flex-row sm:items-center"
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
              <ArrowRight size={16} className="shrink-0 text-ink-950/25" />
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
            <motion.button
              type="submit"
              whileHover={{ scale: 1.02, y: -1 }}
              whileTap={{ scale: 0.96 }}
              transition={{ type: 'spring', stiffness: 400, damping: 17 }}
              className="flex items-center justify-center gap-2 rounded-xl bg-sunset-500 px-6 py-3 text-sm font-semibold text-white shadow-sm shadow-sunset-500/30 hover:bg-sunset-600"
            >
              <Search size={16} />
              Search flights
            </motion.button>
          </form>

          {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

          {searched && (
            <motion.ul
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              className="mt-6 flex max-w-xl flex-col gap-2"
            >
              {flights.length === 0 && (
                <li className="rounded-xl border border-ink-950/10 bg-white/60 px-4 py-3 text-sm text-ink-800">
                  No flights found for that route right now - try LIS &rarr; GRU or explore below.
                </li>
              )}
              {flights.map((flight, i) => (
                <motion.li
                  key={flight.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.06 }}
                  className="flex items-center justify-between rounded-xl border border-ink-950/10 bg-white/80 px-4 py-3"
                >
                  <span className="text-sm text-ink-900">
                    {flight.origin} &rarr; {flight.destination} &middot; {flight.availableSeats}{' '}
                    seats left
                  </span>
                  <span className="font-display text-lg text-ink-950">
                    {flight.priceAmount} {flight.priceCurrency}
                  </span>
                </motion.li>
              ))}
              {flights.length > 0 && (
                <Link
                  to="/search"
                  className="mt-2 inline-flex w-fit items-center gap-1 text-sm font-semibold text-pine-600 underline decoration-pine-600/40 underline-offset-4 hover:text-pine-500"
                >
                  Log in to book this flight <ArrowRight size={14} />
                </Link>
              )}
            </motion.ul>
          )}
        </div>
      </section>

      {/* Stats */}
      <ScrollReveal className="border-y border-ink-950/10 bg-white/50" stagger={0.12}>
        <div className="mx-auto grid max-w-6xl grid-cols-1 gap-8 px-6 py-14 sm:grid-cols-3">
          {STATS.map((stat) => (
            <div key={stat.label} className="text-center">
              <CountUp
                value={stat.value}
                suffix={stat.suffix}
                className="font-display text-4xl text-ink-950 sm:text-5xl"
              />
              <p className="mt-2 text-sm text-ink-800/60">{stat.label}</p>
            </div>
          ))}
        </div>
      </ScrollReveal>

      {/* How it works */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <ScrollReveal>
          <h2 className="text-3xl font-medium text-ink-950">How it works</h2>
        </ScrollReveal>
        <ScrollReveal className="mt-10 grid gap-6 sm:grid-cols-3" stagger={0.12}>
          {HOW_IT_WORKS.map((item) => (
            <div
              key={item.step}
              className="shadow-elevated rounded-2xl border border-ink-950/10 bg-white/70 p-6 transition hover:-translate-y-1 hover:border-pine-500/30"
            >
              <div className="flex items-center justify-between">
                <span className="flex h-10 w-10 items-center justify-center rounded-full bg-sunset-500/10 text-sunset-600">
                  <item.icon size={18} />
                </span>
                <span className="font-display text-sm text-ink-950/20">{item.step}</span>
              </div>
              <h3 className="mt-4 text-xl font-medium text-ink-950">{item.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-800">{item.body}</p>
            </div>
          ))}
        </ScrollReveal>
      </section>

      {/* Featured destinations */}
      <section className="bg-grain bg-ink-950 py-20">
        <div className="mx-auto max-w-6xl px-6">
          <ScrollReveal>
            <h2 className="text-3xl font-medium text-sand-50">Popular right now</h2>
            <p className="mt-2 text-sm text-sand-50/60">
              Illustrative destinations from this demo's seeded inventory.
            </p>
          </ScrollReveal>
          <ScrollReveal className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3" stagger={0.08}>
            {FEATURED_DESTINATIONS.map((dest, i) => (
              <motion.div
                key={dest.city}
                whileHover={{ y: -4 }}
                transition={{ type: 'spring', stiffness: 300, damping: 20 }}
                className="group relative overflow-hidden rounded-2xl border border-sand-50/10 p-6"
                style={{
                  background: `linear-gradient(135deg, hsl(${170 + i * 24} 35% 16%), hsl(${170 + i * 24} 45% 10%))`,
                }}
              >
                <div className="flex items-center gap-1.5 text-sand-50/50">
                  <MapPin size={13} />
                  <p className="text-xs">{dest.country}</p>
                </div>
                <h3 className="mt-2 font-display text-2xl text-sand-50">{dest.city}</h3>
                <p className="mt-3 text-sm text-sand-50/70">{dest.blurb}</p>
              </motion.div>
            ))}
          </ScrollReveal>
        </div>
      </section>

      {/* CTA */}
      <ScrollReveal className="mx-auto max-w-6xl px-6 py-24 text-center">
        <h2 className="mx-auto max-w-2xl text-4xl font-medium text-ink-950">
          Ready to see it work end to end?
        </h2>
        <p className="mx-auto mt-4 max-w-lg text-ink-800">
          Create a free account and book a flight or hotel - the whole saga runs for real behind
          the scenes.
        </p>
        <motion.div whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.97 }} className="mt-8 inline-block">
          <Link
            to="/register"
            className="inline-flex items-center gap-2 rounded-full bg-sunset-500 px-8 py-3.5 text-sm font-semibold text-white shadow-md shadow-sunset-500/25 hover:bg-sunset-600"
          >
            Create your account
            <ArrowRight size={16} />
          </Link>
        </motion.div>
      </ScrollReveal>

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
