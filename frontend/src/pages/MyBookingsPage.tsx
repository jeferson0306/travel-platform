import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Plane, Building2, Inbox } from 'lucide-react';
import { api, friendlyErrorMessage, type Booking } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { SiteHeader } from '../components/SiteHeader';

const STATUS_STYLES: Record<Booking['status'], string> = {
  PENDING: 'bg-amber-100 text-amber-700',
  CONFIRMED: 'bg-pine-600/10 text-pine-600',
  CANCELLED: 'bg-red-100 text-red-600',
};

function SkeletonRow() {
  return <div className="h-[68px] animate-pulse rounded-xl bg-ink-950/[0.05]" />;
}

export function MyBookingsPage() {
  const { token } = useAuth();
  const [bookings, setBookings] = useState<Booking[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    api
      .listBookings(token)
      .then(setBookings)
      .catch((err) => setError(friendlyErrorMessage(err)));
  }, [token]);

  return (
    <div className="min-h-screen">
      <SiteHeader />
      <div className="mx-auto max-w-3xl px-6 py-12">
        <h1 className="text-3xl font-medium text-ink-950">My bookings</h1>

        {error && <p className="mt-4 rounded-lg bg-red-50 px-4 py-2 text-sm text-red-600">{error}</p>}

        {bookings === null && !error && (
          <div className="mt-6 flex flex-col gap-3">
            <SkeletonRow />
            <SkeletonRow />
          </div>
        )}

        {bookings !== null && bookings.length === 0 && (
          <div className="mt-6 flex flex-col items-center gap-3 rounded-xl bg-ink-950/[0.03] px-4 py-10 text-center text-sm text-ink-800/60">
            <Inbox size={26} className="text-ink-950/25" />
            <p>
              No bookings yet -{' '}
              <a href="/search" className="text-pine-600 underline underline-offset-2">
                search flights or hotels
              </a>{' '}
              to make your first one.
            </p>
          </div>
        )}

        <ul className="mt-6 flex flex-col gap-3">
          {bookings?.map((booking, i) => {
            const Icon = booking.itemType === 'FLIGHT' ? Plane : Building2;
            return (
              <motion.li
                key={booking.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                className="shadow-elevated flex items-center justify-between rounded-xl border border-ink-950/10 bg-white/80 px-5 py-4"
              >
                <div className="flex items-center gap-3">
                  <span className="flex h-9 w-9 items-center justify-center rounded-full bg-ink-950/[0.05] text-ink-800">
                    <Icon size={16} />
                  </span>
                  <div>
                    <p className="text-sm font-medium text-ink-900">
                      {booking.itemSummary ?? `${booking.itemType === 'FLIGHT' ? 'Flight' : 'Hotel'} booking`}
                    </p>
                    <p className="text-xs text-ink-800/50">
                      {new Date(booking.createdAt).toLocaleDateString()} &middot; ref{' '}
                      <code>{booking.id.slice(0, 8)}</code>
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="font-display text-lg text-ink-950">
                    {booking.amount} {booking.currency}
                  </span>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-medium ${STATUS_STYLES[booking.status]}`}
                  >
                    {booking.status}
                  </span>
                </div>
              </motion.li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
