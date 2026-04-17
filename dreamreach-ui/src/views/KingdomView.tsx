import { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Icon } from '../components/Icon';
import './KingdomView.css';

/**
 * Interface representing the data structure coming from our backend
 */
interface PlayerProfile {
    displayName: string;
    totalPopulation: number;
    maxPopulation: number;
    food: number;
    wood: number;
    stone: number;
    gold: number;
    woodcutters: number;
    stoneworkers: number;
    hunters: number;
    bakers: number;
}

interface Building {
    id: string;
    name: string;
    type: 'keep' | 'house' | 'bakery' | 'lodge';
    level: number;
    icon: any;
    description: string;
}

export default function KingdomView() {
    // LEARNING NOTE: We use 'profile' here to pull the real numbers
    // from the backend so the UI is dynamic, not hardcoded.
    const { profile } = useOutletContext<{ profile: PlayerProfile }>();

    const [selectedBuilding, setSelectedBuilding] = useState<Building | null>(null);

    // Initial building set for the prototype.
    // In Issue #4, these 'level' values will come directly from profile.structures
    const buildings: Building[] = [
        { id: '1', name: 'The Keep', type: 'keep', level: 1, icon: 'kingdom', description: 'The seat of your power. Dictates kingdom progression.' },
        { id: '2', name: 'Peasant House', type: 'house', level: 1, icon: 'home', description: 'Safe housing for your growing population.' },
        { id: '3', name: 'Royal Bakery', type: 'bakery', level: 1, icon: 'food', description: 'Produces food steadily for your citizens.' },
    ];

    /**
     * Helper to get worker stats based on building type
     */
    const getWorkerStats = (type: string) => {
        switch(type) {
            case 'bakery': return { current: profile.bakers || 0, max: 2, label: 'Bakers' };
            case 'lodge': return { current: profile.hunters || 0, max: 2, label: 'Hunters' };
            default: return null;
        }
    };

    return (
        <div className="kingdom-container">

            {/* TACTICAL GRID AREA */}
            <div className="kingdom-grid">
                {buildings.map((b) => (
                    <div
                        key={b.id}
                        className={`building ${selectedBuilding?.id === b.id ? 'building--active' : ''}`}
                        onClick={() => setSelectedBuilding(b)}
                    >
                        <span className="building-level">LV.{b.level}</span>
                        <Icon name={b.icon} size={32} style={{ color: selectedBuilding?.id === b.id ? 'var(--accent-gold)' : 'var(--text-muted)' }} />
                        <span className="building-name">{b.name}</span>
                    </div>
                ))}
            </div>

            {/* INTERACTION DRAWER (Progressive Disclosure) */}
            {selectedBuilding && (
                <aside className="side-panel">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <h3 style={{ color: 'var(--accent-gold)' }}>{selectedBuilding.name}</h3>
                        <button className="button" style={{ padding: '2px 8px' }} onClick={() => setSelectedBuilding(null)}>×</button>
                    </div>

                    <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                        {selectedBuilding.description}
                    </p>

                    {/* POPULATION DATA SECTION */}
                    <div className="panel" style={{ background: 'var(--bg-elevated)' }}>
                        <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-sm)' }}>LABOR & POPULATION</h4>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '0.9rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span>Total Villagers:</span>
                                <span>{profile.totalPopulation} / {profile.maxPopulation}</span>
                            </div>

                            {/* Dynamic Specialist Data */}
                            {getWorkerStats(selectedBuilding.type) && (
                                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--accent-gold)' }}>
                                    <span>{getWorkerStats(selectedBuilding.type)?.label}:</span>
                                    <span>{getWorkerStats(selectedBuilding.type)?.current} / {getWorkerStats(selectedBuilding.type)?.max}</span>
                                </div>
                            )}
                        </div>
                    </div>

                    <div style={{ marginTop: 'auto' }}>
                        <button className="button--primary" style={{ width: '100%' }}>UPGRADE TO LV.{selectedBuilding.level + 1}</button>
                        <p style={{ fontSize: '0.7rem', textAlign: 'center', marginTop: 'var(--space-sm)', color: 'var(--text-muted)' }}>
                            Check the Keep for requirements.
                        </p>
                    </div>
                </aside>
            )}
        </div>
    );
}