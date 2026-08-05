import { useEffect, useRef, useState } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, ContactShadows, PerspectiveCamera, Sparkles } from '@react-three/drei';
import type { Group } from 'three';
import { AirplaneModel } from './AirplaneModel';
import { gsap, ScrollTrigger } from '../../lib/gsap';

function prefersReducedMotion(): boolean {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/** Scroll-driven entrance: the plane banks in from far off-screen, small and rotated like it's
 * approaching from a distance, growing and leveling out as this section crosses the viewport -
 * then stays draggable (OrbitControls) for the user to spin it and look at it from any angle.
 * A single gsap.timeline (not separate fromTo calls) so position/rotation/scale stay in lockstep
 * on the same scrub, and the bank overshoots slightly past level before settling for a bit of
 * life instead of a flat linear slide. Skips the choreography under prefers-reduced-motion. */
function ScrollRig({ children }: { children: React.ReactNode }) {
  const rigRef = useRef<Group>(null);
  const sectionEl = typeof document !== 'undefined' ? document.getElementById('airplane-3d-section') : null;

  useEffect(() => {
    if (!rigRef.current || !sectionEl) return;
    if (prefersReducedMotion()) return;

    const ctx = gsap.context(() => {
      const rig = rigRef.current!;
      gsap.set(rig.position, { x: -5, y: 0.6, z: -3.5 });
      gsap.set(rig.rotation, { y: -1.3, z: 0.5 });
      gsap.set(rig.scale, { x: 0.55, y: 0.55, z: 0.55 });

      const tl = gsap.timeline({
        scrollTrigger: {
          trigger: sectionEl,
          start: 'top 85%',
          end: 'top 25%',
          scrub: 0.7,
        },
      });

      tl.to(rig.position, { x: 0.4, y: 0.6, z: 0, ease: 'power1.in' }, 0)
        .to(rig.position, { x: 0, y: 0, z: 0, ease: 'power2.out' }, 0.55)
        .to(rig.rotation, { y: -0.15, z: -0.12, ease: 'power1.in' }, 0)
        .to(rig.rotation, { y: 0, z: 0, ease: 'back.out(1.4)' }, 0.55)
        .to(rig.scale, { x: 1, y: 1, z: 1, ease: 'power1.out' }, 0);
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
        <ambientLight intensity={0.45} />
        <directionalLight
          position={[4, 5, 3]}
          intensity={1.5}
          color="#ff8a66"
          castShadow
          shadow-mapSize={[1024, 1024]}
        />
        <directionalLight position={[-4, 2, -3]} intensity={0.55} color="#4aa494" />
        {/* Cool rim light from behind to separate the plane's silhouette from the dark background */}
        <pointLight position={[-2, 1, -4]} intensity={12} color="#7fd8c8" distance={9} decay={2} />
        <Sparkles count={60} scale={[10, 4, 6]} size={2} speed={0.15} opacity={0.25} color="#ff8a66" />
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
