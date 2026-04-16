import { useOutletContext } from 'react-router-dom';
import {useEffect, useState} from "react";
import LoginCalendarModal from '../components/LoginCalendarModal'; // Import the modal

interface PlayerProfile {
    email: string;
    displayName: string;
    pvpEnabled: boolean;
}

export default function DashboardView() {
    // Grab the profile passed down from Layout.tsx
    const { profile } = useOutletContext<{ profile: PlayerProfile }>();

    //------------- Login Streak Check Start -----------------------

    // Check local storage to see if we should show the modal
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
        // Remove the flag so it doesn't trigger again today
        localStorage.removeItem('dreamreach_first_login');
    };

    // ------------ Login Streak Check End -------------------------

    return (
        <div>
            {/* Render the modal if the state is true */}
            {showModal && <LoginCalendarModal streak={streak} track={rewardTrack} onClose={handleCloseModal} />}

            <h2 style={{ marginTop: 0 }}>Overview</h2>

            <div style={{ backgroundColor: 'white', padding: '20px', borderRadius: '8px', border: '1px solid #e2e8f0', maxWidth: '600px' }}>
                <h3 style={{ marginTop: 0, color: '#334155' }}>Account Status</h3>
                <p style={{ margin: '10px 0' }}><strong>Email:</strong> {profile.email}</p>
                <p style={{ margin: '10px 0' }}><strong>PvP Status:</strong> {profile.pvpEnabled ? '🔴 Enabled' : '🟢 Protected'}</p>
            </div>

            <p style={{ marginTop: '20px', color: '#64748b' }}>
                Welcome, commander. Select a destination from the menu.
            </p>
        </div>
    );
}