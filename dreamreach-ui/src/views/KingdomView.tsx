import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import api from '../api/client';
import BuildingSidePanel from '../components/BuildingSidePanel';
import WorldLayer from '../components/WorldLayer';
import RoyalLedger from '../components/RoyalLedger';
import CitizenDashboard from '../components/CitizenDashboard';
import UniversalGachaModal from '../components/UniversalGachaModal';
import type { Character } from './HeroesView';
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
    maxWorkers: number;
    productionRate: number;
    unlockKeepLevel: number;
}

export interface BuildingInstance {
    id: string;
    level: number;
    assignedWorkers: number;
    maxWorkers: number;
    productionRate: number;
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
    gems: number;

    pendingFood: number;
    pendingWood: number;
    pendingStone: number;
    pendingGold: number;

    happiness: number;
    maxHappiness: number;
    taxBracket: string;
    lastTaxCollectionTimeEpoch: number;

    keepLevel: number;
    houses: number;
    towers: number;
    bakeries: number;
    huntingLodges: number;
    buildings: { id: string, buildingType: string, level: number, assignedWorkers: number }[];

    activeConstructions?: ConstructionTaskResponse[];
    activeTrainingTasks?: TrainingTaskResponse[];
    trainingConfigs?: TrainingConfigResponse[];
    buildingConfigs?: BuildingConfigResponse[];
}

export interface BuildingCost {
    wood: number;
    stone: number;
    timeSeconds: number;
}

export interface BuildingGroup {
    type: string;
    name: string;
    singularName: string;
    icon: any;
    description: string;
    cost?: BuildingCost;
    instances: BuildingInstance[];
    isActionReady?: boolean;
    unlockKeepLevel: number; // Added to resolve TS2339
}

export interface KingdomEvent {
    id: string;
    timestamp: string;
    message: string;
    type: 'good' | 'bad' | 'neutral';
}

// DTO for the hero waiting in the Tavern
export interface TavernListing {
    listingId: string;
    name: string;
    dndClass: string;
    portraitUrl: string;
    goldCost: number;
    gemCost: number;
    expiryTimeEpoch: number;
}

export default function KingdomView() {
    const { profile, fetchProfile } = useOutletContext<{ profile: PlayerProfile, fetchProfile: () => void }>();

    const [activeTab, setActiveTab] = useState<'buildings' | 'citizens'>('buildings');
    const [selectedGroup, setSelectedGroup] = useState<BuildingGroup | null>(null);
    const [selectedInstance, setSelectedInstance] = useState<BuildingInstance | null>(null);
    const [isBusy, setIsBusy] = useState(false);
    const [now, setNow] = useState(Date.now());

    // Tavern State
    const [tavernListing, setTavernListing] = useState<TavernListing | null>(null);
    const [gachaResult, setGachaResult] = useState<Character | null>(null);

    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(timer);
    }, []);

    // Check for tavern arrivals when the kingdom view loads or when a construction completes
    useEffect(() => {
        const fetchTavern = async () => {
            try {
                const res = await api.get('/tavern');
                // HTTP 204 No Content means no hero is waiting
                if (res.status === 204) {
                    setTavernListing(null);
                } else {
                    setTavernListing(res.data);
                }
            } catch (err) {
                console.error("Failed to fetch tavern data");
            }
        };
        fetchTavern();
    }, [profile?.buildings.length]); // Re-fetch if building count changes (e.g., Tavern finishes building)

    const handleTabChange = (tab: 'buildings' | 'citizens') => {
        setActiveTab(tab);
        if (tab === 'citizens') {
            setSelectedGroup(null);
            setSelectedInstance(null);
        }
    };

    const getBuildingConfig = (type: string) => profile?.buildingConfigs?.find(c => c.buildingType === type);

    const houseConfig = getBuildingConfig('house');
    const bakeryConfig = getBuildingConfig('bakery');
    const lodgeConfig = getBuildingConfig('lodge');
    const tavernConfig = getBuildingConfig('tavern');

    const canCollectTaxes = profile?.lastTaxCollectionTimeEpoch ? (now - profile.lastTaxCollectionTimeEpoch) >= 3600000 : false;

    const mapInstances = (type: string, config: any): BuildingInstance[] => {
        if (type === 'keep') return [{ id: 'keep-1', level: profile?.keepLevel || 1, assignedWorkers: 0, maxWorkers: 0, productionRate: 0 }];

        return (profile?.buildings || [])
            .filter(b => b.buildingType.toLowerCase() === type.toLowerCase())
            .map(b => ({
                id: b.id,
                level: b.level,
                assignedWorkers: b.assignedWorkers,
                maxWorkers: config?.maxWorkers || 0,
                productionRate: config?.productionRate || 0
            }));
    };

    const buildingGroups: BuildingGroup[] = [
        {
            type: 'keep',
            name: 'The Keep',
            singularName: 'Keep',
            icon: 'kingdom',
            description: 'The central hub of your kingdom. Upgrading it unlocks new tiers of structures.',
            isActionReady: canCollectTaxes,
            instances: mapInstances('keep', null),
            unlockKeepLevel: 1 // Default fallback for Keep
        },
        {
            type: 'house',
            name: 'Houses',
            singularName: 'House',
            icon: 'home',
            description: 'Provides housing for your peasant population.',
            cost: houseConfig ? { wood: houseConfig.woodCost, stone: houseConfig.stoneCost, timeSeconds: houseConfig.buildTimeSeconds } : undefined,
            instances: mapInstances('house', houseConfig),
            unlockKeepLevel: houseConfig?.unlockKeepLevel || 1
        },
        {
            type: 'bakery',
            name: 'Bakeries',
            singularName: 'Bakery',
            icon: 'food',
            description: 'Assigned peasants bake bread to slowly generate food.',
            cost: bakeryConfig ? { wood: bakeryConfig.woodCost, stone: bakeryConfig.stoneCost, timeSeconds: bakeryConfig.buildTimeSeconds } : undefined,
            instances: mapInstances('bakery', bakeryConfig),
            unlockKeepLevel: bakeryConfig?.unlockKeepLevel || 1
        },
        {
            type: 'lodge',
            name: 'Hunting Lodges',
            singularName: 'Hunting Lodge',
            icon: 'combat',
            description: 'Hunters yield a faster, riskier food supply.',
            cost: lodgeConfig ? { wood: lodgeConfig.woodCost, stone: lodgeConfig.stoneCost, timeSeconds: lodgeConfig.buildTimeSeconds } : undefined,
            instances: mapInstances('lodge', lodgeConfig),
            unlockKeepLevel: lodgeConfig?.unlockKeepLevel || 1
        },
        {
            type: 'tavern',
            name: 'The Tavern',
            singularName: 'Tavern',
            icon: 'user',
            description: 'Attracts wandering adventurers who can be recruited into your party.',
            cost: tavernConfig ? { wood: tavernConfig.woodCost, stone: tavernConfig.stoneCost, timeSeconds: tavernConfig.buildTimeSeconds } : undefined,
            instances: mapInstances('tavern', tavernConfig),
            isActionReady: tavernListing != null, // Show a notification dot if a hero is waiting!
            unlockKeepLevel: tavernConfig?.unlockKeepLevel || 5
        }
    ];

    const events: KingdomEvent[] = [{ id: 'e5', timestamp: '06:00', message: 'A new day dawns over the kingdom.', type: 'neutral' }];

    const handleConstruct = async (type: string) => {
        if (isBusy) return;
        setIsBusy(true);
        try {
            await api.post(`/player/construct?buildingType=${type}`);
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to start construction");
        } finally { setIsBusy(false); }
    };

    const handleComplete = async (type: string) => {
        if (isBusy) return;
        setIsBusy(true);
        try {
            await api.post(`/player/construct/complete?buildingType=${type}`);
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to complete construction");
        } finally { setIsBusy(false); }
    };

    // --- THE GACHA TRANSACTION ---
    const handleRecruitHero = async (currencyType: 'gold' | 'gems') => {
        if (isBusy) return;
        setIsBusy(true);
        try {
            // The backend instantly does the math and returns the full D&D Character DTO
            const res = await api.post(`/tavern/recruit?currencyType=${currencyType}`);

            // Clear the tavern listing
            setTavernListing(null);
            fetchProfile();

            // Trigger the Universal Modal with the pulled character!
            setGachaResult(res.data);
            setSelectedGroup(null); // Close the side panel to focus on the modal
        } catch (err: any) {
            alert(err.response?.data || "Failed to recruit hero");
        } finally {
            setIsBusy(false);
        }
    };

    return (
        <>
            <div className="kingdom-container">
                <div className="kingdom-main">
                    <div className="kingdom-tabs">
                        <button className={`kingdom-tab ${activeTab === 'buildings' ? 'active' : ''}`} onClick={() => handleTabChange('buildings')}>Structures</button>
                        <button className={`kingdom-tab ${activeTab === 'citizens' ? 'active' : ''}`} onClick={() => handleTabChange('citizens')}>Citizens</button>
                    </div>

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
                        <CitizenDashboard profile={profile} now={now} fetchProfile={fetchProfile} />
                    )}
                </div>

                {selectedGroup && activeTab === 'buildings' && (
                    <BuildingSidePanel
                        selectedGroup={selectedGroup}
                        selectedInstance={selectedInstance}
                        profile={profile}
                        now={now}
                        isBusy={isBusy}
                        tavernListing={tavernListing}
                        onRecruit={handleRecruitHero}
                        onClose={() => {
                            setSelectedGroup(null);
                            setSelectedInstance(null);
                        }}
                        onSelectInstance={setSelectedInstance}
                        onConstruct={handleConstruct}
                        onComplete={handleComplete}
                        fetchProfile={fetchProfile}
                    />
                )}
            </div>

            {/* THE UNIVERSAL GACHA THEATER */}
            <UniversalGachaModal
                character={gachaResult}
                onAccept={() => setGachaResult(null)}
            />
        </>
    );
}