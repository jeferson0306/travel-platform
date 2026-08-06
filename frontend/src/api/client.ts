const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

/** Thrown when `fetch` itself fails (server unreachable, DNS, CORS) - never an HTTP error status. */
export class NetworkError extends Error {
  constructor() {
    super('Network request failed');
  }
}

/** Turns any error this module can throw into a short, non-technical sentence for a toast. */
export function friendlyErrorMessage(err: unknown): string {
  if (err instanceof NetworkError) {
    return "Can't reach the server. Check your connection and try again.";
  }
  if (err instanceof ApiError) {
    // NO_TOKEN/FORBIDDEN-by-missing-auth means the session itself is the problem - every other
    // 4xx code (INVALID_CREDENTIALS, VALIDATION_ERROR, CONFLICT, NOT_FOUND, role-based FORBIDDEN)
    // already carries a message the backend wrote for a human, see docs on the error taxonomy.
    if (err.code === 'NO_TOKEN') {
      return 'Your session expired - please log in again.';
    }
    if (err.status >= 500) {
      return 'Something went wrong on our end. Please try again in a moment.';
    }
    return err.message || 'That request could not be completed.';
  }
  return 'Something unexpected happened. Please try again.';
}

async function request<T>(
  path: string,
  options: { method?: string; body?: unknown; token?: string } = {},
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method || 'GET',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new NetworkError();
  }

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ error: 'UNKNOWN', message: response.statusText }));
    throw new ApiError(response.status, errorBody.error ?? 'UNKNOWN', errorBody.message ?? response.statusText);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface RegisterResponse {
  userId: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
}

export interface Flight {
  id: string;
  origin: string;
  destination: string;
  departureAt: string;
  arrivalAt: string;
  priceAmount: number;
  priceCurrency: string;
  availableSeats: number;
  airline: string;
  airlineCode: string;
  flightNumber: string;
  cabinClass: string | null;
  stops: number;
}

export interface Hotel {
  id: string;
  name: string;
  city: string;
  pricePerNightAmount: number;
  pricePerNightCurrency: string;
  availableRooms: number;
  address: string | null;
  starRating: number;
  amenities: string[];
  description: string | null;
  reviewScore: number | null;
  reviewCount: number;
}

export interface CreateBookingRequest {
  travelerEmail: string;
  itemType: 'FLIGHT' | 'HOTEL';
  itemId: string;
  quantity: number;
  amount: number;
  currency: string;
  itemSummary?: string;
}

export interface CreateBookingResponse {
  bookingId: string;
}

export interface Booking {
  id: string;
  itemType: 'FLIGHT' | 'HOTEL';
  itemId: string;
  quantity: number;
  amount: number;
  currency: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
  itemSummary: string | null;
}

export const api = {
  register: (body: RegisterRequest) => request<RegisterResponse>('/api/v1/auth/register', { method: 'POST', body }),
  login: (body: LoginRequest) => request<LoginResponse>('/api/v1/auth/login', { method: 'POST', body }),
  searchFlights: (origin: string, destination: string, departureDate?: string) => {
    const params = new URLSearchParams({ origin, destination });
    if (departureDate) params.set('departureDate', departureDate);
    return request<Flight[]>(`/api/v1/flights?${params.toString()}`);
  },
  searchHotels: (city: string) => request<Hotel[]>(`/api/v1/hotels?city=${encodeURIComponent(city)}`),
  createBooking: (body: CreateBookingRequest, token: string) =>
    request<CreateBookingResponse>('/api/v1/bookings', { method: 'POST', body, token }),
  listBookings: (token: string) => request<Booking[]>('/api/v1/bookings', { token }),
};
