import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';
import { Icon } from '../components/Icon';
import BuildingSidePanel from '../components/BuildingSidePanel';
import './KingdomView.css';

export interface ConstructionTaskResponse {
    buildingType: string;
    targetLevel: number;
    startTimeEpoch: number;
    completionTimeEpoch: number;
}

export interface PlayerProfile {
    displayName: string;
    totalPopulation: number;
    maxPopulation: number;
    woodcutters: number;
    stoneworkers: number;
    hunters: number;
    bakers: number;

    keepLevel: number;
    houses: number;
    towers: number;
    bakeries: number;
    huntingLodges: number;

    activeConstructions?: ConstructionTaskResponse[];
}

export interface BuildingInstance {
    id: string;
    level: number;
    workersAssigned: number;
    maxWorkers: number;
    productionRate: number;
}

export interface BuildingGroup {
    type: string;
    name: string;
    singularName: string;
    icon: any;
    description: string;
    instances: BuildingInstance[];
}

export interface KingdomEvent {
    id: string;
    timestamp: string;
    message: string;
    type: 'good' | 'bad' | 'neutral';
}

export default function KingdomView() {
    const { profile, fetchProfile } = useOutletContext<{ profile: PlayerProfile, fetchProfile: () => void }>();

    const [selectedGroup, setSelectedGroup] = useState<BuildingGroup | null>(null);
    const [selectedInstance, setSelectedInstance] = useState<BuildingInstance | null>(null);
    const [isBusy, setIsBusy] = useState(false);

    const [now, setNow] = useState(Date.now());

    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(timer);
    }, []);

    const buildingGroups: BuildingGroup[] = [
        {
            type: 'keep',
            name: 'The Keep',
            singularName: 'Keep',
            icon: 'kingdom',
            description: 'The central hub of your kingdom. Upgrading it unlocks new tiers of structures.',
            instances: [{ id: 'keep-1', level: profile?.keepLevel || 1, workersAssigned: 0, maxWorkers: 0, productionRate: 0 }]
        },
        {
            type: 'house',
            name: 'Houses',
            singularName: 'House',
            icon: 'home',
            description: 'Provides housing for your peasant population. More houses mean a higher population cap.',
            instances: Array.from({ length: profile?.houses || 0 }).map((_, i) => ({
                id: `house-${i + 1}`, level: 1, workersAssigned: 0, maxWorkers: 0, productionRate: 0
            }))
        },
        {
            type: 'bakery',
            name: 'Bakeries',
            singularName: 'Bakery',
            icon: 'food',
            description: 'Specialized structures where assigned peasants bake bread to slowly generate food.',
            instances: Array.from({ length: profile?.bakeries || 0 }).map((_, i) => ({
                id: `bakery-${i + 1}`, level: 1, workersAssigned: 0, maxWorkers: 2, productionRate: 10
            }))
        },
        {
            type: 'lodge',
            name: 'Hunting Lodges',
            singularName: 'Hunting Lodge',
            icon: 'combat',
            description: 'Hunters stationed here yield a faster, riskier food supply for the kingdom.',
            instances: Array.from({ length: profile?.huntingLodges || 0 }).map((_, i) => ({
                id: `lodge-${i + 1}`, level: 1, workersAssigned: 0, maxWorkers: 2, productionRate: 0
            }))
        }
    ];

    const events: KingdomEvent[] = [
        { id: 'e5', timestamp: '06:00', message: 'A new day dawns over the kingdom.', type: 'neutral' },
    ];

    const handleConstruct = async (type: string) => {
        if (isBusy) return;
        setIsBusy(true);
        try {
            await api.post(`/player/construct?buildingType=${type}`);
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to start construction");
        } finally {
            setIsBusy(false);
        }
    };

    const handleComplete = async (type: string) => {
        if (isBusy) return;
        setIsBusy(true);
        try {
            await api.post(`/player/construct/complete?buildingType=${type}`);
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to complete construction");
        } finally {
            setIsBusy(false);
        }
    };

    return (
        <div className="kingdom-container">
            <div className="kingdom-main">
                <div className="world-layer">
                    {buildingGroups.map((group) => {
                        const task = profile?.activeConstructions?.find(t => t.buildingType === group.type);
                        const isReady = task && now >= task.completionTimeEpoch;

                        return (
                            <div
                                key={group.type}
                                className={`map-node ${selectedGroup?.type === group.type ? 'active' : ''} ${isReady ? 'ready' : ''}`}
                                onClick={() => {
                                    setSelectedGroup(group);
                                    setSelectedInstance(null);
                                }}
                            >
                                {group.instances.length > 0 && (
                                    <div className="node-badge">{group.instances.length}</div>
                                )}
                                <div className="node-icon-wrapper">
                                    <Icon name={group.icon} size={32} />
                                </div>
                                <div className="node-label">{group.name}</div>
                            </div>
                        );
                    })}
                </div>

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

            {selectedGroup && (
                <BuildingSidePanel
                    selectedGroup={selectedGroup}
                    selectedInstance={selectedInstance}
                    profile={profile}
                    now={now}
                    isBusy={isBusy}
                    onClose={() => {
                        setSelectedGroup(null);
                        setSelectedInstance(null);
                    }}
                    onSelectInstance={setSelectedInstance}
                    onConstruct={handleConstruct}
                    onComplete={handleComplete}
                />
            )}
        </div>
    );
}