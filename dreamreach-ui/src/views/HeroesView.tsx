import { useState, useEffect } from 'react';
import api from '../api/client';
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

    useEffect(() => {
        api.get('/roster')
            .then(res => {
                setRoster(res.data);
                setLoading(false);
            })
            .catch(err => {
                console.error("Failed to load roster", err);
                setLoading(false);
            });
    }, []);

    const formatMod = (mod: number) => mod >= 0 ? `+${mod}` : `${mod}`;

    // Dynamic styling based on the actual Gacha Tier
    const getRarityColor = (rarity: string) => {
        switch(rarity.toUpperCase()) {
            case 'LEGENDARY': return 'var(--accent-gold)';
            case 'EPIC': return '#a335ee';
            case 'RARE': return '#0070dd';
            case 'UNCOMMON': return '#1eff00';
            default: return 'var(--border-subtle)';
        }
    };

    if (loading) return <div className="panel" style={{ margin: 'var(--space-md)' }}>Gathering Party...</div>;

    return (
        <div className="heroes-container">
            <div className="heroes-header">
                <h2 style={{ color: 'var(--accent-gold)', margin: 0 }}>Your Heroes</h2>
                <span style={{ color: 'var(--text-muted)' }}>{roster.length} Active Characters</span>
            </div>

            {roster.length === 0 ? (
                <div className="panel" style={{ textAlign: 'center', padding: '40px' }}>
                    <p style={{ color: 'var(--text-secondary)' }}>You haven't summoned any heroes yet.</p>
                </div>
            ) : (
                <div className="heroes-grid">
                    {roster.map(char => (
                        <div key={char.characterId} className="hero-card" style={{ borderColor: getRarityColor(char.rarity) }}>
                            <div className="hero-card-header">
                                <div className="hero-rarity-badge" style={{ backgroundColor: getRarityColor(char.rarity) }}>
                                    {char.rarity}
                                </div>
                                <div className="hero-status-badge">{char.status}</div>
                            </div>

                            <div className="hero-portrait-container" style={{ borderColor: getRarityColor(char.rarity) }}>
                                <img src={char.portraitUrl || '/assets/hero.png'} alt={char.name} className="hero-portrait" />
                            </div>

                            <div className="hero-info">
                                <h3 className="hero-name">{char.name}</h3>
                                <div className="hero-class">Level {char.level} {char.dndClass}</div>
                            </div>

                            <div className="hero-vitals">
                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '4px' }}>
                                    <span>HP</span>
                                    <span style={{ color: char.currentHp > 0 ? 'var(--success)' : 'var(--danger)' }}>
                                        {char.currentHp} / {char.maxHp}
                                    </span>
                                </div>
                                <div className="progress-bar-container">
                                    <div
                                        className="progress-bar-fill"
                                        style={{
                                            width: `${(char.currentHp / char.maxHp) * 100}%`,
                                            backgroundColor: char.currentHp > 0 ? 'var(--success)' : 'var(--danger)'
                                        }}
                                    ></div>
                                </div>
                            </div>

                            <div className="hero-stats-grid">
                                <div className="hero-stat-box">
                                    <span className="stat-label">STR</span>
                                    <span className="stat-mod">{formatMod(char.strMod)}</span>
                                    <span className="stat-val">{char.totalStrength}</span>
                                </div>
                                <div className="hero-stat-box">
                                    <span className="stat-label">DEX</span>
                                    <span className="stat-mod">{formatMod(char.dexMod)}</span>
                                    <span className="stat-val">{char.totalDexterity}</span>
                                </div>
                                <div className="hero-stat-box">
                                    <span className="stat-label">CON</span>
                                    <span className="stat-mod">{formatMod(char.conMod)}</span>
                                    <span className="stat-val">{char.totalConstitution}</span>
                                </div>
                                <div className="hero-stat-box">
                                    <span className="stat-label">INT</span>
                                    <span className="stat-mod">{formatMod(char.intMod)}</span>
                                    <span className="stat-val">{char.totalIntelligence}</span>
                                </div>
                                <div className="hero-stat-box">
                                    <span className="stat-label">WIS</span>
                                    <span className="stat-mod">{formatMod(char.wisMod)}</span>
                                    <span className="stat-val">{char.totalWisdom}</span>
                                </div>
                                <div className="hero-stat-box">
                                    <span className="stat-label">CHA</span>
                                    <span className="stat-mod">{formatMod(char.chaMod)}</span>
                                    <span className="stat-val">{char.totalCharisma}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}