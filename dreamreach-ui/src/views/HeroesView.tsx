import { useState, useEffect, useMemo, useCallback } from 'react';
import api from '../api/client';
import { Icon } from '../components/Icon';
import './HeroesView.css';

export interface Character {
    characterId: string;
    name: string;
    rarity: string;
    dndClass: string;
    level: number;
    currentXp: number;
    totalStrength: number;
    totalDexterity: number;
    totalConstitution: number;
    totalIntelligence: number;
    totalWisdom: number;
    totalCharisma: number;
    strMod: number;
    dexMod: number;
    conMod: number;
    intMod: number;
    wisMod: number;
    chaMod: number;
    currentHp: number;
    maxHp: number;
    spentHitDice: number;
    maxHitDice: number;
    status: string;
    weaponTier: string;
    armorTier: string;
    portraitUrl?: string;
    longRestEndTimeEpoch?: number; // Added to interface
}

export default function HeroesView() {
    const [roster, setRoster] = useState<Character[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedCharacterId, setSelectedCharacterId] = useState<string | null>(null);
    const [isResting, setIsResting] = useState(false);

    // Dynamic Clock State
    const [now, setNow] = useState(Date.now());
    const [isResolving, setIsResolving] = useState(false);

    const fetchRoster = useCallback(() => {
        return api.get('/roster')
            .then(res => {
                setRoster(res.data);
                if (res.data.length > 0 && !selectedCharacterId) {
                    setSelectedCharacterId(res.data[0].characterId);
                }
            })
            .catch(err => {
                console.error("Failed to load roster", err);
            });
    }, [selectedCharacterId]);

    // Initial Load
    useEffect(() => {
        setLoading(true);
        fetchRoster().finally(() => setLoading(false));
    }, [fetchRoster]);

    // 1-Second Global Tick for Countdown Timers
    useEffect(() => {
        const interval = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(interval);
    }, []);

    // Auto-Awaken Hook: Dynamically fetches roster if any character finishes resting
    useEffect(() => {
        const hasFinishedResting = roster.some(
            char => char.status === 'RESTING' && char.longRestEndTimeEpoch && now >= char.longRestEndTimeEpoch
        );

        if (hasFinishedResting && !isResolving) {
            setIsResolving(true);
            setTimeout(() => {
                fetchRoster().finally(() => setIsResolving(false));
            }, 1000);
        }
    }, [now, roster, isResolving, fetchRoster]);

    const handleLongRest = async () => {
        if (!selectedCharacter || isResting) return;
        setIsResting(true);
        try {
            await api.post(`/roster/${selectedCharacter.characterId}/rest`);
            fetchRoster(); // Refresh the roster to pull the new timestamp
        } catch (err: any) {
            alert(err.response?.data || "Failed to initiate Long Rest.");
        } finally {
            setIsResting(false);
        }
    };

    const formatMod = (mod: number) => mod >= 0 ? `+${mod}` : `${mod}`;

    const getRarityClass = (rarity: string) => {
        switch(rarity.toUpperCase()) {
            case 'LEGENDARY': return 'rarity-legendary';
            case 'EPIC': return 'rarity-epic';
            case 'RARE': return 'rarity-rare';
            case 'UNCOMMON': return 'rarity-uncommon';
            default: return 'rarity-common';
        }
    };

    const formatTimeLeft = (endTimeMs: number) => {
        const diff = Math.max(0, endTimeMs - now);
        if (diff === 0) return "Awakening...";
        const h = Math.floor(diff / 3600000).toString().padStart(2, '0');
        const m = Math.floor((diff % 3600000) / 60000).toString().padStart(2, '0');
        const s = Math.floor((diff % 60000) / 1000).toString().padStart(2, '0');
        return `${h}:${m}:${s}`;
    };

    const selectedCharacter = useMemo(() => {
        if (!selectedCharacterId) return null;
        return roster.find(char => char.characterId === selectedCharacterId) || null;
    }, [roster, selectedCharacterId]);

    if (loading) return <div className="panel" style={{ margin: 'var(--space-md)' }}>Gathering Party...</div>;

    const availableHitDice = selectedCharacter ? selectedCharacter.maxHitDice - selectedCharacter.spentHitDice : 0;

    return (
        <div className="heroes-split-layout">
            <aside className="heroes-deck-pane">
                <div className="deck-header">
                    <h2>Roster</h2>
                    <div className="deck-filters">
                        <input type="text" placeholder="Filter Heroes..." className="filter-input" />
                        <button className="button--secondary"><Icon name="filter" size={16} /></button>
                    </div>
                </div>

                {roster.length === 0 ? (
                    <div className="deck-empty-state">
                        <p>No heroes recruited.</p>
                    </div>
                ) : (
                    <div className="deck-list">
                        {roster.map(char => (
                            <div
                                key={char.characterId}
                                className={`deck-item ${selectedCharacterId === char.characterId ? 'selected' : ''} ${getRarityClass(char.rarity)}`}
                                onClick={() => setSelectedCharacterId(char.characterId)}
                            >
                                <img src={char.portraitUrl || '/assets/hero.png'} alt={char.name} className="deck-item-portrait" />
                                <div className="deck-item-info">
                                    <span className="deck-item-name">{char.name}</span>
                                    <span className="deck-item-class">Level {char.level} {char.dndClass}</span>
                                </div>
                                <div className="deck-item-rarity-badge"></div>
                            </div>
                        ))}
                    </div>
                )}
            </aside>

            <main className="heroes-sheet-pane">
                {selectedCharacter ? (
                    <div className={`parchment-sheet ${getRarityClass(selectedCharacter.rarity)}`}>
                        <header className="sheet-header">
                            <div className="sheet-header-left">
                                <img src={selectedCharacter.portraitUrl || '/assets/hero.png'} alt={selectedCharacter.name} className="sheet-portrait" />
                                <div className="sheet-header-text">
                                    <h1>{selectedCharacter.name}</h1>
                                    <div className="sheet-subtitle">
                                        <span className="sheet-rarity">{selectedCharacter.rarity}</span>
                                        <span className="sheet-class-level">Lv.{selectedCharacter.level} {selectedCharacter.dndClass}</span>
                                    </div>
                                    <div className="sheet-status">Status: {selectedCharacter.status}</div>
                                </div>
                            </div>

                            <div className="sheet-vitals-container">
                                <div>
                                    <div className="sheet-hp-display">
                                        <span>HP</span>
                                        <span className="hp-vals">{selectedCharacter.currentHp} / {selectedCharacter.maxHp}</span>
                                    </div>
                                    <div className="progress-bar-container">
                                        <div className="progress-bar-fill success" style={{ width: `${(selectedCharacter.currentHp / selectedCharacter.maxHp) * 100}%`, background: '#8b572a' }}></div>
                                    </div>
                                </div>

                                <div className="tooltip-container" style={{width: '100%'}}>
                                    <div className="sheet-hd-display">
                                        <span>Hit Dice ⓘ</span>
                                        <span>{availableHitDice} / {selectedCharacter.maxHitDice}</span>
                                    </div>
                                    <div style={{ display: 'flex', gap: '4px' }}>
                                        {Array.from({ length: selectedCharacter.maxHitDice }).map((_, i) => (
                                            <div key={i} style={{
                                                width: '14px', height: '14px', borderRadius: '3px',
                                                background: i < availableHitDice ? '#8b572a' : 'transparent',
                                                border: '1px solid #8b572a'
                                            }}></div>
                                        ))}
                                    </div>
                                    <span className="tooltip-text">
                                        <strong>Hit Dice</strong> are your character's natural stamina. <br/><br/>
                                        Characters automatically spend 1 Hit Die for every hour they are left IDLE in the Kingdom to heal missing HP.
                                    </span>
                                </div>
                            </div>
                        </header>

                        <section className="sheet-stats-block">
                            <h2 className="section-title">Ability Scores</h2>
                            <div className="stats-grid">
                                {[
                                    {label: 'STR', val: selectedCharacter.totalStrength, mod: selectedCharacter.strMod},
                                    {label: 'DEX', val: selectedCharacter.totalDexterity, mod: selectedCharacter.dexMod},
                                    {label: 'CON', val: selectedCharacter.totalConstitution, mod: selectedCharacter.conMod},
                                    {label: 'INT', val: selectedCharacter.totalIntelligence, mod: selectedCharacter.intMod},
                                    {label: 'WIS', val: selectedCharacter.totalWisdom, mod: selectedCharacter.wisMod},
                                    {label: 'CHA', val: selectedCharacter.totalCharisma, mod: selectedCharacter.chaMod}
                                ].map(stat => (
                                    <div key={stat.label} className="stat-box">
                                        <span className="stat-label">{stat.label}</span>
                                        <span className="stat-mod">{formatMod(stat.mod)}</span>
                                        <span className="stat-score">{stat.val}</span>
                                    </div>
                                ))}
                            </div>
                        </section>

                        <section className="sheet-section">
                            <h2 className="section-title"><Icon name="inventory" size={18} style={{marginRight: '8px'}} /> Inventory</h2>
                            <div className="equipment-summary">
                                <div><Icon name="combat" size={16} /> Weapon: {selectedCharacter.weaponTier}</div>
                                <div style={{marginLeft: '20px'}}><Icon name="kingdom" size={16} /> Armor: {selectedCharacter.armorTier}</div>
                            </div>
                        </section>

                        <section className="sheet-section">
                            <h2 className="section-title"><Icon name="plus" size={18} style={{marginRight: '8px'}} /> Abilities & Powers</h2>
                            <p className="placeholder-text">Class features and active skills will appear here in future updates.</p>
                        </section>

                        <div style={{marginTop: 'auto', display: 'flex', justifyContent: 'flex-end'}}>
                            <div className="tooltip-container">
                                {selectedCharacter.status === 'RESTING' ? (
                                    <button className="button" style={{ opacity: 0.7, padding: '12px 24px', border: '2px solid #8b572a', background: 'transparent', color: '#3e2714', fontWeight: 'bold' }} disabled>
                                        Currently Resting ({selectedCharacter.longRestEndTimeEpoch ? formatTimeLeft(selectedCharacter.longRestEndTimeEpoch) : '8 Hrs'})
                                    </button>
                                ) : (
                                    <button
                                        className="button"
                                        style={{ padding: '12px 24px', border: '2px solid #8b572a', background: '#fdf6e3', color: '#3e2714', fontWeight: 'bold', cursor: 'pointer' }}
                                        onClick={handleLongRest}
                                        disabled={isResting || selectedCharacter.status === 'MISSION'}
                                    >
                                        Initiate Long Rest
                                    </button>
                                )}
                                <span className="tooltip-text" style={{bottom: '100%', left: '50%', marginBottom: '10px'}}>
                                    A <strong>Long Rest</strong> locks the character for 8 real-time hours.<br/><br/>
                                    It completely restores their HP and abilities, clears status effects, and regenerates hit dice.
                                </span>
                            </div>
                        </div>

                    </div>
                ) : (
                    <div className="sheet-empty-state">
                        <Icon name="user" size={48} />
                        <h2>Select a Hero</h2>
                        <p>Select a hero to view their details.</p>
                    </div>
                )}
            </main>
        </div>
    );
}