"use strict";

// Deployment configuration; cookies and authorization always come from the server.
window.CrmApi = (() => {
    const base = (window.CRM_BACKEND_BASE || `${location.protocol}//${location.hostname || "localhost"}:8080/crm`).replace(/\/$/, "");
    let sessionPromise;
    let sessionValue;
    const pendingReads = new Map();
    let lookupPromise;
    function request(path, options = {}) {
        if ((options.method || "GET").toUpperCase() !== "GET") return performRequest(path, options);
        if (path === "/api/customer-lookups") return lookupPromise ||= performRequest(path, options).catch(error => { lookupPromise=null; throw error; });
        if (!pendingReads.has(path)) pendingReads.set(path,performRequest(path,options).finally(()=>pendingReads.delete(path)));
        return pendingReads.get(path);
    }
    async function performRequest(path, options = {}) {
        const form = options.body instanceof FormData;
        const response = await fetch(base + path, {
            ...options, credentials: "include",
            headers: { Accept: "application/json", ...(options.body && !form ? { "Content-Type": "application/json" } : {}), ...options.headers }
        });
        const result = await response.json().catch(() => null);
        if (!response.ok || !result?.success) {
            const error = new Error(result?.message || `Không thể thực hiện yêu cầu (HTTP ${response.status}).`);
            error.status = response.status;
            if (response.status === 401) {
                sessionPromise = null;
                sessionValue = null;
                location.replace("login.html");
            }
            throw error;
        }
        return result.data;
    }
    async function session() {
        return sessionPromise ||= request("/api/auth/session").then(value => { sessionValue=value; return value; }).catch(error => { sessionPromise = null; sessionValue=null; throw error; });
    }
    async function customers(query = {}) {
        const items = [];
        for (let page = 1; ; page++) {
            const data = await request("/api/customers?" + new URLSearchParams({ ...query, page, size: 100 }));
            items.push(...data.items);
            if (page >= data.totalPages || !data.items.length) return items;
        }
    }
    async function can(permission) { return (await session()).permissions?.includes(permission) === true; }
    const canSync = permission => sessionValue?.permissions?.includes(permission) === true;
    return { base, request, session, customers, can, canSync };
})();
