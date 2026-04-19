import { useState, useEffect, useMemo, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';
import { Icon } from '../components/Icon';
import './MissionsView.css';

export interface Character {
    characterId: string;
    name: string;
    rarity: string;
    dndClass: string;
    level: number;
    status: string;
    portraitUrl?: string;
}

export interface Quest {
    id: string;
    type: string;
    title: string;
    description: string;
    durationHours: number;
}

export interface ActiveMission {
    missionId: string;
    questTitle: string;
    questType: string;
    successChance: number;
    dispatchTimeEpoch: number;
    endTimeEpoch: number;
    partyMembers: { characterId: string; name: string; portraitUrl: string; flavorQuipsJson: string }[];
}

export default function MissionsView() {
    const { fetchProfile } = useOutletContext<any>();
    const [activeTab, setActiveTab] = useState<'ASSEMBLY' | 'EXPEDITIONS'>('ASSEMBLY');
    const [roster, setRoster] = useState<Character[]>([]);
    const [quests, setQuests] = useState<Quest[]>([]);
    const [activeMissions, setActiveMissions] = useState<ActiveMission[]>([]);
    const [selectedQuestId, setSelectedQuestId] = useState<string | null>(null);

    const [partySlots, setPartySlots] = useState<(Character | null)[]>([null, null, null, null, null]);
    const [successChance, setSuccessChance] = useState<number>(0);
    const [loading, setLoading] = useState(true);

    const [showModal, setShowModal] = useState(false);
    const [isDispatching, setIsDispatching] = useState(false);
    const [now, setNow] = useState(Date.now());
    const [isResolving, setIsResolving] = useState(false);

    const fetchAllData = useCallback(() => {
        return Promise.all([
            api.get('/roster'),
            api.get('/missions/quests'),
            api.get('/missions/active')
        ]).then(([rosterRes, questsRes, activeRes]) => {
            setRoster(rosterRes.data);
            setQuests(questsRes.data);
            setActiveMissions(activeRes.data);
            setSelectedQuestId(prev => (!prev && questsRes.data.length > 0) ? questsRes.data[0].id : prev);
        }).catch(err => {
            console.error("Failed to load missions data", err);
        });
    }, []);

    useEffect(() => {
        fetchAllData().finally(() => setLoading(false));
    }, [fetchAllData]);

    useEffect(() => {
        const interval = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        const hasExpired = activeMissions.some(m => now >= m.endTimeEpoch);
        if (hasExpired && !isResolving) {
            setIsResolving(true);
            const timer = setTimeout(() => {
                fetchAllData().then(() => {
                    if (fetchProfile) fetchProfile();
                }).finally(() => setIsResolving(false));
            }, 2000);
            return () => clearTimeout(timer);
        }
    }, [now, activeMissions, isResolving, fetchAllData, fetchProfile]);

    useEffect(() => {
        if (!selectedQuestId || activeTab !== 'ASSEMBLY') {
            setSuccessChance(0);
            return;
        }

        const activeCharacterIds = partySlots.filter(slot => slot !== null).map(slot => slot!.characterId);
        if (activeCharacterIds.length === 0) {
            setSuccessChance(0);
            return;
        }

        api.post('/missions/party/calculate', {
            characterIds: activeCharacterIds,
            questId: selectedQuestId
        }).then(res => {
            setSuccessChance(res.data.successChance);
        }).catch(() => setSuccessChance(0));
    }, [partySlots, selectedQuestId, activeTab]);

    const handleDispatch = () => {
        const activeCharacterIds = partySlots.filter(slot => slot !== null).map(slot => slot!.characterId);
        setIsDispatching(true);
        api.post('/missions/dispatch', { questId: selectedQuestId, characterIds: activeCharacterIds })
            .then(() => {
                setShowModal(false);
                setPartySlots([null, null, null, null, null]);
                fetchAllData();
                setActiveTab('EXPEDITIONS');
            })
            .catch(err => alert("Failed to embark on mission: " + (err.response?.data || err.message)))
            .finally(() => setIsDispatching(false));
    };

    const addToSlot = (char: Character) => {
        const emptyIndex = partySlots.findIndex(slot => slot === null);
        if (emptyIndex !== -1) {
            const newSlots = [...partySlots];
            newSlots[emptyIndex] = char;
            setPartySlots(newSlots);
        }
    };

    const removeFromSlot = (index: number) => {
        const newSlots = [...partySlots];
        newSlots[index] = null;
        setPartySlots(newSlots);
    };

    const availableRoster = useMemo(() => {
        const slotIds = partySlots.filter(s => s !== null).map(s => s!.characterId);
        return roster.filter(char => char.status.toUpperCase() === 'IDLE' && !slotIds.includes(char.characterId));
    }, [roster, partySlots]);

    const selectedQuest = quests.find(q => q.id === selectedQuestId);

    const getSuccessColorClass = () => {
        if (successChance >= 80) return 'success-high';
        if (successChance >= 40) return 'success-med';
        return 'success-low';
    };

    const formatTimeLeft = (endTimeMs: number) => {
        const diff = Math.max(0, endTimeMs - now);
        if (diff === 0) return "Resolving...";
        const h = Math.floor(diff / 3600000).toString().padStart(2, '0');
        const m = Math.floor((diff % 3600000) / 60000).toString().padStart(2, '0');
        const s = Math.floor((diff % 60000) / 1000).toString().padStart(2, '0');
        return `${h}:${m}:${s}`;
    };

    const parseMissionQuip = (jsonString: string | undefined | null) => {
        if (!jsonString) return "Ready for the journey.";
        try {
            const obj = JSON.parse(jsonString);
            return obj.MISSION || obj.IDLE || "Ready for the journey.";
        } catch {
            return "Ready for the journey.";
        }
    };

    if (loading) return <div className="panel" style={{ margin: 'var(--space-md)' }}>Loading Mission Board...</div>;

    return (
        <div className="missions-wrapper">
            <div className="missions-nav">
                <div className={`missions-tab ${activeTab === 'ASSEMBLY' ? 'active' : ''}`} onClick={() => setActiveTab('ASSEMBLY')}>
                    Mission Board
                </div>
                <div className={`missions-tab ${activeTab === 'EXPEDITIONS' ? 'active' : ''}`} onClick={() => setActiveTab('EXPEDITIONS')}>
                    Active Missions ({activeMissions.length})
                </div>
            </div>

            <div className="missions-container">
                {activeTab === 'ASSEMBLY' && (
                    <>
                        <aside className="ledger-pane">
                            <div className="ledger-header">
                                <h2>Mission Board</h2>
                            </div>
                            <div className="quest-list">
                                {quests.map(quest => (
                                    <div key={quest.id} className={`quest-card ${selectedQuestId === quest.id ? 'selected' : ''}`} data-type={quest.type} onClick={() => setSelectedQuestId(quest.id)}>
                                        <div className="quest-card-title">{quest.title}</div>
                                        <div className="quest-card-type">{quest.type}</div>
                                    </div>
                                ))}
                            </div>
                            <div className="quest-details-panel">
                                {selectedQuest ? (
                                    <>
                                        <h3 className="quest-details-title">{selectedQuest.title}</h3>
                                        <div className="quest-details-desc">"{selectedQuest.description}"</div>
                                        <div className={`success-indicator ${getSuccessColorClass()}`}>Success Chance: {successChance}%</div>
                                    </>
                                ) : (
                                    <div style={{ textAlign: 'center', color: 'var(--text-muted)', margin: 'auto 0' }}>Select a mission.</div>
                                )}
                            </div>
                        </aside>

                        <main className="assembly-pane">
                            <div className="party-slots-container">
                                <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>Party Assembly</h3>
                                <div className="party-slots-grid">
                                    {partySlots.map((slot, index) => (
                                        <div key={index} className={`party-slot ${slot ? 'filled' : 'empty'}`} onClick={() => slot && removeFromSlot(index)}>
                                            {slot ? (
                                                <>
                                                    <img src={slot.portraitUrl || '/assets/hero.png'} alt={slot.name} className="slot-portrait" />
                                                    <div className="slot-name">{slot.name}</div>
                                                    <div className="slot-class">Lv.{slot.level} {slot.dndClass}</div>
                                                </>
                                            ) : <Icon name="plus" size={24} style={{ color: 'var(--border-strong)' }} />}
                                        </div>
                                    ))}
                                </div>
                                <button
                                    className="button button--claim" style={{ padding: '12px 32px', fontSize: '1rem' }}
                                    onClick={() => setShowModal(true)}
                                    disabled={partySlots.every(slot => slot === null) || !selectedQuestId}
                                >
                                    Embark on Mission
                                </button>
                            </div>

                            <div className="roster-carousel-container">
                                <div className="roster-carousel-header">Available Heroes</div>
                                <div className="roster-carousel">
                                    {availableRoster.map(char => (
                                        <div key={char.characterId} className="carousel-card" onClick={() => addToSlot(char)}>
                                            <img src={char.portraitUrl || '/assets/hero.png'} alt={char.name} className="carousel-portrait" />
                                            <div className="carousel-name" title={char.name}>{char.name}</div>
                                            <div className="carousel-class">{char.dndClass}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </main>
                    </>
                )}

                {activeTab === 'EXPEDITIONS' && (
                    <main className="expeditions-pane">
                        {activeMissions.length === 0 ? (
                            <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '40px' }}>
                                <Icon name="kingdom" size={48} />
                                <h2>No Active Missions</h2>
                                <p>Your heroes rest. Assemble a party and embark from the Mission Board.</p>
                            </div>
                        ) : (
                            activeMissions.sort((a,b) => b.dispatchTimeEpoch - a.dispatchTimeEpoch).map(mission => (
                                <div key={mission.missionId} className="expedition-card">
                                    <div className="expedition-info">
                                        <h3>{mission.questTitle}</h3>
                                        <p>Type: {mission.questType} | Success Prob: {mission.successChance}%</p>
                                    </div>

                                    <div className="countdown-timer" style={{ color: now > mission.endTimeEpoch ? 'var(--accent-gold)' : 'var(--text-primary)' }}>
                                        {formatTimeLeft(mission.endTimeEpoch)}
                                    </div>

                                    <div className="deployed-party">
                                        {mission.partyMembers.map(member => (
                                            <div key={member.characterId} className="portrait-wrapper">
                                                <img src={member.portraitUrl || '/assets/hero.png'} alt={member.name} />
                                                <div className="speech-bubble">"{parseMissionQuip(member.flavorQuipsJson)}"</div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            ))
                        )}
                    </main>
                )}
            </div>

            {showModal && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2>Confirm Mission</h2>
                        <p style={{marginBottom: '20px', color: 'var(--text-muted)'}}>
                            Are you sure you want to send this party on <strong>{selectedQuest?.title}</strong>?
                        </p>
                        <div style={{ background: 'var(--surface-2)', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
                            <div style={{ fontSize: '1.2rem', fontWeight: 'bold', color: getSuccessColorClass() === 'success-high' ? 'var(--success)' : getSuccessColorClass() === 'success-med' ? 'var(--warning)' : 'var(--danger)' }}>
                                Success Chance: {successChance}%
                            </div>
                            <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginTop: '8px' }}>
                                Duration: {selectedQuest?.durationHours || 4} Hours (XP & Loot awarded upon safe return)
                            </div>
                        </div>
                        <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                            <button className="button button--danger" onClick={() => setShowModal(false)}>Cancel</button>
                            <button className="button button--claim" onClick={handleDispatch} disabled={isDispatching}>
                                {isDispatching ? 'Embarking...' : 'Confirm & Embark'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}