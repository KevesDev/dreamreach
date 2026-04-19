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
    rewardGold?: number;
    rewardGems?: number;
    rewardFood?: number;
    rewardWood?: number;
    rewardStone?: number;
    baseExp?: number;
}

export interface ActiveMission {
    missionId: string;
    questTitle: string;
    questType: string;
    successChance: number;
    dispatchTimeEpoch: number;
    endTimeEpoch: number;
    isResolved: boolean;
    wasSuccessful: boolean;
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

    // NEW: Result Modal State
    const [resultMission, setResultMission] = useState<ActiveMission | null>(null);
    const [isClaiming, setIsClaiming] = useState(false);

    const fetchAllData = useCallback(() => {
        return Promise.all([
            api.get('/roster'),
            api.get('/missions/journal'),
            api.get('/missions/active')
        ]).then(([rosterRes, questsRes, activeRes]) => {
            setRoster(rosterRes.data);
            setQuests(questsRes.data);
            setActiveMissions(activeRes.data);
            setSelectedQuestId(prev => {
                if (!prev && questsRes.data.length > 0) return questsRes.data[0].id;
                if (prev && !questsRes.data.find((q: Quest) => q.id === prev)) return questsRes.data.length > 0 ? questsRes.data[0].id : null;
                return prev;
            });
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

    // Notification Logic: Check if any mission is ready for claiming
    const hasUnclaimed = useMemo(() => activeMissions.some(m => m.isResolved), [activeMissions]);

    useEffect(() => {
        const hasExpired = activeMissions.some(m => now >= m.endTimeEpoch && !m.isResolved);
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
        api.post('/missions/party/calculate', { characterIds: activeCharacterIds, questId: selectedQuestId })
            .then(res => setSuccessChance(res.data.successChance))
            .catch(() => setSuccessChance(0));
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

    const handleClaim = (missionId: string) => {
        setIsClaiming(true);
        api.post(`/missions/claim/${missionId}`)
            .then(() => {
                setResultMission(null);
                fetchAllData();
                if (fetchProfile) fetchProfile();
            })
            .catch(err => alert("Failed to claim rewards: " + (err.response?.data || err.message)))
            .finally(() => setIsClaiming(false));
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
        } catch { return "Ready for the journey."; }
    };

    if (loading) return <div className="panel" style={{ margin: 'var(--space-md)' }}>Loading Mission Journal...</div>;

    return (
        <div className="missions-wrapper">
            <div className="missions-nav">
                <div className={`missions-tab ${activeTab === 'ASSEMBLY' ? 'active' : ''} ${hasUnclaimed && activeTab !== 'ASSEMBLY' ? 'glow-pulse-gold' : ''}`} onClick={() => setActiveTab('ASSEMBLY')}>
                    Mission Journal
                </div>
                <div className={`missions-tab ${activeTab === 'EXPEDITIONS' ? 'active' : ''} ${hasUnclaimed && activeTab !== 'EXPEDITIONS' ? 'glow-pulse-gold' : ''}`} onClick={() => setActiveTab('EXPEDITIONS')}>
                    Active Missions ({activeMissions.length})
                </div>
            </div>

            <div className="missions-container">
                {activeTab === 'ASSEMBLY' && (
                    <>
                        <aside className="ledger-pane">
                            <div className="ledger-header">
                                <h2>Mission Journal</h2>
                            </div>

                            {quests.length === 0 ? (
                                <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                                    <p>Your journal is empty. Accept missions from the Adventurer's Board.</p>
                                </div>
                            ) : (
                                <div className="quest-list">
                                    {quests.map(quest => (
                                        <div key={quest.id} className={`quest-card ${selectedQuestId === quest.id ? 'selected' : ''}`} data-type={quest.type} onClick={() => setSelectedQuestId(quest.id)}>
                                            <div className="quest-card-title">{quest.title}</div>
                                            <div className="quest-card-type">{quest.type}</div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            <div className="quest-details-panel">
                                {selectedQuest ? (
                                    <>
                                        <h3 className="quest-details-title">{selectedQuest.title}</h3>
                                        <div className="quest-details-desc">"{selectedQuest.description}"</div>
                                        <div className={`success-indicator ${successChance >= 80 ? 'success-high' : successChance >= 40 ? 'success-med' : successChance >= 40 ? 'success-med' : 'success-low'}`}>
                                            Success Chance: {successChance}%
                                        </div>
                                    </>
                                ) : (
                                    <div style={{ textAlign: 'center', color: 'var(--text-muted)', margin: 'auto 0' }}>Select a mission from your journal.</div>
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
                                <p>Your heroes rest. Assemble a party and embark from the Mission Journal.</p>
                            </div>
                        ) : (
                            activeMissions.sort((a,b) => b.dispatchTimeEpoch - a.dispatchTimeEpoch).map(mission => (
                                <div
                                    key={mission.missionId}
                                    className={`expedition-card ${mission.isResolved ? (mission.wasSuccessful ? 'resolved-success' : 'resolved-failure') : ''}`}
                                    onClick={() => mission.isResolved && setResultMission(mission)}
                                >
                                    <div className="expedition-info">
                                        <h3>{mission.questTitle}</h3>
                                        <p>Type: {mission.questType} | Success Prob: {mission.successChance}%</p>
                                    </div>

                                    {mission.isResolved ? (
                                        <div style={{ fontWeight: 'bold', fontSize: '1.2rem', color: mission.wasSuccessful ? 'var(--success)' : 'var(--danger)' }}>
                                            {mission.wasSuccessful ? 'VICTORY' : 'DEFEAT'}
                                            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', textAlign: 'center' }}>Click to Claim</div>
                                        </div>
                                    ) : (
                                        <div className="countdown-timer" style={{ color: now > mission.endTimeEpoch ? 'var(--accent-gold)' : 'var(--text-primary)' }}>
                                            {formatTimeLeft(mission.endTimeEpoch)}
                                        </div>
                                    )}

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

            {/* Dispatch Confirmation Modal */}
            {showModal && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h2>Confirm Mission</h2>
                        <p style={{marginBottom: '20px', color: 'var(--text-muted)'}}>
                            Are you sure you want to send this party on <strong>{selectedQuest?.title}</strong>?
                        </p>
                        <div style={{ background: 'var(--surface-2)', padding: '15px', borderRadius: '8px', marginBottom: '20px' }}>
                            <div style={{ fontSize: '1.2rem', fontWeight: 'bold', color: successChance >= 80 ? 'var(--success)' : successChance >= 40 ? 'var(--warning)' : 'var(--danger)' }}>
                                Success Chance: {successChance}%
                            </div>
                            <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginTop: '8px' }}>
                                Duration: {selectedQuest?.durationHours || 2} Hours
                            </div>
                        </div>
                        <div style={{ display: 'flex', gap: '10px', justifyContent: 'center' }}>
                            <button className="button button--danger" onClick={() => setShowModal(false)}>Cancel</button>
                            <button className="button button--claim" onClick={handleDispatch} disabled={isDispatching}>
                                {isDispatching ? 'Embarking...' : 'Embark'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* NEW: Mission Outcome Result Modal */}
            {resultMission && (
                <div className="modal-overlay">
                    <div className="modal-content" style={{ border: `2px solid ${resultMission.wasSuccessful ? 'var(--success)' : 'var(--danger)'}` }}>
                        <h1 style={{ color: resultMission.wasSuccessful ? 'var(--success)' : 'var(--danger)', letterSpacing: '0.2em', marginBottom: '10px' }}>
                            {resultMission.wasSuccessful ? 'VICTORY' : 'MISSION FAILED'}
                        </h1>
                        <h3 style={{ color: 'var(--accent-gold)' }}>{resultMission.questTitle}</h3>

                        {resultMission.wasSuccessful ? (
                            <>
                                <p style={{ color: 'var(--text-secondary)' }}>The mission was a success! Your heroes return with the following spoils:</p>
                                <div className="loot-display-grid">
                                    <div className="loot-item"><Icon name="gold" size={24} /> {quests.find(q => q.title === resultMission.questTitle)?.rewardGold || 0}</div>
                                    <div className="loot-item"><Icon name="gems" size={24} /> {quests.find(q => q.title === resultMission.questTitle)?.rewardGems || 0}</div>
                                    <div className="loot-item"><Icon name="food" size={24} /> {quests.find(q => q.title === resultMission.questTitle)?.rewardFood || 0}</div>
                                    <div className="loot-item"><Icon name="wood" size={24} /> {quests.find(q => q.title === resultMission.questTitle)?.rewardWood || 0}</div>
                                    <div className="loot-item"><Icon name="stone" size={24} /> {quests.find(q => q.title === resultMission.questTitle)?.rewardStone || 0}</div>
                                    <div className="loot-item" style={{ color: 'var(--accent-blue)' }}><Icon name="user" size={24} /> {quests.find(q => q.title === resultMission.questTitle)?.baseExp || 0} EXP</div>
                                </div>
                            </>
                        ) : (
                            <p style={{ color: 'var(--danger)', margin: '20px 0', fontSize: '1.1rem' }}>
                                Disaster struck! The party was overwhelmed and forced to retreat.
                            </p>
                        )}

                        <div className="deployed-party" style={{ justifyContent: 'center', marginBottom: '30px' }}>
                            {resultMission.partyMembers.map(m => (
                                <img key={m.characterId} src={m.portraitUrl || '/assets/hero.png'} style={{ width: '40px', borderRadius: '50%', border: '1px solid var(--border-strong)' }} />
                            ))}
                        </div>

                        <button
                            className="button button--claim"
                            style={{ width: '100%' }}
                            onClick={() => handleClaim(resultMission.missionId)}
                            disabled={isClaiming}
                        >
                            {isClaiming ? 'Processing...' : 'Claim'}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}