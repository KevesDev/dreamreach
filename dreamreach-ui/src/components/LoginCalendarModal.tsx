import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

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
    const [claiming, setClaiming] = useState(false);
    const navigate = useNavigate();

    // Cap the visual streak at 7. If they are on day 8, it shows as day 1 of a new cycle.
    const visualStreak = streak > 7 ? streak % 7 || 7 : streak;
    const isPremiumDay = visualStreak === 7;

    const handleClaim = () => {
        setClaiming(true);

        // TODO (Backend): In the future, this click will send an API call to actually
        // grant the resources in the database.

        setTimeout(() => {
            onClose();
            // If it's day 7, claiming immediately teleports them to the Summon portal
            if (isPremiumDay) {
                navigate('/summon');
            }
        }, 800);
    };

    return (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(15, 23, 42, 0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, fontFamily: 'sans-serif' }}>
            <div style={{
                backgroundColor: isPremiumDay ? '#1e1b4b' : '#ffffff', // Dark premium background for day 7
                color: isPremiumDay ? '#ffffff' : '#1e293b',
                padding: '30px',
                borderRadius: '12px',
                maxWidth: '650px',
                width: '90%',
                textAlign: 'center',
                boxShadow: isPremiumDay ? '0 0 40px rgba(99, 102, 241, 0.5)' : '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
                border: isPremiumDay ? '2px solid #818cf8' : 'none'
            }}>

                <h2 style={{ margin: '0 0 5px 0', color: isPremiumDay ? '#c7d2fe' : '#1e293b' }}>
                    {isPremiumDay ? '✨ Epic Login Reward ✨' : 'Daily Supply Drop'}
                </h2>
                <p style={{ color: isPremiumDay ? '#a5b4fc' : '#64748b', marginBottom: '25px' }}>
                    You are on a <strong>{streak}-day</strong> consecutive streak!
                </p>

                {/* 7-Day Grid */}
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
                                gridColumn: isDay7 ? 'span 2' : 'span 1', // Day 7 takes up the bottom two slots
                                opacity: isPastDay ? 0.5 : 1,
                                transform: isCurrentDay ? 'scale(1.05)' : 'scale(1)',
                                transition: 'all 0.3s ease',
                                position: 'relative'
                            }}>
                                <div style={{ fontSize: '0.8rem', fontWeight: 'bold', color: isCurrentDay ? (isDay7 ? '#e0e7ff' : '#2563eb') : '#94a3b8' }}>
                                    DAY {reward.day}
                                </div>

                                {/* Render Specific Resources */}
                                {!isDay7 ? (
                                    <div style={{ fontSize: '0.8rem', marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '2px', color: '#475569' }}>
                                        <div>🌾 {reward.food}</div>
                                        <div>🪵 {reward.wood}</div>
                                        <div>𪨧 {reward.stone}</div>
                                        {reward.gold > 0 && <div style={{ color: '#d97706', fontWeight: 'bold' }}>💰 {reward.gold}</div>}
                                    </div>
                                ) : (
                                    <div style={{ padding: '10px 0' }}>
                                        <div style={{ fontSize: '2.5rem', textShadow: '0 0 10px rgba(255,255,255,0.5)' }}>✨🎟️✨</div>
                                        <div style={{ fontSize: '1rem', fontWeight: 'bold', color: '#c7d2fe', marginTop: '5px' }}>Free Epic Summon</div>
                                    </div>
                                )}

                                {isPastDay && (
                                    <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', fontSize: '2rem', color: '#22c55e', opacity: 0.8 }}>
                                        ✓
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>

                <button
                    onClick={handleClaim}
                    disabled={claiming}
                    style={{
                        padding: '15px 30px',
                        fontSize: '1.1rem',
                        fontWeight: 'bold',
                        backgroundColor: claiming ? '#93c5fd' : (isPremiumDay ? '#4f46e5' : '#2563eb'),
                        color: 'white',
                        border: 'none',
                        borderRadius: '8px',
                        cursor: claiming ? 'not-allowed' : 'pointer',
                        width: '100%',
                        transition: 'all 0.2s',
                        boxShadow: isPremiumDay ? '0 4px 15px rgba(79, 70, 229, 0.4)' : 'none'
                    }}
                >
                    {claiming ? 'Processing...' : (isPremiumDay ? 'Claim & Enter Portal' : 'Claim Daily Supplies')}
                </button>
            </div>
        </div>
    );
}