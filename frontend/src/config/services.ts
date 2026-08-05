export interface ServiceDependency {
  /** Substring matched against a SmallRye health check's `name` field. */
  match: string;
  label: string;
}

export interface ServiceDef {
  id: string;
  name: string;
  description: string;
  /** Base URL of the service's own port - health checks are read directly, not via the gateway,
   * so a gateway outage doesn't take down the whole status board with it. */
  baseUrl: string;
  dependencies: ServiceDependency[];
}

function envUrl(key: string, fallback: string): string {
  return (import.meta.env[key] as string | undefined) || fallback;
}

/** One entry per deployable backend unit. Base URLs default to the local docker-compose ports
 * (see infrastructure/docker/docker-compose.yml) and can be overridden per-service once each one
 * gets a public Render URL, without needing a single shared gateway origin. */
export const SERVICES: ServiceDef[] = [
  {
    id: 'gateway',
    name: 'API Gateway',
    description: 'Reverse proxy, JWT validation, rate limiting, CORS',
    baseUrl: envUrl('VITE_GATEWAY_HEALTH_URL', 'http://localhost:8080'),
    dependencies: [{ match: 'Redis', label: 'Redis' }],
  },
  {
    id: 'identity',
    name: 'Identity Service',
    description: 'Auth, registration, JWT issuing',
    baseUrl: envUrl('VITE_IDENTITY_HEALTH_URL', 'http://localhost:8081'),
    dependencies: [{ match: 'MongoDB', label: 'MongoDB' }],
  },
  {
    id: 'booking',
    name: 'Booking Service',
    description: 'Orders, outbox pattern, S3 receipt upload',
    baseUrl: envUrl('VITE_BOOKING_HEALTH_URL', 'http://localhost:8082'),
    dependencies: [
      { match: 'MongoDB', label: 'MongoDB' },
      { match: 'Reactive Messaging', label: 'Kafka' },
    ],
  },
  {
    id: 'flight',
    name: 'Flight Service',
    description: 'Flight inventory, JWT-verified writes',
    baseUrl: envUrl('VITE_FLIGHT_HEALTH_URL', 'http://localhost:18083'),
    dependencies: [
      { match: 'MongoDB', label: 'MongoDB' },
      { match: 'Reactive Messaging', label: 'Kafka' },
    ],
  },
  {
    id: 'hotel',
    name: 'Hotel Service',
    description: 'Hotel inventory, JWT-verified writes',
    baseUrl: envUrl('VITE_HOTEL_HEALTH_URL', 'http://localhost:8084'),
    dependencies: [
      { match: 'MongoDB', label: 'MongoDB' },
      { match: 'Reactive Messaging', label: 'Kafka' },
    ],
  },
  {
    id: 'payment',
    name: 'Payment Service',
    description: 'Authorizes/declines bookings via events',
    baseUrl: envUrl('VITE_PAYMENT_HEALTH_URL', 'http://localhost:8085'),
    dependencies: [
      { match: 'MongoDB', label: 'MongoDB' },
      { match: 'Reactive Messaging', label: 'Kafka' },
    ],
  },
  {
    id: 'notification',
    name: 'Notification Service',
    description: 'Sends confirmation emails on booking events',
    baseUrl: envUrl('VITE_NOTIFICATION_HEALTH_URL', 'http://localhost:8086'),
    dependencies: [
      { match: 'MongoDB', label: 'MongoDB' },
      { match: 'Reactive Messaging', label: 'Kafka' },
    ],
  },
  {
    id: 'search',
    name: 'Search Service',
    description: 'OpenSearch index, kept in sync via Kafka',
    baseUrl: envUrl('VITE_SEARCH_HEALTH_URL', 'http://localhost:8087'),
    dependencies: [{ match: 'Reactive Messaging', label: 'Kafka' }],
  },
  {
    id: 'assistant',
    name: 'Assistant Service',
    description: 'LLM-backed trip Q&A (Ollama)',
    baseUrl: envUrl('VITE_ASSISTANT_HEALTH_URL', 'http://localhost:8088'),
    dependencies: [],
  },
];
