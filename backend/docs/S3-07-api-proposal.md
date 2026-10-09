# S3-07: Proposed Saved Filter and region API extensions

Author: Nguyễn Việt Tiến. Branch: `feature/BE-S3-07-customer-search-filter`.

The following endpoints and Customer `region` extension are implemented proposals.
They require group confirmation and are **not an officially agreed Contract**.
Migration 017 was reported successfully applied to `crm_db` by the user; no additional migration is needed.

## Saved Filter

- `GET /api/saved-filters?entity=CUSTOMER&page=1&size=20`: returns `data` as an array of the current user's filters. Pagination defaults to 1/20, size 1–100.
- `POST /api/saved-filters`: returns 201 with the created filter in `data`.
- `GET /api/saved-filters/{id}`: returns the creator's filter in `data`.
- `DELETE /api/saved-filters/{id}`: returns 200, `data:null`.

Create request:

```json
{"entity":"CUSTOMER","name":"Khách hàng tiềm năng Hà Nội","criteria":{"keyword":"","status":"TIEM_NANG","industry":null,"size":null,"region":"Hà Nội","ownerId":null}}
```

Each filter response contains `id`, `entity`, `name`, `criteria`, `createdAt`.
Criteria retain the six proposed keys, including nullable fields. `industry` accepts a positive industry ID or master_data industry code; `size` is a positive company-size master_data ID. Blank keyword becomes null.
Names are required, trimmed, maximum 100 characters. Region is optional, trimmed, maximum 20 characters; both existing MB/MT/MN values and explicit names such as Hà Nội are supported.
JSON is bounded to 16384 characters. Unsupported criteria fields (including scope) are rejected.
Only CUSTOMER is supported by this implementation; it does not claim to implement Sprint 4 LEAD filters.

Authentication uses the existing filter/session. The servlet takes the actor ID from the session and ignores any client userId.
The service requires customer.read. DAO queries bind both filter ID and user_id; another user's detail/delete returns 404.
No scope or role is persisted in criteria. All responses use the existing success/message/data ApiResponse.
Errors: 400 invalid request/entity/criteria, 401 unauthenticated, 403 permission denied, 404 missing/not owned, 405 unsupported method, 500 sanitized server failure.

## Applying criteria

Fetch the saved filter, then call `GET /api/customers/search` using its criteria:
`keyword` → keyword, `status` → status, `industry` → industry,
`size` → **companySizeId**, `region` → region, `ownerId` → ownerId.
Supply pagination page/size separately; criteria.size is not pagination size.
The Customer service resolves current DataScope SELF/TEAM/ALL again. Stored owner criteria only narrow that scope.
No additional apply endpoint is introduced.

## Customer region

`POST /api/customers` and `PUT /api/customers/{id}` accept optional `region`, a string or null.
Region is returned alongside existing Customer response fields and can be filtered by the search endpoint.
Absent region on POST stores null. Absent region on PUT preserves the stored region;
explicit null/blank clears it. Values are trimmed and limited to 20 Unicode characters.
Region is never inferred from address. Other PUT fields retain the existing full-update behavior;
this change does not introduce general PATCH semantics.
Existing Customer create/update permissions and owner-scope checks remain in force.
Import and merge SQL are unchanged and therefore retain the existing target region; merge does not add region as a selectable override.

No Frontend changes, commit, push or persisted Customer data edits are part of this implementation.
