import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import RegisterView from './views/RegisterView';
import VerifyView from './views/VerifyView';
import LoginView from './views/LoginView';
import DashboardView from './views/DashboardView';
import KingdomView from './views/KingdomView';
import HeroesView from './views/HeroesView';
import Layout from './components/Layout';

// Placeholder component
const SummonPlaceholder = () => <div className="panel"><h2>Summoning Portal (Under Construction)</h2></div>;

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginView />} />
                <Route path="/register" element={<RegisterView />} />
                <Route path="/verify" element={<VerifyView />} />

                <Route element={<Layout />}>
                    <Route path="/dashboard" element={<DashboardView />} />
                    <Route path="/kingdom" element={<KingdomView />} />
                    <Route path="/heroes" element={<HeroesView />} />
                    <Route path="/summon" element={<SummonPlaceholder />} />
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                </Route>

                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}