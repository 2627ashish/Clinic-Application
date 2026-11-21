import { useEffect } from 'react';

const INACTIVITY_LIMIT_MS = 30 * 60 * 1000;
const ACTIVITY_EVENTS: (keyof WindowEventMap)[] = ['mousemove', 'keydown', 'touchstart', 'scroll'];

export default function useInactivityLogout(enabled: boolean): void {
  useEffect(() => {
    if (!enabled) return undefined;
    let timer = 0;
    const resetTimer = () => {
      window.clearTimeout(timer);
      timer = window.setTimeout(() => {
        localStorage.removeItem('doctor_auth_token');
        localStorage.removeItem('doctor_password_change_required');
        window.alert('Session expired due to 30 minutes of inactivity. Please log in again.');
        window.location.assign('/doctor/login');
      }, INACTIVITY_LIMIT_MS);
    };

    ACTIVITY_EVENTS.forEach((eventName) => {
      window.addEventListener(eventName, resetTimer, { passive: true });
    });
    resetTimer();
    return () => {
      window.clearTimeout(timer);
      ACTIVITY_EVENTS.forEach((eventName) => window.removeEventListener(eventName, resetTimer));
    };
  }, [enabled]);
}
