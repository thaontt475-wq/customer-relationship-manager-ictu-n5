async function apiRequest(endpoint, options = {}) {

    const defaultOptions = {
        headers: {
            "Content-Type": "application/json"
        }
    };

    const config = {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...(options.headers || {})
        }
    };

    const response = await fetch(
        CONFIG.API_BASE_URL + endpoint,
        config
    );

    if (!response.ok) {
        throw new Error(
            `HTTP ${response.status}: ${response.statusText}`
        );
    }

    if (response.status === 204) {
        return null;
    }

    return await response.json();
}