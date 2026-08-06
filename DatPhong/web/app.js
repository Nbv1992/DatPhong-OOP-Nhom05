/* ================================================================
   EduRoom – app.js  (Final, clean)
   ADMIN   : quản lý phòng, sinh viên, tất cả lịch đặt
   HOCVIEN : xem phòng, lịch trống, đặt phòng, lịch của mình,
             hủy lịch, cập nhật thông tin cá nhân
   ================================================================ */

// Tự động dùng host của server đang serve file này
// → hoạt động đúng dù truy cập qua localhost hay IP mạng LAN
const API = `${location.protocol}//${location.hostname}:8080/api`;
const SESSION_KEY = 'eduroom_session';

/* ═══ Session helpers ═══ */
const getSession = () => { try { return JSON.parse(localStorage.getItem(SESSION_KEY)); } catch { return null; } };
const isAdmin    = () => getSession()?.role === 'ADMIN';
const isHocVien  = () => getSession()?.role === 'HOCVIEN';
const getMyId    = () => getSession()?.studentId ?? null;
const logout     = () => { localStorage.removeItem(SESSION_KEY); location.href = '/login.html'; };

/* ═══ Session guard – chạy ngay khi load ═══ */
(function () {
  const s = getSession();
  if (!s?.studentId || !s?.role) location.href = '/login.html';
}());

/* ═══ State ═══ */
let allRooms = [], allStudents = [], allBookings = [], myBookings = [];
let cancelTarget = null, statusTargetRoom = null;

/* ═══ API helper ═══ */
async function apiFetch(url, opts = {}) {
  try {
    const res = await fetch(url, {
      headers: { 'Content-Type': 'application/json; charset=UTF-8' },
      ...opts
    });
    return await res.json();
  } catch { return { success: false, message: 'Không kết nối được server.' }; }
}

/* ═══ User Dropdown ═══ */
function toggleUserMenu() { document.getElementById('userMenu').classList.toggle('open'); }
document.addEventListener('click', e => {
  if (!document.getElementById('userDropdown')?.contains(e.target))
    document.getElementById('userMenu')?.classList.remove('open');
});

/* ═══ Init UI theo role – gọi từ DOMContentLoaded ═══ */
function initRoleUI() {
  const s    = getSession();
  const role = s.role;
  const name = s.displayName || s.studentId;

  /* topbar */
  document.getElementById('topbarName').textContent        = name;
  document.getElementById('topbarRole').textContent        = role === 'ADMIN' ? 'Quản trị viên' : 'Học viên';
  document.getElementById('menuName').textContent          = name;
  document.getElementById('menuStudentId').textContent     = s.studentId;
  document.getElementById('userAvatar').textContent        = name.charAt(0).toUpperCase();
  document.getElementById('menuRoleBadge').textContent     = role === 'ADMIN' ? 'Admin' : 'Học viên';
  document.getElementById('menuRoleBadge').style.background =
    role === 'ADMIN' ? 'linear-gradient(135deg,#f7a641,#f45d5d)' : 'linear-gradient(135deg,#6c63ff,#a78bfa)';
  document.getElementById('sidebarRoleBadge').textContent  = role === 'ADMIN' ? '👑 Admin' : '🎓 Học viên';

  /* hiện/ẩn sidebar items */
  document.querySelectorAll('.role-admin').forEach(el   => { el.style.display = isAdmin()   ? '' : 'none'; });
  document.querySelectorAll('.role-hocvien').forEach(el => { el.style.display = isHocVien() ? '' : 'none'; });

  /* dashboard labels cho hocvien */
  if (isHocVien()) {
    document.getElementById('dashBookingsTitle').textContent     = 'Lịch đặt của tôi';
    document.getElementById('dashBookingsMoreLink').dataset.page = 'mybookings';
    document.getElementById('statBookingsLabel').textContent     = 'Lịch đặt của tôi';
  }

  /* welcome */
  const h = new Date().getHours();
  const g = h < 12 ? 'Chào buổi sáng' : h < 18 ? 'Chào buổi chiều' : 'Chào buổi tối';
  document.getElementById('welcomeGreeting').textContent = `${g}, ${name}!`;
  document.getElementById('welcomeSub').textContent      = isAdmin()
    ? 'Bạn đang đăng nhập với quyền Quản trị viên.'
    : `Mã sinh viên: ${s.studentId} · Chào mừng bạn đến với EduRoom.`;
}

/* ═══ Router ═══ */
const PAGE_TITLES = {
  dashboard: 'Dashboard', rooms: 'Phòng học nhóm', students: 'Quản lý sinh viên',
  book: 'Đặt phòng mới', mybookings: 'Lịch đặt của tôi', allbookings: 'Tất cả lịch đặt',
  profile: 'Thông tin cá nhân', changepw: 'Đổi mật khẩu'
};

function navigate(page) {
  /* kiểm soát quyền */
  if (['students', 'allbookings'].includes(page) && !isAdmin())   { toast('Chỉ admin mới truy cập được.', 'error'); return; }
  if (['book', 'mybookings', 'profile'].includes(page) && !isHocVien()) { toast('Chỉ học viên mới truy cập được.', 'error'); return; }

  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById('page-' + page)?.classList.add('active');
  document.querySelector(`.nav-item[data-page="${page}"]`)?.classList.add('active');
  document.getElementById('pageTitle').textContent = PAGE_TITLES[page] || page;
  loadPage(page);
}

function loadCurrentPage() {
  const a = document.querySelector('.page.active');
  if (a) loadPage(a.id.replace('page-', ''));
}

function loadPage(page) {
  switch (page) {
    case 'dashboard':   loadDashboard();   break;
    case 'rooms':       loadRooms();       break;
    case 'students':    loadStudents();    break;
    case 'book':        loadBookForm();    break;
    case 'mybookings':  loadMyBookings();  break;
    case 'allbookings': loadAllBookings(); break;
    case 'profile':     loadProfile();     break;
  }
}

/* ═══ Helpers ═══ */
const rColor = t => t === 'Phòng thường' ? '#43d477' : t === 'Phòng có máy chiếu' ? '#3b9eff' : t === 'Phòng họp seminar' ? '#f7a641' : '#6c63ff';
const rIcon  = t => t === 'Phòng thường' ? 'fas fa-door-open' : t === 'Phòng có máy chiếu' ? 'fas fa-video' : 'fas fa-chalkboard-teacher';

function fmtDT(iso) {
  try { return new Date(iso).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }); }
  catch { return iso || '--'; }
}
const emptyState = msg => `<div class="empty-state"><i class="fas fa-inbox"></i><p>${msg}</p></div>`;

function animNum(id, target) {
  const el = document.getElementById(id); if (!el) return;
  let c = 0; const step = Math.max(1, Math.ceil(target / 20));
  const t = setInterval(() => { c = Math.min(c + step, target); el.textContent = c; if (c >= target) clearInterval(t); }, 40);
}

/* ═══ Modal ═══ */
const openModal  = id => document.getElementById(id)?.classList.add('open');
const closeModal = id => document.getElementById(id)?.classList.remove('open');
document.addEventListener('click', e => { if (e.target.classList.contains('modal-overlay')) e.target.classList.remove('open'); });

/* ═══ Toast ═══ */
function toast(msg, type = 'info') {
  const c = document.getElementById('toastContainer');
  const t = document.createElement('div');
  const clr = { success: 'var(--green)', error: 'var(--red)', info: '#3b9eff' };
  const ico  = { success: 'fa-check-circle', error: 'fa-circle-xmark', info: 'fa-circle-info' };
  t.innerHTML = `<i class="fas ${ico[type] || ico.info}"></i><span>${msg}</span>`;
  t.style.cssText = `position:fixed;bottom:${20 + c.childElementCount * 68}px;right:24px;`
    + `background:var(--bg2);border:1px solid var(--border);border-left:4px solid ${clr[type] || clr.info};`
    + `color:var(--text);padding:14px 18px;border-radius:12px;display:flex;align-items:center;gap:10px;`
    + `box-shadow:0 8px 24px rgba(0,0,0,.5);z-index:9999;animation:slideUp .3s ease;max-width:380px;font-size:13px;transition:opacity .3s`;
  t.querySelector('i').style.color = clr[type] || clr.info;
  c.appendChild(t);
  setTimeout(() => { t.style.opacity = '0'; setTimeout(() => t.remove(), 300); }, 3800);
}

/* ═══ Dashboard ═══ */
async function loadDashboard() {
  const [rRes, sRes, bRes] = await Promise.all([
    apiFetch(`${API}/rooms`),
    apiFetch(`${API}/students`),
    isAdmin() ? apiFetch(`${API}/bookings`) : apiFetch(`${API}/bookings?studentId=${encodeURIComponent(getMyId())}`)
  ]);
  if (rRes.success) {
    allRooms = rRes.data;
    animNum('statTotalRooms', allRooms.length);
    animNum('statActiveRooms', allRooms.filter(r => r.status === 'Đang hoạt động').length);
    renderDashRooms(allRooms.filter(r => r.status === 'Đang hoạt động').slice(0, 5));
  }
  if (sRes.success) { allStudents = sRes.data; animNum('statStudents', allStudents.length); }
  if (bRes.success) { animNum('statBookings', bRes.data.length); renderDashBookings(bRes.data.slice(-5).reverse()); }
}

function renderDashRooms(rooms) {
  const el = document.getElementById('dashRooms');
  el.innerHTML = rooms.length ? rooms.map(r => `
    <div class="dash-room-item">
      <div class="dash-room-dot" style="background:${rColor(r.roomType)}"></div>
      <div><div class="dash-room-name">${r.roomName}</div><div class="dash-room-sub">${r.roomType} · Tầng ${r.floor}</div></div>
      <div class="dash-room-right">
        <span class="status-badge status-active"><i class="fas fa-circle" style="font-size:7px"></i> Active</span>
        <div style="font-size:11px;color:var(--text-muted);margin-top:3px">${r.maxCapacity} người</div>
      </div>
    </div>`).join('') : emptyState('Chưa có phòng nào');
}

function renderDashBookings(bks) {
  const el = document.getElementById('dashBookings');
  el.innerHTML = bks.length ? bks.map(b => `
    <div class="dash-booking-item">
      <div>
        <div style="font-weight:600;font-size:13px">${b.bookingId}</div>
        <div style="font-size:11px;color:var(--text-muted)">${isAdmin() ? 'SV: ' + b.studentId + ' · ' : ''}Phòng: ${b.roomId}</div>
      </div>
      <span class="status-badge ${b.status === 'Đã đặt' ? 'status-booked' : 'status-cancelled'}">${b.status}</span>
    </div>`).join('') : emptyState('Chưa có lịch đặt nào');
}

/* ═══ Rooms ═══ */
async function loadRooms() {
  const el = document.getElementById('roomsGrid');
  el.innerHTML = '<div class="loader big"></div>';
  const res = await apiFetch(`${API}/rooms`);
  if (!res.success) { el.innerHTML = emptyState(res.message); return; }
  allRooms = res.data; renderRooms(allRooms);
}

function renderRooms(rooms) {
  const el = document.getElementById('roomsGrid');
  if (!rooms.length) { el.innerHTML = emptyState('Không tìm thấy phòng nào'); return; }
  el.innerHTML = rooms.map(r => `
    <div class="room-card ${r.status !== 'Đang hoạt động' ? 'maintenance' : ''}" style="--room-color:${rColor(r.roomType)}">
      <div class="room-type-badge"><i class="${rIcon(r.roomType)}"></i> ${r.roomType}</div>
      <div class="room-name">${r.roomName}</div>
      <div class="room-id"># ${r.roomId}</div>
      <div class="room-stats">
        <div class="room-stat"><i class="fas fa-layer-group"></i> Tầng ${r.floor}</div>
        <div class="room-stat"><i class="fas fa-users"></i> ${r.maxCapacity} người</div>
      </div>
      <div class="room-footer">
        <span class="status-badge ${r.status === 'Đang hoạt động' ? 'status-active' : 'status-maintenance'}">
          <i class="fas fa-circle" style="font-size:7px"></i> ${r.status}</span>
        <span class="fee-badge">${r.feeDescription}</span>
      </div>
      <div style="margin-top:10px;display:flex;gap:6px">
        <button class="btn btn-sm btn-ghost" style="flex:1"
          onclick="showRoomSchedule('${r.roomId}','${r.roomName.replace(/'/g, "\\'")}')">
          <i class="fas fa-calendar-days"></i> Lịch trống
        </button>
        ${isAdmin() ? `<button class="btn btn-sm btn-ghost" title="Đổi trạng thái"
          onclick="openStatusModal('${r.roomId}','${r.status}')">
          <i class="fas fa-toggle-on"></i></button>` : ''}
      </div>
    </div>`).join('');
}

function filterRooms(type, search) {
  let f = allRooms;
  if (type)   f = f.filter(r => r.roomType === type);
  if (search) f = f.filter(r => r.roomName.toLowerCase().includes(search) || r.roomId.toLowerCase().includes(search));
  renderRooms(f);
}

async function showRoomSchedule(roomId, roomName) {
  document.getElementById('scheduleRoomName').textContent = `${roomName} (${roomId})`;
  const body = document.getElementById('scheduleBody');
  body.innerHTML = '<div class="loader"></div>';
  openModal('modalSchedule');
  const res = await apiFetch(`${API}/bookings?roomId=${encodeURIComponent(roomId)}`);
  const active = (res.success ? res.data : []).filter(b => b.status === 'Đã đặt');
  if (!active.length) {
    body.innerHTML = `<div class="empty-state"><i class="fas fa-calendar-check" style="color:var(--green)"></i>
      <p style="color:var(--green)">Phòng trống hoàn toàn! Bạn có thể đặt phòng này.</p></div>`; return;
  }
  body.innerHTML = `<p style="color:var(--text-muted);font-size:13px;margin-bottom:12px">
    <i class="fas fa-circle-exclamation" style="color:var(--orange)"></i> Đang có ${active.length} lịch đặt:
  </p>
  <table class="data-table" style="font-size:12px">
    <thead><tr><th>Bắt đầu</th><th>Kết thúc</th><th>Số người</th><th>Mục đích</th></tr></thead>
    <tbody>${active.map(b => `<tr>
      <td>${fmtDT(b.startTime)}</td><td>${fmtDT(b.endTime)}</td>
      <td style="text-align:center">${b.numberOfPeople}</td><td>${b.purpose}</td>
    </tr>`).join('')}</tbody>
  </table>`;
}

/* Admin: trạng thái + thêm phòng */
function openStatusModal(roomId, cur) {
  statusTargetRoom = roomId;
  document.getElementById('statusRoomId').textContent = roomId;
  document.getElementById('newRoomStatus').value = cur;
  openModal('modalRoomStatus');
}
async function confirmUpdateStatus() {
  const status = document.getElementById('newRoomStatus').value;
  const res = await apiFetch(`${API}/rooms/${statusTargetRoom}/status`, { method: 'PUT', body: JSON.stringify({ status }) });
  if (res.success) { toast('Cập nhật thành công!', 'success'); closeModal('modalRoomStatus'); loadRooms(); }
  else toast(res.message, 'error');
}
function openAddRoomModal() { document.getElementById('addRoomForm').reset(); openModal('modalAddRoom'); }
async function submitAddRoom(e) {
  e.preventDefault();
  const payload = {
    roomId: document.getElementById('rm_id').value.trim(),
    roomName: document.getElementById('rm_name').value.trim(),
    floor: parseInt(document.getElementById('rm_floor').value),
    maxCapacity: parseInt(document.getElementById('rm_capacity').value),
    roomType: document.getElementById('rm_type').value
  };
  const res = await apiFetch(`${API}/rooms`, { method: 'POST', body: JSON.stringify(payload) });
  if (res.success) { toast('Thêm phòng thành công!', 'success'); closeModal('modalAddRoom'); loadRooms(); }
  else toast(res.message, 'error');
}

/* ═══ Students (Admin) ═══ */
async function loadStudents() {
  const tbody = document.getElementById('studentsTbody');
  tbody.innerHTML = '<tr><td colspan="6"><div class="loader"></div></td></tr>';
  const res = await apiFetch(`${API}/students`);
  if (!res.success) { tbody.innerHTML = `<tr><td colspan="6" style="text-align:center">${res.message}</td></tr>`; return; }
  allStudents = res.data; renderStudents(allStudents);
}
function renderStudents(list) {
  const tbody = document.getElementById('studentsTbody');
  if (!list.length) { tbody.innerHTML = `<tr><td colspan="6">${emptyState('Chưa có sinh viên nào')}</td></tr>`; return; }
  tbody.innerHTML = list.map(s => `<tr>
    <td><strong>${s.studentId}</strong></td>
    <td>${s.fullName}</td>
    <td><span class="status-badge status-active">${s.className}</span></td>
    <td>${s.phone}</td>
    <td>${s.email}</td>
    <td class="actions">
      <button class="btn btn-sm btn-ghost" onclick="viewStudentBookings('${s.studentId}')">
        <i class="fas fa-calendar"></i> Lịch đặt</button>
    </td></tr>`).join('');
}
function filterStudents(q) {
  const f = q ? allStudents.filter(s =>
    s.studentId.toLowerCase().includes(q) || s.fullName.toLowerCase().includes(q) || s.className.toLowerCase().includes(q)
  ) : allStudents;
  renderStudents(f);
}
function viewStudentBookings(id) {
  navigate('allbookings');
  setTimeout(() => { const inp = document.getElementById('bookingSearch'); if (inp) { inp.value = id; filterAllBookings(); } }, 300);
}
function openAddStudentModal() { document.getElementById('addStudentForm').reset(); openModal('modalAddStudent'); }
async function submitAddStudent(e) {
  e.preventDefault();
  const payload = {
    studentId: document.getElementById('sv_id').value.trim(),
    fullName:  document.getElementById('sv_name').value.trim(),
    phone:     document.getElementById('sv_phone').value.trim(),
    email:     document.getElementById('sv_email').value.trim(),
    className: document.getElementById('sv_class').value.trim()
  };
  const res = await apiFetch(`${API}/students`, { method: 'POST', body: JSON.stringify(payload) });
  if (res.success) { toast('Thêm sinh viên thành công!', 'success'); closeModal('modalAddStudent'); loadStudents(); }
  else toast(res.message, 'error');
}

/* ═══ Profile – HOCVIEN cập nhật thông tin cá nhân ═══ */
async function loadProfile() {
  const res = await apiFetch(`${API}/students/${encodeURIComponent(getMyId())}`);
  if (!res.success) { toast(res.message, 'error'); return; }
  const s = res.data;
  document.getElementById('pf_id').value    = s.studentId;
  document.getElementById('pf_name').value  = s.fullName;
  document.getElementById('pf_phone').value = s.phone;
  document.getElementById('pf_email').value = s.email;
  document.getElementById('pf_class').value = s.className;
}
async function submitProfile(e) {
  e.preventDefault();
  const id = getMyId();
  const payload = {
    fullName:  document.getElementById('pf_name').value.trim(),
    phone:     document.getElementById('pf_phone').value.trim(),
    email:     document.getElementById('pf_email').value.trim(),
    className: document.getElementById('pf_class').value.trim()
  };
  const res = await apiFetch(`${API}/students/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(payload) });
  if (res.success) {
    const session = getSession(); session.displayName = payload.fullName;
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    document.getElementById('topbarName').textContent = payload.fullName;
    document.getElementById('menuName').textContent   = payload.fullName;
    toast('Cập nhật thông tin thành công!', 'success');
  } else toast(res.message, 'error');
}

/* ═══ Book Form – HOCVIEN ═══ */
async function loadBookForm() {
  const res = await apiFetch(`${API}/rooms?active=true`);
  const sel = document.getElementById('bk_roomId');
  if (res.success && res.data.length) {
    sel.innerHTML = '<option value="">-- Chọn phòng --</option>' +
      res.data.map(r => `<option value="${r.roomId}" data-fee="${r.feePerHour}">
        ${r.roomName} (${r.roomId}) – ${r.maxCapacity} người – ${r.feeDescription}</option>`).join('');
  }
  const inp = document.getElementById('bk_studentId'); if (inp) inp.value = getMyId();
  const now = new Date(); now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  const ns = now.toISOString().slice(0, 16);
  document.getElementById('bk_startTime').min = ns;
  document.getElementById('bk_endTime').min   = ns;
}
function updateFeePreview() {
  const sel = document.getElementById('bk_roomId');
  const sv  = document.getElementById('bk_startTime').value;
  const ev  = document.getElementById('bk_endTime').value;
  const fd  = document.getElementById('feePreview');
  if (!sel.value || !sv || !ev) { fd.classList.add('hidden'); return; }
  const fph = parseFloat(sel.selectedOptions[0]?.dataset.fee || 0);
  const s = new Date(sv), e = new Date(ev);
  if (e <= s) { fd.classList.add('hidden'); return; }
  const h = (e - s) / 3600000;
  document.getElementById('feeText').textContent =
    `Ước tính: ${h.toFixed(1)}h × ${fph.toLocaleString('vi-VN')}đ = ${(fph * h).toLocaleString('vi-VN')}đ`;
  fd.classList.remove('hidden');
}
async function submitBooking(e) {
  e.preventDefault();
  const btn = document.getElementById('btnSubmitBook');
  btn.disabled = true; btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang đặt...';
  const sv = document.getElementById('bk_startTime').value;
  const ev = document.getElementById('bk_endTime').value;
  const res = await apiFetch(`${API}/bookings`, { method: 'POST', body: JSON.stringify({
    studentId: getMyId(), roomId: document.getElementById('bk_roomId').value,
    startTime: sv + ':00', endTime: ev + ':00',
    numberOfPeople: parseInt(document.getElementById('bk_people').value),
    purpose: document.getElementById('bk_purpose').value.trim()
  })});
  btn.disabled = false; btn.innerHTML = '<i class="fas fa-calendar-plus"></i> Xác nhận đặt phòng';
  if (res.success) {
    toast(`Đặt phòng thành công! Mã: ${res.data.bookingId}`, 'success');
    document.getElementById('bookingForm').reset();
    document.getElementById('bk_studentId').value = getMyId();
    document.getElementById('feePreview').classList.add('hidden');
  } else toast(res.message, 'error');
}

/* ═══ My Bookings – HOCVIEN ═══ */
async function loadMyBookings() {
  const tbody = document.getElementById('myBookingsTbody');
  tbody.innerHTML = '<tr><td colspan="8"><div class="loader"></div></td></tr>';
  const res = await apiFetch(`${API}/bookings?studentId=${encodeURIComponent(getMyId())}`);
  if (!res.success) { tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:var(--red)">${res.message}</td></tr>`; return; }
  myBookings = res.data; renderMyBookings(myBookings);
}
function renderMyBookings(bks) {
  const tbody = document.getElementById('myBookingsTbody');
  if (!bks.length) { tbody.innerHTML = `<tr><td colspan="8">${emptyState('Bạn chưa có lịch đặt nào')}</td></tr>`; return; }
  tbody.innerHTML = bks.map(b => {
    const a = b.status === 'Đã đặt';
    return `<tr>
      <td><strong style="font-size:12px">${b.bookingId}</strong></td>
      <td><span style="color:var(--primary)">${b.roomId}</span></td>
      <td style="font-size:12px">${fmtDT(b.startTime)}<br><span style="color:var(--text-muted)">→ ${fmtDT(b.endTime)}</span></td>
      <td style="text-align:center">${b.numberOfPeople}</td>
      <td style="font-size:12px;max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${b.purpose}">${b.purpose}</td>
      <td>${b.fee > 0 ? b.fee.toLocaleString('vi-VN') + 'đ' : '<span style="color:var(--green)">Miễn phí</span>'}</td>
      <td><span class="status-badge ${a ? 'status-booked' : 'status-cancelled'}">${b.status}</span></td>
      <td class="actions">
        <button class="btn btn-sm btn-ghost" onclick="showBookingDetail('${b.bookingId}')"><i class="fas fa-eye"></i></button>
        ${a ? `<button class="btn btn-sm btn-danger" onclick="openCancelModal('${b.bookingId}')"><i class="fas fa-trash-can"></i></button>` : ''}
      </td></tr>`;
  }).join('');
}
function filterMyBookings() {
  const q  = (document.getElementById('myBookingSearch')?.value || '').toLowerCase();
  const st = document.querySelector('.btn-filter[data-mystatus].active')?.dataset.mystatus || '';
  let f = myBookings;
  if (st) f = f.filter(b => b.status === st);
  if (q)  f = f.filter(b => b.bookingId.toLowerCase().includes(q) || b.roomId.toLowerCase().includes(q) || b.purpose.toLowerCase().includes(q));
  renderMyBookings(f);
}

/* ═══ All Bookings – Admin ═══ */
async function loadAllBookings() {
  const tbody = document.getElementById('bookingsTbody');
  tbody.innerHTML = '<tr><td colspan="8"><div class="loader"></div></td></tr>';
  const res = await apiFetch(`${API}/bookings`);
  if (!res.success) { tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;color:var(--red)">${res.message}</td></tr>`; return; }
  allBookings = res.data; renderAllBookings(allBookings);
}
function renderAllBookings(bks) {
  const tbody = document.getElementById('bookingsTbody');
  if (!bks.length) { tbody.innerHTML = `<tr><td colspan="8">${emptyState('Chưa có lịch đặt nào')}</td></tr>`; return; }
  tbody.innerHTML = bks.map(b => {
    const a = b.status === 'Đã đặt';
    return `<tr>
      <td><strong style="font-size:12px">${b.bookingId}</strong></td>
      <td>${b.studentId}</td>
      <td><span style="color:var(--primary)">${b.roomId}</span></td>
      <td style="font-size:12px">${fmtDT(b.startTime)}<br><span style="color:var(--text-muted)">→ ${fmtDT(b.endTime)}</span></td>
      <td style="text-align:center">${b.numberOfPeople}</td>
      <td>${b.fee > 0 ? b.fee.toLocaleString('vi-VN') + 'đ' : '<span style="color:var(--green)">Miễn phí</span>'}</td>
      <td><span class="status-badge ${a ? 'status-booked' : 'status-cancelled'}">${b.status}</span></td>
      <td class="actions">
        <button class="btn btn-sm btn-ghost" onclick="showBookingDetail('${b.bookingId}')"><i class="fas fa-eye"></i></button>
        ${a ? `<button class="btn btn-sm btn-danger" onclick="openCancelModal('${b.bookingId}')"><i class="fas fa-trash-can"></i></button>` : ''}
      </td></tr>`;
  }).join('');
}
function filterAllBookings() {
  const q  = (document.getElementById('bookingSearch')?.value || '').toLowerCase();
  const st = document.querySelector('.btn-filter[data-status].active')?.dataset.status || '';
  let f = allBookings;
  if (st) f = f.filter(b => b.status === st);
  if (q)  f = f.filter(b => b.bookingId.toLowerCase().includes(q) || b.studentId.toLowerCase().includes(q) || b.roomId.toLowerCase().includes(q));
  renderAllBookings(f);
}

/* ═══ Cancel ═══ */
function openCancelModal(bookingId) {
  cancelTarget = bookingId;
  document.getElementById('cancelBookingId').textContent = bookingId;
  const row = document.getElementById('cancelStudentRow');
  const inp = document.getElementById('cancelStudentId');
  if (isHocVien()) { row.style.display = 'none'; inp.value = getMyId(); }
  else             { row.style.display = '';     inp.value = ''; }
  openModal('modalCancel');
}
async function confirmCancel() {
  const studentId = isHocVien() ? getMyId() : document.getElementById('cancelStudentId').value.trim();
  if (!studentId) { toast('Vui lòng nhập mã sinh viên.', 'error'); return; }
  const res = await apiFetch(`${API}/bookings/${cancelTarget}?studentId=${encodeURIComponent(studentId)}`, { method: 'DELETE' });
  if (res.success) { toast('Hủy lịch đặt thành công!', 'success'); closeModal('modalCancel'); isHocVien() ? loadMyBookings() : loadAllBookings(); }
  else toast(res.message, 'error');
}

/* ═══ Booking Detail ═══ */
async function showBookingDetail(bookingId) {
  const body = document.getElementById('modalDetailBody');
  body.innerHTML = '<div class="loader"></div>';
  openModal('modalDetail');
  const res = await apiFetch(`${API}/bookings/${bookingId}/detail`);
  if (!res.success) { body.innerHTML = `<p style="color:var(--red)">${res.message}</p>`; return; }
  const d = res.data; const a = d.status === 'Đã đặt';
  body.innerHTML = `<div class="detail-grid">
    <div class="detail-item full"><div class="detail-label">Mã lịch đặt</div><div class="detail-value" style="color:var(--primary)">${d.bookingId}</div></div>
    <div class="detail-item"><div class="detail-label">Sinh viên</div><div class="detail-value">${d.studentName}</div><div style="font-size:12px;color:var(--text-muted)">Mã: ${d.studentId} · Lớp: ${d.className}</div></div>
    <div class="detail-item"><div class="detail-label">Phòng</div><div class="detail-value">${d.roomName}</div><div style="font-size:12px;color:var(--text-muted)">Mã: ${d.roomId} · Tầng ${d.floor}</div></div>
    <div class="detail-item"><div class="detail-label">Loại phòng</div><div class="detail-value" style="color:${rColor(d.roomType)}">${d.roomType}</div></div>
    <div class="detail-item"><div class="detail-label">Trạng thái</div><span class="status-badge ${a ? 'status-booked' : 'status-cancelled'}">${d.status}</span></div>
    <div class="detail-item"><div class="detail-label">Bắt đầu</div><div class="detail-value">${fmtDT(d.startTime)}</div></div>
    <div class="detail-item"><div class="detail-label">Kết thúc</div><div class="detail-value">${fmtDT(d.endTime)}</div></div>
    <div class="detail-item"><div class="detail-label">Số người</div><div class="detail-value">${d.numberOfPeople} người</div></div>
    <div class="detail-item"><div class="detail-label">Phí</div><div class="detail-value" style="color:${d.fee > 0 ? 'var(--orange)' : 'var(--green)'}">${d.fee > 0 ? d.fee.toLocaleString('vi-VN') + 'đ' : 'Miễn phí'}</div></div>
    <div class="detail-item full"><div class="detail-label">Mục đích</div><div class="detail-value">${d.purpose}</div></div>
  </div>`;
}

/* ═══ Đổi mật khẩu ═══ */
async function submitChangePw(e) {
  e.preventDefault();
  const o = document.getElementById('cp_old').value;
  const n = document.getElementById('cp_new').value;
  const n2 = document.getElementById('cp_new2').value;
  if (n !== n2) { toast('Mật khẩu xác nhận không khớp.', 'error'); return; }
  if (n.length < 6) { toast('Mật khẩu mới phải có ít nhất 6 ký tự.', 'error'); return; }
  const res = await apiFetch(`${API}/auth/change-password`, { method: 'POST', body: JSON.stringify({ studentId: getMyId(), oldPassword: o, newPassword: n }) });
  if (res.success) { toast('Đổi mật khẩu thành công! Vui lòng đăng nhập lại.', 'success'); document.getElementById('changePwForm').reset(); setTimeout(logout, 2000); }
  else toast(res.message, 'error');
}

/* ═══ DOMContentLoaded ═══ */
document.addEventListener('DOMContentLoaded', () => {
  /* áp dụng UI theo role TRƯỚC KHI navigate */
  initRoleUI();

  /* sidebar nav */
  document.querySelectorAll('.nav-item[data-page]').forEach(el =>
    el.addEventListener('click', e => { e.preventDefault(); navigate(el.dataset.page); }));

  /* "xem tất cả" links trên dashboard */
  document.querySelectorAll('.link-more[data-page]').forEach(el =>
    el.addEventListener('click', e => { e.preventDefault(); navigate(el.dataset.page); }));

  /* mobile menu */
  document.getElementById('menuToggle').addEventListener('click', () =>
    document.getElementById('sidebar').classList.toggle('open'));

  /* room filter */
  document.querySelectorAll('.btn-filter[data-filter]').forEach(btn =>
    btn.addEventListener('click', () => {
      document.querySelectorAll('.btn-filter[data-filter]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      filterRooms(btn.dataset.filter, document.getElementById('roomSearch')?.value?.toLowerCase() || '');
    }));
  document.getElementById('roomSearch')?.addEventListener('input', e => {
    const f = document.querySelector('.btn-filter[data-filter].active')?.dataset.filter || '';
    filterRooms(f, e.target.value.toLowerCase());
  });

  /* student search */
  document.getElementById('studentSearch')?.addEventListener('input', e => filterStudents(e.target.value.toLowerCase()));

  /* all bookings filter */
  document.getElementById('bookingSearch')?.addEventListener('input', filterAllBookings);
  document.querySelectorAll('.btn-filter[data-status]').forEach(btn =>
    btn.addEventListener('click', () => {
      document.querySelectorAll('.btn-filter[data-status]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active'); filterAllBookings();
    }));

  /* my bookings filter */
  document.getElementById('myBookingSearch')?.addEventListener('input', filterMyBookings);
  document.querySelectorAll('.btn-filter[data-mystatus]').forEach(btn =>
    btn.addEventListener('click', () => {
      document.querySelectorAll('.btn-filter[data-mystatus]').forEach(b => b.classList.remove('active'));
      btn.classList.add('active'); filterMyBookings();
    }));

  /* fee preview */
  ['bk_roomId', 'bk_startTime', 'bk_endTime'].forEach(id =>
    document.getElementById(id)?.addEventListener('change', updateFeePreview));

  /* load trang mặc định */
  navigate('dashboard');
});
