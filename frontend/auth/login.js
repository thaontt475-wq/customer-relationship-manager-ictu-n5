document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();

  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  const errorDiv = document.getElementById('errorMessage');
  const btnLogin = document.getElementById('btnLogin');

  errorDiv.style.display = 'none';
  btnLogin.disabled = true;
  btnLogin.innerText = 'Đang xử lý...';

  try {
    const response = await fetch(API.AUTH.LOGIN, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, password })
    });

    const result = await response.json();

    if (response.ok && result.success) {
      localStorage.setItem('user', JSON.stringify(result.data));
      window.location.href = '/dashboard/index.html';
    } else {
      errorDiv.innerText = result.message || 'Email hoặc mật khẩu không đúng!';
      errorDiv.style.display = 'block';
    }
  } catch (err) {
    console.error('Lỗi kết nối API:', err);
    errorDiv.innerText = 'Không thể kết nối tới Server. Vui lòng kiểm tra lại Backend!';
    errorDiv.style.display = 'block';
  } finally {
    btnLogin.disabled = false;
    btnLogin.innerText = 'Đăng nhập';
  }
});