import { Routes, Route, useLocation } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { LandingPage } from './pages/LandingPage';
import { RegisterPage } from './pages/RegisterPage';
import { LoginPage } from './pages/LoginPage';
import { SearchPage } from './pages/SearchPage';
import { BookingConfirmationPage } from './pages/BookingConfirmationPage';
import { MyBookingsPage } from './pages/MyBookingsPage';
import { ProtectedRoute } from './components/ProtectedRoute';

export default function App() {
  const location = useLocation();
  return (
    // Subtle cross-fade between routes (no layout-shifting properties - only opacity/y) instead
    // of an instant flash between pages. <Routes> MUST get an explicit `location` prop here -
    // without it, the exiting motion.div's <Routes> stays "live" and re-renders against the
    // *current* (new) location on any re-render tick, so both the exiting and entering divs end
    // up showing the same new page (confirmed by inspecting both nodes' content while debugging).
    // Deliberately NOT `mode="wait"` - that combination breaks when a route redirects
    // synchronously (e.g. ProtectedRoute's <Navigate> when logged out fires a second location
    // change before the first exit animation resolves, leaving AnimatePresence stuck forever).
    <AnimatePresence initial={false}>
      <motion.div
        key={location.pathname}
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.18, ease: [0.25, 0.46, 0.45, 0.94] }}
      >
        <Routes location={location}>
          <Route path="/" element={<LandingPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/search"
            element={
              <ProtectedRoute>
                <SearchPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/bookings"
            element={
              <ProtectedRoute>
                <MyBookingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/booking/:bookingId"
            element={
              <ProtectedRoute>
                <BookingConfirmationPage />
              </ProtectedRoute>
            }
          />
        </Routes>
      </motion.div>
    </AnimatePresence>
  );
}
