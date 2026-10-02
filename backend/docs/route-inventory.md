# URL và chuỗi xử lý thực tế

> Các đường dẫn mã nguồn trong tài liệu tính từ gốc repository; URL runtime không thay đổi.

Sinh từ annotation/import/JSP dispatcher trong mã nguồn; đây là inventory tĩnh, không thay thế kiểm thử HTTP.

| Servlet | URL | JSP forward | Service imports |
|---|---|---|---|
| AuditLogServlet | /api/audit-logs |  | com.crm.service.audit.AuditLogService |
| AuditPageServlet | /audit | /jsp/audit/audit-log.jsp | com.crm.service.permissions.MenuService, com.crm.service.audit.AuditLogService |
| ChangePasswordServlet | /change-password, /api/auth/change-password | /jsp/auth/change-password.jsp | com.crm.service.auth.AuthService, com.crm.service.auth.AuthService.ChangePasswordResult |
| ForgotPasswordServlet | /forgot-password, /api/auth/forgot-password | /jsp/auth/forgot-password.jsp | com.crm.service.auth.AuthService |
| LoginServlet | /login, /api/auth/login | /jsp/auth/login.jsp | com.crm.service.auth.AuthService |
| LogoutServlet | /api/auth/logout |  |  |
| ResetPasswordServlet | /reset-password, /api/auth/reset-password | /jsp/auth/reset-password.jsp | com.crm.service.auth.AuthService |
| SessionServlet | /api/auth/session |  |  |
| CategoryPageServlet | /configuration, /configuration/page | /jsp/configuration/configuration.jsp | com.crm.service.categories.CategoryService, com.crm.service.categories.CategoryInUseException |
| CategoryServlet | /api/categories, /api/categories/*, /api/master-data, /api/master-data/* |  | com.crm.service.categories.CategoryInUseException, com.crm.service.categories.CategoryService |
| CustomFieldPageServlet | /customfields, /customfields/page | /jsp/customfields/custom-field-list.jsp | com.crm.service.customfields.CustomFieldService |
| CustomFieldServlet | /api/custom-fields, /api/custom-fields/* |  | com.crm.service.customfields.CustomFieldService, com.crm.service.customfields.CustomFieldService.DeleteOutcome, com.crm.service.customfields.CustomFieldService.NotFoundException |
| DashboardServlet | /dashboard | /jsp/dashboard/dashboard.jsp | com.crm.service.teams.TeamService, com.crm.service.users.UserService |
| ErrorPageServlet | /errors/401, /errors/403, /errors/404, /errors/500 | /jsp/errors/401.jsp, /jsp/errors/403.jsp, /jsp/errors/404.jsp, /jsp/errors/500.jsp |  |
| UserImportServlet | /users/import, /users/import/*, /api/users/import, /api/users/import/* | /jsp/users/user-import.jsp | com.crm.service.excel.ExcelService |
| OrganizationPageServlet | /organization, /organization/page | /jsp/organization/organization.jsp | com.crm.service.organization.OrganizationService, com.crm.service.organization.OrganizationService.OrganizationException, com.crm.service.organization.OrganizationService.UnitInput |
| OrganizationServlet | /api/organization/units, /api/organization/units/* |  | com.crm.service.organization.OrganizationService, com.crm.service.organization.OrganizationService.OrganizationException, com.crm.service.organization.OrganizationService.UnitInput |
| MenuServlet | /api/navigation/menu, /api/menu |  | com.crm.service.permissions.MenuService |
| NavigationServlet | /navigation, /menu |  | com.crm.service.permissions.MenuService |
| PermissionApiServlet | /api/permissions/users/*, /api/permissions/assign |  | com.crm.service.permissions.PermissionService, com.crm.service.permissions.PermissionService.AssignmentResult |
| PermissionServlet | /permissions, /permissions/assign, /permissions/team | /jsp/permissions/role-permission.jsp | com.crm.service.permissions.PermissionService, com.crm.service.permissions.PermissionService.AssignmentResult, com.crm.service.teams.TeamService |
| UserRoleServlet | /api/roles, /api/roles/* |  | com.crm.service.permissions.UserRoleService, com.crm.service.permissions.UserRoleService.RoleAssignmentResult |
| PipelinePageServlet | /pipeline, /pipeline/page | /jsp/pipeline/pipeline-config.jsp | com.crm.service.pipeline.PipelineService, com.crm.service.pipeline.StageInUseException |
| PipelineServlet | /api/pipeline/stages, /api/pipeline/stages/*, /api/stages, /api/stages/* |  | com.crm.service.pipeline.PipelineService, com.crm.service.pipeline.PipelineService.TransitionContext, com.crm.service.pipeline.PipelineService.TransitionValidationResult, com.crm.service.pipeline.StageInUseException |
| ProductPageServlet | /products/page | /jsp/products/product-list.jsp | com.crm.service.products.ProductService |
| ProductServlet | /products, /products/*, /api/products, /api/products/* |  | com.crm.service.products.ProductInUseException, com.crm.service.products.ProductService, com.crm.service.products.ProductService.ProductSearchResult |
| ScopedEntityPageServlet | /customers, /opportunities, /activities, /quotes | /jsp/shared/scoped-records.jsp | com.crm.service.scope.* |
| ScopedEntityServlet | /api/customers, /api/customers/*, /api/opportunities, /api/opportunities/*, /api/activities, /api/activities/*, /api/quotes, /api/quotes/* |  | com.crm.service.scope.DataScopeService, com.crm.service.scope.ScopeEntityType, com.crm.service.scope.ScopeRecord |
| TeamServlet | /api/teams |  | com.crm.service.teams.TeamService |
| AvatarServlet | /profile/avatar, /profile/avatar/image, /profile/avatar/thumbnail, /api/users/me/avatar | /jsp/users/avatar.jsp | com.crm.service.users.AvatarException, com.crm.service.users.AvatarService |
| ProfileServlet | /profile, /user/profile, /api/profile, /api/user/profile, /api/users/profile, /api/users/me | /jsp/users/profile.jsp | com.crm.service.users.ProfileService |
| UserServlet | /users, /users/detail, /users/lock-handover, /users/unlock, /api/users/* | /jsp/users/user-list.jsp, /jsp/users/user-detail.jsp | com.crm.service.teams.TeamService, com.crm.service.teams.TeamService.AssignmentResult, com.crm.service.users.UserService, com.crm.service.users.UserService.StatusChangeResult, com.crm.service.users.UserService.TransferValidationResult |
| WinLossPageServlet | /winloss | /jsp/winloss/winloss.jsp | com.crm.service.winloss.WinLossService |
| WinLossServlet | /api/winloss/reasons, /api/winloss/competitors |  | com.crm.service.winloss.WinLossService, com.crm.service.winloss.WinLossService.DeleteOutcome, com.crm.service.winloss.WinLossService.DuplicateException, com.crm.service.winloss.WinLossService.NotFoundException |

## Filter

EncodingFilter → CsrfFilter áp dụng REQUEST /* theo thứ tự web.xml. Các Filter annotation còn lại có mapping riêng; không giả định thứ tự giữa annotation filters.

ViewAccessFilter chặn REQUEST /jsp/*; forward nội bộ vẫn được phép.

## Service → DAO → MySQL

| Service | DAO imports |
|---|---|
| AuditLogService | com.crm.dao.audit.AuditLogDAO |
| AuthService | com.crm.dao.auth.PasswordResetTokenDAO, com.crm.dao.users.UserDAO |
| CategoryService | com.crm.dao.categories.CategoryDAO |
| CustomFieldService | com.crm.dao.customfields.CustomFieldDAO |
| ExcelService | com.crm.dao.excel.UserImportDAO |
| OrganizationService | com.crm.dao.organization.OrganizationDAO, com.crm.dao.teams.UserTeamDAO, com.crm.dao.users.UserDAO |
| MenuService | com.crm.dao.permissions.MenuDAO |
| PermissionService | com.crm.dao.permissions.PermissionDAO, com.crm.dao.users.UserDAO |
| UserRoleService | com.crm.dao.permissions.PermissionDAO, com.crm.dao.permissions.UserRoleDAO, com.crm.dao.teams.UserTeamDAO, com.crm.dao.users.UserDAO |
| PipelineService | com.crm.dao.pipeline.PipelineDAO |
| ProductService | com.crm.dao.products.ProductDAO |
| DataScopeService | com.crm.dao.scope.DataScopeHelper, com.crm.dao.scope.ScopedEntityDAO, com.crm.dao.users.UserDAO |
| TeamService | com.crm.dao.teams.TeamDAO, com.crm.dao.users.UserDAO |
| AvatarService | com.crm.dao.users.AvatarDAO |
| ProfileService | com.crm.dao.users.UserDAO |
| UserService | com.crm.dao.users.UserDAO, com.crm.dao.users.UserLockHandoverDAO |
| WinLossService | com.crm.dao.winloss.WinLossDAO |
