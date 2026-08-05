import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Search, CreditCard, MailCheck, ArrowRight } from 'lucide-react';
import { SiteHeader } from '../components/SiteHeader';
import { ScrollReveal } from '../components/ScrollReveal';
import { CountUp } from '../components/CountUp';
import { DestinationsGallery } from '../components/DestinationsGallery';
import { api, friendlyErrorMessage, type Flight } from '../api/client';
import { gsap } from '../lib/gsap';
// Static import, not React.lazy - LandingPage itself is already the lazy route boundary (see
// App.tsx). A nested lazy() one level down here is what caused Vite's dev dependency scanner to
// discover three.js/@react-three late and double-bundle it (see App.tsx's comment).
import Airplane3DSection from '../components/airplane/Airplane3DSection';

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
        <div className="bg-grain relative mx-auto max-w-6xl px-6 py-28 sm:py-36">
          <p className="hero-badge mb-6 inline-flex items-center gap-2 border-b border-pine-600/30 pb-1 text-xs font-semibold uppercase tracking-[0.2em] text-pine-600">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-pine-500" />
            Live route: Lisbon &rarr; Sao Paulo from 589 EUR
          </p>
          <h1 className="hero-title max-w-4xl text-6xl font-medium leading-[0.95] tracking-tight text-ink-950 sm:text-8xl">
            Book flights and hotels
            <span className="block text-pine-600">without the runaround.</span>
          </h1>
          <p className="hero-subtitle mt-8 max-w-lg text-lg leading-relaxed text-ink-800/80">
            Search real inventory, book in a couple of clicks, get a confirmation the moment
            payment clears. No hidden steps, no dark patterns.
          </p>

          {/* Mini search widget - hits the real, public search API */}
          <form
            onSubmit={handleSearch}
            className="hero-form shadow-elevated mt-12 flex max-w-xl flex-col gap-3 rounded-lg border border-ink-950/15 bg-white p-3 sm:flex-row sm:items-center"
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

      {/* 3D airplane - draggable, flies into view on scroll */}
      <Airplane3DSection />

      {/* Stats - hairline-divided row, not a floating card, per the Editorial Grid rule set */}
      <ScrollReveal className="rule-hairline border-b border-ink-950/10" stagger={0.12}>
        <div className="mx-auto grid max-w-6xl grid-cols-1 divide-y divide-ink-950/10 px-6 sm:grid-cols-3 sm:divide-x sm:divide-y-0">
          {STATS.map((stat) => (
            <div key={stat.label} className="px-6 py-14 first:pl-0 last:pr-0 sm:text-center">
              <CountUp
                value={stat.value}
                suffix={stat.suffix}
                className="font-display text-5xl tracking-tight text-ink-950 sm:text-6xl"
              />
              <p className="mt-3 text-sm uppercase tracking-wide text-ink-800/50">{stat.label}</p>
            </div>
          ))}
        </div>
      </ScrollReveal>

      {/* How it works */}
      <section className="mx-auto max-w-6xl px-6 py-28">
        <ScrollReveal className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <h2 className="max-w-md text-4xl font-medium tracking-tight text-ink-950 sm:text-5xl">
            How it works
          </h2>
          <p className="max-w-sm text-sm text-ink-800/60">
            Three steps, no filler screens between them.
          </p>
        </ScrollReveal>
        <ScrollReveal
          className="mt-14 grid divide-y divide-ink-950/10 border-t border-ink-950/10 sm:grid-cols-3 sm:divide-x sm:divide-y-0"
          stagger={0.12}
        >
          {HOW_IT_WORKS.map((item) => (
            <div key={item.step} className="group px-1 py-8 transition sm:px-8 sm:first:pl-0 sm:last:pr-0">
              <div className="flex items-center justify-between">
                <span className="flex h-11 w-11 items-center justify-center rounded-full bg-sunset-500/10 text-sunset-600 transition group-hover:bg-sunset-500 group-hover:text-white">
                  <item.icon size={18} />
                </span>
                <span className="font-display text-sm text-ink-950/20">{item.step}</span>
              </div>
              <h3 className="mt-6 text-2xl font-medium tracking-tight text-ink-950">{item.title}</h3>
              <p className="mt-3 text-sm leading-relaxed text-ink-800/70">{item.body}</p>
            </div>
          ))}
        </ScrollReveal>
      </section>

      {/* Featured destinations - horizontal scroll pinned to vertical scroll (the "premium
          travel site" signature move) instead of a static grid. */}
      <DestinationsGallery />

      {/* CTA */}
      <ScrollReveal className="rule-hairline mx-auto max-w-6xl px-6 py-28 text-center">
        <h2 className="mx-auto max-w-2xl text-5xl font-medium tracking-tight text-ink-950 sm:text-6xl">
          Ready to see it work end to end?
        </h2>
        <p className="mx-auto mt-5 max-w-lg text-ink-800/70">
          Create a free account and book a flight or hotel - the whole saga runs for real behind
          the scenes.
        </p>
        <motion.div whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.97 }} className="mt-10 inline-block">
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
            {' - or '}
            <Link
              to="/status"
              className="font-medium text-pine-600 underline decoration-pine-600/30 underline-offset-4 hover:text-pine-500"
            >
              watch every service's health live
            </Link>
            .
          </p>
        </div>
      </footer>
    </div>
  );
}
