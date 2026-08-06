import { useEffect, useRef } from 'react';
import { MapPin } from 'lucide-react';
import { gsap } from '../lib/gsap';

const FEATURED_DESTINATIONS = [
  { city: 'Lisbon', country: 'Portugal', blurb: 'Pastel facades, river light, tram bells.' },
  { city: 'Porto', country: 'Portugal', blurb: 'Port wine cellars and a bridge by Eiffel.' },
  { city: 'Sao Paulo', country: 'Brazil', blurb: 'Skyline, food scene, non-stop energy.' },
  { city: 'New York', country: 'USA', blurb: 'The city that never sits down.' },
  { city: 'Madrid', country: 'Spain', blurb: 'Late dinners, wide boulevards, Prado art.' },
];

/**
 * The vertical scroll pins this section and drives a horizontal translate on the card track -
 * a signature move on premium travel sites, and a clean escape from "static grid of cards" which
 * is most of what read as dated here. Falls back to a normal (non-pinned) horizontally-scrollable
 * row on narrow viewports, where a pin+translate fight with the page's own vertical momentum.
 */
export function DestinationsGallery() {
  const sectionRef = useRef<HTMLElement>(null);
  const trackRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const section = sectionRef.current;
    const track = trackRef.current;
    if (!section || !track) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    if (window.innerWidth < 768) return;

    const ctx = gsap.context(() => {
      const distance = track.scrollWidth - section.clientWidth;
      if (distance <= 0) return;
      gsap.to(track, {
        x: -distance,
        ease: 'none',
        scrollTrigger: {
          trigger: section,
          start: 'top top',
          end: () => `+=${distance}`,
          scrub: 0.6,
          pin: true,
          invalidateOnRefresh: true,
        },
      });
    }, sectionRef);
    return () => ctx.revert();
  }, []);

  return (
    <section ref={sectionRef} className="bg-grain relative overflow-hidden bg-ink-950 py-24">
      <div className="mx-auto max-w-6xl px-6">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sand-50/50">
          Popular right now
        </p>
        <h2 className="mt-3 max-w-lg text-4xl font-medium tracking-tight text-sand-50 sm:text-5xl">
          Illustrative destinations from this demo's seeded inventory.
        </h2>
      </div>
      <div
        ref={trackRef}
        className="mt-14 flex w-max gap-5 overflow-x-auto px-6 pb-4 md:overflow-visible md:pb-0"
      >
        {FEATURED_DESTINATIONS.map((dest, i) => (
          <div
            key={dest.city}
            className="group relative h-[22rem] w-[19rem] shrink-0 overflow-hidden rounded-lg border border-sand-50/10 p-7 transition-colors hover:border-sunset-400/40"
            style={{
              background: `linear-gradient(160deg, hsl(${170 + i * 24} 35% 15%), hsl(${170 + i * 24} 48% 9%))`,
            }}
          >
            <div className="flex items-center gap-1.5 text-sand-50/50">
              <MapPin size={13} />
              <p className="text-xs">{dest.country}</p>
            </div>
            <h3 className="mt-4 font-display text-3xl text-sand-50">{dest.city}</h3>
            <p className="mt-4 text-sm leading-relaxed text-sand-50/70">{dest.blurb}</p>
            <span className="absolute bottom-7 left-7 font-display text-6xl font-medium text-sand-50/[0.06] transition-colors group-hover:text-sunset-400/10">
              {String(i + 1).padStart(2, '0')}
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}
