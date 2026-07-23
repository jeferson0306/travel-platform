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

async function request<T>(
  path: string,
  options: { method?: string; body?: unknown; token?: string } = {},
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

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
}

export interface Hotel {
  id: string;
  name: string;
  city: string;
  pricePerNightAmount: number;
  pricePerNightCurrency: string;
  availableRooms: number;
}

export interface CreateBookingRequest {
  travelerId: string;
  travelerEmail: string;
  itemType: 'FLIGHT' | 'HOTEL';
  itemId: string;
  quantity: number;
  amount: number;
  currency: string;
}

export interface CreateBookingResponse {
  bookingId: string;
}

export const api = {
  register: (body: RegisterRequest) => request<RegisterResponse>('/api/v1/auth/register', { method: 'POST', body }),
  login: (body: LoginRequest) => request<LoginResponse>('/api/v1/auth/login', { method: 'POST', body }),
  searchFlights: (origin: string, destination: string) =>
    request<Flight[]>(`/api/v1/flights?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`),
  searchHotels: (city: string) => request<Hotel[]>(`/api/v1/hotels?city=${encodeURIComponent(city)}`),
  createBooking: (body: CreateBookingRequest, token: string) =>
    request<CreateBookingResponse>('/api/v1/bookings', { method: 'POST', body, token }),
};
