import { useEffect, useState } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import api from '../api/client';
import { Icon } from './Icon';

export default function Layout() {
    const [profile, setProfile] = useState<any>(null);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        api.get('/player/me')
            .then(res => setProfile(res.data))
            .catch(() => {
                localStorage.removeItem('dreamreach_token');
                navigate('/login');
            });
    }, [navigate]);

    if (!profile) return <div style={{ color: 'white', padding: '50px', textAlign: 'center' }}>Syncing...</div>;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <header className="hud">
                <div className="hud-stat">
                    <h3 style={{ color: 'var(--accent-gold)', fontSize: '0.9rem', margin: 0 }}>DREAMREACH</h3>
                    <span style={{ fontSize: '0.8rem', marginLeft: '10px' }}>{profile.displayName}</span>
                </div>

                <div style={{ display: 'flex', gap: 'var(--space-lg)' }}>
                    <div className="hud-stat"><Icon name="population" size={14} /> {profile.totalPopulation}/{profile.maxPopulation}</div>
                    <div className="hud-stat"><Icon name="food" size={14} /> {profile.food}</div>
                    <div className="hud-stat"><Icon name="wood" size={14} /> {profile.wood}</div>
                    <div className="hud-stat"><Icon name="stone" size={14} /> {profile.stone}</div>
                </div>

                <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
                    <div style={{ color: 'var(--accent-gold)' }}><Icon name="gold" size={14} /> {profile.gold}</div>
                    <button onClick={() => { localStorage.removeItem('dreamreach_token'); navigate('/login'); }} className="button--danger">
                        <Icon name="logout" size={14} />
                    </button>
                </div>
            </header>

            <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
                <nav className="sidebar">
                    <Link to="/dashboard" className={`list-item ${location.pathname === '/dashboard' ? 'active' : ''}`}>
                        <Icon name="home" size={18} style={{ marginRight: '12px' }} /> Overview
                    </Link>
                    <Link to="/kingdom" className={`list-item ${location.pathname === '/kingdom' ? 'active' : ''}`}>
                        <Icon name="kingdom" size={18} style={{ marginRight: '12px' }} /> Kingdom
                    </Link>
                    <Link to="/roster" className={`list-item ${location.pathname === '/roster' ? 'active' : ''}`}>
                        <Icon name="combat" size={18} style={{ marginRight: '12px' }} /> War Room
                    </Link>
                    <Link to="/summon" className={`list-item ${location.pathname === '/summon' ? 'active' : ''}`}>
                        <Icon name="summon" size={18} style={{ marginRight: '12px' }} /> Summon
                    </Link>
                </nav>

                <main style={{ flex: 1, padding: 'var(--space-md)', overflowY: 'auto' }}>
                    <Outlet context={{ profile }} />
                </main>
            </div>
        </div>
    );
}