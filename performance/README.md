# NovaCRM QA performance
Run only against a dedicated localhost Tomcat backed by the verified QA database.
`k6 run --summary-export result.json -e VUS=1 -e DURATION=30s performance/leads-read.js`
Then run 5, 10, 25, 50 VUs for 60s each, inspecting the previous result and machine health before increasing.
Default port is 18085. Set QA_BASE for another loopback port. AUTH=1 reads the Lead list and requires QA_EMAIL/QA_PASSWORD in the process environment. Never put credentials or sessions in this directory.
Each VU sleeps one second; each run caps measured GETs at 3000. Unexpected errors above 1% abort after a 10s evaluation period. Stop escalation on resource exhaustion even if HTTP still responds.
