import { useState, useEffect } from 'react';
import api from '../api/client';
import type { Quest } from './MissionsView';
import './BoardView.css';

export default function BoardView() {
    const [boardQuests, setBoardQuests] = useState<Quest[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchBoard = () => {
        api.get('/missions/board').then(res => {
            setBoardQuests(res.data);
            setLoading(false);
        }).catch(err => {
            console.error(err);
            setLoading(false);
        });
    };

    useEffect(() => {
        fetchBoard();
    }, []);

    const handleAccept = (questId: string) => {
        api.post(`/missions/accept/${questId}`).then(() => {
            fetchBoard();
            alert("Mission accepted to your Journal!");
        }).catch(err => alert("Failed to accept mission: " + (err.response?.data || err.message)));
    };

    if (loading) return <div className="panel" style={{margin: 'var(--space-md)'}}>Loading Adventurer's Board...</div>;

    return (
        <div className="board-container">
            <h1 className="board-title">Adventurer's Board</h1>
            <p className="board-subtitle">Accept bounties and quests into your Journal to assemble a party.</p>
            <div className="board-grid">
                {boardQuests.length === 0 ? (
                    <div style={{color: 'var(--text-muted)'}}>No new missions available at this time. Check back later.</div>
                ) : boardQuests.map(q => (
                    <div key={q.id} className={`board-card quest-type-${q.type.toLowerCase()}`}>
                        <div className="board-card-header">
                            <h3>{q.title}</h3>
                            <span className="board-card-type">{q.type}</span>
                        </div>
                        <p className="board-card-desc">"{q.description}"</p>
                        <div className="board-card-footer">
                            <span className="board-duration">⏱ {q.durationHours} Hours</span>
                            <button className="button button--claim" onClick={() => handleAccept(q.id)}>Accept Mission</button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}