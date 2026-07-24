import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { api, friendlyErrorMessage } from '../api/client';
import { AuthLayout } from '../components/AuthLayout';

const inputClass =
  'mt-1 w-full rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20';
const tap = { whileHover: { scale: 1.02, y: -1 }, whileTap: { scale: 0.97 } };
const spring = { type: 'spring' as const, stiffness: 400, damping: 17 };

export function RegisterPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await api.register({ fullName, email, password });
      navigate('/login', { state: { registered: true } });
    } catch (err) {
      setError(friendlyErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout title="Create an account">
      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        <label className="text-sm font-medium text-ink-800">
          Full name
          <input
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
            className={inputClass}
          />
        </label>
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
            minLength={8}
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
          {submitting ? 'Creating account...' : 'Register'}
        </motion.button>
      </form>
      <p className="mt-6 text-sm text-ink-800">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-pine-600 hover:text-pine-500">
          Log in
        </Link>
      </p>
    </AuthLayout>
  );
}
