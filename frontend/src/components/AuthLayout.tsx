import { type ReactNode } from 'react';
import { motion } from 'framer-motion';
import { SiteHeader } from './SiteHeader';

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="min-h-screen">
      <SiteHeader />
      <div className="mx-auto flex max-w-md flex-col px-6 py-20">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="rounded-2xl border border-ink-950/10 bg-white/80 p-8 shadow-lg shadow-ink-950/5"
        >
          <h1 className="text-2xl font-medium text-ink-950">{title}</h1>
          {children}
        </motion.div>
      </div>
    </div>
  );
}
