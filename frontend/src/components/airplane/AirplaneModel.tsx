import { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';

/**
 * A commercial airliner built from primitive/procedural geometry - no external .glb asset (see
 * Airplane3DSection.tsx's own note on why: every free source we checked needed an API key, a
 * login, or wasn't the right subject). The fuselage is a single THREE.LatheGeometry revolved from
 * a hand-tuned nose-to-tail radius profile (smooth continuous taper, not a capsule+cone stack),
 * and the wings/stabilizers are two tapered segments each instead of a uniform box, so the
 * silhouette actually reads as an airliner instead of a toy.
 */

const FUSELAGE_PROFILE: [number, number][] = [
  [0.001, -1.95],
  [0.16, -1.75],
  [0.34, -1.35],
  [0.42, -0.95],
  [0.43, 0.9],
  [0.4, 1.3],
  [0.3, 1.65],
  [0.14, 1.95],
  [0.001, 2.15],
];

function fuselagePoints() {
  return FUSELAGE_PROFILE.map(([radius, y]) => new THREE.Vector2(radius, y));
}

export function AirplaneModel({ idle = true }: { idle?: boolean }) {
  const group = useRef<THREE.Group>(null);

  useFrame((state) => {
    if (!idle || !group.current) return;
    const t = state.clock.getElapsedTime();
    group.current.position.y = Math.sin(t * 0.6) * 0.12;
    group.current.rotation.z = Math.sin(t * 0.4) * 0.025;
    group.current.rotation.x = Math.sin(t * 0.35 + 1.4) * 0.012;
  });

  const bodyMaterial = (
    <meshStandardMaterial color="#fbf7f1" metalness={0.4} roughness={0.28} />
  );
  const bellyMaterial = <meshStandardMaterial color="#e8ddc9" metalness={0.35} roughness={0.35} />;
  const accentMaterial = <meshStandardMaterial color="#ff6b4a" metalness={0.3} roughness={0.4} />;
  const darkMaterial = <meshStandardMaterial color="#173a3f" metalness={0.55} roughness={0.28} />;
  const windowMaterial = (
    <meshStandardMaterial color="#0b1b1e" metalness={0.1} roughness={0.2} emissive="#173a3f" emissiveIntensity={0.15} />
  );

  const windowCount = 11;

  return (
    <group ref={group} rotation={[0, Math.PI * 0.15, 0]}>
      {/* Fuselage - single revolved profile, nose tip at +x, tail tip at -x */}
      <mesh rotation={[0, 0, -Math.PI / 2]} castShadow receiveShadow>
        <latheGeometry args={[fuselagePoints(), 28]} />
        {bodyMaterial}
      </mesh>

      {/* Darker belly wedge (bottom half only) for a two-tone livery read */}
      <mesh position={[0, -0.02, 0]} rotation={[0, 0, -Math.PI / 2 + Math.PI]} scale={[1, 0.985, 1]}>
        <latheGeometry args={[fuselagePoints(), 28, Math.PI, Math.PI]} />
        {bellyMaterial}
      </mesh>

      {/* Cockpit window band */}
      <mesh position={[1.85, 0.16, 0]} rotation={[0, 0, -Math.PI / 2]}>
        <cylinderGeometry args={[0.33, 0.3, 0.16, 16, 1, true, 0, Math.PI]} />
        {windowMaterial}
      </mesh>

      {/* Sunset accent stripe along the fuselage's constant-radius section */}
      <mesh position={[0, -0.02, 0]} rotation={[0, 0, Math.PI / 2]}>
        <cylinderGeometry args={[0.445, 0.44, 2.1, 24, 1, true, -0.32, 0.28]} />
        {accentMaterial}
      </mesh>

      {/* Passenger windows */}
      {Array.from({ length: windowCount }).map((_, i) => (
        <mesh key={i} position={[1.35 - i * 0.29, 0.14, 0.43]} rotation={[Math.PI / 2, 0, 0]}>
          <circleGeometry args={[0.055, 12]} />
          {windowMaterial}
        </mesh>
      ))}

      {/* Wings - two tapered segments (root + tip) per side, swept, plus an upturned winglet */}
      {[1, -1].map((side) => (
        <group key={side} position={[-0.1, -0.08, side * 0.55]} rotation={[0, side > 0 ? -0.24 : 0.24, 0]}>
          <mesh position={[0, 0, side * 0.7]} castShadow>
            <boxGeometry args={[1.7, 0.09, 1.15]} />
            {darkMaterial}
          </mesh>
          <mesh position={[-0.3, 0, side * 1.75]} castShadow>
            <boxGeometry args={[1.15, 0.07, 1.0]} />
            {darkMaterial}
          </mesh>
          <mesh position={[-0.55, 0.28, side * 2.25]} rotation={[0, 0, side > 0 ? -1.15 : 1.15]}>
            <boxGeometry args={[0.5, 0.5, 0.05]} />
            {accentMaterial}
          </mesh>
        </group>
      ))}

      {/* Engines - nacelle with a front intake ring, under each wing */}
      {[1, -1].map((side) => (
        <group key={side} position={[0.55, -0.44, side * 1.15]} rotation={[0, 0, Math.PI / 2]}>
          <mesh castShadow>
            <cylinderGeometry args={[0.23, 0.21, 0.8, 16]} />
            {darkMaterial}
          </mesh>
          <mesh position={[0, 0.42, 0]}>
            <torusGeometry args={[0.23, 0.03, 8, 20]} />
            {bodyMaterial}
          </mesh>
        </group>
      ))}

      {/* Horizontal stabilizers - tapered */}
      {[1, -1].map((side) => (
        <mesh key={side} position={[-1.7, 0.08, side * 0.4]} rotation={[0, side > 0 ? -0.22 : 0.22, 0]}>
          <boxGeometry args={[0.6, 0.06, 0.5]} />
          {darkMaterial}
        </mesh>
      ))}

      {/* Vertical tail fin */}
      <mesh position={[-1.7, 0.5, 0]} rotation={[0, 0, -0.1]}>
        <boxGeometry args={[0.6, 0.85, 0.07]} />
        {accentMaterial}
      </mesh>
    </group>
  );
}
