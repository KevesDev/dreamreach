import { useState, useEffect, useMemo } from 'react';
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
}

export default function MissionsView() {
    const [roster, setRoster] = useState<Character[]>([]);
    const [quests, setQuests] = useState<Quest[]>([]);
    const [selectedQuestId, setSelectedQuestId] = useState<string | null>(null);

    const [partySlots, setPartySlots] = useState<(Character | null)[]>([null, null, null, null, null]);
    const [successChance, setSuccessChance] = useState<number>(0);
    const [loading, setLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        Promise.all([
            api.get('/roster'),
            api.get('/missions/quests')
        ]).then(([rosterRes, questsRes]) => {
            setRoster(rosterRes.data);
            setQuests(questsRes.data);
            if (questsRes.data.length > 0) {
                setSelectedQuestId(questsRes.data[0].id);
            }
            setLoading(false);
        }).catch(err => {
            console.error("Failed to load missions data", err);
            setLoading(false);
        });
    }, []);

    // Re-calculate math against backend when assembly changes
    useEffect(() => {
        if (!selectedQuestId) {
            setSuccessChance(0);
            return;
        }

        const activeCharacterIds = partySlots
            .filter(slot => slot !== null)
            .map(slot => slot!.characterId);

        if (activeCharacterIds.length === 0) {
            setSuccessChance(0);
            return;
        }

        api.post('/missions/party/calculate', {
            characterIds: activeCharacterIds,
            questId: selectedQuestId
        }).then(res => {
            setSuccessChance(res.data.successChance);
        }).catch(err => {
            console.error('Failed to calculate chance', err);
            setSuccessChance(0);
        });
    }, [partySlots, selectedQuestId]);

    const handleSaveParty = () => {
        const activeCharacterIds = partySlots
            .filter(slot => slot !== null)
            .map(slot => slot!.characterId);

        setIsSaving(true);
        api.post('/missions/party/save', { characterIds: activeCharacterIds })
            .then(() => alert("Party assembly locked in!"))
            .catch(() => alert("Failed to save party."))
            .finally(() => setIsSaving(false));
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

    // Strict Filter Rules: Must be IDLE, and not already sitting in an active slot
    const availableRoster = useMemo(() => {
        const slotIds = partySlots.filter(s => s !== null).map(s => s!.characterId);
        return roster.filter(char =>
            char.status.toUpperCase() === 'IDLE' &&
            !slotIds.includes(char.characterId)
        );
    }, [roster, partySlots]);

    const selectedQuest = quests.find(q => q.id === selectedQuestId);

    const getSuccessColorClass = () => {
        if (successChance >= 80) return 'success-high';
        if (successChance >= 40) return 'success-med';
        return 'success-low';
    };

    if (loading) return <div className="panel" style={{ margin: 'var(--space-md)' }}>Loading War Room...</div>;

    return (
        <div className="missions-container">
            {/* Left Panel: Ledger */}
            <aside className="ledger-pane">
                <div className="ledger-header">
                    <h2>Mission Journal</h2>
                </div>

                <div className="quest-list">
                    {quests.length === 0 ? (
                        <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                            No active missions available.
                        </div>
                    ) : (
                        quests.map(quest => (
                            <div
                                key={quest.id}
                                className={`quest-card ${selectedQuestId === quest.id ? 'selected' : ''}`}
                                data-type={quest.type}
                                onClick={() => setSelectedQuestId(quest.id)}
                            >
                                <div className="quest-card-title">{quest.title}</div>
                                <div className="quest-card-type">{quest.type}</div>
                            </div>
                        ))
                    )}
                </div>

                <div className="quest-details-panel">
                    {selectedQuest ? (
                        <>
                            <h3 className="quest-details-title">{selectedQuest.title}</h3>
                            <div className="quest-details-desc">"{selectedQuest.description}"</div>
                            <div className={`success-indicator ${getSuccessColorClass()}`}>
                                Success Chance: {successChance}%
                            </div>
                        </>
                    ) : (
                        <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: 'auto', marginBottom: 'auto' }}>
                            Select a quest to view details.
                        </div>
                    )}
                </div>
            </aside>

            {/* Right Panel: Assembly */}
            <main className="assembly-pane">
                <div className="party-slots-container">
                    <h3 style={{ color: 'var(--text-primary)', marginBottom: 'var(--space-lg)' }}>Party Assembly</h3>
                    <div className="party-slots-grid">
                        {partySlots.map((slot, index) => (
                            <div
                                key={index}
                                className={`party-slot ${slot ? 'filled' : 'empty'}`}
                                onClick={() => slot && removeFromSlot(index)}
                            >
                                {slot ? (
                                    <>
                                        <img src={slot.portraitUrl || '/assets/hero.png'} alt={slot.name} className="slot-portrait" />
                                        <div className="slot-name">{slot.name}</div>
                                        <div className="slot-class">Lv.{slot.level} {slot.dndClass}</div>
                                    </>
                                ) : (
                                    <Icon name="plus" size={24} style={{ color: 'var(--border-strong)' }} />
                                )}
                            </div>
                        ))}
                    </div>

                    <button
                        className="button button--claim"
                        style={{ padding: '12px 32px', fontSize: '1rem' }}
                        onClick={handleSaveParty}
                        disabled={isSaving || partySlots.every(slot => slot === null) || !selectedQuestId}
                    >
                        Lock In Party
                    </button>
                </div>

                <div className="roster-carousel-container">
                    <div className="roster-carousel-header">Available Heroes</div>
                    {availableRoster.length === 0 ? (
                        <div style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '20px' }}>
                            No available heroes. (Check if they are resting or already assigned).
                        </div>
                    ) : (
                        <div className="roster-carousel">
                            {availableRoster.map(char => (
                                <div
                                    key={char.characterId}
                                    className="carousel-card"
                                    onClick={() => addToSlot(char)}
                                >
                                    <img src={char.portraitUrl || '/assets/hero.png'} alt={char.name} className="carousel-portrait" />
                                    <div className="carousel-name" title={char.name}>{char.name}</div>
                                    <div className="carousel-class">{char.dndClass}</div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}