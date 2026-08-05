import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Plane, Search, BookText, LogOut, Activity } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const tap = { whileHover: { scale: 1.03, y: -1 }, whileTap: { scale: 0.96 } };
const spring = { type: 'spring' as const, stiffness: 400, damping: 17 };

export function SiteHeader() {
  const { token, email, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 border-b border-ink-950/10 bg-sand-50/90 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link
          to="/"
          className="flex items-center gap-1.5 font-display text-xl font-semibold tracking-tight text-ink-950"
        >
          <Plane size={18} className="-rotate-45 text-sunset-500" />
          Aerostay
        </Link>
        <nav className="flex items-center gap-2">
          <motion.div {...tap} transition={spring}>
            <Link
              to="/status"
              className="hidden items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5 sm:flex"
            >
              <Activity size={14} />
              System status
            </Link>
          </motion.div>
          {token ? (
            <>
              <span className="hidden pr-2 text-sm text-ink-800 sm:inline">{email}</span>
              <motion.div {...tap} transition={spring}>
                <Link
                  to="/search"
                  className="flex items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5"
                >
                  <Search size={14} />
                  Search
                </Link>
              </motion.div>
              <motion.div {...tap} transition={spring}>
                <Link
                  to="/bookings"
                  className="flex items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5"
                >
                  <BookText size={14} />
                  My bookings
                </Link>
              </motion.div>
              <motion.button
                {...tap}
                transition={spring}
                onClick={logout}
                className="flex items-center gap-1.5 rounded-full bg-ink-950 px-4 py-2 text-sm font-medium text-sand-50 hover:bg-ink-800"
              >
                <LogOut size={14} />
                Log out
              </motion.button>
            </>
          ) : (
            <>
              <motion.div {...tap} transition={spring}>
                <Link
                  to="/login"
                  className="rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5"
                >
                  Log in
                </Link>
              </motion.div>
              <motion.div {...tap} transition={spring}>
                <Link
                  to="/register"
                  className="rounded-full bg-sunset-500 px-4 py-2 text-sm font-medium text-white shadow-sm shadow-sunset-500/30 hover:bg-sunset-600"
                >
                  Get started
                </Link>
              </motion.div>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
