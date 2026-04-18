import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';
import BuildingSidePanel from '../components/BuildingSidePanel';
import WorldLayer from '../components/WorldLayer';
import RoyalLedger from '../components/RoyalLedger';
import CitizenDashboard from '../components/CitizenDashboard';
import './KingdomView.css';

export interface ConstructionTaskResponse {
    buildingType: string;
    targetLevel: number;
    startTimeEpoch: number;
    completionTimeEpoch: number;
}

export interface TrainingTaskResponse {
    id: string;
    professionType: string;
    startTimeEpoch: number;
    completionTimeEpoch: number;
}

export interface TrainingConfigResponse {
    professionType: string;
    goldCost: number;
    foodCost: number;
    trainTimeSeconds: number;
}

export interface BuildingConfigResponse {
    buildingType: string;
    woodCost: number;
    stoneCost: number;
    buildTimeSeconds: number;
}

export interface PlayerProfile {
    displayName: string;
    totalPopulation: number;
    maxPopulation: number;
    idlePeasants: number;
    woodcutters: number;
    stoneworkers: number;
    hunters: number;
    bakers: number;

    food: number;
    wood: number;
    stone: number;
    gold: number;

    keepLevel: number;
    houses: number;
    towers: number;
    bakeries: number;
    huntingLodges: number;

    activeConstructions?: ConstructionTaskResponse[];
    activeTrainingTasks?: TrainingTaskResponse[];
    trainingConfigs?: TrainingConfigResponse[];
    buildingConfigs?: BuildingConfigResponse[]; // Receive dynamic costs from backend
}

export interface BuildingCost {
    wood: number;
    stone: number;
    timeSeconds: number;
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
    cost?: BuildingCost;
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

    // Top-level navigation state
    const [activeTab, setActiveTab] = useState<'buildings' | 'citizens'>('buildings');

    // Building management state
    const [selectedGroup, setSelectedGroup] = useState<BuildingGroup | null>(null);
    const [selectedInstance, setSelectedInstance] = useState<BuildingInstance | null>(null);
    const [isBusy, setIsBusy] = useState(false);

    const [now, setNow] = useState(Date.now());

    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(timer);
    }, []);

    // Handle tab switching safely
    const handleTabChange = (tab: 'buildings' | 'citizens') => {
        setActiveTab(tab);
        if (tab === 'citizens') {
            // Close the building panel immediately when switching to Citizens
            setSelectedGroup(null);
            setSelectedInstance(null);
        }
    };

    // Helper to dynamically locate backend config values
    const getBuildingConfig = (type: string) => profile?.buildingConfigs?.find(c => c.buildingType === type);

    // Grab the dynamic configs (fallback to null if loading)
    const houseConfig = getBuildingConfig('house');
    const bakeryConfig = getBuildingConfig('bakery');
    const lodgeConfig = getBuildingConfig('lodge');

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
            cost: houseConfig ? {
                wood: houseConfig.woodCost,
                stone: houseConfig.stoneCost,
                timeSeconds: houseConfig.buildTimeSeconds
            } : undefined,
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
            cost: bakeryConfig ? {
                wood: bakeryConfig.woodCost,
                stone: bakeryConfig.stoneCost,
                timeSeconds: bakeryConfig.buildTimeSeconds
            } : undefined,
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
            cost: lodgeConfig ? {
                wood: lodgeConfig.woodCost,
                stone: lodgeConfig.stoneCost,
                timeSeconds: lodgeConfig.buildTimeSeconds
            } : undefined,
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
                {/* Master Tab Navigation */}
                <div className="kingdom-tabs">
                    <button
                        className={`kingdom-tab ${activeTab === 'buildings' ? 'active' : ''}`}
                        onClick={() => handleTabChange('buildings')}
                    >
                        Structures
                    </button>
                    <button
                        className={`kingdom-tab ${activeTab === 'citizens' ? 'active' : ''}`}
                        onClick={() => handleTabChange('citizens')}
                    >
                        Citizens
                    </button>
                </div>

                {/* Sub-Views */}
                {activeTab === 'buildings' ? (
                    <>
                        <WorldLayer
                            buildingGroups={buildingGroups}
                            selectedGroup={selectedGroup}
                            activeConstructions={profile?.activeConstructions}
                            now={now}
                            onSelectGroup={(group) => {
                                setSelectedGroup(group);
                                setSelectedInstance(null);
                            }}
                        />
                        <RoyalLedger events={events} />
                    </>
                ) : (
                    <CitizenDashboard
                        profile={profile}
                        now={now}
                        fetchProfile={fetchProfile}
                    />
                )}
            </div>

            {selectedGroup && activeTab === 'buildings' && (
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