import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import RegisterView from './views/RegisterView';
import VerifyView from "./views/VerifyView.tsx";
import LoginView from "./views/LoginView.tsx";
import DashboardView from "./views/DashboardView.tsx";

export default function App() {
  return (
      <BrowserRouter>
        <Routes>
          <Route path="/register" element={<RegisterView />} />
            <Route path="/verify" element={<VerifyView />} />
            <Route path="/login" element={<LoginView />} />
            <Route path="/dashboard" element={<DashboardView />} />

          {/* Fallback route to catch any undefined URLs and send them to register */}
          <Route path="*" element={<Navigate to="/register" replace />} />
        </Routes>
      </BrowserRouter>
  );
}