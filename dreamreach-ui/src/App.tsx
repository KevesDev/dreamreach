import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import RegisterView from './views/RegisterView';
import VerifyView from './views/VerifyView';
import LoginView from './views/LoginView';
import DashboardView from './views/DashboardView';
import KingdomView from './views/KingdomView';
import Layout from './components/Layout';

// Placeholder components for new routes
const RosterPlaceholder = () => <div className="panel"><h2>Your Roster (Under Construction)</h2></div>;
const SummonPlaceholder = () => <div className="panel"><h2>Summoning Portal (Under Construction)</h2></div>;

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginView />} />
                <Route path="/register" element={<RegisterView />} />
                <Route path="/verify" element={<VerifyView />} />

                {/* Protected Routes wrapped in the Global Shell */}
                <Route element={<Layout />}>
                    <Route path="/dashboard" element={<DashboardView />} />
                    <Route path="/kingdom" element={<KingdomView />} />
                    <Route path="/roster" element={<RosterPlaceholder />} />
                    <Route path="/summon" element={<SummonPlaceholder />} />
                    {/* Default protected route */}
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                </Route>

                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}