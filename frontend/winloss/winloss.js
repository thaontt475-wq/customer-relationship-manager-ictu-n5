let reasons = [
  { id: 1, text: 'Giá cả cạnh tranh, ưu đãi tốt', type: 'WIN', status: 'Kích hoạt' },
  { id: 2, text: 'Sản phẩm chưa đủ tính năng nâng cao', type: 'LOSS', status: 'Kích hoạt' },
  { id: 3, text: 'Đối thủ chiết khấu cao hơn', type: 'LOSS', status: 'Kích hoạt' }
];

let competitors = [
  { id: 1, name: 'Công ty CRM Việt Nam', strength: 'Giá rẻ, thương hiệu lâu năm', weakness: 'Giao diện cũ, hỗ trợ chậm' },
  { id: 2, name: 'Tập đoàn Công nghệ Global', strength: 'Nhiều tính năng, đa ngôn ngữ', weakness: 'Chi phí đắt, khó sử dụng' }
];

document.addEventListener('DOMContentLoaded', () => {
  renderReasons();
  renderCompetitors();
  setupForms();
});

function switchTab(tabName) {
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

  if (tabName === 'reasons') {
    document.querySelectorAll('.tab-btn')[0].classList.add('active');
    document.getElementById('tab-reasons').classList.add('active');
  } else {
    document.querySelectorAll('.tab-btn')[1].classList.add('active');
    document.getElementById('tab-competitors').classList.add('active');
  }
}

function renderReasons() {
  const tbody = document.getElementById('reasonTableBody');
  if (!tbody) return;
  tbody.innerHTML = reasons.map((r, index) => `
    <tr>
      <td>${index + 1}</td>
      <td><strong>${r.text}</strong></td>
      <td><span class="${r.type === 'WIN' ? 'badge-win' : 'badge-loss'}">${r.type === 'WIN' ? 'THẮNG (WIN)' : 'THUA (LOSS)'}</span></td>
      <td>${r.status}</td>
      <td><button class="btn-delete" onclick="deleteReason(${r.id})">Xóa</button></td>
    </tr>
  `).join('');
}

function renderCompetitors() {
  const tbody = document.getElementById('competitorTableBody');
  if (!tbody) return;
  tbody.innerHTML = competitors.map((c, index) => `
    <tr>
      <td>${index + 1}</td>
      <td><strong>${c.name}</strong></td>
      <td>${c.strength || '-'}</td>
      <td>${c.weakness || '-'}</td>
      <td><button class="btn-delete" onclick="deleteCompetitor(${c.id})">Xóa</button></td>
    </tr>
  `).join('');
}

function openReasonModal() { document.getElementById('reasonModal').style.display = 'flex'; }
function openCompetitorModal() { document.getElementById('competitorModal').style.display = 'flex'; }
function closeModals() {
  document.getElementById('reasonModal').style.display = 'none';
  document.getElementById('competitorModal').style.display = 'none';
}

function deleteReason(id) {
  if (confirm('Xóa lý do này?')) {
    reasons = reasons.filter(r => r.id !== id);
    renderReasons();
  }
}

function deleteCompetitor(id) {
  if (confirm('Xóa đối thủ này?')) {
    competitors = competitors.filter(c => c.id !== id);
    renderCompetitors();
  }
}

function setupForms() {
  const rForm = document.getElementById('reasonForm');
  const cForm = document.getElementById('competitorForm');

  if (rForm) {
    rForm.onsubmit = (e) => {
      e.preventDefault();
      reasons.push({
        id: Date.now(),
        text: document.getElementById('reasonText').value,
        type: document.getElementById('reasonType').value,
        status: 'Kích hoạt'
      });
      renderReasons();
      e.target.reset();
      closeModals();
    };
  }

  if (cForm) {
    cForm.onsubmit = (e) => {
      e.preventDefault();
      competitors.push({
        id: Date.now(),
        name: document.getElementById('compName').value,
        strength: document.getElementById('compStrength').value,
        weakness: document.getElementById('compWeakness').value
      });
      renderCompetitors();
      e.target.reset();
      closeModals();
    };
  }
}