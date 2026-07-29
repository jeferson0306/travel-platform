import { useEffect, useRef, useState } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, ContactShadows, PerspectiveCamera } from '@react-three/drei';
import type { Group } from 'three';
import { AirplaneModel } from './AirplaneModel';
import { gsap, ScrollTrigger } from '../../lib/gsap';

function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/** Scroll-driven entrance: the plane flies in from the side and settles into view as this
 * section crosses the viewport, then stays draggable (OrbitControls) for the user to spin it
 * and look at it from any angle - the "ir e sair" (fly in/out), "visualizar o avião mesmo"
 * moment. Skips the scroll choreography (settles immediately) under prefers-reduced-motion. */
function ScrollRig({ children }: { children: React.ReactNode }) {
  const rigRef = useRef<Group>(null);
  const sectionEl = typeof document !== 'undefined' ? document.getElementById('airplane-3d-section') : null;

  useEffect(() => {
    if (!rigRef.current || !sectionEl) return;
    if (prefersReducedMotion()) return;

    const ctx = gsap.context(() => {
      gsap.fromTo(
        rigRef.current!.position,
        { x: -3.5, z: -2 },
        {
          x: 0,
          z: 0,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: sectionEl,
            start: 'top 80%',
            end: 'top 30%',
            scrub: 0.6,
          },
        },
      );
      gsap.fromTo(
        rigRef.current!.rotation,
        { y: -1.2 },
        {
          y: 0,
          ease: 'power2.out',
          scrollTrigger: {
            trigger: sectionEl,
            start: 'top 80%',
            end: 'top 30%',
            scrub: 0.6,
          },
        },
      );
    });
    return () => ctx.revert();
  }, [sectionEl]);

  return <group ref={rigRef}>{children}</group>;
}

export default function Airplane3DSection() {
  const [reduced, setReduced] = useState(false);

  useEffect(() => {
    setReduced(prefersReducedMotion());
    return () => {
      // Scene unmounting mid-scroll (route change) can leave a stale pin/measurement behind.
      ScrollTrigger.getAll().forEach((t) => t.trigger === document.getElementById('airplane-3d-section') && t.kill());
    };
  }, []);

  return (
    <section
      id="airplane-3d-section"
      className="bg-grain relative h-[70vh] min-h-[420px] overflow-hidden bg-ink-950"
    >
      <div className="pointer-events-none absolute inset-x-0 top-8 z-10 mx-auto max-w-6xl px-6 text-center">
        <p className="text-xs font-medium uppercase tracking-widest text-sand-50/50">
          Drag to look around
        </p>
        <h2 className="mt-2 font-display text-3xl text-sand-50 sm:text-4xl">
          Every seat, every angle
        </h2>
      </div>
      <Canvas shadows dpr={[1, 1.5]} gl={{ antialias: true }}>
        <PerspectiveCamera makeDefault position={[0, 0.6, 6.5]} fov={38} />
        <ambientLight intensity={0.5} />
        <directionalLight
          position={[4, 5, 3]}
          intensity={1.4}
          color="#ff8a66"
          castShadow
          shadow-mapSize={[1024, 1024]}
        />
        <directionalLight position={[-4, 2, -3]} intensity={0.5} color="#4aa494" />
        <ScrollRig>
          <AirplaneModel idle={!reduced} />
        </ScrollRig>
        <ContactShadows position={[0, -1.1, 0]} opacity={0.4} scale={10} blur={2.5} far={2} />
        <OrbitControls
          enablePan={false}
          enableZoom={false}
          autoRotate={!reduced}
          autoRotateSpeed={0.6}
          minPolarAngle={Math.PI / 3}
          maxPolarAngle={Math.PI / 1.7}
          enableDamping
          dampingFactor={0.08}
        />
      </Canvas>
    </section>
  );
}
