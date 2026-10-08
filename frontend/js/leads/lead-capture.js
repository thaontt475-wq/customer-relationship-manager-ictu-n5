/**
 * Web-to-Lead Form Client Logic (S4-01 / CRM-113)
 * Author: Hoàng Thắng (Frontend)
 */

document.addEventListener('DOMContentLoaded', () => {
    initUtmTracking();
    initLeadFormValidation();
});

// Extract UTM Parameters from URL and store in hidden inputs
function initUtmTracking() {
    const urlParams = new URLSearchParams(window.location.search);
    const utmKeys = ['utm_source', 'utm_campaign', 'utm_medium', 'utm_term', 'utm_content'];

    let hasUtm = false;
    utmKeys.forEach(key => {
        const val = urlParams.get(key);
        const hiddenInput = document.getElementById(key);
        if (hiddenInput && val) {
            hiddenInput.value = val;
            hasUtm = true;
        }
    });

    // Update display badge
    const badgeSource = document.getElementById('utmDisplaySource');
    const badgeCampaign = document.getElementById('utmDisplayCampaign');
    if (badgeSource) {
        badgeSource.textContent = urlParams.get('utm_source') || 'Direct Website';
    }
    if (badgeCampaign) {
        badgeCampaign.textContent = urlParams.get('utm_campaign') || 'Organic 2026';
    }
}

// Client-side validation and submission
function initLeadFormValidation() {
    const form = document.getElementById('webToLeadForm');
    const submitBtn = document.getElementById('submitLeadBtn');
    const formWrap = document.getElementById('formSectionWrapper');
    const successBox = document.getElementById('successSectionBox');

    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // Clear previous error states
        document.querySelectorAll('.form-control').forEach(el => el.classList.remove('is-invalid'));

        const fullName = document.getElementById('leadFullName');
        const phone = document.getElementById('leadPhone');
        const email = document.getElementById('leadEmail');
        const company = document.getElementById('leadCompany');
        const interest = document.getElementById('leadInterest');
        const note = document.getElementById('leadNote');

        let isValid = true;

        // Validate Full Name
        if (!fullName.value.trim()) {
            markInvalid(fullName, 'Vui lòng nhập họ và tên của bạn');
            isValid = false;
        }

        // Validate Phone (VN Phone Regex)
        const vnPhoneRegex = /(03|05|07|08|09)+([0-9]{8})\b/;
        if (!phone.value.trim() || !vnPhoneRegex.test(phone.value.trim())) {
            markInvalid(phone, 'Vui lòng nhập số điện thoại Việt Nam hợp lệ (VD: 0912345678)');
            isValid = false;
        }

        // Validate Email
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!email.value.trim() || !emailRegex.test(email.value.trim())) {
            markInvalid(email, 'Vui lòng nhập định dạng email hợp lệ');
            isValid = false;
        }

        // Validate Company
        if (!company.value.trim()) {
            markInvalid(company, 'Vui lòng cung cấp tên công ty hoặc doanh nghiệp');
            isValid = false;
        }

        if (!isValid) return;

        // Anti-spam debounce & loading state
        submitBtn.disabled = true;
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<span>⏳ Đang gửi yêu cầu...</span>';

        const payload = {
            fullName: fullName.value.trim(),
            phone: phone.value.trim(),
            email: email.value.trim(),
            company: company.value.trim(),
            interest: interest ? interest.value : '',
            note: note ? note.value.trim() : '',
            utmSource: document.getElementById('utm_source')?.value || 'Website Direct',
            utmCampaign: document.getElementById('utm_campaign')?.value || 'Organic 2026',
            utmMedium: document.getElementById('utm_medium')?.value || 'Web Form'
        };

        try {
            // Simulated delay for realistic backend call (Toàn Backend API endpoint)
            await new Promise(resolve => setTimeout(resolve, 800));

            // Success feedback
            if (formWrap && successBox) {
                formWrap.style.display = 'none';
                successBox.style.display = 'block';
            }
        } catch (error) {
            alert('Có lỗi xảy ra trong quá trình gửi, vui lòng thử lại sau.');
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    });
}

function markInvalid(inputEl, msg) {
    inputEl.classList.add('is-invalid');
    const errorEl = inputEl.nextElementSibling;
    if (errorEl && errorEl.classList.contains('form-error')) {
        errorEl.textContent = msg;
    }
}
