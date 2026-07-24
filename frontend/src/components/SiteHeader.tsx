import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function SiteHeader() {
  const { token, email, logout } = useAuth();

  return (
    <header className="sticky top-0 z-30 border-b border-ink-950/10 bg-sand-50/90 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link to="/" className="font-display text-xl font-semibold tracking-tight text-ink-950">
          Aerostay
        </Link>
        <nav className="flex items-center gap-4">
          {token ? (
            <>
              <span className="hidden text-sm text-ink-800 sm:inline">{email}</span>
              <Link
                to="/search"
                className="rounded-full px-4 py-2 text-sm font-medium text-ink-900 transition hover:bg-ink-950/5"
              >
                Search
              </Link>
              <button
                onClick={logout}
                className="rounded-full bg-ink-950 px-4 py-2 text-sm font-medium text-sand-50 transition hover:bg-ink-800"
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-full px-4 py-2 text-sm font-medium text-ink-900 transition hover:bg-ink-950/5"
              >
                Log in
              </Link>
              <Link
                to="/register"
                className="rounded-full bg-sunset-500 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-sunset-600"
              >
                Get started
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
