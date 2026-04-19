import { useEffect, useState, useCallback, useRef } from 'react';
import { Outlet, Link, useNavigate, useLocation } from 'react-router-dom';
import api from '../api/client';
import { Icon } from './Icon';

export default function Layout() {
    const [profile, setProfile] = useState<any>(null);
    const navigate = useNavigate();
    const location = useLocation();

    const accumulatorRef = useRef({ wood: 0, stone: 0, food: 0 });
    const lastTickRef = useRef<number>(Date.now());

    const fetchProfile = useCallback(() => {
        api.get('/player/me')
            .then(res => {
                setProfile(res.data);
                accumulatorRef.current = { wood: 0, stone: 0, food: 0 };
                lastTickRef.current = Date.now();
            })
            .catch(() => {
                localStorage.removeItem('dreamreach_token');
                navigate('/login');
            });
    }, [navigate]);

    useEffect(() => {
        fetchProfile();
    }, [fetchProfile]);

    useEffect(() => {
        if (!profile) return;

        const intervalId = setInterval(() => {
            setProfile((prevProfile: any) => {
                if (!prevProfile) return null;

                const now = Date.now();
                const dtSeconds = (now - lastTickRef.current) / 1000;
                lastTickRef.current = now;

                const woodPerSec = (prevProfile.woodRate || 0) / 3600;
                const stonePerSec = (prevProfile.stoneRate || 0) / 3600;
                const foodPerSec = (prevProfile.foodRate || 0) / 3600;

                accumulatorRef.current.wood += woodPerSec * dtSeconds;
                accumulatorRef.current.stone += stonePerSec * dtSeconds;
                accumulatorRef.current.food += foodPerSec * dtSeconds;

                let newPendingWood = prevProfile.pendingWood;
                let newPendingStone = prevProfile.pendingStone;
                let newPendingFood = prevProfile.pendingFood;

                if (Math.abs(accumulatorRef.current.wood) >= 1) {
                    const minted = Math.trunc(accumulatorRef.current.wood);
                    newPendingWood += minted;
                    accumulatorRef.current.wood -= minted;
                }
                if (Math.abs(accumulatorRef.current.stone) >= 1) {
                    const minted = Math.trunc(accumulatorRef.current.stone);
                    newPendingStone += minted;
                    accumulatorRef.current.stone -= minted;
                }
                if (Math.abs(accumulatorRef.current.food) >= 1) {
                    const minted = Math.trunc(accumulatorRef.current.food);
                    newPendingFood += minted;
                    accumulatorRef.current.food -= minted;
                }

                if (
                    newPendingWood === prevProfile.pendingWood &&
                    newPendingStone === prevProfile.pendingStone &&
                    newPendingFood === prevProfile.pendingFood
                ) {
                    return prevProfile;
                }

                return {
                    ...prevProfile,
                    pendingWood: newPendingWood,
                    pendingStone: newPendingStone,
                    pendingFood: newPendingFood
                };
            });
        }, 1000);

        return () => clearInterval(intervalId);
    }, [profile?.woodRate, profile?.stoneRate, profile?.foodRate]);

    const handleClaim = async () => {
        try {
            await api.post('/player/claim');
            fetchProfile();
        } catch (err) {
            console.error("Failed to claim resources");
        }
    };

    if (!profile) return <div style={{ color: 'white', padding: '50px', textAlign: 'center' }}>Syncing...</div>;

    const hasPendingResources = profile.pendingWood > 0 || profile.pendingStone > 0 || profile.pendingFood > 0;

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <header className="hud">
                <div className="hud-stat">
                    <h3 style={{ color: 'var(--accent-gold)', fontSize: '0.9rem', margin: 0 }}>DREAMREACH</h3>
                    <span style={{ fontSize: '0.8rem', marginLeft: '10px' }}>{profile.displayName}</span>
                </div>

                <div style={{ display: 'flex', gap: 'var(--space-lg)' }}>
                    <div className="hud-stat" title="Population">
                        <Icon name="population" size={14} /> {profile.totalPopulation}/{profile.maxPopulation}
                    </div>

                    <div className="hud-stat" title="Food">
                        <Icon name="food" size={14} /> {profile.food}
                        {profile.pendingFood !== 0 && (
                            <span style={{ color: 'var(--accent-gold)', marginLeft: '4px' }}>
                                ({profile.pendingFood > 0 ? `+${profile.pendingFood}` : profile.pendingFood})
                            </span>
                        )}
                        <span style={{ color: profile.foodRate < 0 ? 'var(--danger)' : 'var(--success)', fontSize: '0.7rem', marginLeft: '4px' }}>
                            {profile.foodRate > 0 ? `+${profile.foodRate}` : profile.foodRate}/hr
                        </span>
                    </div>

                    <div className="hud-stat" title="Wood">
                        <Icon name="wood" size={14} /> {profile.wood}
                        {profile.pendingWood !== 0 && (
                            <span style={{ color: 'var(--accent-gold)', marginLeft: '4px' }}>
                                ({profile.pendingWood > 0 ? `+${profile.pendingWood}` : profile.pendingWood})
                            </span>
                        )}
                        <span style={{ color: profile.woodRate < 0 ? 'var(--danger)' : 'var(--success)', fontSize: '0.7rem', marginLeft: '4px' }}>
                            {profile.woodRate > 0 ? `+${profile.woodRate}` : profile.woodRate}/hr
                        </span>
                    </div>

                    <div className="hud-stat" title="Stone">
                        <Icon name="stone" size={14} /> {profile.stone}
                        {profile.pendingStone !== 0 && (
                            <span style={{ color: 'var(--accent-gold)', marginLeft: '4px' }}>
                                ({profile.pendingStone > 0 ? `+${profile.pendingStone}` : profile.pendingStone})
                            </span>
                        )}
                        <span style={{ color: profile.stoneRate < 0 ? 'var(--danger)' : 'var(--success)', fontSize: '0.7rem', marginLeft: '4px' }}>
                            {profile.stoneRate > 0 ? `+${profile.stoneRate}` : profile.stoneRate}/hr
                        </span>
                    </div>
                </div>

                <div style={{ display: 'flex', gap: '15px', alignItems: 'center' }}>
                    {hasPendingResources && (
                        <button onClick={handleClaim} className="button button--claim" style={{ padding: '4px 12px', fontSize: '0.8rem' }}>
                            Collect
                        </button>
                    )}

                    <div style={{ color: 'var(--accent-gold)', display: 'flex', alignItems: 'center', gap: '4px' }} title="Gold">
                        <Icon name="gold" size={14} /> {profile.gold}
                    </div>

                    <div style={{ color: '#a335ee', display: 'flex', alignItems: 'center', gap: '4px' }} title="Gems">
                        <Icon name="gems" size={14} /> {profile.gems}
                    </div>

                    <button title="Logout" onClick={() => { localStorage.removeItem('dreamreach_token'); navigate('/login'); }} className="button--danger">
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
                    <Link to="/heroes" className={`list-item ${location.pathname === '/heroes' ? 'active' : ''}`}>
                        <Icon name="combat" size={18} style={{ marginRight: '12px' }} /> Heroes
                    </Link>
                    <Link to="/shop" className={`list-item ${location.pathname === '/shop' ? 'active' : ''}`}>
                        <Icon name="shop" size={18} style={{ marginRight: '12px' }} /> Shop
                    </Link>

                    {profile?.isAdmin && (
                        <Link to="/admin" className={`list-item ${location.pathname === '/admin' ? 'active' : ''}`} style={{ marginTop: 'auto', borderTop: '1px solid var(--border-subtle)' }}>
                            <Icon name="user" size={18} style={{ marginRight: '12px' }} /> Admin
                        </Link>
                    )}
                </nav>

                <main style={{ flex: 1, padding: 'var(--space-md)', overflowY: 'auto' }}>
                    <Outlet context={{ profile, fetchProfile }} />
                </main>
            </div>
        </div>
    );
}