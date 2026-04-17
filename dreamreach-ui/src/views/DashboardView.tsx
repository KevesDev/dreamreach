import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import LoginCalendarModal from '../components/LoginCalendarModal';

interface PlayerProfile {
    email: string;
    displayName: string;
    pvpEnabled: boolean;
}

export default function DashboardView() {
    const { profile } = useOutletContext<{ profile: PlayerProfile }>();

    const [showModal, setShowModal] = useState(false);
    const [streak, setStreak] = useState(1);
    const [rewardTrack, setRewardTrack] = useState([]);

    useEffect(() => {
        const isFirstLogin = localStorage.getItem('dreamreach_first_login') === 'true';
        if (isFirstLogin) {
            const savedStreak = parseInt(localStorage.getItem('dreamreach_streak') || '1', 10);
            const savedTrack = JSON.parse(localStorage.getItem('dreamreach_reward_track') || '[]');

            setStreak(savedStreak);
            setRewardTrack(savedTrack);
            setShowModal(true);
        }
    }, []);

    const handleCloseModal = () => {
        setShowModal(false);
        localStorage.removeItem('dreamreach_first_login');
        window.location.reload();
    };

    return (
        <div>
            {showModal && <LoginCalendarModal streak={streak} track={rewardTrack} onClose={handleCloseModal} />}

            <h2 style={{ marginBottom: 'var(--space-md)' }}>Overview</h2>

            <div className="panel" style={{ maxWidth: '600px' }}>
                <h3 style={{ marginTop: 0, color: 'var(--accent-gold)' }}>Commander Status</h3>
                <div style={{ marginTop: 'var(--space-md)' }}>
                    <p><strong>Identity:</strong> {profile.displayName}</p>
                    <p><strong>Email:</strong> {profile.email}</p>
                    <p><strong>Security:</strong> {profile.pvpEnabled ? '🔴 PvP Active' : '🟢 Protected'}</p>
                </div>
            </div>

            <p style={{ marginTop: 'var(--space-lg)', color: 'var(--text-muted)' }}>
                Your kingdom awaits orders. Select a destination from the navigational menu.
            </p>
        </div>
    );
}