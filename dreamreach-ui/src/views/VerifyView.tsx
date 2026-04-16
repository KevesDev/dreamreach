import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';

export default function VerifyView() {
    const [code, setCode] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);

    const navigate = useNavigate();

    const handleVerify = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);

        try {
            await api.post('/auth/verify', { code });

            setSuccess('Account verified! Redirecting to login...');

            // Wait 2 seconds so the user can read the success message, then send them to login
            setTimeout(() => {
                navigate('/login');
            }, 2000);

        } catch (err: any) {
            if (err.response && err.response.data && err.response.data.message) {
                setError(err.response.data.message);
            } else {
                setError('Invalid or expired verification code.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: '400px', margin: '50px auto', fontFamily: 'sans-serif', textAlign: 'center' }}>
            <h2>Check Your Email</h2>
            <p>We sent a 6-digit verification code to your email.</p>

            {error && (
                <div style={{ color: 'red', marginBottom: '15px', padding: '10px', border: '1px solid red' }}>
                    {error}
                </div>
            )}

            {success && (
                <div style={{ color: 'green', marginBottom: '15px', padding: '10px', border: '1px solid green' }}>
                    {success}
                </div>
            )}

            <form onSubmit={handleVerify} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <div>
                    <input
                        type="text"
                        placeholder="Enter 6-digit code"
                        value={code}
                        onChange={(e) => setCode(e.target.value)}
                        maxLength={6}
                        required
                        style={{ width: '100%', padding: '10px', fontSize: '1.2rem', textAlign: 'center', letterSpacing: '5px' }}
                    />
                </div>

                <button
                    type="submit"
                    disabled={loading || code.length !== 6}
                    style={{ padding: '10px', backgroundColor: '#28a745', color: 'white', border: 'none', cursor: 'pointer' }}
                >
                    {loading ? 'Verifying...' : 'Verify Account'}
                </button>
            </form>
        </div>
    );
}