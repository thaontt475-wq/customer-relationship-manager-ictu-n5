// frontend/shared/js/endpoints.js

const API_BASE_URL = "http://localhost:8080/api";

const API = {
    AUTH: {
        LOGIN: `${API_BASE_URL}/auth/login`,
        LOGOUT: `${API_BASE_URL}/auth/logout`,
        ME: `${API_BASE_URL}/auth/me`
    },
    ROLES: {
        ASSIGN: `${API_BASE_URL}/roles/assign`,
        LIST: `${API_BASE_URL}/roles`
    },
    AUDIT: {
        LIST: `${API_BASE_URL}/audit-logs`
    },
    PRODUCTS: {
        LIST: `${API_BASE_URL}/products`,
        DETAIL: (id) => `${API_BASE_URL}/products/${id}`,
        CREATE: `${API_BASE_URL}/products`,
        UPDATE: (id) => `${API_BASE_URL}/products/${id}`,
        DELETE: (id) => `${API_BASE_URL}/products/${id}`,
        PRICE_BOOKS: `${API_BASE_URL}/price-books`
    }
};

window.API = API;