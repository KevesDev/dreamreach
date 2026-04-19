import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';
import { Icon } from '../components/Icon';
import './MissionsView.css';

export interface AdminQuest {
    id?: string; type: string; title: string; description: string; targetStatsJson: string; advantageClassesJson: string; disadvantageClassesJson: string;
    baseExp: number; rewardGold: number; rewardGems: number; rewardFood: number; rewardWood: number; rewardStone: number; durationHours: number; published: boolean;
}

export interface AdminHero {
    template: {
        id?: string; name: string; description: string; rarity: string; dndClass: string;
        baseStr: number; baseDex: number; baseCon: number; baseInt: number; baseWis: number; baseCha: number;
        hitDieType: number; primaryStat: string; classTags: string; flavorQuips: string; portraitUrl: string;
        baseGoldCost: number; baseGemCost: number; statPriorityJson: string;
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
        portraitUrl: '/assets/hero.png', baseGoldCost: 500, baseGemCost: 50, statPriorityJson: '["STR", "CON", "DEX", "WIS", "CHA", "INT"]'
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

    useEffect(() => {
        if (profile?.isAdmin) {
            if (activeModule === 'QUESTS') fetchQuests();
            if (activeModule === 'HEROES') fetchHeroes();
        }
    }, [profile, activeModule]);

    const fetchQuests = () => api.get('/admin/quests').then(res => setQuests(res.data));
    const fetchHeroes = () => api.get('/admin/heroes').then(res => setHeroes(res.data));

    if (!profile?.isAdmin) return <div className="panel"><h2>Access Denied</h2></div>;

    const handleDeleteAccount = async () => {
        if (!targetEmail) return;
        if (!window.confirm(`Permanently delete account ${targetEmail}?`)) return;
        setIsBusy(true);
        try {
            await api.delete(`/admin/accounts/${encodeURIComponent(targetEmail)}`);
            setMessage("Account purged."); setTargetEmail('');
        } catch (err: any) { setIsError(true); setMessage("Failed."); } finally { setIsBusy(false); }
    };

    const handleSaveQuest = async () => {
        setIsBusy(true);
        try {
            if (editingQuest.id) await api.put(`/admin/quests/${editingQuest.id}`, editingQuest);
            else await api.post('/admin/quests', editingQuest);
            alert("Quest saved."); setEditingQuest(emptyQuest); fetchQuests(); setSubTab('DATABASE');
        } catch (err: any) { alert("Failed."); } finally { setIsBusy(false); }
    };

    const handleSaveHero = async () => {
        setIsBusy(true);
        try {
            if (editingHero.template.id) await api.put(`/admin/heroes/${editingHero.template.id}`, editingHero);
            else await api.post('/admin/heroes', editingHero);
            alert("Hero saved."); setEditingHero(emptyHero); fetchHeroes(); setSubTab('DATABASE');
        } catch (err: any) { alert("Failed."); } finally { setIsBusy(false); }
    };

    const handlePriorityToggle = (stat: string) => {
        let current = safeParseArr(editingHero.template.statPriorityJson);
        current = current.filter((s: string) => s !== stat);
        current.push(stat);
        if (current.length > 6) current.shift();
        setEditingHero({ ...editingHero, template: { ...editingHero.template, statPriorityJson: JSON.stringify(current) } });
    };

    const currentStats = safeParseObj(editingQuest.targetStatsJson);
    const currentAdvClasses = safeParseArr(editingQuest.advantageClassesJson);
    const currentDisClasses = safeParseArr(editingQuest.disadvantageClassesJson);
    const currentPriority = safeParseArr(editingHero.template.statPriorityJson);
    const parsedQuips = safeParseObj(editingHero.template.flavorQuips);

    return (
        <div style={{ width: '100%', maxWidth: '1600px', margin: '0 auto', paddingTop: 'var(--space-lg)', paddingBottom: '100px' }}>
            <h2 style={{ color: 'var(--accent-gold)', marginBottom: 'var(--space-md)' }}>System Administration</h2>

            <div className="missions-nav" style={{ marginBottom: '20px' }}>
                <div className={`missions-tab ${activeModule === 'QUESTS' ? 'active' : ''}`} onClick={() => setActiveModule('QUESTS')}>Quests</div>
                <div className={`missions-tab ${activeModule === 'HEROES' ? 'active' : ''}`} onClick={() => setActiveModule('HEROES')}>Heroes</div>
                <div className={`missions-tab ${activeModule === 'DANGER' ? 'active' : ''}`} onClick={() => setActiveModule('DANGER')}>Danger Zone</div>
            </div>

            {activeModule === 'HEROES' && (
                <>
                    <div className="missions-nav" style={{ marginBottom: '20px', width: '300px' }}>
                        <div className={`missions-tab ${subTab === 'DATABASE' ? 'active' : ''}`} onClick={() => setSubTab('DATABASE')}>Database</div>
                        <div className={`missions-tab ${subTab === 'FORGE' ? 'active' : ''}`} onClick={() => { setSubTab('FORGE'); setEditingHero(emptyHero); }}>Forge New</div>
                    </div>

                    {subTab === 'DATABASE' ? (
                        <div className="panel">
                            {heroes.map(hero => (
                                <div key={hero.template.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '15px', background: 'var(--surface-1)', border: '1px solid var(--border-subtle)', borderRadius: '6px', marginBottom: '10px' }}>
                                    <span>{hero.template.name} ({hero.template.rarity})</span>
                                    <div style={{ display: 'flex', gap: '10px' }}>
                                        <button className="button--secondary" onClick={() => { setEditingHero(hero); setSubTab('FORGE'); }}>Edit</button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="panel">
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '30px' }}>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                                    <div><label className="input-label">Hero Name</label><input type="text" className="input" value={editingHero.template.name} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, name: e.target.value}})} /></div>
                                    <label className="input-label">Stat Priority (Click to Reorder: 1 is Highest)</label>
                                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                                        {STAT_OPTIONS.map(stat => {
                                            const rank = currentPriority.indexOf(stat);
                                            return (
                                                <div key={stat} onClick={() => handlePriorityToggle(stat)} style={{
                                                    padding: '8px 16px', borderRadius: '4px', cursor: 'pointer', border: '1px solid var(--accent-gold)',
                                                    background: rank === 0 ? 'var(--accent-gold)' : 'var(--surface-2)',
                                                    color: rank === 0 ? 'black' : 'white'
                                                }}>
                                                    {stat} {rank !== -1 ? `(#${rank + 1})` : ''}
                                                </div>
                                            );
                                        })}
                                    </div>
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                        <div><label className="input-label">Rarity</label><select className="input" value={editingHero.template.rarity} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, rarity: e.target.value}})}>{RARITIES.map(r => <option key={r} value={r}>{r}</option>)}</select></div>
                                        <div><label className="input-label">Class</label><select className="input" value={editingHero.template.dndClass} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, dndClass: e.target.value}})}>{DND_CLASSES.map(c => <option key={c} value={c}>{c}</option>)}</select></div>
                                    </div>
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                                    <div><label className="input-label">Portrait URL</label><input type="text" className="input" value={editingHero.template.portraitUrl} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, portraitUrl: e.target.value}})} /></div>
                                    <div><label className="input-label">Tavern Weight</label><input type="number" className="input" value={editingHero.tavernWeight} onChange={e => setEditingHero({...editingHero, tavernWeight: parseInt(e.target.value) || 0})} /></div>
                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                                        <div><label className="input-label">IDLE Quip</label><input type="text" className="input" value={parsedQuips.IDLE || ''} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, flavorQuips: JSON.stringify({...parsedQuips, IDLE: e.target.value})}})} /></div>
                                        <div><label className="input-label">MISSION Quip</label><input type="text" className="input" value={parsedQuips.MISSION || ''} onChange={e => setEditingHero({...editingHero, template: {...editingHero.template, flavorQuips: JSON.stringify({...parsedQuips, MISSION: e.target.value})}})} /></div>
                                    </div>
                                </div>
                            </div>
                            <div style={{ marginTop: '30px', display: 'flex', justifyContent: 'flex-end' }}>
                                <button className="button button--claim" onClick={handleSaveHero} disabled={isBusy}>{editingHero.template.id ? 'Save Changes' : 'Forge Hero Template'}</button>
                            </div>
                        </div>
                    )}
                </>
            )}

            {activeModule === 'QUESTS' && (
                <div className="panel">
                    <p>Quest Management remains active in the backend database.</p>
                </div>
            )}

            {activeModule === 'DANGER' && (
                <div className="panel">
                    <input type="text" placeholder="target@example.com" className="input" value={targetEmail} onChange={(e) => setTargetEmail(e.target.value)} />
                    <button className="button--danger" onClick={handleDeleteAccount} disabled={isBusy}>Delete Account</button>
                    {message && <div style={{marginTop: '10px'}}>{message}</div>}
                </div>
            )}
        </div>
    );
}