import React from 'react';

interface IconProps {
    name: 'population' | 'food' | 'wood' | 'stone' | 'gold' | 'gems' | 'logout' | 'home' | 'combat' | 'summon' | 'kingdom' | 'filter' | 'inventory' | 'plus' | 'user' | 'health' | 'shop' | 'close';
    size?: number;
    className?: string;
    style?: React.CSSProperties;
}

const paths = {
    population: "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
    food: "M12 3L4 9v11a1 1 0 001 1h14a1 1 0 001-1V9l-8-6zm0 13a3 3 0 110-6 3 3 0 010 6z",
    wood: "M4.41 22L2 19.59L19.59 2l2.41 2.41L4.41 22zM14.5 6.5l2.25 2.25L11 14.5L8.75 12.25L14.5 6.5z",
    stone: "M12 2L2 19h20L12 2zm0 4.19L18.74 17H5.26L12 6.19z",
    gold: "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z",
    gems: "M12 2l-5.5 9 5.5 9 5.5-9-5.5-9z",
    home: "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z",
    kingdom: "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z",
    combat: "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z",
    summon: "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z",
    logout: "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5-5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z",
    filter: "M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z",
    inventory: "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8c0-1.11-.89-2-2-2zm-8 0h-4V4h4v2z",
    plus: "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z",
    user: "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z",
    health: "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z",
    shop: "M20 4H4v2h16V4zm1 10v-2l-1-5H4l-1 5v2h1v6h10v-6h4v6h2v-6h1zm-9 4H6v-4h6v4z",
    close: "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"
};

export const Icon: React.FC<IconProps> = ({ name, size = 20, className, style }) => {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className} style={{ display: 'inline-block', verticalAlign: 'middle', ...style }}>
            <path d={paths[name]} />
        </svg>
    );
};