import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import RegisterView from './views/RegisterView';
import VerifyView from './views/VerifyView';
import LoginView from './views/LoginView';
import DashboardView from './views/DashboardView';
import Layout from './components/Layout'; // Import the new shell

// Placeholder components for the new routes to prevent crashing
const RosterPlaceholder = () => <h2>Your Roster (Under Construction)</h2>;
const SummonPlaceholder = () => <h2>Summoning Portal (Under Construction)</h2>;

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Public Routes */}
                <Route path="/login" element={<LoginView />} />
                <Route path="/register" element={<RegisterView />} />
                <Route path="/verify" element={<VerifyView />} />

                {/* Protected Routes wrapped in the Global Shell */}
                <Route element={<Layout />}>
                    <Route path="/dashboard" element={<DashboardView />} />
                    <Route path="/roster" element={<RosterPlaceholder />} />
                    <Route path="/summon" element={<SummonPlaceholder />} />
                </Route>

                {/* Fallback */}
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}