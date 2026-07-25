import { useParams, useLocation, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { CheckCircle2 } from 'lucide-react';
import { SiteHeader } from '../components/SiteHeader';

const tap = { whileHover: { scale: 1.03, y: -1 }, whileTap: { scale: 0.96 } };
const spring = { type: 'spring' as const, stiffness: 400, damping: 17 };

export function BookingConfirmationPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  // Passed via navigate() state from SearchPage, which already had the full flight/hotel
  // details in hand - there is no GET /bookings/{id} endpoint (documented platform gap), so a
  // direct visit/refresh of this page simply won't have it, and that's fine (falls back below).
  const itemSummary = (useLocation().state as { itemSummary?: string } | null)?.itemSummary;

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <div className="mx-auto max-w-lg px-6 py-20 text-center">
        <motion.div
          initial={{ opacity: 0, scale: 0.5 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ type: 'spring', stiffness: 260, damping: 15 }}
          className="relative mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-pine-600/10 text-pine-600"
        >
          <motion.span
            initial={{ opacity: 0.5, scale: 1 }}
            animate={{ opacity: 0, scale: 1.6 }}
            transition={{ duration: 1, delay: 0.2, ease: 'easeOut' }}
            className="absolute inset-0 rounded-full bg-pine-500/30"
          />
          <CheckCircle2 size={30} />
        </motion.div>
        <motion.h1
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.15 }}
          className="mt-6 text-3xl font-medium text-ink-950"
        >
          Booking confirmed
        </motion.h1>
        {itemSummary && (
          <motion.p
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.12 }}
            className="mt-4 text-lg font-medium text-ink-950"
          >
            {itemSummary}
          </motion.p>
        )}
        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.2 }}
          className="mt-3 text-ink-800"
        >
          Thanks! Your booking (reference <code className="rounded bg-ink-950/5 px-1.5 py-0.5">{bookingId}</code>)
          was received. Payment processing and a confirmation email happen automatically in the
          background - you don't need to do anything else.
        </motion.p>
        <div className="mt-8 flex justify-center gap-3">
          <motion.div {...tap} transition={spring}>
            <Link
              to="/bookings"
              className="inline-block rounded-full bg-sunset-500 px-6 py-2.5 text-sm font-medium text-white hover:bg-sunset-600"
            >
              View my bookings
            </Link>
          </motion.div>
          <motion.div {...tap} transition={spring}>
            <Link
              to="/search"
              className="inline-block rounded-full border border-ink-950/15 px-6 py-2.5 text-sm font-medium text-ink-900 hover:bg-ink-950/5"
            >
              Back to search
            </Link>
          </motion.div>
        </div>
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
            . There is no single-booking status endpoint (a documented platform gap) - check
            "My bookings" for the current status instead of polling this page.
          </p>
        </details>
      </div>
    </div>
  );
}
