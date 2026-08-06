import { useEffect, useRef } from 'react';
import { gsap } from '../lib/gsap';

const COLORS = ['#f97316', '#0d9488', '#fbbf24', '#1e293b', '#ffffff'];

interface Particle {
  x: number;
  y: number;
  rotation: number;
  size: number;
  color: string;
}

/**
 * A one-shot canvas confetti burst for genuine "this succeeded" moments (booking confirmed) -
 * per ui-ux-pro-max guidance, celebrations should be a real animated payoff, not just a text
 * message. Plain canvas + GSAP (both already dependencies) instead of adding canvas-confetti,
 * to keep this a few KB of app code rather than a new package. Runs once on mount, cleans
 * itself up, and is a no-op under prefers-reduced-motion.
 */
export function ConfettiBurst() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!canvas || !ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const { innerWidth: width, innerHeight: height } = window;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
    ctx.scale(dpr, dpr);

    const originX = width / 2;
    const particles: Particle[] = Array.from({ length: 60 }, () => ({
      x: originX + (Math.random() - 0.5) * 60,
      y: height * 0.22,
      rotation: Math.random() * 360,
      size: 5 + Math.random() * 5,
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
    }));

    const state = particles.map((p) => ({ ...p, opacity: 1 }));

    function draw() {
      ctx!.clearRect(0, 0, width, height);
      for (const p of state) {
        ctx!.save();
        ctx!.globalAlpha = Math.max(p.opacity, 0);
        ctx!.translate(p.x, p.y);
        ctx!.rotate((p.rotation * Math.PI) / 180);
        ctx!.fillStyle = p.color;
        ctx!.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
        ctx!.restore();
      }
    }

    const tweens = state.map((p) =>
      gsap.to(p, {
        x: p.x + (Math.random() - 0.5) * 260,
        y: p.y + height * (0.55 + Math.random() * 0.25),
        rotation: p.rotation + (Math.random() > 0.5 ? 1 : -1) * (360 + Math.random() * 360),
        opacity: 0,
        duration: 1.3 + Math.random() * 0.5,
        ease: 'power2.out',
        onUpdate: draw,
      }),
    );

    return () => {
      tweens.forEach((t) => t.kill());
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      aria-hidden
      className="pointer-events-none fixed inset-0 z-50"
    />
  );
}
