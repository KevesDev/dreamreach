import { useState, useEffect, useMemo } from 'react';
import api from '../api/client';
import { Icon } from '../components/Icon';
import './HeroesView.css';

interface Character {
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
    status: string;
    weaponTier: string;
    armorTier: string;
    portraitUrl?: string;
}

export default function HeroesView() {
    const [roster, setRoster] = useState<Character[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedCharacterId, setSelectedCharacterId] = useState<string | null>(null);

    useEffect(() => {
        api.get('/roster')
            .then(res => {
                setRoster(res.data);
                if (res.data.length > 0 && !selectedCharacterId) {
                    setSelectedCharacterId(res.data[0].characterId);
                }
                setLoading(false);
            })
            .catch(err => {
                console.error("Failed to load roster", err);
                setLoading(false);
            });
    }, [selectedCharacterId]);

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

    const selectedCharacter = useMemo(() => {
        if (!selectedCharacterId) return null;
        return roster.find(char => char.characterId === selectedCharacterId) || null;
    }, [roster, selectedCharacterId]);

    if (loading) return <div className="panel" style={{ margin: 'var(--space-md)' }}>Gathering Party...</div>;

    return (
        <div className="heroes-split-layout">
            {/* Left: Scrollable Deck List */}
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
                        <Icon name="user" size={32} />
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

            {/* Right: Detailed D&D Character Sheet */}
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
                                <div className="sheet-hp-display">
                                    <span>HP</span>
                                    <span className="hp-vals">{selectedCharacter.currentHp} / {selectedCharacter.maxHp}</span>
                                </div>
                                <div className="progress-bar-container">
                                    <div className="progress-bar-fill success" style={{ width: `${(selectedCharacter.currentHp / selectedCharacter.maxHp) * 100}%` }}></div>
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