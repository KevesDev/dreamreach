import { useState, useEffect } from 'react';
import { Icon } from './Icon';
import type { Character } from '../views/HeroesView'; // Reusing the DTO interface
import './UniversalGachaModal.css';

interface UniversalGachaModalProps {
    character: Character | null;
    onAccept: () => void;
}

export default function UniversalGachaModal({ character, onAccept }: UniversalGachaModalProps) {
    // States: 'waiting' (Big Button) -> 'rolling' (Animation) -> 'revealed' (Result)
    const [step, setStep] = useState<'waiting' | 'rolling' | 'revealed'>('waiting');

    // Reset state if a new character is passed in
    useEffect(() => {
        if (character) {
            setStep('waiting');
        }
    }, [character]);

    if (!character) return null;

    const handleRollClick = () => {
        setStep('rolling');
        // Play the suspense animation for 2.5 seconds before revealing
        setTimeout(() => {
            setStep('revealed');
        }, 2500);
    };

    const getRarityClass = (rarity: string) => {
        switch(rarity.toUpperCase()) {
            case 'LEGENDARY': return 'rarity-legendary';
            case 'EPIC': return 'rarity-epic';
            case 'RARE': return 'rarity-rare';
            case 'UNCOMMON': return 'rarity-uncommon';
            default: return 'rarity-common';
        }
    };

    return (
        <div className="gacha-modal-overlay">
            <div className="gacha-modal-content">

                {step === 'waiting' && (
                    <div className="gacha-step-waiting">
                        <Icon name="summon" size={64} style={{ color: 'var(--accent-gold)', marginBottom: 'var(--space-lg)' }} />
                        <h2>A Hero Answers the Call...</h2>
                        <p>Reveal your destiny.</p>
                        <button className="button--claim gacha-big-button" onClick={handleRollClick}>
                            REVEAL HERO
                        </button>
                    </div>
                )}

                {step === 'rolling' && (
                    <div className="gacha-step-rolling">
                        <div className="gacha-summon-portal"></div>
                        <h2>Channeling the Aether...</h2>
                    </div>
                )}

                {step === 'revealed' && (
                    <div className={`gacha-step-revealed ${getRarityClass(character.rarity)}`}>
                        <div className="gacha-card-glow"></div>
                        <div className="gacha-card">
                            <div className="gacha-card-header">
                                <img src={character.portraitUrl || '/assets/hero.png'} alt={character.name} />
                                <div>
                                    <h3>{character.name}</h3>
                                    <span className="gacha-card-class">Level 1 {character.dndClass}</span>
                                </div>
                            </div>

                            <div className="gacha-card-rarity-banner">
                                {character.rarity}
                            </div>

                            <div className="gacha-card-stats">
                                <div className="gacha-stat"><span>STR</span>{character.totalStrength}</div>
                                <div className="gacha-stat"><span>DEX</span>{character.totalDexterity}</div>
                                <div className="gacha-stat"><span>CON</span>{character.totalConstitution}</div>
                                <div className="gacha-stat"><span>INT</span>{character.totalIntelligence}</div>
                                <div className="gacha-stat"><span>WIS</span>{character.totalWisdom}</div>
                                <div className="gacha-stat"><span>CHA</span>{character.totalCharisma}</div>
                            </div>

                            <div className="gacha-card-hp">
                                <Icon name="health" size={16} /> Max HP: {character.maxHp}
                            </div>
                        </div>

                        <button className="button--primary gacha-accept-button" onClick={onAccept}>
                            Accept
                        </button>
                    </div>
                )}

            </div>
        </div>
    );
}