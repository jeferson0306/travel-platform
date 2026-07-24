import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { SiteHeader } from '../components/SiteHeader';

export function BookingConfirmationPage() {
  const { bookingId } = useParams<{ bookingId: string }>();

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <div className="mx-auto max-w-lg px-6 py-20 text-center">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
          className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-pine-600/10 text-pine-600"
        >
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M20 6 9 17l-5-5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </motion.div>
        <motion.h1
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
          className="mt-6 text-3xl font-medium text-ink-950"
        >
          Booking confirmed
        </motion.h1>
        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.15 }}
          className="mt-3 text-ink-800"
        >
          Thanks! Your booking (reference <code className="rounded bg-ink-950/5 px-1.5 py-0.5">{bookingId}</code>)
          was received. Payment processing and a confirmation email happen automatically in the
          background - you don't need to do anything else.
        </motion.p>
        <Link
          to="/search"
          className="mt-8 inline-block rounded-full bg-sunset-500 px-6 py-2.5 text-sm font-medium text-white transition hover:bg-sunset-600"
        >
          Back to search
        </Link>
        <details className="mt-10 text-left text-sm text-ink-800/70">
          <summary className="cursor-pointer font-medium text-ink-800">Technical details</summary>
          <p className="mt-2">
            This triggers the platform&apos;s real choreography saga: payment authorization,
            booking confirmation, and a confirmation notification - all asynchronous, via Kafka,
            exactly as documented in{' '}
            <a
              href="https://github.com/jeferson0306/travel-platform/blob/main/docs/adr/0010-payment-saga.md"
              className="text-pine-600 underline underline-offset-2 hover:text-pine-500"
            >
              ADR 0010
            </a>
            . This demo has no booking-status endpoint yet (a documented platform gap), so there
            is nothing to poll here.
          </p>
        </details>
      </div>
    </div>
  );
}
