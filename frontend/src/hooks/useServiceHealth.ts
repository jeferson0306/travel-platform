import { useEffect, useRef, useState } from 'react';
import type { ServiceDef } from '../config/services';

export type HealthState = 'checking' | 'up' | 'down';

export interface CheckResult {
  name: string;
  status: HealthState;
}

export interface ServiceHealth {
  state: HealthState;
  checks: CheckResult[];
  latencyMs: number | null;
  lastChecked: Date | null;
}

interface SmallRyeHealthResponse {
  status: 'UP' | 'DOWN';
  checks: { name: string; status: 'UP' | 'DOWN' }[];
}

/** Polls a service's SmallRye `/health` endpoint directly from the browser (no server-side
 * proxy) so what's on screen is exactly what the service itself is reporting right now - not a
 * cached or synthetic status. Each service call is independent, so one unreachable service
 * doesn't block the others from reporting. */
export function useServiceHealth(service: ServiceDef, intervalMs = 15000): ServiceHealth {
  const [health, setHealth] = useState<ServiceHealth>({
    state: 'checking',
    checks: [],
    latencyMs: null,
    lastChecked: null,
  });
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;

    async function check() {
      const start = performance.now();
      try {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 5000);
        const res = await fetch(`${service.baseUrl}/health`, { signal: controller.signal });
        clearTimeout(timeout);
        const latencyMs = Math.round(performance.now() - start);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data: SmallRyeHealthResponse = await res.json();
        if (!mounted.current) return;
        setHealth({
          state: data.status === 'UP' ? 'up' : 'down',
          checks: data.checks.map((c) => ({
            name: c.name,
            status: c.status === 'UP' ? 'up' : 'down',
          })),
          latencyMs,
          lastChecked: new Date(),
        });
      } catch {
        if (!mounted.current) return;
        setHealth((prev) => ({
          state: 'down',
          checks: prev.checks,
          latencyMs: null,
          lastChecked: new Date(),
        }));
      }
    }

    check();
    const id = setInterval(check, intervalMs);
    return () => {
      mounted.current = false;
      clearInterval(id);
    };
  }, [service.baseUrl, intervalMs]);

  return health;
}
