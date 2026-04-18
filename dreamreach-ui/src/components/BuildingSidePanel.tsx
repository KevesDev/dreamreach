import type { BuildingGroup, BuildingInstance, PlayerProfile, ConstructionTaskResponse } from '../views/KingdomView';

interface BuildingSidePanelProps {
    selectedGroup: BuildingGroup;
    selectedInstance: BuildingInstance | null;
    profile: PlayerProfile;
    now: number;
    isBusy: boolean;
    onClose: () => void;
    onSelectInstance: (instance: BuildingInstance | null) => void;
    onConstruct: (type: string) => void;
    onComplete: (type: string) => void;
}

export default function BuildingSidePanel({
                                              selectedGroup,
                                              selectedInstance,
                                              profile,
                                              now,
                                              isBusy,
                                              onClose,
                                              onSelectInstance,
                                              onConstruct,
                                              onComplete
                                          }: BuildingSidePanelProps) {

    const activeTask = profile?.activeConstructions?.find(
        (t: ConstructionTaskResponse) => t.buildingType === selectedGroup.type
    );

    const getGlobalWorkerCount = (type: string) => {
        switch(type) {
            case 'bakery': return { count: profile?.bakers || 0, label: 'Total Bakers' };
            case 'lodge': return { count: profile?.hunters || 0, label: 'Total Hunters' };
            default: return null;
        }
    };

    const calculateProgress = (start: number, end: number, current: number) => {
        if (current >= end) return 100;
        if (current <= start) return 0;
        return Math.min(100, Math.max(0, ((current - start) / (end - start)) * 100));
    };

    const formatTimeRemaining = (end: number, current: number) => {
        if (current >= end) return "Ready!";
        const diffSeconds = Math.ceil((end - current) / 1000);
        const m = Math.floor(diffSeconds / 60);
        const s = diffSeconds % 60;
        return `${m}:${s.toString().padStart(2, '0')}`;
    };

    return (
        <aside className="side-panel">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <h3 style={{ color: 'var(--accent-gold)' }}>
                        {selectedInstance ? `${selectedGroup.singularName} (Lvl ${selectedInstance.level})` : selectedGroup.name}
                    </h3>
                    {selectedInstance && selectedGroup.instances.length > 1 && (
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

                        {selectedGroup.instances.length === 0 && (
                            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>You do not have any of these structures.</span>
                        )}

                        {selectedGroup.instances.map((instance, index) => (
                            <div
                                key={instance.id}
                                className="instance-item"
                                onClick={() => onSelectInstance(instance)}
                            >
                                <span className="instance-item-title">{selectedGroup.singularName} #{index + 1}</span>
                                <span className="instance-item-level">Lvl {instance.level}</span>
                            </div>
                        ))}

                        {selectedGroup.type !== 'keep' && (
                            activeTask ? (
                                <div className="panel" style={{ background: 'var(--bg-elevated)', marginTop: 'var(--space-sm)', padding: '12px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                                        <span style={{ color: 'var(--accent-gold)' }}>Constructing Lvl {activeTask.targetLevel}...</span>
                                        <span style={{ fontFamily: 'monospace' }}>
                                            {formatTimeRemaining(activeTask.completionTimeEpoch, now)}
                                        </span>
                                    </div>

                                    <div className="progress-bar-container">
                                        <div
                                            className={`progress-bar-fill ${now >= activeTask.completionTimeEpoch ? 'ready' : ''}`}
                                            style={{ width: `${calculateProgress(activeTask.startTimeEpoch, activeTask.completionTimeEpoch, now)}%` }}
                                        ></div>
                                    </div>

                                    {now >= activeTask.completionTimeEpoch && (
                                        <button
                                            className="button button--claim"
                                            style={{ width: '100%', marginTop: 'var(--space-md)' }}
                                            onClick={() => onComplete(selectedGroup.type)}
                                            disabled={isBusy}
                                        >
                                            Complete Construction
                                        </button>
                                    )}
                                </div>
                            ) : (
                                <button
                                    className="button"
                                    style={{ marginTop: 'var(--space-sm)' }}
                                    onClick={() => onConstruct(selectedGroup.type)}
                                    disabled={isBusy}
                                >
                                    + Construct New
                                </button>
                            )
                        )}
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
                        <button className="button--primary" style={{ width: '100%' }} disabled>
                            UPGRADE TO LV.{selectedInstance.level + 1}
                        </button>
                        <p style={{ fontSize: '0.7rem', textAlign: 'center', marginTop: 'var(--space-sm)', color: 'var(--text-muted)' }}>
                            {selectedGroup.type === 'keep'
                                ? 'Upgrade requirements Not Met'
                                : `Requires Keep Lvl ${selectedInstance.level + 1}`}
                        </p>
                    </div>
                </>
            )}
        </aside>
    );
}