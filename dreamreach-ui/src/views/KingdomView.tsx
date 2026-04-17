import { useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Icon } from '../components/Icon';
import './KingdomView.css';

interface PlayerProfile {
    displayName: string;
    totalPopulation: number;
    maxPopulation: number;
    woodcutters: number;
    stoneworkers: number;
    hunters: number;
    bakers: number;
}

interface BuildingInstance {
    id: string;
    level: number;
    workersAssigned: number;
    maxWorkers: number;
    productionRate: number;
}

interface BuildingGroup {
    type: string;
    name: string;
    singularName: string;
    icon: any;
    description: string;
    instances: BuildingInstance[];
}

interface KingdomEvent {
    id: string;
    timestamp: string;
    message: string;
    type: 'good' | 'bad' | 'neutral';
}

export default function KingdomView() {
    const { profile } = useOutletContext<{ profile: PlayerProfile }>();

    const [selectedGroup, setSelectedGroup] = useState<BuildingGroup | null>(null);
    const [selectedInstance, setSelectedInstance] = useState<BuildingInstance | null>(null);

    // Mock physical presence
    const buildingGroups: BuildingGroup[] = [
        {
            type: 'keep',
            name: 'The Keep',
            singularName: 'Keep',
            icon: 'kingdom',
            description: 'The central hub of your kingdom. Upgrading it unlocks new tiers of structures.',
            instances: [{ id: 'keep-1', level: 1, workersAssigned: 0, maxWorkers: 0, productionRate: 0 }]
        },
        {
            type: 'house',
            name: 'Houses',
            singularName: 'House',
            icon: 'home',
            description: 'Provides housing for your peasant population. More houses mean a higher population cap.',
            instances: [
                { id: 'house-1', level: 2, workersAssigned: 0, maxWorkers: 0, productionRate: 0 },
                { id: 'house-2', level: 1, workersAssigned: 0, maxWorkers: 0, productionRate: 0 },
                { id: 'house-3', level: 1, workersAssigned: 0, maxWorkers: 0, productionRate: 0 }
            ]
        },
        {
            type: 'bakery',
            name: 'Bakeries',
            singularName: 'Bakery',
            icon: 'food',
            description: 'Specialized structures where assigned peasants bake bread to slowly generate food.',
            instances: [
                { id: 'bakery-1', level: 1, workersAssigned: 2, maxWorkers: 2, productionRate: 10 },
                { id: 'bakery-2', level: 1, workersAssigned: 1, maxWorkers: 2, productionRate: 5 }
            ]
        },
        {
            type: 'lodge',
            name: 'Hunting Lodges',
            singularName: 'Hunting Lodge',
            icon: 'combat',
            description: 'Hunters stationed here yield a faster, riskier food supply for the kingdom.',
            instances: [{ id: 'lodge-1', level: 1, workersAssigned: 0, maxWorkers: 2, productionRate: 0 }]
        }
    ];

    // Mock Event Log Data
    const events: KingdomEvent[] = [
        { id: 'e1', timestamp: '14:32', message: 'Bakery #2 construction finished!', type: 'good' },
        { id: 'e2', timestamp: '11:15', message: 'A peasant arrived seeking shelter. Population +1.', type: 'good' },
        { id: 'e3', timestamp: '09:00', message: 'Your hunters secured a large elk. +50 Food.', type: 'good' },
        { id: 'e4', timestamp: '08:45', message: 'A worker deserted due to starvation.', type: 'bad' },
        { id: 'e5', timestamp: '06:00', message: 'A new day dawns over the kingdom.', type: 'neutral' },
    ];

    const getGlobalWorkerCount = (type: string) => {
        switch(type) {
            case 'bakery': return { count: profile?.bakers || 0, label: 'Total Bakers' };
            case 'lodge': return { count: profile?.hunters || 0, label: 'Total Hunters' };
            default: return null;
        }
    };

    const handleGroupClick = (group: BuildingGroup) => {
        setSelectedGroup(group);
        setSelectedInstance(null);
    };

    const handleClosePanel = () => {
        setSelectedGroup(null);
        setSelectedInstance(null);
    };

    return (
        <div className="kingdom-container">

            {/* MAIN AREA: World Layer + Journal */}
            <div className="kingdom-main">

                {/* TOP: THE WORLD LAYER */}
                <div className="world-layer">
                    {buildingGroups.map((group) => (
                        <div
                            key={group.type}
                            className={`map-node ${selectedGroup?.type === group.type ? 'active' : ''}`}
                            onClick={() => handleGroupClick(group)}
                        >
                            {group.instances.length > 1 && (
                                <div className="node-badge">{group.instances.length}</div>
                            )}
                            <div className="node-icon-wrapper">
                                <Icon name={group.icon} size={32} />
                            </div>
                            <div className="node-label">{group.name}</div>
                        </div>
                    ))}
                </div>

                {/* BOTTOM: THE KINGDOM JOURNAL */}
                <div className="journal-container">
                    <div className="journal-header">
                        <h2>Royal Ledger</h2>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Latest Events</span>
                    </div>

                    <div className="journal-log">
                        {events.map(event => (
                            <div key={event.id} className={`journal-entry journal-entry--${event.type}`}>
                                <span className="journal-time">[{event.timestamp}]</span>
                                <span className="journal-text">{event.message}</span>
                            </div>
                        ))}
                    </div>
                </div>

            </div>

            {/* RIGHT: THE INTERACTION PANEL */}
            {selectedGroup && (
                <aside className="side-panel">

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                            <h3 style={{ color: 'var(--accent-gold)' }}>
                                {selectedInstance ? `${selectedGroup.singularName} (Lvl ${selectedInstance.level})` : selectedGroup.name}
                            </h3>
                            {selectedInstance && selectedGroup.instances.length > 1 && (
                                <button
                                    style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 0, fontSize: '0.8rem', marginTop: '4px' }}
                                    onClick={() => setSelectedInstance(null)}
                                >
                                    ← Back to overview
                                </button>
                            )}
                        </div>
                        <button className="button" style={{ padding: '2px 8px' }} onClick={handleClosePanel}>×</button>
                    </div>

                    {!selectedInstance && (
                        <>
                            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                                {selectedGroup.description}
                            </p>

                            <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)' }}>
                                <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-sm)', color: 'var(--text-muted)' }}>KINGDOM LABOR</h4>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '0.9rem' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                        <span>Total Villagers:</span>
                                        <span>{profile?.totalPopulation || 0} / {profile?.maxPopulation || 0}</span>
                                    </div>

                                    {getGlobalWorkerCount(selectedGroup.type) && (
                                        <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--accent-gold)' }}>
                                            <span>{getGlobalWorkerCount(selectedGroup.type)?.label}:</span>
                                            <span>{getGlobalWorkerCount(selectedGroup.type)?.count}</span>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: 'var(--space-lg)' }}>
                                <h4 style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>YOUR STRUCTURES</h4>
                                {selectedGroup.instances.map((instance, index) => (
                                    <div
                                        key={instance.id}
                                        className="instance-item"
                                        onClick={() => setSelectedInstance(instance)}
                                    >
                                        <span className="instance-item-title">{selectedGroup.singularName} #{index + 1}</span>
                                        <span className="instance-item-level">Lvl {instance.level}</span>
                                    </div>
                                ))}
                                <button className="button" style={{ marginTop: 'var(--space-sm)' }}>
                                    + Construct New
                                </button>
                            </div>
                        </>
                    )}

                    {selectedInstance && (
                        <>
                            <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)' }}>
                                <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-md)', color: 'var(--text-muted)' }}>MANAGEMENT</h4>

                                {selectedInstance.maxWorkers > 0 && (
                                    <div style={{ marginBottom: 'var(--space-lg)' }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', marginBottom: '8px' }}>
                                            <span>Assigned Workers:</span>
                                            <span style={{ color: 'var(--accent-gold)' }}>
                                                {selectedInstance.workersAssigned} / {selectedInstance.maxWorkers}
                                            </span>
                                        </div>
                                        <div style={{ display: 'flex', gap: '8px' }}>
                                            <button className="button" style={{ flex: 1 }} disabled={selectedInstance.workersAssigned === 0}>- Remove</button>
                                            <button className="button" style={{ flex: 1 }} disabled={selectedInstance.workersAssigned === selectedInstance.maxWorkers}>+ Assign</button>
                                        </div>
                                    </div>
                                )}

                                {selectedInstance.productionRate > 0 && (
                                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', borderTop: '1px solid var(--border-subtle)', paddingTop: 'var(--space-md)' }}>
                                        <span>Current Output:</span>
                                        <span style={{ color: 'var(--success)' }}>+{selectedInstance.productionRate}/hr</span>
                                    </div>
                                )}
                            </div>

                            <div style={{ marginTop: 'auto' }}>
                                <button className="button--primary" style={{ width: '100%' }}>UPGRADE TO LV.{selectedInstance.level + 1}</button>
                                <p style={{ fontSize: '0.7rem', textAlign: 'center', marginTop: 'var(--space-sm)', color: 'var(--text-muted)' }}>
                                    Requires Keep Lvl {selectedInstance.level + 1}
                                </p>
                            </div>
                        </>
                    )}
                </aside>
            )}
        </div>
    );
}