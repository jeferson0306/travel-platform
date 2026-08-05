import { useMemo, useState, useEffect } from 'react';
import {
  Server,
  Database,
  Radio,
  ShieldCheck,
  Boxes,
  GitBranch,
  Gauge,
  KeyRound,
} from 'lucide-react';
import { SiteHeader } from '../components/SiteHeader';
import { ScrollReveal } from '../components/ScrollReveal';
import { SERVICES } from '../config/services';
import { useServiceHealth, type HealthState } from '../hooks/useServiceHealth';

const STATE_STYLES: Record<HealthState, { dot: string; text: string; label: string }> = {
  up: { dot: 'bg-pine-500', text: 'text-pine-600', label: 'Up' },
  down: { dot: 'bg-sunset-500', text: 'text-sunset-600', label: 'Unreachable' },
  checking: { dot: 'bg-ink-950/30', text: 'text-ink-800/60', label: 'Checking...' },
};

function StatusDot({ state }: { state: HealthState }) {
  return (
    <span className="relative flex h-2.5 w-2.5">
      {state === 'up' && (
        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-pine-400 opacity-60" />
      )}
      <span className={`relative inline-flex h-2.5 w-2.5 rounded-full ${STATE_STYLES[state].dot}`} />
    </span>
  );
}

function ServiceCard({ service }: { service: (typeof SERVICES)[number] }) {
  const health = useServiceHealth(service);
  const style = STATE_STYLES[health.state];

  return (
    <div className="bg-grain rounded-xl border border-ink-950/10 bg-white/60 p-5 shadow-elevated">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <StatusDot state={health.state} />
            <h3 className="font-display text-base font-semibold text-ink-950">{service.name}</h3>
          </div>
          <p className="mt-1 text-xs text-ink-800/60">{service.description}</p>
        </div>
        <span className={`shrink-0 text-xs font-medium ${style.text}`}>{style.label}</span>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-1.5">
        {service.dependencies.length === 0 && health.state !== 'checking' && (
          <span className="text-xs text-ink-800/40">No external dependencies reported</span>
        )}
        {service.dependencies.map((dep) => {
          const check = health.checks.find((c) => c.name.includes(dep.match));
          const depState: HealthState = check?.status ?? (health.state === 'up' ? 'up' : 'checking');
          return (
            <span
              key={dep.label}
              className={`inline-flex items-center gap-1 rounded-full border border-ink-950/10 px-2 py-0.5 text-[11px] font-medium ${STATE_STYLES[depState].text}`}
            >
              <StatusDot state={depState} />
              {dep.label}
            </span>
          );
        })}
      </div>

      <div className="mt-4 flex items-center justify-between text-[11px] text-ink-800/40">
        <span>{service.baseUrl.replace('http://', '')}</span>
        <span>{health.latencyMs != null ? `${health.latencyMs} ms` : '-'}</span>
      </div>
    </div>
  );
}

const PIPELINE_STAGES = [
  {
    icon: Server,
    title: 'Browser',
    body: 'React + Vite frontend, this page included',
  },
  {
    icon: ShieldCheck,
    title: 'API Gateway',
    body: 'JWT validation, rate limiting, CORS, circuit breaker + retry on GET',
  },
  {
    icon: Boxes,
    title: '8 microservices',
    body: 'Identity, Booking, Flight, Hotel, Payment, Notification, Search, Assistant',
  },
  {
    icon: Radio,
    title: 'Kafka events',
    body: 'booking-created, payment-authorized, flight-created... consumed asynchronously',
  },
  {
    icon: Database,
    title: 'Data & storage',
    body: 'MongoDB per service, Redis for rate limits, OpenSearch index, S3 receipts',
  },
];

const ENGINEERING_HIGHLIGHTS = [
  { icon: GitBranch, text: 'Outbox pattern for booking-service - no dual-write inconsistency between DB and Kafka' },
  { icon: ShieldCheck, text: 'Fault tolerance: timeout, circuit breaker, bulkhead, retry only on idempotent GETs' },
  { icon: KeyRound, text: 'JWT-verified writes on every service, fast-failed at the gateway before hitting business logic' },
  { icon: Gauge, text: 'Mutation testing (PIT) + JaCoCo coverage gate enforced in the Maven reactor, not just line coverage' },
];

export default function StatusPage() {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const upCount = useMemo(() => SERVICES.length, []);

  return (
    <div className="min-h-screen overflow-x-hidden">
      <SiteHeader />

      <section className="bg-grain relative overflow-hidden border-b border-ink-950/10">
        <div className="pointer-events-none absolute -right-24 -top-24 h-96 w-96 rounded-full bg-pine-400/20 blur-3xl" />
        <div className="relative mx-auto max-w-6xl px-6 py-16">
          <p className="text-xs font-medium uppercase tracking-widest text-pine-600">
            Live system status
          </p>
          <h1 className="mt-2 font-display text-4xl text-ink-950 sm:text-5xl">
            Every service, checked live
          </h1>
          <p className="mt-4 max-w-2xl text-base text-ink-800/70">
            This page calls each of Aerostay's {upCount} services' own health endpoint directly
            from your browser, right now - not a cached badge, not a fake "all systems
            operational" banner. What you see below is what's actually running.
          </p>
          <p className="mt-2 text-xs text-ink-800/40">
            Last refreshed {now.toLocaleTimeString()} - services re-check every 15s.
          </p>
        </div>
      </section>

      <ScrollReveal className="mx-auto max-w-6xl px-6 py-14" stagger={0.04}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {SERVICES.map((service) => (
            <ServiceCard key={service.id} service={service} />
          ))}
        </div>
      </ScrollReveal>

      <section className="border-t border-ink-950/10 bg-ink-950/[0.02] py-16">
        <div className="mx-auto max-w-6xl px-6">
          <h2 className="font-display text-2xl text-ink-950">How a request flows</h2>
          <p className="mt-2 max-w-2xl text-sm text-ink-800/60">
            A static map of the architecture above - the live cards prove each stage is actually
            up; this shows how they connect.
          </p>

          <div className="mt-8 flex flex-col gap-3 lg:flex-row lg:items-stretch lg:gap-0">
            {PIPELINE_STAGES.map((stage, i) => (
              <div key={stage.title} className="flex items-center lg:flex-1">
                <div className="shadow-elevated flex-1 rounded-xl border border-ink-950/10 bg-white/70 p-5">
                  <stage.icon size={20} className="text-pine-600" />
                  <h3 className="mt-3 font-display text-sm font-semibold text-ink-950">
                    {stage.title}
                  </h3>
                  <p className="mt-1 text-xs leading-relaxed text-ink-800/60">{stage.body}</p>
                </div>
                {i < PIPELINE_STAGES.length - 1 && (
                  <div className="hidden shrink-0 px-2 text-ink-950/20 lg:block" aria-hidden>
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path
                        d="M2 10h14m0 0-5-5m5 5-5 5"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      <ScrollReveal className="mx-auto max-w-6xl px-6 py-16" stagger={0.05}>
        <h2 className="font-display text-2xl text-ink-950">Engineering highlights</h2>
        <p className="mt-2 max-w-2xl text-sm text-ink-800/60">
          The parts that don't show up just by clicking around the UI.
        </p>
        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
          {ENGINEERING_HIGHLIGHTS.map((item) => (
            <div
              key={item.text}
              className="flex items-start gap-3 rounded-xl border border-ink-950/10 bg-white/60 p-4"
            >
              <item.icon size={18} className="mt-0.5 shrink-0 text-sunset-500" />
              <p className="text-sm text-ink-800/80">{item.text}</p>
            </div>
          ))}
        </div>
        <p className="mt-8 text-sm text-ink-800/60">
          Full write-up:{' '}
          <a
            href="https://github.com/jeferson0306/travel-platform"
            className="font-medium text-pine-600 underline decoration-pine-600/30 underline-offset-4 hover:text-pine-500"
          >
            source, ADRs and architecture docs
          </a>
          .
        </p>
      </ScrollReveal>
    </div>
  );
}
