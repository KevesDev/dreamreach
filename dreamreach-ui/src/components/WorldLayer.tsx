import type { BuildingGroup } from '../views/KingdomView';
import { Icon } from './Icon';

interface WorldLayerProps {
    buildingGroups: BuildingGroup[];
    selectedGroup: BuildingGroup | null;
    activeConstructions?: { buildingType: string; completionTimeEpoch: number }[];
    now: number;
    onSelectGroup: (group: BuildingGroup) => void;
}

export default function WorldLayer({
                                       buildingGroups,
                                       selectedGroup,
                                       activeConstructions,
                                       now,
                                       onSelectGroup
                                   }: WorldLayerProps) {
    return (
        <div className="world-layer">
            {buildingGroups.map((group) => {
                const task = activeConstructions?.find(t => t.buildingType === group.type);
                const isReady = task && now >= task.completionTimeEpoch;

                return (
                    <div
                        key={group.type}
                        className={`map-node ${selectedGroup?.type === group.type ? 'active' : ''} ${isReady ? 'ready' : ''}`}
                        onClick={() => onSelectGroup(group)}
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
    );
}