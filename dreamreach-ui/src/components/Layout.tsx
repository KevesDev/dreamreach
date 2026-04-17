import { useEffect, useState } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import api from '../api/client';
import { Icon } from './Icon';

export default function Layout() {
    const [profile, setProfile] = useState<any>(null);
    const [isChatOpen, setIsChatOpen] = useState(false);
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

    if (!profile) return <div className="panel" style={{ margin: '100px auto', width: '200px' }}>Synchronizing...</div>;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <header className="hud">
                <div className="hud-stat">
                    <h3 style={{ color: 'var(--accent-gold)', fontSize: '0.9rem', marginRight: 'var(--space-md)' }}>DREAMREACH</h3>
                    <span style={{ fontSize: '0.8rem' }}>{profile.displayName} <span style={{ color: 'var(--text-muted)' }}>Lv. 1</span></span>
                </div>

                <div style={{ display: 'flex', gap: 'var(--space-lg)' }}>
                    <div className="hud-stat">
                        <Icon name="population" size={14} style={{ color: 'var(--text-muted)', marginRight: '4px' }} />
                        {profile.totalPopulation}/{profile.maxPopulation}
                    </div>
                    <div className="hud-stat">
                        <Icon name="food" size={14} style={{ color: 'var(--text-muted)', marginRight: '4px' }} />
                        {profile.food} <span style={{ color: 'var(--success)', fontSize: '0.7rem', marginLeft: '2px' }}>+12/hr</span>
                    </div>
                    <div className="hud-stat">
                        <Icon name="wood" size={14} style={{ color: 'var(--text-muted)', marginRight: '4px' }} />
                        {profile.wood} <span style={{ color: 'var(--success)', fontSize: '0.7rem', marginLeft: '2px' }}>+5/hr</span>
                    </div>
                    <div className="hud-stat">
                        <Icon name="stone" size={14} style={{ color: 'var(--text-muted)', marginRight: '4px' }} />
                        {profile.stone} <span style={{ color: 'var(--success)', fontSize: '0.7rem', marginLeft: '2px' }}>+2/hr</span>
                    </div>
                </div>

                <div style={{ display: 'flex', gap: 'var(--space-md)', alignItems: 'center' }}>
                    <div style={{ color: 'var(--accent-gold)', fontWeight: 'bold' }}><Icon name="gold" size={14} /> {profile.gold}</div>
                    <div style={{ color: 'var(--accent-blue)', fontWeight: 'bold' }}><Icon name="gems" size={14} /> {profile.gems}</div>
                    <button className="button" onClick={() => setIsChatOpen(!isChatOpen)}>Chat</button>
                    <button className="button--danger" onClick={() => { localStorage.removeItem('dreamreach_token'); navigate('/login'); }}><Icon name="logout" size={14} /></button>
                </div>
            </header>

            <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
                <nav className="sidebar">
                    <Link to="/dashboard" className={`list-item ${location.pathname === '/dashboard' ? 'active' : ''}`}><Icon name="home" size={18} style={{ marginRight: '12px' }} /> Overview</Link>
                    <Link to="/kingdom" className={`list-item ${location.pathname === '/kingdom' ? 'active' : ''}`}><Icon name="kingdom" size={18} style={{ marginRight: '12px' }} /> Kingdom</Link>
                    <Link to="/roster" className={`list-item ${location.pathname === '/roster' ? 'active' : ''}`}><Icon name="combat" size={18} style={{ marginRight: '12px' }} /> War Room</Link>
                    <Link to="/summon" className={`list-item ${location.pathname === '/summon' ? 'active' : ''}`}><Icon name="summon" size={18} style={{ marginRight: '12px' }} /> Summon</Link>
                </nav>

                <main style={{ flex: 1, padding: 'var(--space-xl)', overflowY: 'auto' }}>
                    <Outlet context={{ profile }} />
                </main>

                <aside className="chat-drawer" style={{ transform: isChatOpen ? 'translateX(0)' : 'translateX(100%)' }}>
                    <div className="list-item" style={{ justifyContent: 'space-between', borderBottom: '1px solid var(--border-strong)' }}>
                        <h4>Global Chat</h4>
                        <button className="button" style={{ padding: '2px 8px' }} onClick={() => setIsChatOpen(false)}>×</button>
                    </div>
                    <div style={{ padding: 'var(--space-md)', color: 'var(--text-muted)', fontSize: '0.8rem' }}>No messages yet.</div>
                </aside>
            </div>
        </div>
    );
}