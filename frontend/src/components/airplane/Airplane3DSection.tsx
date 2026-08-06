import { useEffect, useRef, useState } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, ContactShadows, PerspectiveCamera, Environment, Lightformer } from '@react-three/drei';
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
        {/* Subtle rim light from behind to separate the plane's silhouette from the dark
            background - branded pine tone, low intensity, not a glowing neon edge. */}
        <pointLight position={[-2, 1, -4]} intensity={5} color="#2c8577" distance={9} decay={2} />
        {/* Environment reflections are what sell the clearcoat paint materials below - without
            one, clearcoat has nothing to reflect and reads flat/plasticky. A `preset` (e.g.
            "city") downloads an HDR from a remote CDN and runs an expensive PMREM pass to
            process it - that combination crashed the WebGL context entirely on real hardware
            (confirmed via "THREE.WebGLRenderer: Context Lost" in the console, canvas going
            blank). These <Lightformer> children generate a small environment map procedurally,
            entirely on-GPU, no network fetch - same reflective effect, none of the crash risk. */}
        <Environment resolution={64} background={false}>
          <Lightformer intensity={2} color="#fbf7f1" position={[0, 4, -4]} scale={[6, 3, 1]} />
          <Lightformer intensity={1} color="#4aa494" position={[-4, 1, 2]} scale={[3, 2, 1]} />
          <Lightformer intensity={1.2} color="#ff8a66" position={[4, -1, 3]} scale={[3, 2, 1]} />
        </Environment>
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
