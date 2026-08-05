import { useState, type FormEvent } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { api, friendlyErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { AuthLayout } from '../components/AuthLayout';

const inputClass =
  'mt-1 w-full rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20';
const tap = { whileHover: { scale: 1.02, y: -1 }, whileTap: { scale: 0.97 } };
const spring = { type: 'spring' as const, stiffness: 400, damping: 17 };

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const justRegistered = Boolean((location.state as { registered?: boolean } | null)?.registered);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const { accessToken } = await api.login({ email, password });
      login(accessToken, email);
      navigate('/search');
    } catch (err) {
      setError(friendlyErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout title="Welcome back">
      {justRegistered && (
        <p className="mt-3 rounded-lg bg-pine-600/10 px-3 py-2 text-sm text-pine-600 dark:text-pine-400">
          Account created - log in to continue.
        </p>
      )}
      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        <label className="text-sm font-medium text-ink-800">
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            className={inputClass}
          />
        </label>
        <label className="text-sm font-medium text-ink-800">
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className={inputClass}
          />
        </label>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <motion.button
          {...tap}
          transition={spring}
          type="submit"
          disabled={submitting}
          className="mt-2 rounded-lg bg-sunset-500 px-4 py-2.5 font-medium text-white hover:bg-sunset-600 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {submitting ? 'Logging in...' : 'Log in'}
        </motion.button>
      </form>
      <p className="mt-6 text-sm text-ink-800">
        No account yet?{' '}
        <Link to="/register" className="font-medium text-pine-600 dark:text-pine-400 hover:text-pine-500">
          Register
        </Link>
      </p>
    </AuthLayout>
  );
}
