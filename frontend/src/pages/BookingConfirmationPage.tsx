import { useParams, Link } from 'react-router-dom';

export function BookingConfirmationPage() {
  const { bookingId } = useParams<{ bookingId: string }>();

  return (
    <div className="confirmation-page">
      <h1>Booking confirmed</h1>
      <p>
        Thanks! Your booking (reference <code>{bookingId}</code>) was received. Payment
        processing and a confirmation email happen automatically in the background - you don't
        need to do anything else.
      </p>
      <Link to="/search">Back to search</Link>
      <details>
        <summary>Technical details</summary>
        <p>
          This triggers the platform&apos;s real choreography saga: payment authorization,
          booking confirmation, and a confirmation notification - all asynchronous, via Kafka,
          exactly as documented in{' '}
          <a href="https://github.com/jeferson0306/travel-platform/blob/main/docs/adr/0010-payment-saga.md">
            ADR 0010
          </a>
          . This demo has no booking-status endpoint yet (a documented platform gap), so there is
          nothing to poll here.
        </p>
      </details>
    </div>
  );
}
