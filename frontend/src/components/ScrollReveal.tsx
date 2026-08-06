import { useEffect, useRef, type ReactNode } from 'react';
import { gsap } from '../lib/gsap';

// Importing the module registers the ScrollTrigger plugin as a side effect (see lib/gsap.ts) -
// gsap.fromTo's `scrollTrigger` option below needs the plugin to be registered, not the export.

interface ScrollRevealProps {
  children: ReactNode;
  className?: string;
  y?: number;
  stagger?: number;
}

/**
 * GSAP + ScrollTrigger instead of Framer Motion's `whileInView` - the latter got stuck mid-
 * transition in this codebase's automated verification (opacity frozen around 0.06, never
 * reaching 1) and there was no reliable fix short of dropping the reveal entirely. GSAP's
 * ScrollTrigger is the dev-standards-recommended tool for this exact case and doesn't set the
 * hidden state until its own JS runs, so a page where the animation never fires still renders
 * visible content instead of stuck-invisible content.
 */
export function ScrollReveal({ children, className, y = 28, stagger }: ScrollRevealProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const targets = stagger ? Array.from(el.children) : el;
    const ctx = gsap.context(() => {
      gsap.fromTo(
        targets,
        { opacity: 0, y },
        {
          opacity: 1,
          y: 0,
          duration: 0.7,
          ease: 'power3.out',
          stagger: stagger ?? 0,
          scrollTrigger: {
            trigger: el,
            start: 'top 82%',
            once: true,
          },
        },
      );
    }, ref);
    return () => ctx.revert();
  }, [y, stagger]);

  return (
    <div ref={ref} className={className}>
      {children}
    </div>
  );
}
