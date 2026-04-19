import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';
import './MissionsView.css';

export interface AdminQuest {
    id?: string;
    type: string;
    title: string;
    description: string;
    targetStatsJson: string;
    advantageClassesJson: string;
    disadvantageClassesJson: string;
    baseExp: number;
    rewardGold: number;
    rewardGems: number;
    rewardFood: number;
    rewardWood: number;
    rewardStone: number;
    durationHours: number;
    published: boolean;
}

const emptyQuest: AdminQuest = {
    type: 'SCOUTING',
    title: '',
    description: '',
    targetStatsJson: '{}',
    advantageClassesJson: '[]',
    disadvantageClassesJson: '[]',
    baseExp: 0,
    rewardGold: 0,
    rewardGems: 0,
    rewardFood: 0,
    rewardWood: 0,
    rewardStone: 0,
    durationHours: 4,
    published: true
};

export default function AdminView() {
    const { profile } = useOutletContext<{ profile: any }>();
    const [activeTab, setActiveTab] = useState<'DANGER' | 'FORGE' | 'BOARD'>('FORGE');

    // Danger Zone State
    const [targetEmail, setTargetEmail] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    // Quest Management State
    const [quests, setQuests] = useState<AdminQuest[]>([]);
    const [editingQuest, setEditingQuest] = useState<AdminQuest>(emptyQuest);

    useEffect(() => {
        if (profile?.isAdmin && (activeTab === 'BOARD' || activeTab === 'FORGE')) {
            fetchQuests();
        }
    }, [profile, activeTab]);

    const fetchQuests = () => {
        api.get('/admin/quests').then(res => setQuests(res.data)).catch(err => console.error(err));
    };

    if (!profile?.isAdmin) {
        return (
            <div className="panel" style={{ color: 'var(--danger)', textAlign: 'center', marginTop: 'var(--space-xl)' }}>
                <h2>Access Denied</h2>
                <p>You do not have authorization to view this page.</p>
            </div>
        );
    }

    const handleDeleteAccount = async () => {
        if (!targetEmail) return;
        if (!window.confirm(`Are you absolutely sure you want to permanently delete the account for ${targetEmail}? This cannot be undone.`)) return;

        setIsBusy(true); setMessage(''); setIsError(false);
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

    const handleSaveQuest = async () => {
        setIsBusy(true);
        try {
            if (editingQuest.id) {
                await api.put(`/admin/quests/${editingQuest.id}`, editingQuest);
                alert("Quest successfully updated!");
            } else {
                await api.post('/admin/quests', editingQuest);
                alert("New Quest successfully forged!");
                setEditingQuest(emptyQuest); // Reset form on fresh create
            }
            fetchQuests();
            setActiveTab('BOARD');
        } catch (err: any) {
            alert(err.response?.data || "Failed to save quest. Check your JSON formatting.");
        } finally {
            setIsBusy(false);
        }
    };

    const handleTogglePublish = async (quest: AdminQuest) => {
        if (!quest.id) return;
        try {
            await api.put(`/admin/quests/${quest.id}`, { ...quest, published: !quest.published });
            fetchQuests();
        } catch (err) {
            alert("Failed to update publish status.");
        }
    };

    const handleDeleteQuest = async (id: string) => {
        if (!window.confirm("Warning: Hard-deleting a quest will fail if players have already completed it or accepted it into their journals. Proceed?")) return;
        try {
            await api.delete(`/admin/quests/${id}`);
            fetchQuests();
        } catch (err: any) {
            alert(err.response?.data || "Failed to delete quest.");
        }
    };

    return (
        <div style={{ maxWidth: '1000px', margin: '0 auto', paddingTop: 'var(--space-lg)' }}>
            <h2 style={{ color: 'var(--accent-gold)', marginBottom: 'var(--space-md)' }}>DM Tools & Administration</h2>

            <div className="missions-nav" style={{ marginBottom: '20px' }}>
                <div className={`missions-tab ${activeTab === 'FORGE' ? 'active' : ''}`} onClick={() => { setActiveTab('FORGE'); setEditingQuest(emptyQuest); }}>Quest Forge</div>
                <div className={`missions-tab ${activeTab === 'BOARD' ? 'active' : ''}`} onClick={() => setActiveTab('BOARD')}>Board Manager</div>
                <div className={`missions-tab ${activeTab === 'DANGER' ? 'active' : ''}`} onClick={() => setActiveTab('DANGER')} style={{ color: activeTab === 'DANGER' ? 'var(--danger)' : '' }}>Danger Zone</div>
            </div>

            {activeTab === 'DANGER' && (
                <div className="panel" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--danger)' }}>
                    <h3 style={{ color: 'var(--danger)', marginBottom: 'var(--space-sm)' }}>Danger Zone: Account Purge</h3>
                    <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 'var(--space-lg)' }}>
                        Enter the email address of the account you wish to permanently delete. This will wipe all characters, buildings, resources, and the root profile from the database.
                    </p>
                    <div style={{ display: 'flex', gap: '12px' }}>
                        <input type="text" placeholder="target@example.com" className="input" style={{ flex: 1 }} value={targetEmail} onChange={(e) => setTargetEmail(e.target.value)} disabled={isBusy} />
                        <button className="button--danger" onClick={handleDeleteAccount} disabled={isBusy || !targetEmail} style={{ padding: '0 24px', whiteSpace: 'nowrap' }}>
                            {isBusy ? 'Processing...' : 'Delete Account'}
                        </button>
                    </div>
                    {message && (
                        <div style={{ marginTop: 'var(--space-lg)', padding: '12px', background: isError ? 'rgba(220, 53, 69, 0.1)' : 'rgba(40, 167, 69, 0.1)', border: `1px solid ${isError ? 'var(--danger)' : 'var(--success)'}`, borderRadius: '4px', color: isError ? 'var(--danger)' : 'var(--success)', fontSize: '0.9rem', fontWeight: 'bold' }}>
                            {message}
                        </div>
                    )}
                </div>
            )}

            {activeTab === 'FORGE' && (
                <div className="panel">
                    <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>{editingQuest.id ? 'Edit Existing Quest' : 'Forge New Quest'}</h3>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Quest Title</label>
                                <input type="text" className="input" value={editingQuest.title} onChange={e => setEditingQuest({...editingQuest, title: e.target.value})} />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Quest Type</label>
                                <select className="input" value={editingQuest.type} onChange={e => setEditingQuest({...editingQuest, type: e.target.value})}>
                                    <option value="SCOUTING">SCOUTING</option>
                                    <option value="HUNT">HUNT</option>
                                    <option value="RAIDING">RAIDING</option>
                                    <option value="STORY">STORY</option>
                                    <option value="ESCORT">ESCORT</option>
                                </select>
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Description</label>
                                <textarea className="input" rows={4} value={editingQuest.description} onChange={e => setEditingQuest({...editingQuest, description: e.target.value})}></textarea>
                            </div>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                <div>
                                    <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Base EXP</label>
                                    <input type="number" className="input" value={editingQuest.baseExp} onChange={e => setEditingQuest({...editingQuest, baseExp: parseInt(e.target.value) || 0})} />
                                </div>
                                <div>
                                    <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Duration (Hours)</label>
                                    <input type="number" className="input" value={editingQuest.durationHours} onChange={e => setEditingQuest({...editingQuest, durationHours: parseInt(e.target.value) || 0})} />
                                </div>
                            </div>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Target Stats (JSON)</label>
                                <input type="text" className="input" value={editingQuest.targetStatsJson} onChange={e => setEditingQuest({...editingQuest, targetStatsJson: e.target.value})} placeholder='{"STR": 20}' />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Advantage Classes (JSON)</label>
                                <input type="text" className="input" value={editingQuest.advantageClassesJson} onChange={e => setEditingQuest({...editingQuest, advantageClassesJson: e.target.value})} placeholder='["FIGHTER", "PALADIN"]' />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '5px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Disadvantage Classes (JSON)</label>
                                <input type="text" className="input" value={editingQuest.disadvantageClassesJson} onChange={e => setEditingQuest({...editingQuest, disadvantageClassesJson: e.target.value})} placeholder='["ROGUE"]' />
                            </div>

                            <label style={{ display: 'block', marginTop: '10px', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Loot Yields</label>
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '5px' }}>
                                <input type="number" title="Gold" className="input" value={editingQuest.rewardGold} onChange={e => setEditingQuest({...editingQuest, rewardGold: parseInt(e.target.value) || 0})} placeholder="Gold" />
                                <input type="number" title="Gems" className="input" value={editingQuest.rewardGems} onChange={e => setEditingQuest({...editingQuest, rewardGems: parseInt(e.target.value) || 0})} placeholder="Gems" />
                                <input type="number" title="Food" className="input" value={editingQuest.rewardFood} onChange={e => setEditingQuest({...editingQuest, rewardFood: parseInt(e.target.value) || 0})} placeholder="Food" />
                                <input type="number" title="Wood" className="input" value={editingQuest.rewardWood} onChange={e => setEditingQuest({...editingQuest, rewardWood: parseInt(e.target.value) || 0})} placeholder="Wood" />
                                <input type="number" title="Stone" className="input" value={editingQuest.rewardStone} onChange={e => setEditingQuest({...editingQuest, rewardStone: parseInt(e.target.value) || 0})} placeholder="Stone" />
                            </div>
                        </div>
                    </div>

                    <div style={{ marginTop: '30px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                        <button className="button button--claim" onClick={handleSaveQuest} disabled={isBusy || !editingQuest.title}>
                            {editingQuest.id ? 'Save Changes' : 'Forge Quest Template'}
                        </button>
                    </div>
                </div>
            )}

            {activeTab === 'BOARD' && (
                <div className="panel">
                    <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>Adventurer's Board Database</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {quests.map(quest => (
                            <div key={quest.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '15px', background: 'var(--surface-1)', border: '1px solid var(--border-subtle)', borderRadius: '6px' }}>
                                <div>
                                    <div style={{ fontWeight: 'bold', color: 'var(--accent-gold)' }}>{quest.title} <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginLeft: '8px' }}>{quest.type}</span></div>
                                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '4px' }}>{quest.durationHours} Hours | {quest.baseExp} EXP</div>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                                    <button
                                        onClick={() => handleTogglePublish(quest)}
                                        style={{ padding: '6px 12px', fontSize: '0.8rem', borderRadius: '4px', border: '1px solid', cursor: 'pointer', background: 'transparent',
                                            borderColor: quest.published ? 'var(--success)' : 'var(--text-muted)',
                                            color: quest.published ? 'var(--success)' : 'var(--text-muted)'
                                        }}>
                                        {quest.published ? '🟢 Published' : '⚫ Hidden'}
                                    </button>
                                    <button className="button--secondary" onClick={() => { setEditingQuest(quest); setActiveTab('FORGE'); }} style={{ padding: '6px 12px', fontSize: '0.8rem' }}>Edit</button>
                                    <button className="button--danger" onClick={() => handleDeleteQuest(quest.id!)} style={{ padding: '6px 12px', fontSize: '0.8rem' }}>Delete</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}