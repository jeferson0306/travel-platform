import { useEffect, useRef } from 'react';
import { gsap, ScrollTrigger } from '../lib/gsap';

interface CountUpProps {
  value: number;
  suffix?: string;
  className?: string;
}

export function CountUp({ value, suffix = '', className }: CountUpProps) {
  const ref = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const counter = { n: 0 };
    const trigger = ScrollTrigger.create({
      trigger: el,
      start: 'top 85%',
      once: true,
      onEnter: () => {
        gsap.to(counter, {
          n: value,
          duration: 1.4,
          ease: 'power3.out',
          onUpdate: () => {
            el.textContent = Math.round(counter.n).toString() + suffix;
          },
        });
      },
    });
    return () => trigger.kill();
  }, [value, suffix]);

  return (
    <span ref={ref} className={className}>
      0{suffix}
    </span>
  );
}
