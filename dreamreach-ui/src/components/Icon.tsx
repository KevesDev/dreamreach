import React from 'react';

interface IconProps {
    name: 'population' | 'food' | 'wood' | 'stone' | 'gold' | 'gems' | 'logout' | 'home' | 'combat' | 'summon' | 'kingdom';
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
    logout: "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5-5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"
};

export const Icon: React.FC<IconProps> = ({ name, size = 20, className, style }) => {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className} style={{ display: 'inline-block', verticalAlign: 'middle', ...style }}>
            <path d={paths[name]} />
        </svg>
    );
};