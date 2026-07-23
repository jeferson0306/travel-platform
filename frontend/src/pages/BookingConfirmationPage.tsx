import { useParams, Link } from 'react-router-dom';

export function BookingConfirmationPage() {
  const { bookingId } = useParams<{ bookingId: string }>();

  return (
    <div className="confirmation-page">
      <h1>Booking created</h1>
      <p>
        Booking <code>{bookingId}</code> was created.
      </p>
      <p>
        Behind the scenes, this triggers the platform&apos;s real choreography saga: payment
        authorization, booking confirmation, and a confirmation notification - all asynchronous,
        via Kafka, exactly as documented in{' '}
        <a href="https://github.com/jeferson0306/travel-platform/blob/main/docs/adr/0010-payment-saga.md">
          ADR 0010
        </a>
        . This demo has no booking-status endpoint (the platform itself doesn&apos;t expose one
        yet - a documented gap), so there is nothing to poll here.
      </p>
      <Link to="/search">Back to search</Link>
    </div>
  );
}
