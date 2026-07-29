import { useEffect, useRef, useState } from 'react';
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react';

interface DatePickerProps {
  label: string;
  value: string; // yyyy-mm-dd, or '' for "any date"
  onChange: (value: string) => void;
  className?: string;
}

const WEEKDAYS = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

function toIso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatDisplay(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
}

/**
 * A hand-rolled calendar popover instead of the browser's native <input type="date"> - the native
 * control renders as unstyled OS chrome ("dd/mm/yyyy") that clashes with everything else on the
 * page, the single most "generic/unfinished" element flagged in the design audit. No date library
 * dependency needed: this is just month-grid arithmetic.
 */
export function DatePicker({ label, value, onChange, className }: DatePickerProps) {
  const [open, setOpen] = useState(false);
  const [viewMonth, setViewMonth] = useState(() => {
    const base = value ? new Date(value) : new Date();
    return new Date(base.getFullYear(), base.getMonth(), 1);
  });
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const year = viewMonth.getFullYear();
  const month = viewMonth.getMonth();
  const firstWeekday = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells: Array<{ date: Date; iso: string } | null> = [
    ...Array.from({ length: firstWeekday }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => {
      const date = new Date(year, month, i + 1);
      return { date, iso: toIso(date) };
    }),
  ];

  const today = toIso(new Date());

  return (
    <div ref={containerRef} className={`relative ${className ?? ''}`}>
      <label className="block text-sm font-medium text-ink-800">
        {label}
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          className="mt-1 flex w-full items-center gap-2 rounded-lg border border-ink-950/15 bg-white px-3 py-2 text-left text-ink-950 outline-none transition focus:border-pine-500 focus:ring-2 focus:ring-pine-500/20"
        >
          <CalendarDays size={15} className="shrink-0 text-ink-800/50" />
          <span className={value ? '' : 'text-ink-800/40'}>{value ? formatDisplay(value) : 'Any date'}</span>
        </button>
      </label>
      {open && (
        <div className="absolute z-20 mt-1 w-64 rounded-xl border border-ink-950/10 bg-white p-3 shadow-lg">
          <div className="flex items-center justify-between">
            <button
              type="button"
              onClick={() => setViewMonth(new Date(year, month - 1, 1))}
              className="rounded-md p-1 text-ink-800 hover:bg-ink-950/5"
              aria-label="Previous month"
            >
              <ChevronLeft size={16} />
            </button>
            <span className="text-sm font-medium text-ink-950">
              {viewMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
            </span>
            <button
              type="button"
              onClick={() => setViewMonth(new Date(year, month + 1, 1))}
              className="rounded-md p-1 text-ink-800 hover:bg-ink-950/5"
              aria-label="Next month"
            >
              <ChevronRight size={16} />
            </button>
          </div>
          <div className="mt-2 grid grid-cols-7 gap-1 text-center text-[11px] text-ink-800/50">
            {WEEKDAYS.map((w, i) => (
              <span key={i}>{w}</span>
            ))}
          </div>
          <div className="mt-1 grid grid-cols-7 gap-1">
            {cells.map((cell, i) =>
              cell ? (
                <button
                  key={cell.iso}
                  type="button"
                  onClick={() => {
                    onChange(cell.iso);
                    setOpen(false);
                  }}
                  className={`aspect-square rounded-md text-xs transition ${
                    cell.iso === value
                      ? 'bg-sunset-500 font-medium text-white'
                      : cell.iso === today
                        ? 'bg-ink-950/[0.06] font-medium text-ink-950'
                        : 'text-ink-900 hover:bg-ink-950/5'
                  }`}
                >
                  {cell.date.getDate()}
                </button>
              ) : (
                <span key={i} />
              ),
            )}
          </div>
          {value && (
            <button
              type="button"
              onClick={() => {
                onChange('');
                setOpen(false);
              }}
              className="mt-2 w-full rounded-md py-1 text-center text-xs text-ink-800/60 hover:bg-ink-950/5"
            >
              Clear date
            </button>
          )}
        </div>
      )}
    </div>
  );
}
