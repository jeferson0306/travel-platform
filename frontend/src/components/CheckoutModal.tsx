import { useState, type FormEvent } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ShieldCheck, CreditCard } from 'lucide-react';

const tap = { whileHover: { scale: 1.02, y: -1 }, whileTap: { scale: 0.96 } };
const spring = { type: 'spring' as const, stiffness: 400, damping: 17 };

export interface CheckoutSummary {
  title: string;
  subtitle: string;
  amount: number;
  currency: string;
}

interface CheckoutModalProps {
  summary: CheckoutSummary | null;
  submitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

const inputClass =
  'mt-1 w-full rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20';

function formatCardNumber(value: string) {
  return value
    .replace(/\D/g, '')
    .slice(0, 16)
    .replace(/(.{4})/g, '$1 ')
    .trim();
}

function formatExpiry(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 4);
  if (digits.length < 3) return digits;
  return `${digits.slice(0, 2)}/${digits.slice(2)}`;
}

export function CheckoutModal({ summary, submitting, onCancel, onConfirm }: CheckoutModalProps) {
  const [cardNumber, setCardNumber] = useState('4242 4242 4242 4242');
  const [expiry, setExpiry] = useState('12/29');
  const [cvc, setCvc] = useState('123');
  const [cardholder, setCardholder] = useState('');

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    onConfirm();
  };

  return (
    <AnimatePresence>
      {summary && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-40 flex items-center justify-center bg-ink-950/40 px-4"
          onClick={onCancel}
        >
          <motion.div
            initial={{ opacity: 0, y: 16, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.98 }}
            transition={{ duration: 0.25 }}
            onClick={(e) => e.stopPropagation()}
            className="shadow-popover w-full max-w-md rounded-2xl bg-white p-6"
          >
            <h2 className="flex items-center gap-2 text-xl font-medium text-ink-950">
              <CreditCard size={19} className="text-sunset-600 dark:text-sunset-400" />
              Confirm & pay
            </h2>
            <div className="mt-4 flex items-center justify-between rounded-xl bg-ink-950/[0.03] px-4 py-3">
              <div>
                <p className="text-sm font-medium text-ink-900">{summary.title}</p>
                <p className="text-xs text-ink-800/60">{summary.subtitle}</p>
              </div>
              <p className="font-display text-lg text-ink-950">
                {summary.amount} {summary.currency}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="mt-5 flex flex-col gap-3">
              <label className="text-sm font-medium text-ink-800">
                Cardholder name
                <input
                  value={cardholder}
                  onChange={(e) => setCardholder(e.target.value)}
                  placeholder="Jane Doe"
                  required
                  className={inputClass}
                />
              </label>
              <label className="text-sm font-medium text-ink-800">
                Card number
                <input
                  value={cardNumber}
                  onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
                  inputMode="numeric"
                  required
                  className={`${inputClass} font-mono`}
                />
              </label>
              <div className="flex gap-3">
                <label className="flex-1 text-sm font-medium text-ink-800">
                  Expiry
                  <input
                    value={expiry}
                    onChange={(e) => setExpiry(formatExpiry(e.target.value))}
                    placeholder="MM/YY"
                    inputMode="numeric"
                    required
                    className={`${inputClass} font-mono`}
                  />
                </label>
                <label className="w-24 text-sm font-medium text-ink-800">
                  CVC
                  <input
                    value={cvc}
                    onChange={(e) => setCvc(e.target.value.replace(/\D/g, '').slice(0, 3))}
                    inputMode="numeric"
                    required
                    className={`${inputClass} font-mono`}
                  />
                </label>
              </div>

              <p className="mt-1 flex items-start gap-1.5 text-xs text-ink-800/50">
                <ShieldCheck size={14} className="mt-0.5 shrink-0 text-pine-500" />
                Test mode - this is a portfolio demo. No real card is charged and none of these
                details are sent anywhere; any values work.
              </p>

              <div className="mt-3 flex gap-3">
                <motion.button
                  {...tap}
                  transition={spring}
                  type="button"
                  onClick={onCancel}
                  className="flex-1 rounded-lg border border-ink-950/15 px-4 py-2.5 text-sm font-medium text-ink-900 hover:bg-ink-950/5"
                >
                  Cancel
                </motion.button>
                <motion.button
                  {...tap}
                  transition={spring}
                  type="submit"
                  disabled={submitting}
                  className="flex-1 rounded-lg bg-sunset-500 px-4 py-2.5 text-sm font-medium text-white hover:bg-sunset-600 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {submitting ? 'Processing...' : `Pay ${summary.amount} ${summary.currency}`}
                </motion.button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
