import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Plane, Search, BookText, LogOut, Activity, Home, Sun, Moon } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';

const tap = { whileHover: { scale: 1.03, y: -1 }, whileTap: { scale: 0.96 } };
const spring = { type: 'spring' as const, stiffness: 400, damping: 17 };

function ThemeToggle() {
  const { theme, toggle } = useTheme();
  return (
    <motion.button
      {...tap}
      transition={spring}
      onClick={toggle}
      aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
      className="flex h-9 w-9 items-center justify-center rounded-full text-ink-900 hover:bg-ink-950/5 dark:text-sand-50 dark:hover:bg-sand-50/10"
    >
      {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
    </motion.button>
  );
}

export function SiteHeader() {
  const { token, email, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 border-b border-ink-950/10 bg-sand-50/90 backdrop-blur dark:border-sand-50/10 dark:bg-ink-950/90">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link
          to="/"
          className="flex items-center gap-1.5 font-display text-xl font-semibold tracking-tight text-ink-950 dark:text-sand-50"
        >
          <Plane size={18} className="-rotate-45 text-sunset-500" />
          Aerostay
        </Link>
        <nav className="flex items-center gap-1">
          <motion.div {...tap} transition={spring}>
            <Link
              to="/"
              className="hidden items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5 sm:flex dark:text-sand-50 dark:hover:bg-sand-50/10"
            >
              <Home size={14} />
              Home
            </Link>
          </motion.div>
          <motion.div {...tap} transition={spring}>
            <Link
              to="/status"
              className="hidden items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5 sm:flex dark:text-sand-50 dark:hover:bg-sand-50/10"
            >
              <Activity size={14} />
              System status
            </Link>
          </motion.div>
          {token ? (
            <>
              <span className="hidden pr-2 text-sm text-ink-800 sm:inline dark:text-sand-50/70">
                {email}
              </span>
              <motion.div {...tap} transition={spring}>
                <Link
                  to="/search"
                  className="flex items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5 dark:text-sand-50 dark:hover:bg-sand-50/10"
                >
                  <Search size={14} />
                  Search
                </Link>
              </motion.div>
              <motion.div {...tap} transition={spring}>
                <Link
                  to="/bookings"
                  className="flex items-center gap-1.5 rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5 dark:text-sand-50 dark:hover:bg-sand-50/10"
                >
                  <BookText size={14} />
                  My bookings
                </Link>
              </motion.div>
              <motion.button
                {...tap}
                transition={spring}
                onClick={logout}
                className="flex items-center gap-1.5 rounded-full bg-ink-950 px-4 py-2 text-sm font-medium text-sand-50 hover:bg-ink-800 dark:bg-sand-50 dark:text-ink-950 dark:hover:bg-sand-100"
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
                  className="rounded-full px-4 py-2 text-sm font-medium text-ink-900 hover:bg-ink-950/5 dark:text-sand-50 dark:hover:bg-sand-50/10"
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
          <ThemeToggle />
        </nav>
      </div>
    </header>
  );
}
