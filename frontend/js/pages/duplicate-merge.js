"use strict";
window.DuplicateMergeEngine = (() => {
    let duplicates = new Map();
    let role = "";
    let permitted = false;
    async function load() {
        const session = await CrmApi.session();
        role = session.roles.map(r => r.code).find(code => ["ADMIN","DIRECTOR","TEAM_LEAD"].includes(code)) || "SALES_REP";
        permitted = ["ADMIN","DIRECTOR","TEAM_LEAD"].includes(role) && session.permissions.includes("customer.merge");
        const data = await CrmApi.request("/api/customers/duplicates");
        const next = new Map();
        for (const group of data.items) {
            for (const match of group.matches) {
                for (const [record, other] of [[group.record,match.record],[match.record,group.record]]) {
                    const entry = next.get(Number(record.id)) || {isDuplicate:true,matches:[]};
                    entry.matches.push({...match, record:other});
                    next.set(Number(record.id),entry);
                }
            }
        }
        duplicates = next;
    }
    return {
        load, scanAllDuplicates: () => duplicates,
        detectDuplicatesForCustomer: customer => duplicates.get(Number(customer.id)) || {isDuplicate:false,matches:[]},
        getSimulatedRole: () => role, setSimulatedRole: () => {}, canExecuteMerge: () => permitted
    };
})();
