import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';
import { Icon } from '../components/Icon';
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

export interface AdminHero {
    template: {
        id?: string;
        name: string;
        description: string;
        rarity: string;
        dndClass: string;
        baseStr: number;
        baseDex: number;
        baseCon: number;
        baseInt: number;
        baseWis: number;
        baseCha: number;
        hitDieType: number;
        primaryStat: string;
        classTags: string;
        flavorQuips: string;
        portraitUrl: string;
        baseGoldCost: number;
        baseGemCost: number;
    };
    tavernWeight: number;
}

const emptyQuest: AdminQuest = {
    type: 'SCOUTING', title: '', description: '', targetStatsJson: '{}', advantageClassesJson: '[]', disadvantageClassesJson: '[]',
    baseExp: 0, rewardGold: 0, rewardGems: 0, rewardFood: 0, rewardWood: 0, rewardStone: 0, durationHours: 4, published: true
};

const emptyHero: AdminHero = {
    template: {
        name: '', description: '', rarity: 'COMMON', dndClass: 'FIGHTER', baseStr: 10, baseDex: 10, baseCon: 10, baseInt: 10, baseWis: 10, baseCha: 10,
        hitDieType: 8, primaryStat: 'STR', classTags: '[]', flavorQuips: '{"IDLE": "Ready.", "MISSION": "Let\'s go."}',
        portraitUrl: '/assets/hero.png', baseGoldCost: 500, baseGemCost: 50
    },
    tavernWeight: 10
};

const DND_CLASSES = ['FIGHTER', 'WIZARD', 'CLERIC', 'ROGUE', 'RANGER', 'PALADIN', 'BARD', 'WARLOCK', 'SORCERER', 'DRUID', 'BARBARIAN', 'MONK'];
const RARITIES = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY'];
const STAT_OPTIONS = ['STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA'];

const safeParseObj = (str: string) => { try { return JSON.parse(str || '{}'); } catch { return {}; } };
const safeParseArr = (str: string) => { try { return JSON.parse(str || '[]'); } catch { return []; } };

export default function AdminView() {
    const { profile } = useOutletContext<{ profile: any }>();
    const [activeModule, setActiveModule] = useState<'QUESTS' | 'HEROES' | 'DANGER'>('QUESTS');
    const [subTab, setSubTab] = useState<'DATABASE' | 'FORGE'>('DATABASE');

    const [targetEmail, setTargetEmail] = useState('');
    const [message, setMessage] = useState('');
    const [isError, setIsError] = useState(false);
    const [isBusy, setIsBusy] = useState(false);

    const [quests, setQuests] = useState<AdminQuest[]>([]);
    const [editingQuest, setEditingQuest] = useState<AdminQuest>(emptyQuest);

    const [heroes, setHeroes] = useState<AdminHero[]>([]);
    const [editingHero, setEditingHero] = useState<AdminHero>(emptyHero);

    const [newStatKey, setNewStatKey] = useState('STR');
    const [newStatVal, setNewStatVal] = useState(10);

    useEffect(() => {
        if (profile?.isAdmin) {
            if (activeModule === 'QUESTS') fetchQuests();
            if (activeModule === 'HEROES') fetchHeroes();
        }
    }, [profile, activeModule]);

    const fetchQuests = () => api.get('/admin/quests').then(res => setQuests(res.data)).catch(err => console.error(err));
    const fetchHeroes = () => api.get('/admin/heroes').then(res => setHeroes(res.data)).catch(err => console.error(err));

    if (!profile?.isAdmin) return <div className="panel" style={{ color: 'var(--danger)', textAlign: 'center', marginTop: 'var(--space-xl)' }}><h2>Access Denied</h2></div>;

    const handleDeleteAccount = async () => {
        if (!targetEmail) return;
        if (!window.confirm(`Permanently delete account ${targetEmail}? This cannot be undone.`)) return;
        setIsBusy(true); setMessage(''); setIsError(false);
        try {
            const res = await api.delete(`/admin/accounts/${encodeURIComponent(targetEmail)}`);
            setMessage(res.data || `Account successfully vaporized.`); setTargetEmail('');
        } catch (err: any) {
            setIsError(true); setMessage(err.response?.data || "Failed to delete account.");
        } finally { setIsBusy(false); }
    };

    // --- Quest Logic ---
    const handleSaveQuest = async () => {
        setIsBusy(true);
        try {
            if (editingQuest.id) await api.put(`/admin/quests/${editingQuest.id}`, editingQuest);
            else await api.post('/admin/quests', editingQuest);
            alert("Quest successfully saved!");
            setEditingQuest(emptyQuest); fetchQuests(); setSubTab('DATABASE');
        } catch (err: any) { alert(err.response?.data || "Failed to save quest."); } finally { setIsBusy(false); }
    };

    const handleTogglePublish = async (quest: AdminQuest) => {
        if (!quest.id) return;
        try { await api.put(`/admin/quests/${quest.id}`, { ...quest, published: !quest.published }); fetchQuests(); } catch (err) { alert("Failed to update status."); }
    };

    const handleDeleteQuest = async (id: string) => {
        if (!window.confirm("Warning: Hard-deleting will fail if players have already completed it. Unpublish instead. Proceed?")) return;
        try { await api.delete(`/admin/quests/${id}`); fetchQuests(); } catch (err: any) { alert(err.response?.data || "Failed to delete quest."); }
    };

    // --- Hero Logic ---
    const handleSaveHero = async () => {
        setIsBusy(true);
        try {
            if (editingHero.template.id) await api.put(`/admin/heroes/${editingHero.template.id}`, editingHero);
            else await api.post('/admin/heroes', editingHero);
            alert("Hero successfully saved!");
            setEditingHero(emptyHero); fetchHeroes(); setSubTab('DATABASE');
        } catch (err: any) { alert(err.response?.data || "Failed to save hero."); } finally { setIsBusy(false); }
    };

    const handleDeleteHero = async (id: string) => {
        if (!window.confirm("Warning: Hard-deleting will fail if players already own this hero. Set Spawn Weight to 0 instead. Proceed?")) return;
        try { await api.delete(`/admin/heroes/${id}`); fetchHeroes(); } catch (err: any) { alert(err.response?.data || "Failed to delete hero."); }
    };

    // --- Builders ---
    const currentStats = safeParseObj(editingQuest.targetStatsJson);
    const currentAdvClasses = safeParseArr(editingQuest.advantageClassesJson);
    const currentDisClasses = safeParseArr(editingQuest.disadvantageClassesJson);
    const parsedQuips = safeParseObj(editingHero.template.flavorQuips);

    return (
        <div style={{ width: '100%', maxWidth: '1600px', margin: '0 auto', paddingTop: 'var(--space-lg)', paddingBottom: '100px' }}>
            <h2 style={{ color: 'var(--accent-gold)', marginBottom: 'var(--space-md)' }}>System Administration</h2>

            <div style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
                <div className="missions-nav" style={{ flex: 1, margin: 0 }}>
                    <div className={`missions-tab ${activeModule === 'QUESTS' ? 'active' : ''}`} onClick={() => { setActiveModule('QUESTS'); setSubTab('DATABASE'); }}>Quests Manager</div>
                    <div className={`missions-tab ${activeModule === 'HEROES' ? 'active' : ''}`} onClick={() => { setActiveModule('HEROES'); setSubTab('DATABASE'); }}>Heroes Manager</div>
                    <div className={`missions-tab ${activeModule === 'DANGER' ? 'active' : ''}`} onClick={() => setActiveModule('DANGER')} style={{ color: activeModule === 'DANGER' ? 'var(--danger)' : '' }}>Danger Zone</div>
                </div>
            </div>

            {activeModule !== 'DANGER' && (
                <div className="missions-nav" style={{ marginBottom: '20px', width: '300px' }}>
                    <div className={`missions-tab ${subTab === 'DATABASE' ? 'active' : ''}`} onClick={() => setSubTab('DATABASE')}>Database</div>
                    <div className={`missions-tab ${subTab === 'FORGE' ? 'active' : ''}`} onClick={() => { setSubTab('FORGE'); if (activeModule==='QUESTS') setEditingQuest(emptyQuest); else setEditingHero(emptyHero); }}>Forge New</div>
                </div>
            )}

            {/* --- DANGER ZONE --- */}
            {activeModule === 'DANGER' && (
                <div className="panel" style={{ background: 'var(--bg-elevated)', border: '1px solid var(--danger)' }}>
                    <h3 style={{ color: 'var(--danger)', marginBottom: 'var(--space-sm)' }}>Account Purge</h3>
                    <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: 'var(--space-lg)' }}>Wipe all characters, buildings, resources, and the root profile from the database.</p>
                    <div style={{ display: 'flex', gap: '12px' }}>
                        <input type="text" placeholder="target@example.com" className="input" style={{ flex: 1 }} value={targetEmail} onChange={(e) => setTargetEmail(e.target.value)} disabled={isBusy} />
                        <button className="button--danger" onClick={handleDeleteAccount} disabled={isBusy || !targetEmail} style={{ padding: '0 24px', whiteSpace: 'nowrap' }}>{isBusy ? 'Processing...' : 'Delete Account'}</button>
                    </div>
                    {message && <div style={{ marginTop: 'var(--space-lg)', padding: '12px', background: isError ? 'rgba(220, 53, 69, 0.1)' : 'rgba(40, 167, 69, 0.1)', border: `1px solid ${isError ? 'var(--danger)' : 'var(--success)'}`, borderRadius: '4px', color: isError ? 'var(--danger)' : 'var(--success)', fontSize: '0.9rem', fontWeight: 'bold' }}>{message}</div>}
                </div>
            )}

            {/* --- QUESTS --- */}
            {activeModule === 'QUESTS' && subTab === 'DATABASE' && (
                <div className="panel">
                    <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>Quest Database</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {quests.map(quest => (
                            <div key={quest.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '15px', background: 'var(--surface-1)', border: '1px solid var(--border-subtle)', borderRadius: '6px' }}>
                                <div>
                                    <div style={{ fontWeight: 'bold', color: 'var(--accent-gold)' }}>{quest.title} <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginLeft: '8px' }}>{quest.type}</span></div>
                                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '4px' }}>{quest.durationHours} Hours | {quest.baseExp} EXP</div>
                                </div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                                    <button onClick={() => handleTogglePublish(quest)} style={{ padding: '6px 12px', fontSize: '0.8rem', borderRadius: '4px', border: '1px solid', cursor: 'pointer', background: 'transparent', borderColor: quest.published ? 'var(--success)' : 'var(--text-muted)', color: quest.published ? 'var(--success)' : 'var(--text-muted)' }}>{quest.published ? '🟢 Published' : '⚫ Hidden'}</button>
                                    <button className="button--secondary" onClick={() => { setEditingQuest(quest); setSubTab('FORGE'); }} style={{ padding: '6px 12px', fontSize: '0.8rem' }}>Edit</button>
                                    <button className="button--danger" onClick={() => handleDeleteQuest(quest.id!)} style={{ padding: '6px 12px', fontSize: '0.8rem' }}>Delete</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {activeModule === 'QUESTS' && subTab === 'FORGE' && (
                <div className="panel">
                    <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>{editingQuest.id ? 'Edit Existing Quest' : 'Forge New Quest'}</h3>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '30px' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div><label className="input-label">Quest Title</label><input type="text" className="input" value={editingQuest.title} onChange={e => setEditingQuest({...editingQuest, title: e.target.value})} /></div>
                            <div><label className="input-label">Quest Type</label><select className="input" value={editingQuest.type} onChange={e => setEditingQuest({...editingQuest, type: e.target.value})}><option value="SCOUTING">SCOUTING</option><option value="HUNT">HUNT</option><option value="RAIDING">RAIDING</option><option value="STORY">STORY</option><option value="ESCORT">ESCORT</option></select></div>
                            <div><label className="input-label">Description</label><textarea className="input" rows={4} value={editingQuest.description} onChange={e => setEditingQuest({...editingQuest, description: e.target.value})}></textarea></div>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                <div><label className="input-label">Base EXP</label><input type="number" className="input" value={editingQuest.baseExp} onChange={e => setEditingQuest({...editingQuest, baseExp: parseInt(e.target.value) || 0})} /></div>
                                <div><label className="input-label">Duration (Hours)</label><input type="number" className="input" value={editingQuest.durationHours} onChange={e => setEditingQuest({...editingQuest, durationHours: parseInt(e.target.value) || 0})} /></div>
                            </div>

                            <div style={{ marginTop: '10px', padding: '15px', background: 'var(--surface-2)', borderRadius: '6px', border: '1px solid var(--border-subtle)' }}>
                                <label className="input-label">Loot Yields</label>
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '15px' }}>
                                    <div style={{flex: '1 1 80px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>Gold</span><input type="number" className="input" value={editingQuest.rewardGold} onChange={e => setEditingQuest({...editingQuest, rewardGold: parseInt(e.target.value) || 0})} /></div>
                                    <div style={{flex: '1 1 80px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>Gems</span><input type="number" className="input" value={editingQuest.rewardGems} onChange={e => setEditingQuest({...editingQuest, rewardGems: parseInt(e.target.value) || 0})} /></div>
                                    <div style={{flex: '1 1 80px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>Food</span><input type="number" className="input" value={editingQuest.rewardFood} onChange={e => setEditingQuest({...editingQuest, rewardFood: parseInt(e.target.value) || 0})} /></div>
                                    <div style={{flex: '1 1 80px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>Wood</span><input type="number" className="input" value={editingQuest.rewardWood} onChange={e => setEditingQuest({...editingQuest, rewardWood: parseInt(e.target.value) || 0})} /></div>
                                    <div style={{flex: '1 1 80px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>Stone</span><input type="number" className="input" value={editingQuest.rewardStone} onChange={e => setEditingQuest({...editingQuest, rewardStone: parseInt(e.target.value) || 0})} /></div>
                                </div>
                            </div>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                            <div>
                                <label className="input-label">Target Party Stats Requirements</label>
                                <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
                                    <select className="input" value={newStatKey} onChange={e => setNewStatKey(e.target.value)} style={{ width: '100px' }}>{STAT_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}</select>
                                    <input type="number" className="input" value={newStatVal} onChange={e => setNewStatVal(parseInt(e.target.value) || 0)} style={{ width: '80px' }} />
                                    <button className="button--secondary" onClick={() => setEditingQuest({...editingQuest, targetStatsJson: JSON.stringify({...currentStats, [newStatKey]: newStatVal})})}>Add</button>
                                </div>
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                    {Object.entries(currentStats).map(([key, val]) => (
                                        <div key={key} style={{ display: 'flex', alignItems: 'center', background: 'var(--surface-2)', border: '1px solid var(--accent-gold)', borderRadius: '4px', padding: '4px 8px', fontSize: '0.85rem' }}>
                                            <span style={{ fontWeight: 'bold', marginRight: '6px' }}>{key}:</span> {val as number}
                                            <Icon name="close" size={14} style={{ marginLeft: '8px', cursor: 'pointer', color: 'var(--danger)' }} onClick={() => { const u = {...currentStats}; delete u[key]; setEditingQuest({...editingQuest, targetStatsJson: JSON.stringify(u)}); }} />
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div>
                                <label className="input-label">Class Affinities</label>
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '10px' }}>Left-Click for <span style={{color: 'var(--success)'}}>Advantage</span>. Right-Click for <span style={{color: 'var(--danger)'}}>Disadvantage</span>.</div>
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                    {DND_CLASSES.map(cls => {
                                        const isAdv = currentAdvClasses.includes(cls);
                                        const isDis = currentDisClasses.includes(cls);
                                        return (
                                            <div key={cls}
                                                 onClick={() => { let a = [...currentAdvClasses], d = [...currentDisClasses]; d=d.filter(c=>c!==cls); if(a.includes(cls)) a=a.filter(c=>c!==cls); else a.push(cls); setEditingQuest({...editingQuest, advantageClassesJson: JSON.stringify(a), disadvantageClassesJson: JSON.stringify(d)}); }}
                                                 onContextMenu={(e) => { e.preventDefault(); let a = [...currentAdvClasses], d = [...currentDisClasses]; a=a.filter(c=>c!==cls); if(d.includes(cls)) d=d.filter(c=>c!==cls); else d.push(cls); setEditingQuest({...editingQuest, advantageClassesJson: JSON.stringify(a), disadvantageClassesJson: JSON.stringify(d)}); }}
                                                 style={{ padding: '6px 12px', fontSize: '0.75rem', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', border: '1px solid', userSelect: 'none', background: isAdv ? 'rgba(40, 167, 69, 0.15)' : isDis ? 'rgba(220, 53, 69, 0.15)' : 'var(--surface-2)', borderColor: isAdv ? 'var(--success)' : isDis ? 'var(--danger)' : 'var(--border-strong)', color: isAdv ? 'var(--success)' : isDis ? 'var(--danger)' : 'var(--text-muted)' }}
                                            >{cls} {isAdv && '▲'} {isDis && '▼'}</div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div style={{ marginTop: '30px', display: 'flex', justifyContent: 'flex-end' }}>
                        <button className="button button--claim" onClick={handleSaveQuest} disabled={isBusy || !editingQuest.title}>{editingQuest.id ? 'Save Changes' : 'Forge Quest Template'}</button>
                    </div>
                </div>
            )}

            {/* --- HEROES --- */}
            {activeModule === 'HEROES' && subTab === 'DATABASE' && (
                <div className="panel">
                    <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>Hero Database</h3>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {heroes.map(hero => (
                            <div key={hero.template.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '15px', background: 'var(--surface-1)', border: '1px solid var(--border-subtle)', borderRadius: '6px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                                    <img src={hero.template.portraitUrl} alt={hero.template.name} style={{width: '40px', height: '40px', borderRadius: '50%', border: '2px solid var(--accent-gold)'}} />
                                    <div>
                                        <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{hero.template.name} <span style={{ fontSize: '0.7rem', color: 'var(--accent-gold)', marginLeft: '8px' }}>{hero.template.rarity}</span></div>
                                        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '4px' }}>{hero.template.dndClass} | Weight: {hero.tavernWeight}</div>
                                    </div>
                                </div>
                                <div>
                                    <button className="button--secondary" onClick={() => { setEditingHero(hero); setSubTab('FORGE'); }} style={{ padding: '6px 12px', fontSize: '0.8rem', marginRight: '10px' }}>Edit</button>
                                    <button className="button--danger" onClick={() => handleDeleteHero(hero.template.id!)} style={{ padding: '6px 12px', fontSize: '0.8rem' }}>Delete</button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {activeModule === 'HEROES' && subTab === 'FORGE' && (
                <div className="panel">
                    <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>{editingHero.template.id ? 'Edit Existing Hero' : 'Forge New Hero'}</h3>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '30px' }}>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                <div><label className="input-label">Hero Name</label><input type="text" className="input" value={editingHero.template.name} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, name: e.target.value}})} /></div>
                                <div><label className="input-label">Portrait URL</label><input type="text" className="input" value={editingHero.template.portraitUrl} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, portraitUrl: e.target.value}})} /></div>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                <div><label className="input-label">Rarity</label><select className="input" value={editingHero.template.rarity} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, rarity: e.target.value}})}>{RARITIES.map(r => <option key={r} value={r}>{r}</option>)}</select></div>
                                <div><label className="input-label">Class</label><select className="input" value={editingHero.template.dndClass} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, dndClass: e.target.value}})}>{DND_CLASSES.map(c => <option key={c} value={c}>{c}</option>)}</select></div>
                            </div>

                            <div><label className="input-label">Description</label><textarea className="input" rows={3} value={editingHero.template.description} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, description: e.target.value}})}></textarea></div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '10px' }}>
                                <div><label className="input-label">Hit Die (dX)</label><select className="input" value={editingHero.template.hitDieType} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, hitDieType: parseInt(e.target.value)}})}><option value="6">d6</option><option value="8">d8</option><option value="10">d10</option><option value="12">d12</option></select></div>
                                <div><label className="input-label">Tavern Spawn Weight</label><input type="number" className="input" value={editingHero.tavernWeight} onChange={e => setEditingHero({...editingHero, tavernWeight: parseInt(e.target.value) || 0})} /></div>
                                <div><label className="input-label">Primary Stat</label><select className="input" value={editingHero.template.primaryStat} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, primaryStat: e.target.value}})}>{STAT_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}</select></div>
                            </div>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                            <label className="input-label" style={{marginBottom: '-5px'}}>Base Stats</label>
                            {/* FIXED: Using flex with wrap instead of a rigid grid to prevent overflow */}
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', background: 'var(--surface-2)', padding: '15px', borderRadius: '6px', border: '1px solid var(--border-subtle)' }}>
                                <div style={{flex: '1 1 60px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>STR</span><input type="number" className="input" value={editingHero.template.baseStr} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseStr: parseInt(e.target.value) || 0}})} /></div>
                                <div style={{flex: '1 1 60px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>DEX</span><input type="number" className="input" value={editingHero.template.baseDex} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseDex: parseInt(e.target.value) || 0}})} /></div>
                                <div style={{flex: '1 1 60px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>CON</span><input type="number" className="input" value={editingHero.template.baseCon} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseCon: parseInt(e.target.value) || 0}})} /></div>
                                <div style={{flex: '1 1 60px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>INT</span><input type="number" className="input" value={editingHero.template.baseInt} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseInt: parseInt(e.target.value) || 0}})} /></div>
                                <div style={{flex: '1 1 60px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>WIS</span><input type="number" className="input" value={editingHero.template.baseWis} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseWis: parseInt(e.target.value) || 0}})} /></div>
                                <div style={{flex: '1 1 60px'}}><span style={{fontSize: '0.7rem', color: 'var(--text-muted)'}}>CHA</span><input type="number" className="input" value={editingHero.template.baseCha} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseCha: parseInt(e.target.value) || 0}})} /></div>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                <div><label className="input-label">Base Gold Cost</label><input type="number" className="input" value={editingHero.template.baseGoldCost} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseGoldCost: parseInt(e.target.value) || 0}})} /></div>
                                <div><label className="input-label">Base Gem Cost</label><input type="number" className="input" value={editingHero.template.baseGemCost} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, baseGemCost: parseInt(e.target.value) || 0}})} /></div>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                <div><label className="input-label">IDLE Quip</label><input type="text" className="input" value={parsedQuips.IDLE || ''} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, flavorQuips: JSON.stringify({...parsedQuips, IDLE: e.target.value})}})} /></div>
                                <div><label className="input-label">MISSION Quip</label><input type="text" className="input" value={parsedQuips.MISSION || ''} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, flavorQuips: JSON.stringify({...parsedQuips, MISSION: e.target.value})}})} /></div>
                            </div>
                        </div>
                    </div>

                    <div style={{ marginTop: '30px', display: 'flex', justifyContent: 'flex-end' }}>
                        <button className="button button--claim" onClick={handleSaveHero} disabled={isBusy || !editingHero.template.name}>{editingHero.template.id ? 'Save Changes' : 'Forge Hero Template'}</button>
                    </div>
                </div>
            )}
        </div>
    );
}