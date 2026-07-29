import { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';

/**
 * A commercial airliner built from primitive geometry (capsule fuselage, cone nose, swept wing
 * boxes, tail fin, engine cylinders, a ring of window dots) - not a single generic cube. No
 * external .glb asset: every source we checked for a free, directly-downloadable, properly
 * licensed commercial-jet model needed either an API key (Poly Pizza), a login (Sketchfab), or
 * wasn't the right subject (Kenney has no airliner kit), so this is a from-scratch stand-in with
 * real airplane proportions instead of risking an unverified download.
 */
export function AirplaneModel({ idle = true }: { idle?: boolean }) {
  const group = useRef<THREE.Group>(null);

  useFrame((state) => {
    if (!idle || !group.current) return;
    const t = state.clock.getElapsedTime();
    group.current.position.y = Math.sin(t * 0.6) * 0.12;
    group.current.rotation.z = Math.sin(t * 0.4) * 0.02;
  });

  const bodyMaterial = (
    <meshStandardMaterial color="#fbf7f1" metalness={0.35} roughness={0.35} />
  );
  const accentMaterial = <meshStandardMaterial color="#ff6b4a" metalness={0.3} roughness={0.4} />;
  const darkMaterial = <meshStandardMaterial color="#173a3f" metalness={0.5} roughness={0.3} />;
  const windowMaterial = (
    <meshStandardMaterial color="#0b1b1e" metalness={0.1} roughness={0.2} emissive="#173a3f" emissiveIntensity={0.15} />
  );

  const windowCount = 10;

  return (
    <group ref={group} rotation={[0, Math.PI * 0.15, 0]}>
      {/* Fuselage */}
      <mesh rotation={[0, 0, Math.PI / 2]} castShadow receiveShadow>
        <capsuleGeometry args={[0.42, 3.4, 8, 16]} />
        {bodyMaterial}
      </mesh>

      {/* Nose cone */}
      <mesh position={[2.1, 0, 0]} rotation={[0, 0, -Math.PI / 2]} castShadow>
        <coneGeometry args={[0.42, 0.9, 16]} />
        {bodyMaterial}
      </mesh>

      {/* Cockpit window band */}
      <mesh position={[1.9, 0.18, 0]} rotation={[0, 0, -Math.PI / 2]}>
        <cylinderGeometry args={[0.32, 0.32, 0.18, 16, 1, true, 0, Math.PI]} />
        {windowMaterial}
      </mesh>

      {/* Sunset accent stripe along the fuselage */}
      <mesh position={[0, -0.05, 0.4]} rotation={[0, 0, Math.PI / 2]}>
        <cylinderGeometry args={[0.435, 0.435, 3.2, 24, 1, true, -0.3, 0.25]} />
        {accentMaterial}
      </mesh>

      {/* Passenger windows */}
      {Array.from({ length: windowCount }).map((_, i) => (
        <mesh key={i} position={[1.3 - i * 0.33, 0.12, 0.4]} rotation={[Math.PI / 2, 0, 0]}>
          <circleGeometry args={[0.06, 12]} />
          {windowMaterial}
        </mesh>
      ))}

      {/* Wings (swept, tapered) */}
      {[1, -1].map((side) => (
        <mesh
          key={side}
          position={[-0.2, -0.05, side * 1.1]}
          rotation={[0, side > 0 ? -0.28 : 0.28, 0.06 * side]}
          castShadow
        >
          <boxGeometry args={[2.6, 0.08, 1.3]} />
          {darkMaterial}
        </mesh>
      ))}

      {/* Engines under the wings */}
      {[1, -1].map((side) => (
        <mesh key={side} position={[0.5, -0.42, side * 1.15]} rotation={[0, 0, Math.PI / 2]} castShadow>
          <cylinderGeometry args={[0.22, 0.22, 0.75, 16]} />
          {darkMaterial}
        </mesh>
      ))}

      {/* Horizontal stabilizers */}
      {[1, -1].map((side) => (
        <mesh key={side} position={[-1.85, 0.05, side * 0.45]} rotation={[0, side > 0 ? -0.2 : 0.2, 0]}>
          <boxGeometry args={[0.7, 0.06, 0.55]} />
          {darkMaterial}
        </mesh>
      ))}

      {/* Vertical tail fin */}
      <mesh position={[-1.85, 0.55, 0]} rotation={[0, 0, -0.12]}>
        <boxGeometry args={[0.7, 0.9, 0.08]} />
        {accentMaterial}
      </mesh>
    </group>
  );
}
