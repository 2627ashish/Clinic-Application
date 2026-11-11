import { useState } from 'react';
import PatientLandingPage from './pages/PatientLandingPage';
import PatientPortal from './pages/PatientPortal';
import DoctorPortal from './pages/DoctorPortal';
import Navbar from './components/Navbar';
import type { ClinicPage } from './components/Navbar';

export default function App() {
  const [page, setPage] = useState<ClinicPage>('patient');

  return (
    <div className="min-h-screen bg-clinic-paper">
      <Navbar activePage={page} onNavigate={setPage} />
      {page === 'patient' ? <PatientLandingPage onNavigate={setPage} />
        : page === 'patientPortal' ? <PatientPortal onNavigate={setPage} />
          : <DoctorPortal />}
    </div>
  );
}
