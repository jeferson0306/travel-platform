import { createContext, useContext, useState, type ReactNode } from 'react';
import { decodeUserId } from '../api/jwt';

interface AuthState {
  token: string | null;
  userId: string | null;
  email: string | null;
}

interface AuthContextValue extends AuthState {
  login: (token: string, email: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = 'travel-platform-demo-token';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return { token: null, userId: null, email: null };
    try {
      const parsed = JSON.parse(stored) as { token: string; email: string };
      return { token: parsed.token, userId: decodeUserId(parsed.token), email: parsed.email };
    } catch {
      return { token: null, userId: null, email: null };
    }
  });

  const login = (token: string, email: string) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, email }));
    setState({ token, userId: decodeUserId(token), email });
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_KEY);
    setState({ token: null, userId: null, email: null });
  };

  return <AuthContext.Provider value={{ ...state, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
