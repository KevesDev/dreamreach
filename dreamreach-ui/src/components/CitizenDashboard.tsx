import { useState, useEffect } from 'react';
import api from '../api/client';
import type { PlayerProfile } from '../views/KingdomView';
import { Icon } from './Icon';
import './CitizenDashboard.css';

interface CitizenDashboardProps {
    profile: PlayerProfile;
    now: number;
    fetchProfile: () => void;
}

export default function CitizenDashboard({ profile, now, fetchProfile }: CitizenDashboardProps) {
    const [isBusy, setIsBusy] = useState(false);
    const [completingTasks, setCompletingTasks] = useState<Set<string>>(new Set());
    const [quantities, setQuantities] = useState<Record<string, number>>({
        woodcutter: 1, stoneworker: 1, hunter: 1, baker: 1
    });

    // Dynamically retrieve the config sent by the backend
    const getConfig = (type: string) => profile?.trainingConfigs?.find(c => c.professionType === type);

    // Added 'as const' to the icons so TypeScript strict-mode correctly infers them as explicit literals,
    // and updated the woodcutter icon from 'tree' to 'wood' to match the Icon component props.
    const professions = [
        { type: 'woodcutter', name: 'Woodcutter', icon: 'wood' as const, count: profile?.woodcutters || 0, config: getConfig('woodcutter') },
        { type: 'stoneworker', name: 'Stoneworker', icon: 'stone' as const, count: profile?.stoneworkers || 0, config: getConfig('stoneworker') },
        { type: 'hunter', name: 'Hunter', icon: 'combat' as const, count: profile?.hunters || 0, config: getConfig('hunter') },
        { type: 'baker', name: 'Baker', icon: 'food' as const, count: profile?.bakers || 0, config: getConfig('baker') }
    ];

    const trainedTotal = professions.reduce((sum, p) => sum + p.count, 0);

    // Automatically complete tasks when their timer expires
    useEffect(() => {
        profile?.activeTrainingTasks?.forEach(task => {
            if (now >= task.completionTimeEpoch && !completingTasks.has(task.id)) {
                setCompletingTasks(prev => new Set(prev).add(task.id));
                api.post(`/player/train/complete?taskId=${task.id}`).then(() => {
                    fetchProfile();
                    setCompletingTasks(prev => {
                        const next = new Set(prev);
                        next.delete(task.id);
                        return next;
                    });
                }).catch(err => console.error("Failed to complete training task", err));
            }
        });
    }, [now, profile?.activeTrainingTasks, completingTasks, fetchProfile]);

    const handleQtyChange = (type: string, delta: number) => {
        setQuantities(prev => ({
            ...prev,
            [type]: Math.max(1, Math.min(profile?.idlePeasants || 1, (prev[type] || 1) + delta))
        }));
    };

    const handleTrain = async (profession: string) => {
        if (isBusy) return;
        setIsBusy(true);
        const qty = quantities[profession] || 1;
        try {
            await api.post(`/player/train?profession=${profession}&quantity=${qty}`);
            fetchProfile();
            // Reset quantity back to 1 after successful queue
            setQuantities(prev => ({ ...prev, [profession]: 1 }));
        } catch (err: any) {
            alert(err.response?.data || "Failed to start training.");
        } finally {
            setIsBusy(false);
        }
    };

    const renderQueue = (type: string) => {
        const tasks = profile?.activeTrainingTasks?.filter(t => t.professionType === type) || [];
        if (tasks.length === 0) return null;

        // Find the task that is active RIGHT NOW, or the next upcoming one
        const activeTask = tasks.find(t => now >= t.startTimeEpoch && now <= t.completionTimeEpoch) || tasks[0];
        const futureCount = tasks.length - (now >= activeTask.startTimeEpoch ? 1 : 0);

        let progress = 0;
        let timeLabel = "";

        if (now < activeTask.startTimeEpoch) {
            progress = 0;
            timeLabel = "Waiting in queue...";
        } else {
            progress = Math.min(100, Math.max(0, ((now - activeTask.startTimeEpoch) / (activeTask.completionTimeEpoch - activeTask.startTimeEpoch)) * 100));
            const diffSeconds = Math.ceil((activeTask.completionTimeEpoch - now) / 1000);
            timeLabel = `Training... ${Math.floor(diffSeconds / 60)}m ${diffSeconds % 60}s`;
        }

        return (
            <div className="training-queue-block">
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '4px', color: 'var(--text-secondary)' }}>
                    <span>{timeLabel}</span>
                    {futureCount > 0 && <span style={{ color: 'var(--accent-gold)' }}>+{futureCount} in queue</span>}
                </div>
                <div className="progress-bar-container">
                    <div className="progress-bar-fill" style={{ width: `${progress}%` }}></div>
                </div>
            </div>
        );
    };

    return (
        <div className="citizen-dashboard">
            <div className="roster-header">
                <div className="roster-stat-card">
                    <h4>Total Population</h4>
                    <div className="roster-stat-value">
                        {profile?.totalPopulation || 0} <span className="roster-stat-max">/ {profile?.maxPopulation || 0}</span>
                    </div>
                </div>
                <div className="roster-stat-card">
                    <h4>Peasants (Idle)</h4>
                    <div className="roster-stat-value idle-color">{profile?.idlePeasants || 0}</div>
                </div>
                <div className="roster-stat-card">
                    <h4>Professionals</h4>
                    <div className="roster-stat-value trained-color">{trainedTotal}</div>
                </div>
            </div>

            <div className="profession-list">
                {professions.map(prof => {
                    const qty = quantities[prof.type] || 1;
                    const totalGold = (prof.config?.goldCost || 0) * qty;
                    const totalFood = (prof.config?.foodCost || 0) * qty;
                    const canAfford = (profile?.gold || 0) >= totalGold && (profile?.food || 0) >= totalFood && (profile?.idlePeasants || 0) >= qty;

                    return (
                        <div key={prof.type} className="profession-row">
                            <div className="profession-info">
                                <div className="profession-icon-wrapper">
                                    <Icon name={prof.icon} size={24} />
                                </div>
                                <div>
                                    <h3 className="profession-name">{prof.name} <span className="profession-count">({prof.count})</span></h3>
                                </div>
                            </div>

                            <div className="profession-controls-container">
                                <div className="profession-controls">
                                    <div className="training-cost">
                                        <span style={{ color: canAfford ? 'inherit' : 'var(--danger)' }}>
                                            {totalGold} Gold, {totalFood} Food
                                        </span>
                                        <span style={{ marginLeft: '12px', color: 'var(--text-muted)' }}>
                                            ⏱ {prof.config?.trainTimeSeconds || 0}s
                                        </span>
                                    </div>
                                    <div className="quantity-selector">
                                        <button onClick={() => handleQtyChange(prof.type, -1)} disabled={qty <= 1}>-</button>
                                        <span>{qty}</span>
                                        <button onClick={() => handleQtyChange(prof.type, 1)} disabled={qty >= (profile?.idlePeasants || 1)}>+</button>
                                    </div>
                                    <button
                                        className="button button--claim"
                                        style={{ minWidth: '100px' }}
                                        onClick={() => handleTrain(prof.type)}
                                        disabled={!canAfford || isBusy || (profile?.idlePeasants || 0) < 1}
                                    >
                                        TRAIN
                                    </button>
                                </div>
                                {renderQueue(prof.type)}
                            </div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}