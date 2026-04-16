import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

interface DailyReward {
    day: number;
    food: number;
    wood: number;
    stone: number;
    gold: number;
    summon: boolean;
}

interface LoginCalendarModalProps {
    streak: number;
    track: DailyReward[];
    onClose: () => void;
}

export default function LoginCalendarModal({ streak, track, onClose }: LoginCalendarModalProps) {
    const [claimState, setClaimState] = useState<'idle' | 'opening' | 'revealed'>('idle');
    const navigate = useNavigate();

    const visualStreak = streak > 7 ? streak % 7 || 7 : streak;
    const isPremiumDay = visualStreak === 7;

    // Grab the exact RNG numbers for today to show on the reveal screen
    const todaysLoot = track.find(r => r.day === visualStreak);

    const handleClaim = async () => {
        setClaimState('opening'); // Triggers the visual change to the button

        try {
            await api.post('/player/reward/claim');
        } catch (error) {
            console.error("Failed to process reward claim:", error);
        }

        // Simulate a 1.2-second tension build before revealing the loot
        setTimeout(() => {
            setClaimState('revealed');
        }, 1200);
    };

    const handleClose = () => {
        onClose();
        if (isPremiumDay) {
            navigate('/summon');
        }
    };

    // --- VIEW 1: THE REVEAL SCREEN (Shows AFTER clicking claim) ---
    if (claimState === 'revealed' && todaysLoot) {
        return (
            <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(15, 23, 42, 0.9)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, fontFamily: 'sans-serif' }}>
                <div style={{ backgroundColor: isPremiumDay ? '#1e1b4b' : '#ffffff', padding: '40px', borderRadius: '16px', maxWidth: '500px', width: '90%', textAlign: 'center', border: isPremiumDay ? '2px solid #818cf8' : 'none', boxShadow: '0 0 50px rgba(255, 255, 255, 0.2)' }}>
                    <h2 style={{ fontSize: '2rem', margin: '0 0 10px 0', color: isPremiumDay ? '#c7d2fe' : '#1e293b' }}>
                        {isPremiumDay ? '✨ Epic Reward! ✨' : 'Loot Acquired!'}
                    </h2>

                    <div style={{ margin: '30px 0', display: 'flex', flexDirection: 'column', gap: '15px', fontSize: '1.2rem', fontWeight: 'bold' }}>
                        {!isPremiumDay ? (
                            <>
                                <div style={{ color: '#475569' }}>🌾 {todaysLoot.food} Food</div>
                                <div style={{ color: '#475569' }}>🪵 {todaysLoot.wood} Wood</div>
                                <div style={{ color: '#475569' }}>𪨧 {todaysLoot.stone} Stone</div>
                                {todaysLoot.gold > 0 && <div style={{ color: '#d97706', fontSize: '1.4rem' }}>💰 {todaysLoot.gold} Gold</div>}
                            </>
                        ) : (
                            <div style={{ fontSize: '1.5rem', color: '#818cf8' }}>🎟️ 1x Epic Summon Token</div>
                        )}
                    </div>

                    <button
                        onClick={handleClose}
                        style={{ padding: '15px 30px', fontSize: '1.1rem', fontWeight: 'bold', backgroundColor: isPremiumDay ? '#4f46e5' : '#2563eb', color: 'white', border: 'none', borderRadius: '8px', cursor: 'pointer', width: '100%' }}
                    >
                        {isPremiumDay ? 'Enter Summoning Portal' : 'Add to Inventory'}
                    </button>
                </div>
            </div>
        );
    }

    // --- VIEW 2: THE CALENDAR SCREEN (Hides future loot, shows crates) ---
    return (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(15, 23, 42, 0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, fontFamily: 'sans-serif' }}>
            <div style={{ backgroundColor: isPremiumDay ? '#1e1b4b' : '#ffffff', color: isPremiumDay ? '#ffffff' : '#1e293b', padding: '30px', borderRadius: '12px', maxWidth: '650px', width: '90%', textAlign: 'center', boxShadow: isPremiumDay ? '0 0 40px rgba(99, 102, 241, 0.5)' : '0 20px 25px -5px rgba(0, 0, 0, 0.1)', border: isPremiumDay ? '2px solid #818cf8' : 'none' }}>

                <h2 style={{ margin: '0 0 5px 0', color: isPremiumDay ? '#c7d2fe' : '#1e293b' }}>
                    {isPremiumDay ? '✨ Epic Login Reward ✨' : 'Daily Supply Drop'}
                </h2>
                <p style={{ color: isPremiumDay ? '#a5b4fc' : '#64748b', marginBottom: '25px' }}>
                    You are on a <strong>{streak}-day</strong> consecutive streak!
                </p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '10px', marginBottom: '25px' }}>
                    {track.map((reward) => {
                        const isCurrentDay = reward.day === visualStreak;
                        const isPastDay = reward.day < visualStreak;
                        const isDay7 = reward.day === 7;

                        return (
                            <div key={reward.day} style={{
                                padding: '15px 5px',
                                backgroundColor: isCurrentDay ? (isDay7 ? '#4338ca' : '#eff6ff') : (isPastDay ? '#f8fafc' : '#ffffff'),
                                border: `2px solid ${isCurrentDay ? (isDay7 ? '#818cf8' : '#3b82f6') : (isPastDay ? '#e2e8f0' : '#cbd5e1')}`,
                                borderRadius: '8px',
                                gridColumn: isDay7 ? 'span 2' : 'span 1',
                                opacity: isPastDay ? 0.5 : 1,
                                transform: isCurrentDay && claimState === 'opening' ? 'scale(1.1)' : 'scale(1)',
                                transition: 'all 0.3s ease',
                                position: 'relative'
                            }}>
                                <div style={{ fontSize: '0.8rem', fontWeight: 'bold', color: isCurrentDay ? (isDay7 ? '#e0e7ff' : '#2563eb') : '#94a3b8' }}>
                                    DAY {reward.day}
                                </div>

                                <div style={{ fontSize: '2rem', padding: '10px 0', opacity: claimState === 'opening' && isCurrentDay ? 0.5 : 1 }}>
                                    {isDay7 ? '✨📜✨' : '📦'}
                                </div>

                                {isPastDay && (
                                    <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', fontSize: '2rem', color: '#22c55e', opacity: 0.9 }}>
                                        ✓
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>

                <button
                    onClick={handleClaim}
                    disabled={claimState === 'opening'}
                    style={{
                        padding: '15px 30px', fontSize: '1.1rem', fontWeight: 'bold',
                        backgroundColor: claimState === 'opening' ? '#93c5fd' : (isPremiumDay ? '#4f46e5' : '#2563eb'),
                        color: 'white', border: 'none', borderRadius: '8px',
                        cursor: claimState === 'opening' ? 'wait' : 'pointer', width: '100%', transition: 'all 0.2s'
                    }}
                >
                    {claimState === 'opening' ? 'Unlocking Crate...' : (isPremiumDay ? 'Open Epic Portal' : 'Open Today\'s Crate')}
                </button>
            </div>
        </div>
    );
}