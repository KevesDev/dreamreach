import { useEffect, useState } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import api from '../api/client';

interface PlayerProfile {
    email: string;
    displayName: string;
    pvpEnabled: boolean;
}

export default function Layout() {
    const [profile, setProfile] = useState<PlayerProfile | null>(null);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        // Fetch the profile once for the global HUD
        api.get('/player/me')
            .then(res => setProfile(res.data))
            .catch(() => {
                // If token is invalid, kick to login
                localStorage.removeItem('dreamreach_token');
                navigate('/login');
            });
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem('dreamreach_token');
        navigate('/login');
    };

    if (!profile) {
        return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', fontFamily: 'sans-serif' }}>Loading Secure Interface...</div>;
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', fontFamily: 'sans-serif', backgroundColor: '#f4f6f8' }}>
            {/* TOP HUD */}
            <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '15px 20px', backgroundColor: '#1e293b', color: 'white' }}>
                <div style={{ fontWeight: 'bold', fontSize: '1.2rem' }}>
                    {profile.displayName} <span style={{ fontSize: '0.8rem', color: '#94a3b8', marginLeft: '10px' }}>Lv. 1</span>
                </div>
                <div style={{ display: 'flex', gap: '20px', fontSize: '0.9rem' }}>
                    <div title="Population">👥 10/25</div>
                    <div title="Gold">💰 500</div>
                    <div title="Gems">💎 50</div>
                    <button onClick={handleLogout} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontWeight: 'bold' }}>Logout</button>
                </div>
            </header>

            <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
                {/* SIDE NAVIGATION */}
                <nav style={{ width: '200px', backgroundColor: '#ffffff', borderRight: '1px solid #e2e8f0', display: 'flex', flexDirection: 'column', padding: '20px 0' }}>
                    <Link
                        to="/dashboard"
                        style={{ padding: '15px 20px', textDecoration: 'none', color: location.pathname === '/dashboard' ? '#2563eb' : '#475569', backgroundColor: location.pathname === '/dashboard' ? '#eff6ff' : 'transparent', fontWeight: location.pathname === '/dashboard' ? 'bold' : 'normal' }}
                    >
                        🏠 Dashboard
                    </Link>
                    <Link
                        to="/roster"
                        style={{ padding: '15px 20px', textDecoration: 'none', color: location.pathname === '/roster' ? '#2563eb' : '#475569', backgroundColor: location.pathname === '/roster' ? '#eff6ff' : 'transparent', fontWeight: location.pathname === '/roster' ? 'bold' : 'normal' }}
                    >
                        ⚔️ Roster
                    </Link>
                    <Link
                        to="/summon"
                        style={{ padding: '15px 20px', textDecoration: 'none', color: location.pathname === '/summon' ? '#2563eb' : '#475569', backgroundColor: location.pathname === '/summon' ? '#eff6ff' : 'transparent', fontWeight: location.pathname === '/summon' ? 'bold' : 'normal' }}
                    >
                        ✨ Summon
                    </Link>
                </nav>

                {/* MAIN CONTENT AREA */}
                <main style={{ flex: 1, padding: '30px', overflowY: 'auto' }}>
                    {/* The Outlet passes the profile down to child components so they don't have to fetch it again */}
                    <Outlet context={{ profile }} />
                </main>
            </div>
        </div>
    );
}