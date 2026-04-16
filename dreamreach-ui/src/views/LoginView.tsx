import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

export default function LoginView() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            // Hit the Bouncer endpoint
            const response = await api.post('/auth/login', { email, password });

            // Securely store the JWT wristband
            localStorage.setItem('dreamreach_token', response.data.token);

            // Store the streak data for the dashboard modal
            localStorage.setItem('dreamreaach_streak', response.data.consecutiveLogins);
            localStorage.setItem('dreamreach_first_login', response.data.isFirstLoginToday);
            localStorage.setItem('dreamreach_reward_track', JSON.stringify(response.data.rewardTrack));

            // Redirect to the protected dashboard
            navigate('/dashboard');

        } catch (err: any) {
            if (err.response && err.response.data && err.response.data.message) {
                setError(err.response.data.message);
            } else {
                setError('Invalid credentials or account not verified.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: '400px', margin: '50px auto', fontFamily: 'sans-serif', textAlign: 'center' }}>
            <h2>Welcome Back</h2>

            {error && (
                <div style={{ color: 'red', marginBottom: '15px', padding: '10px', border: '1px solid red' }}>
                    {error}
                </div>
            )}

            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '15px', textAlign: 'left' }}>
                <div>
                    <label htmlFor="login-email" style={{ display: 'block', marginBottom: '5px' }}>Email</label>
                    <input
                        id="login-email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                <div>
                    <label htmlFor="login-password" style={{ display: 'block', marginBottom: '5px' }}>Password</label>
                    <input
                        id="login-password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                <button
                    type="submit"
                    disabled={loading}
                    style={{ padding: '10px', backgroundColor: '#007bff', color: 'white', border: 'none', cursor: 'pointer', marginTop: '10px' }}
                >
                    {loading ? 'Authenticating...' : 'Login'}
                </button>
            </form>

            <p style={{ marginTop: '20px', fontSize: '0.9rem' }}>
                Need an account? <button onClick={() => navigate('/register')} style={{ background: 'none', border: 'none', color: '#007bff', cursor: 'pointer', textDecoration: 'underline' }}>Register here</button>
            </p>
        </div>
    );
}