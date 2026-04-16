import axios from 'axios';

/**
 * Instead of typing the full URL of your server in every file,
 * we create a central "Client." This allows us to easily swap
 * between localhost and your Render URL later.
 */

const api = axios.create({
    baseURL: 'http://localhost:8080/api', // Pointing to the Spring Boot engine
    headers: {
        'Content-Type': 'application/json',
    },
});

// This interceptor automatically attaches the JWT to every request
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('dreamreach_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;