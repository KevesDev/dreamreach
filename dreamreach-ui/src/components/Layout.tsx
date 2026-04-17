import React, { useEffect, useState } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import api from '../api/client';
import { Icon } from './Icon';

interface PlayerProfile {
    email: string;
    displayName: string;
    pvpEnabled: boolean;
    food: number;
    wood: number;
    stone: number;
    gold: number;
    gems: number;
    totalPopulation: number;
    maxPopulation: number;
}

export default function Layout() {
    const [profile, setProfile] = useState<PlayerProfile | null>(null);
    const [isChatOpen, setIsChatOpen] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    // Fetch profile and handle auth redirection
    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const res = await api.get('/player/me');
                setProfile(res.data);
            } catch (err) {
                localStorage.removeItem('dreamreach_token');
                navigate('/login');
            }
        };
        fetchProfile();
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem('dreamreach_token');
        navigate('/login');
    };

    if (!profile) {
        return (
            <div className="panel" style={{ margin: '100px auto', maxWidth: '300px', textAlign: 'center' }}>
                <p style={{ fontFamily: 'var(--font-heading)' }}>Synchronizing...</p>
            </div>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>

            {/* 7. HUD (TOP BAR) - Thin, persistent horizontal bar */}
            <header className="hud">
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-md)' }}>
                    <h3 style={{ fontSize: '1rem', color: 'var(--accent-gold)' }}>DREAMREACH</h3>
                    <div style={{ color: 'var(--text-primary)', fontSize: '0.85rem' }}>
                        {profile.displayName} <span style={{ color: 'var(--text-muted)' }}>Lv. 1</span>
                    </div>
                </div>

                {/* 2. CENTER: RESOURCES (with +/hr tickers) */}
                <div style={{ display: 'flex', gap: 'var(--space-xl)', alignItems: 'center' }}>
                    <div className="hud-stat" title="Population">
                        <Icon name="population" size={14} style={{ marginRight: 'var(--space-xs)', color: 'var(--text-muted)' }} />
                        <span style={{ color: profile.totalPopulation >= profile.maxPopulation ? 'var(--danger)' : 'var(--text-primary)' }}>
                            {profile.totalPopulation}/{profile.maxPopulation}
                        </span>
                    </div>

                    <div className="hud-stat" title="Food">
                        <Icon name="food" size={14} style={{ marginRight: 'var(--space-xs)', color: 'var(--text-muted)' }} />
                        {profile.food} <span style={{ color: 'var(--success)', fontSize: '0.7rem' }}>+12/hr</span>
                    </div>

                    <div className="hud-stat" title="Wood">
                        <Icon name="wood" size={14} style={{ marginRight: 'var(--space-xs)', color: 'var(--text-muted)' }} />
                        {profile.wood} <span style={{ color: 'var(--success)', fontSize: '0.7rem' }}>+5/hr</span>
                    </div>

                    <div className="hud-stat" title="Stone">
                        <Icon name="stone" size={14} style={{ marginRight: 'var(--space-xs)', color: 'var(--text-muted)' }} />
                        {profile.stone} <span style={{ color: 'var(--success)', fontSize: '0.7rem' }}>+2/hr</span>
                    </div>
                </div>

                {/* 2. RIGHT: CURRENCY & UTILITY */}
                <div style={{ display: 'flex', gap: 'var(--space-md)', alignItems: 'center' }}>
                    <div style={{ color: 'var(--accent-gold)', fontWeight: 'bold', fontSize: '0.9rem' }}>
                        <Icon name="gold" size={14} style={{ marginRight: 'var(--space-xs)' }} />
                        {profile.gold}
                    </div>
                    <div style={{ color: 'var(--accent-blue)', fontWeight: 'bold', fontSize: '0.9rem' }}>
                        <Icon name="gems" size={14} style={{ marginRight: 'var(--space-xs)' }} />
                        {profile.gems}
                    </div>

                    <button
                        className="button"
                        style={{ padding: '4px 8px', marginLeft: 'var(--space-md)' }}
                        onClick={() => setIsChatOpen(!isChatOpen)}
                    >
                        Chat
                    </button>

                    <button onClick={handleLogout} className="button--danger" style={{ padding: '4px 8px' }}>
                        <Icon name="logout" size={14} />
                    </button>
                </div>
            </header>

            <div style={{ display: 'flex', flex: 1, overflow: 'hidden', position: 'relative' }}>

                {/* NAVIGATION SIDEBAR (Desktop) */}
                <nav style={{ width: '220px', background: 'var(--bg-panel)', borderRight: '1px solid var(--border-subtle)', padding: 'var(--space-md) 0' }}>
                    <Link
                        to="/dashboard"
                        className={`list-item ${location.pathname === '/dashboard' ? 'active' : ''}`}
                        style={{ textDecoration: 'none', color: location.pathname === '/dashboard' ? 'var(--accent-gold)' : 'var(--text-secondary)' }}
                    >
                        <Icon name="home" size={18} style={{ marginRight: 'var(--space-md)' }} /> Overview
                    </Link>
                    <Link
                        to="/kingdom"
                        className={`list-item ${location.pathname === '/kingdom' ? 'active' : ''}`}
                        style={{ textDecoration: 'none', color: location.pathname === '/kingdom' ? 'var(--accent-gold)' : 'var(--text-secondary)' }}
                    >
                        <Icon name="home" size={18} style={{ marginRight: 'var(--space-md)' }} /> Kingdom
                    </Link>
                    <Link
                        to="/roster"
                        className={`list-item ${location.pathname === '/roster' ? 'active' : ''}`}
                        style={{ textDecoration: 'none', color: location.pathname === '/roster' ? 'var(--accent-gold)' : 'var(--text-secondary)' }}
                    >
                        <Icon name="combat" size={18} style={{ marginRight: 'var(--space-md)' }} /> War Room
                    </Link>
                    <Link
                        to="/summon"
                        className={`list-item ${location.pathname === '/summon' ? 'active' : ''}`}
                        style={{ textDecoration: 'none', color: location.pathname === '/summon' ? 'var(--accent-gold)' : 'var(--text-secondary)' }}
                    >
                        <Icon name="summon" size={18} style={{ marginRight: 'var(--space-md)' }} /> Summon
                    </Link>
                </nav>

                {/* MAIN CONTENT AREA */}
                <main style={{ flex: 1, overflowY: 'auto', background: 'var(--bg-main)', position: 'relative' }}>
                    <Outlet context={{ profile }} />
                </main>

                {/* CHAT SYSTEM (Right-side drawer) */}
                <aside
                    className="panel"
                    style={{
                        width: '300px',
                        position: 'absolute',
                        right: 0,
                        top: 0,
                        bottom: 0,
                        zIndex: 50,
                        transform: isChatOpen ? 'translateX(0)' : 'translateX(100%)',
                        transition: 'transform 0.3s ease',
                        borderRadius: 0,
                        borderLeft: '1px solid var(--border-strong)'
                    }}
                >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 'var(--space-md)' }}>
                        <h4 style={{ fontSize: '0.9rem' }}>Global Chat</h4>
                        <button className="button" style={{ padding: '2px 8px' }} onClick={() => setIsChatOpen(false)}>×</button>
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textAlign: 'center', marginTop: '100px' }}>
                        No messages yet.
                    </div>
                </aside>
            </div>
        </div>
    );
}