const currencyFormatterCache = new Map<string, Intl.NumberFormat>();

function currencyFormatter(currency: string): Intl.NumberFormat {
  let formatter = currencyFormatterCache.get(currency);
  if (!formatter) {
    formatter = new Intl.NumberFormat('en-US', { style: 'currency', currency });
    currencyFormatterCache.set(currency, formatter);
  }
  return formatter;
}

export function formatMoney(amount: number, currency: string): string {
  try {
    return currencyFormatter(currency).format(amount);
  } catch {
    // Unknown/malformed currency code - fall back to a plain number rather than throwing.
    return `${amount.toFixed(2)} ${currency}`;
  }
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
}

export function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
}

/** e.g. "8h 30m" - flight-service only stores departure/arrival instants, so duration is always derived, never stored. */
export function formatDuration(departureIso: string, arrivalIso: string): string {
  const minutes = Math.max(
    0,
    Math.round((new Date(arrivalIso).getTime() - new Date(departureIso).getTime()) / 60000),
  );
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hours === 0) return `${mins}m`;
  if (mins === 0) return `${hours}h`;
  return `${hours}h ${mins}m`;
}
