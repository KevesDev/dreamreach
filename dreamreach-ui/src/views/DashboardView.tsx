import { useEffect, useState} from "react";
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

interface PlayerProfile {
    email: string;
    displayName: string;
    pvpEnabled: boolean;
}

export default function DashboardView() {
    const [profile, setProfile] = useState<PlayerProfile | null>(null);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                // Notice we don't have to attach the token manually.
                // The Axios interceptor we wrote does it for us!
                const response = await api.get('/player/me');
                setProfile(response.data);
            } catch (err) {
                // If the token is missing, expired, or invalid, kick them out
                setError('Session expired or invalid. Please log in again.');
                localStorage.removeItem('dreamreach_token');
                setTimeout(() => navigate('/login'), 2000);
            }
        };

        fetchProfile();
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem('dreamreach_token');
        navigate('/login');
    };

    if (error) {
        return <div style={{ textAlign: 'center', margin: '50px auto', color: 'red', fontFamily: 'sans-serif' }}>{error}</div>;
    }

    if (!profile) {
        return <div style={{ textAlign: 'center', margin: '50px auto', fontFamily: 'sans-serif' }}>Loading profile secure data...</div>;
    }

    return (
        <div style={{ maxWidth: '500px', margin: '50px auto', fontFamily: 'sans-serif', padding: '20px', border: '1px solid #ccc', borderRadius: '8px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2>Player Profile</h2>
                <button onClick={handleLogout} style={{ padding: '8px 16px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    Logout
                </button>
            </div>

            <div style={{ backgroundColor: '#f8f9fa', padding: '20px', borderRadius: '4px', border: '1px solid #eee' }}>
                <p style={{ margin: '10px 0' }}><strong>Display Name:</strong> {profile.displayName}</p>
                <p style={{ margin: '10px 0' }}><strong>Email:</strong> {profile.email}</p>
                <p style={{ margin: '10px 0' }}><strong>PvP Status:</strong> {profile.pvpEnabled ? 'Enabled' : 'Protected'}</p>
            </div>

            <p style={{ marginTop: '20px', fontSize: '0.8rem', color: '#666', textAlign: 'center' }}>
                You are securely authenticated.
            </p>
        </div>
    );
}