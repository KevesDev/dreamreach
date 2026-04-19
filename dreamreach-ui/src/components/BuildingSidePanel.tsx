import type { BuildingGroup, BuildingInstance, PlayerProfile, ConstructionTaskResponse, TavernListing } from '../views/KingdomView';
import api from '../api/client';
import { Icon } from './Icon';

interface BuildingSidePanelProps {
    selectedGroup: BuildingGroup;
    selectedInstance: BuildingInstance | null;
    profile: PlayerProfile;
    now: number;
    isBusy: boolean;
    tavernListing?: TavernListing | null;
    onRecruit?: (currency: 'gold' | 'gems') => void;
    onClose: () => void;
    onSelectInstance: (instance: BuildingInstance | null) => void;
    onConstruct: (type: string) => void;
    onComplete: (type: string) => void;
    fetchProfile: () => void;
}

export default function BuildingSidePanel({
                                              selectedGroup,
                                              selectedInstance,
                                              profile,
                                              now,
                                              isBusy,
                                              tavernListing,
                                              onRecruit,
                                              onClose,
                                              onSelectInstance,
                                              onConstruct,
                                              onComplete,
                                              fetchProfile
                                          }: BuildingSidePanelProps) {

    const liveInstance = profile.buildings?.find(b => b.id === selectedInstance?.id);
    const displayInstance = liveInstance ? {
        ...liveInstance,
        maxWorkers: selectedInstance?.maxWorkers || 0,
        productionRate: selectedInstance?.productionRate || 0
    } : selectedInstance;

    const activeTask = profile?.activeConstructions?.find(
        (t: ConstructionTaskResponse) => t.buildingType === selectedGroup.type
    );

    const getGlobalWorkerCount = (type: string) => {
        switch(type.toLowerCase()) {
            case 'bakery': return { count: profile?.bakers || 0, label: 'Total Bakers' };
            case 'lodge': return { count: profile?.hunters || 0, label: 'Total Hunters' };
            default: return null;
        }
    };

    const getWorkerName = (type: string) => {
        switch(type.toLowerCase()) {
            case 'bakery': return 'Bakers';
            case 'lodge': return 'Hunters';
            default: return 'Workers';
        }
    };

    const getUnassignedCount = (type: string) => {
        const total = type.toLowerCase() === 'bakery' ? profile?.bakers : profile?.hunters;
        const assigned = (profile?.buildings || [])
            .filter(b => b.buildingType.toLowerCase() === type.toLowerCase())
            .reduce((sum, b) => sum + b.assignedWorkers, 0);
        return Math.max(0, total - assigned);
    };

    const calculateProgress = (start: number, end: number, current: number) => {
        if (current >= end) return 100;
        if (current <= start) return 0;
        return Math.min(100, Math.max(0, ((current - start) / (end - start)) * 100));
    };

    const formatTimeRemaining = (end: number, current: number) => {
        if (current >= end) return "Ready!";
        const diffSeconds = Math.max(0, Math.ceil((end - current) / 1000));
        const h = Math.floor(diffSeconds / 3600);
        const m = Math.floor((diffSeconds % 3600) / 60);
        const s = diffSeconds % 60;

        if (h > 0) {
            return `${h}h ${m}m`;
        }
        return `${m}:${s.toString().padStart(2, '0')}`;
    };

    const formatTimeRemainingTaxes = (end: number, current: number) => {
        if (current >= end) return "Ready!";
        const diffSeconds = Math.max(0, Math.ceil((end - current) / 1000));
        const h = Math.floor(diffSeconds / 3600);
        const m = Math.floor((diffSeconds % 3600) / 60);
        const s = diffSeconds % 60;

        if (h > 0) {
            return `${h}h ${m}m`;
        }
        return `${m}:${s.toString().padStart(2, '0')}`;
    };

    const handleTaxChange = async (bracket: string) => {
        if (isBusy) return;
        try {
            await api.post(`/player/taxes/bracket?bracket=${bracket}`);
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to update tax bracket.");
        }
    };

    const handleCollectTaxes = async () => {
        if (isBusy) return;
        try {
            await api.post('/player/taxes/collect');
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to collect taxes.");
        }
    };

    const handleAssign = async () => {
        if (isBusy || !displayInstance) return;
        try {
            await api.post(`/player/building/assign?buildingId=${displayInstance.id}`);
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to assign worker");
        }
    };

    const handleRemove = async () => {
        if (isBusy || !displayInstance) return;
        try {
            await api.post(`/player/building/remove?buildingId=${displayInstance.id}`);
            fetchProfile();
        } catch (err: any) {
            alert(err.response?.data || "Failed to remove worker");
        }
    };

    const canAfford = selectedGroup.cost
        ? (profile?.wood >= selectedGroup.cost.wood && profile?.stone >= selectedGroup.cost.stone)
        : true;

    const hapHigh = profile.maxHappiness * 0.75;
    const hapLow = profile.maxHappiness * 0.25;

    // --- Dynamic Housing Calculations ---
    const isHouse = selectedGroup.type === 'house';
    let occupants = 0;
    let houseCapacity = 5;

    if (isHouse && displayInstance) {
        houseCapacity = Math.round(profile.maxPopulation / Math.max(1, profile.houses));
        // Find which number house this is to "fill" them up sequentially
        const instanceIndex = profile.buildings?.filter(b => b.buildingType === 'house').findIndex(b => b.id === displayInstance.id) || 0;
        const previousOccupants = instanceIndex * houseCapacity;
        occupants = Math.max(0, Math.min(houseCapacity, profile.totalPopulation - previousOccupants));
    }

    const renderTavernInterior = () => {
        if (!tavernListing) {
            return (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>
                    <Icon name="home" size={48} style={{ opacity: 0.2, marginBottom: '16px' }} />
                    <p>The Tavern is currently empty.</p>
                    <p style={{ fontSize: '0.8rem', opacity: 0.7 }}>Check back later to see if any adventurers have arrived.</p>
                </div>
            );
        }

        return (
            <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)', textAlign: 'center' }}>
                <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-lg)', color: 'var(--accent-gold)' }}>AN ADVENTURER ARRIVES</h4>

                <img src={tavernListing.portraitUrl || '/assets/hero.png'} alt={tavernListing.name} style={{ width: '100px', height: '100px', borderRadius: '8px', border: '2px solid var(--border-strong)', marginBottom: '16px' }} />
                <h3 style={{ margin: '0 0 4px 0' }}>{tavernListing.name}</h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '24px' }}>{tavernListing.dndClass}</p>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    <button
                        className="button"
                        disabled={isBusy || profile.gold < tavernListing.goldCost}
                        onClick={() => onRecruit && onRecruit('gold')}
                        style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', padding: '12px' }}
                    >
                        Hire for {tavernListing.goldCost} <Icon name="gold" size={16} style={{ color: 'var(--accent-gold)' }} />
                    </button>

                    <button
                        className="button"
                        disabled={isBusy || profile.gems < tavernListing.gemCost}
                        onClick={() => onRecruit && onRecruit('gems')}
                        style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '8px', padding: '12px' }}
                    >
                        Hire for {tavernListing.gemCost} <Icon name="gems" size={16} style={{ color: '#a335ee' }} />
                    </button>
                </div>

                <p style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', marginTop: '16px' }}>
                    Expires in: {formatTimeRemaining(tavernListing.expiryTimeEpoch, now)}
                </p>
            </div>
        );
    };

    const globalWorkerData = getGlobalWorkerCount(selectedGroup.type);

    return (
        <aside className="side-panel">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <h3 style={{ color: 'var(--accent-gold)' }}>
                        {displayInstance ? `${selectedGroup.singularName} (Lvl ${displayInstance.level})` : selectedGroup.name}
                    </h3>
                    {displayInstance && selectedGroup.instances.length > 1 && (
                        <button
                            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 0, fontSize: '0.8rem', marginTop: '4px' }}
                            onClick={() => onSelectInstance(null)}
                        >
                            ← Back to overview
                        </button>
                    )}
                </div>
                <button className="button" style={{ padding: '2px 8px' }} onClick={onClose}>×</button>
            </div>

            {!displayInstance && (
                <>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                        {selectedGroup.description}
                    </p>

                    {selectedGroup.type === 'keep' && (
                        <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)' }}>
                            <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-sm)', color: 'var(--text-muted)' }}>ROYAL TREASURY</h4>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.9rem' }}>
                                    <span>Kingdom Happiness</span>
                                    <span style={{ color: profile.happiness >= hapHigh ? 'var(--success)' : profile.happiness <= hapLow ? 'var(--danger)' : 'var(--text-primary)', fontWeight: 'bold' }}>
                                        {profile.happiness} / {profile.maxHappiness}
                                    </span>
                                </div>
                                <div className="progress-bar-container">
                                    <div className="progress-bar-fill" style={{ width: `${(profile.happiness / profile.maxHappiness) * 100}%`, background: profile.happiness >= hapHigh ? 'var(--success)' : profile.happiness <= hapLow ? 'var(--danger)' : 'var(--accent-blue)', transition: 'width 0.5s ease-out' }}></div>
                                </div>

                                <div style={{ marginTop: '8px' }}>
                                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '4px' }}>Tax Policy:</div>
                                    <div style={{ display: 'flex', gap: '4px' }}>
                                        {['LOW', 'NORMAL', 'HIGH'].map(bracket => (
                                            <button key={bracket} style={{ flex: 1, padding: '6px 0', background: profile.taxBracket === bracket ? 'var(--surface-2)' : 'transparent', border: `1px solid ${profile.taxBracket === bracket ? 'var(--accent-gold)' : 'var(--border-subtle)'}`, color: profile.taxBracket === bracket ? 'var(--accent-gold)' : 'var(--text-muted)', borderRadius: '4px', cursor: 'pointer', fontSize: '0.8rem', transition: 'all 0.2s' }} onClick={() => handleTaxChange(bracket)} disabled={isBusy}>{bracket}</button>
                                        ))}
                                    </div>
                                </div>

                                <div style={{ background: 'var(--surface-1)', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-subtle)', marginTop: '8px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                                        <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Pending Vault:</span>
                                        <span style={{ color: 'var(--accent-gold)', fontSize: '1.2rem', fontFamily: 'var(--font-heading)', fontWeight: 'bold' }}>{Math.floor(profile.pendingGold)} G</span>
                                    </div>
                                    <button className="button button--claim" style={{ width: '100%' }} onClick={handleCollectTaxes} disabled={isBusy || (now - profile.lastTaxCollectionTimeEpoch) < 3600000}>
                                        { (now - profile.lastTaxCollectionTimeEpoch) >= 3600000 ? 'Collect Taxes' : `Wait ${formatTimeRemainingTaxes(profile.lastTaxCollectionTimeEpoch + 3600000, now)}` }
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {globalWorkerData && (
                        <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)' }}>
                            <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-sm)', color: 'var(--text-muted)' }}>KINGDOM LABOR</h4>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '0.9rem' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--accent-gold)' }}>
                                    <span>{globalWorkerData.label}:</span>
                                    <span>{globalWorkerData.count}</span>
                                </div>
                            </div>
                        </div>
                    )}

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: 'var(--space-lg)' }}>
                        <h4 style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>YOUR STRUCTURES</h4>
                        {selectedGroup.instances.length === 0 && (<span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>You don't have any of these structures.</span>)}

                        {selectedGroup.instances.length === 1 && selectedGroup.type === 'tavern' ? (
                            <button className="button" style={{ width: '100%', padding: '12px' }} onClick={() => onSelectInstance(selectedGroup.instances[0])}>
                                Enter the Tavern
                            </button>
                        ) : selectedGroup.instances.length === 1 && selectedGroup.type === 'keep' ? (
                            <button className="button" style={{ width: '100%', padding: '12px' }} onClick={() => onSelectInstance(selectedGroup.instances[0])}>
                                View Vault Logistics
                            </button>
                        ) : (
                            selectedGroup.instances.map((instance, index) => (
                                <div key={instance.id} className="instance-item" onClick={() => onSelectInstance(instance)}>
                                    <span className="instance-item-title">{selectedGroup.singularName} #{index + 1}</span>
                                    <span className="instance-item-level">Lvl {instance.level}</span>
                                </div>
                            ))
                        )}

                        {selectedGroup.type !== 'keep' && (
                            activeTask ? (
                                <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-sm)', padding: '12px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                                        <span style={{ color: 'var(--accent-gold)' }}>Constructing Lvl {activeTask.targetLevel}...</span>
                                        <span style={{ fontFamily: 'monospace' }}>{formatTimeRemaining(activeTask.completionTimeEpoch, now)}</span>
                                    </div>
                                    <div className="progress-bar-container">
                                        <div className={`progress-bar-fill ${now >= activeTask.completionTimeEpoch ? 'ready' : ''}`} style={{ width: `${calculateProgress(activeTask.startTimeEpoch, activeTask.completionTimeEpoch, now)}%` }}></div>
                                    </div>
                                    {now >= activeTask.completionTimeEpoch && (
                                        <button className="button button--claim" style={{ width: '100%', marginTop: 'var(--space-md)' }} onClick={() => onComplete(selectedGroup.type)} disabled={isBusy}>Complete Construction</button>
                                    )}
                                </div>
                            ) : (
                                (selectedGroup.type !== 'tavern' || selectedGroup.instances.length === 0) && (
                                    <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-sm)', padding: '12px', borderRadius: '8px' }}>
                                        {selectedGroup.cost && (
                                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '12px', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
                                                <span>Cost: <span style={{ color: canAfford ? 'var(--accent-gold)' : 'var(--danger)' }}>{selectedGroup.cost.wood} Wood, {selectedGroup.cost.stone} Stone</span></span>
                                                <span>⏱ {Math.floor(selectedGroup.cost.timeSeconds / 60)}m {selectedGroup.cost.timeSeconds % 60 > 0 ? `${selectedGroup.cost.timeSeconds % 60}s` : ''}</span>
                                            </div>
                                        )}
                                        {profile.keepLevel < selectedGroup.unlockKeepLevel ? (
                                            <p style={{ fontSize: '0.8rem', color: 'var(--danger)', textAlign: 'center', margin: 0 }}>Requires Keep Lvl {selectedGroup.unlockKeepLevel}</p>
                                        ) : (
                                            <>
                                                <button className="button" style={{ width: '100%' }} onClick={() => onConstruct(selectedGroup.type)} disabled={isBusy || !canAfford}>+ Construct New</button>
                                                {!canAfford && (<p style={{ fontSize: '0.7rem', color: 'var(--danger)', textAlign: 'center', marginTop: '8px' }}>Not enough resources</p>)}
                                            </>
                                        )}
                                    </div>
                                )
                            )
                        )}
                    </div>
                </>
            )}

            {displayInstance && (
                <>
                    {selectedGroup.type === 'keep' ? (
                        <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)' }}>
                            <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-md)', color: 'var(--text-muted)' }}>VAULT STORAGE</h4>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                {[
                                    { label: 'Food', current: profile.food, pending: profile.pendingFood, color: 'var(--success)' },
                                    { label: 'Wood', current: profile.wood, pending: profile.pendingWood, color: '#cd853f' },
                                    { label: 'Stone', current: profile.stone, pending: profile.pendingStone, color: 'var(--text-muted)' },
                                    { label: 'Gold', current: profile.gold, pending: profile.pendingGold, color: 'var(--accent-gold)' }
                                ].map(res => (
                                    <div key={res.label}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                                            <span>{res.label}</span>
                                            <span>
                                                <span style={{ color: (res.current + res.pending) >= profile.maxStorage ? 'var(--danger)' : 'inherit' }}>
                                                    {Math.floor(res.current + res.pending)}
                                                </span> / {profile.maxStorage}
                                            </span>
                                        </div>
                                        <div className="progress-bar-container" style={{ height: '6px', background: 'var(--surface-2)' }}>
                                            <div
                                                className="progress-bar-fill"
                                                style={{
                                                    width: `${Math.min(100, ((res.current + res.pending) / profile.maxStorage) * 100)}%`,
                                                    background: res.color,
                                                    transition: 'width 1s linear' // Smooth real-time update
                                                }}
                                            ></div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ) : selectedGroup.type === 'tavern' ? (
                        renderTavernInterior()
                    ) : isHouse ? (
                        <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)' }}>
                            <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-md)', color: 'var(--text-muted)' }}>RESIDENCE</h4>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                                <span>Occupants:</span>
                                <span style={{ color: 'var(--accent-gold)' }}>{occupants} / {houseCapacity}</span>
                            </div>
                        </div>
                    ) : (displayInstance.maxWorkers > 0 || displayInstance.productionRate > 0) ? (
                        <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-md)' }}>
                            <h4 style={{ fontSize: '0.8rem', marginBottom: 'var(--space-md)', color: 'var(--text-muted)' }}>MANAGEMENT</h4>
                            {displayInstance.maxWorkers > 0 && (
                                <div style={{ marginBottom: 'var(--space-lg)' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', marginBottom: '8px' }}>
                                        <span>Assigned Workers:</span>
                                        <span style={{ color: 'var(--accent-gold)' }}>{displayInstance.assignedWorkers} / {displayInstance.maxWorkers}</span>
                                    </div>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <button className="button" style={{ flex: 1 }} disabled={isBusy || displayInstance.assignedWorkers === 0} onClick={handleRemove}>- Remove</button>
                                        <button className="button" style={{ flex: 1 }} disabled={isBusy || displayInstance.assignedWorkers === displayInstance.maxWorkers || getUnassignedCount(selectedGroup.type) === 0} onClick={handleAssign}>+ Assign</button>
                                    </div>
                                    {getUnassignedCount(selectedGroup.type) === 0 && displayInstance.assignedWorkers < displayInstance.maxWorkers && (
                                        <p style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: '8px', textAlign: 'center' }}>No available {getWorkerName(selectedGroup.type)} trained.</p>
                                    )}
                                </div>
                            )}
                            {displayInstance.productionRate > 0 && (
                                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', borderTop: '1px solid var(--border-subtle)', paddingTop: 'var(--space-md)' }}>
                                    <span>Current Output:</span>
                                    <span style={{ color: 'var(--success)' }}>+{displayInstance.assignedWorkers * displayInstance.productionRate}/hr</span>
                                </div>
                            )}
                        </div>
                    ) : null}

                    <div style={{ marginTop: 'auto' }}>
                        <button className="button--primary" style={{ width: '100%' }} disabled>UPGRADE TO LV.{displayInstance.level + 1}</button>
                        <p style={{ fontSize: '0.7rem', textAlign: 'center', marginTop: 'var(--space-sm)', color: 'var(--text-muted)' }}>{selectedGroup.type === 'keep' ? 'Upgrade requirements Not Met' : `Requires Keep Lvl ${displayInstance.level + 1}`}</p>
                    </div>
                </>
            )}
        </aside>
    );
}