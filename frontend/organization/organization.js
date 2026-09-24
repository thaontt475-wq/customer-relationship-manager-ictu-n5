let orgData = [
  { id: 1, name: 'Khối Kinh Doanh (Sales)', parentId: null, leader: 'Nguyễn Văn Giám Đốc' },
  { id: 2, name: 'Phòng Kinh Doanh Miền Bắc', parentId: 1, leader: 'Trần Văn Trưởng Phòng' },
  { id: 3, name: 'Nhóm Sales Hà Nội', parentId: 2, leader: 'Lê Văn Leader' },
  { id: 4, name: 'Phòng Kinh Doanh Miền Nam', parentId: 1, leader: 'Phạm Thị Trưởng Phòng' }
];

document.addEventListener('DOMContentLoaded', () => {
  renderOrgTree();
  setupModalEvents();
});

function renderOrgTree() {
  const container = document.getElementById('orgTree');
  if (!container) return;
  container.innerHTML = '';

  const rootUnits = orgData.filter(u => !u.parentId);

  rootUnits.forEach(unit => {
    container.appendChild(createTreeNode(unit));
  });

  updateParentSelectOptions();
}

function createTreeNode(unit) {
  const wrapper = document.createElement('div');
  wrapper.className = 'tree-node';

  const card = document.createElement('div');
  card.className = 'tree-card';
  card.innerHTML = `
    <strong>🏢 ${unit.name}</strong>
    <span class="leader">👤 Trưởng nhóm: ${unit.leader || 'Chưa có'}</span>
    <div class="actions">
      <button onclick="deleteUnit(${unit.id})">Xóa</button>
    </div>
  `;
  wrapper.appendChild(card);

  const children = orgData.filter(u => u.parentId === unit.id);
  children.forEach(child => {
    wrapper.appendChild(createTreeNode(child));
  });

  return wrapper;
}

function updateParentSelectOptions() {
  const select = document.getElementById('parentUnit');
  if (!select) return;
  select.innerHTML = '<option value="">-- Là Cấp Cao Nhất (Công ty) --</option>';

  orgData.forEach(unit => {
    const opt = document.createElement('option');
    opt.value = unit.id;
    opt.textContent = unit.name;
    select.appendChild(opt);
  });
}

window.deleteUnit = function(id) {
  if (confirm('Bạn có chắc chắn muốn xóa đơn vị này?')) {
    orgData = orgData.filter(u => u.id !== id && u.parentId !== id);
    renderOrgTree();
  }
};

function setupModalEvents() {
  const modal = document.getElementById('orgModal');
  const btnOpen = document.getElementById('btnOpenAddModal');
  const btnClose = document.getElementById('btnCloseModal');
  const btnCancel = document.getElementById('btnCancel');
  const form = document.getElementById('orgForm');

  if (btnOpen) btnOpen.onclick = () => modal.style.display = 'flex';
  if (btnClose) btnClose.onclick = () => modal.style.display = 'none';
  if (btnCancel) btnCancel.onclick = () => modal.style.display = 'none';

  if (form) {
    form.onsubmit = (e) => {
      e.preventDefault();
      const name = document.getElementById('unitName').value;
      const parentId = document.getElementById('parentUnit').value;
      const leader = document.getElementById('leaderName').value;

      const newUnit = {
        id: Date.now(),
        name: name,
        parentId: parentId ? parseInt(parentId) : null,
        leader: leader
      };

      orgData.push(newUnit);
      renderOrgTree();

      form.reset();
      modal.style.display = 'none';
    };
  }
}