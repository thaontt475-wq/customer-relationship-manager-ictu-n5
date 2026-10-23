import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
const base = __ENV.QA_BASE || 'http://127.0.0.1:18085/crm';
if (!/^http:\/\/(127\.0\.0\.1|localhost):\d+\/crm$/.test(base)) throw Error('Loopback QA target required');
const vus = Number(__ENV.VUS || 1);
if (![1,5,10,25,50].includes(vus)) throw Error('Unsupported VU level');
const auth = __ENV.AUTH === '1';
const duration = __ENV.DURATION || '30s';
if (!['30s','60s','1m'].includes(duration)) throw Error('Duration must be 30s or 60s');
export const options = { vus, duration, maxRedirects: 0,
  summaryTrendStats: ['avg','med','p(95)','p(99)'],
  thresholds: { http_req_failed: [{threshold:'rate<0.01',abortOnFail:true,delayAbortEval:'10s'}], checks:['rate==1'] } };
const successes = new Counter('successful_requests');
export function setup() {
  if (!auth) return null;
  if (!__ENV.QA_EMAIL || !__ENV.QA_PASSWORD) throw Error('QA credentials required from environment');
  const r = http.post(base+'/api/auth/login', JSON.stringify({email:__ENV.QA_EMAIL,password:__ENV.QA_PASSWORD}), {headers:{'Content-Type':'application/json'}});
  if (r.status !== 200 || !r.cookies.JSESSIONID) throw Error('QA login failed');
  return r.cookies.JSESSIONID[0].value;
}
export default function(session) {
  // Per-VU limit bounds measured GET requests to <= 3000 per run, even on a long duration override.
  if (__ITER >= Math.floor(3000/vus)) { sleep(1); return; }
  if (session) http.cookieJar().set(base, 'JSESSIONID', session);
  const r = http.get(base+(auth?'/api/leads?size=10':'/api/leads/health'), {timeout:'5s'});
  if (check(r, {'HTTP 200 and API success':r=>r.status===200 && r.json('success')===true})) successes.add(1);
  sleep(1); // <= 50 requests/second across 50 VUs
}
