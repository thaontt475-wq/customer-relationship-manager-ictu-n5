# NovaCRM Demo - Khoi dong mot lan nhap

Commit demo: `f61d272` (PR #164). URL: http://localhost:8090/crm/login.html

## Su dung
| Viec | Cach lam |
|---|---|
| Khoi dong | Nhap doi `START_NOVACRM_DEMO.cmd` (hoac `tools\demo\start-demo.ps1`) |
| Kiem tra | `powershell -ExecutionPolicy Bypass -File tools\demo\check-demo.ps1` |
| Dung | `powershell -ExecutionPolicy Bypass -File tools\demo\stop-demo.ps1` |
| Deploy lai WAR | build WAR, `stop-demo.ps1`, roi `start-demo.ps1 -Redeploy` |

Start script: kiem tra MySQL84 -> kiem tra database crm_demo -> kiem tra cong 8090 (neu da la Tomcat demo thi khong chay lai;
neu la tien trinh khac thi bao loi, khong kill) -> khoi dong Tomcat tach roi -> cho health API (timeout 120s) -> mo trinh duyet.

## Cau hinh
- Tomcat demo dung CATALINA_BASE rieng: `..\NovaCRM-Demo-Local\tomcat-base` (HTTP 8090, shutdown 8095). Khong dung webapps cua Tomcat chung.
- Database: `crm_demo`. Script tu sinh `tomcat-base\bin\setenv.bat` de GHI DE `setenv.bat` cua Tomcat chung (file do tro vao `crm_db`).
- Mat khau MySQL KHONG nam trong repo. Tao file `..\NovaCRM-Demo-Local\demo.env` (ngoai repo):
  ```
  CRM_DB_USERNAME=root
  CRM_DB_PASSWORD=<mat khau MySQL>
  ```
- Log: `..\NovaCRM-Demo-Local\logs\` va `tomcat-base\logs\`.

## Tai khoan demo (du lieu tong hop)
Xem `backend/database/seed/README.md`. Vi du `admin@company.com` / mat khau seed chung cua README.

## Khoi phuc du lieu mau
Chi chay tren `crm_demo` (khong bao gio tren `crm_db`). Dung Tomcat truoc:
```powershell
$env:CRM_DB_PASSWORD = '<mat khau MySQL>'
$env:Path += ';C:\Program Files\MySQL\MySQL Server 8.4\bin'
.\backend\database\seed\run-seed.ps1 -Database crm_demo -CleanFirst
```
Truoc khi khoi phuc, sao luu: `mysqldump -u root -p crm_demo > crm_demo_backup.sql` (de ngoai repo).

## Build lai
`mvn -f backend\pom.xml clean package -DskipTests` -> `backend\target\crm.war`.
Test Backend chay tren DB rieng `crm_sprint1_test` (dat CRM_DB_URL tro vao DB do), KHONG dung crm_demo.
