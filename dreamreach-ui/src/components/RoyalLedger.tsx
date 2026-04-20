import type { KingdomEvent } from '../views/KingdomView';

interface RoyalLedgerProps {
    events: KingdomEvent[];
}

export default function RoyalLedger({ events }: RoyalLedgerProps) {
    return (
        <div className="journal-container">
            <div className="journal-header">
                <h2>Royal Ledger</h2>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Latest Events</span>
            </div>

            <div className="journal-log">
                {events.map(event => (
                    <div key={event.id} className={`journal-entry`}>
                        <span className={`journal-time journal-time--${event.type}`}>[{event.timestamp}]</span>
                        <span className="journal-text">{event.message}</span>
                    </div>
                ))}
                {events.length === 0 && (
                    <div style={{ color: 'var(--text-muted)', fontStyle: 'italic', padding: '10px' }}>
                        No notable events have occurred recently.
                    </div>
                )}
            </div>
        </div>
    );
}