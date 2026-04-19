import { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';

export default function AdminView() {
    const { profile } = useOutletContext<{ profile: any }>();
    const [targetEmail, setTargetEmail] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    // Hard gate just in case someone manually navigates to the URL
    if (!profile?.isAdmin) {
        return (
            <div className="panel" style={{ color: 'var(--danger)', textAlign: 'center', marginTop: 'var(--space-xl)' }}>
                <h2>Access Denied</h2>
                <p>You do not have authorization to view this page.</p>
            </div>
        );
    }

    const handleDelete = async () => {
        if (!targetEmail) return;

        // Safety confirmation to prevent accidental clicks
        if (!window.confirm(`Are you absolutely sure you want to permanently delete the account for ${targetEmail}? This cannot be undone.`)) {
            return;
        }

        setIsBusy(true);
        setMessage('');
        setIsError(false);

        try {
            const res = await api.delete(`/admin/accounts/${encodeURIComponent(targetEmail)}`);
            setMessage(res.data || `Account ${targetEmail} successfully vaporized.`);
            setTargetEmail('');
        } catch (err: any) {
            setIsError(true);
            setMessage(err.response?.data || "Failed to delete account. Double-check the email address.");
        } finally {
            setIsBusy(false);
        }
    };

    return (
        <div style={{ maxWidth: '700px', margin: '0 auto', paddingTop: 'var(--space-lg)' }}>
            <h2 style={{ color: 'var(--accent-gold)', marginBottom: 'var(--space-md)' }}>System Administration</h2>

            <div className="panel" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--danger)' }}>
                <h3 style={{ color: 'var(--danger)', marginBottom: 'var(--space-sm)' }}>Danger Zone: Account Purge</h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 'var(--space-lg)' }}>
                    Enter the email address of the account you wish to permanently delete. This will wipe all characters, buildings, resources, and the root profile from the database.
                </p>

                <div style={{ display: 'flex', gap: '12px' }}>
                    <input
                        type="text"
                        placeholder="target@example.com"
                        className="input"
                        style={{ flex: 1 }}
                        value={targetEmail}
                        onChange={(e) => setTargetEmail(e.target.value)}
                        disabled={isBusy}
                    />
                    <button
                        className="button--danger"
                        onClick={handleDelete}
                        disabled={isBusy || !targetEmail}
                        style={{ padding: '0 24px', whiteSpace: 'nowrap' }}
                    >
                        {isBusy ? 'Processing...' : 'Delete Account'}
                    </button>
                </div>

                {message && (
                    <div style={{
                        marginTop: 'var(--space-lg)',
                        padding: '12px',
                        background: isError ? 'rgba(220, 53, 69, 0.1)' : 'rgba(40, 167, 69, 0.1)',
                        border: `1px solid ${isError ? 'var(--danger)' : 'var(--success)'}`,
                        borderRadius: '4px',
                        color: isError ? 'var(--danger)' : 'var(--success)',
                        fontSize: '0.9rem',
                        fontWeight: 'bold'
                    }}>
                        {message}
                    </div>
                )}
            </div>
        </div>
    );
}