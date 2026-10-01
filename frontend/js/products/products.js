    (function () {
        'use strict';

        var app = document.getElementById('productApp');
        var contextPath = app.dataset.contextPath;
        var canManageCost = app.dataset.canManageCost === 'true';

        // State quản lý của ứng dụng
        var state = {
            currentTab: 'products',
            products: [],
            priceBooks: [],
            keyword: '',
            filterType: '',
            filterActive: '',
            page: 1,
            size: 10,
            pendingDeactivateProduct: null
        };

        // DOM Elements
        var roleNoticeAlert = document.getElementById('roleNoticeAlert');
        var roleNoticeMessage = document.getElementById('roleNoticeMessage');
        var globalSuccessAlert = document.getElementById('globalSuccessAlert');
        var globalSuccessMessage = document.getElementById('globalSuccessMessage');
        var globalErrorAlert = document.getElementById('globalErrorAlert');
        var globalErrorMessage = document.getElementById('globalErrorMessage');

        var productTableBody = document.getElementById('productTableBody');
        var productEmptyState = document.getElementById('productEmptyState');
        var productPaginationInfo = document.getElementById('productPaginationInfo');
        var productPaginationControls = document.getElementById('productPaginationControls');
        var productLoadingOverlay = document.getElementById('productLoadingOverlay');

        var badgeProductsTabCount = document.getElementById('badgeProductsTabCount');
        var badgePriceBooksTabCount = document.getElementById('badgePriceBooksTabCount');

        var productSearchInput = document.getElementById('productSearchInput');
        var filterProductType = document.getElementById('filterProductType');
        var filterProductActive = document.getElementById('filterProductActive');
        var btnResetProductFilter = document.getElementById('btnResetProductFilter');

        // Modal Elements
        var productModal = document.getElementById('productModal');
        var productForm = document.getElementById('productForm');
        var productModalHeading = document.getElementById('productModalHeading');
        var btnOpenCreateProductModal = document.getElementById('btnOpenCreateProductModal');
        var btnCloseProductModal = document.getElementById('btnCloseProductModal');
        var btnCancelProductModal = document.getElementById('btnCancelProductModal');

        var formProductId = document.getElementById('formProductId');
        var formProductCode = document.getElementById('formProductCode');
        var formProductName = document.getElementById('formProductName');
        var formProductType = document.getElementById('formProductType');
        var formProductUnit = document.getElementById('formProductUnit');
        var formListPrice = document.getElementById('formListPrice');
        var formFloorPrice = document.getElementById('formFloorPrice');
        var formCostPrice = document.getElementById('formCostPrice');
        var formProductActive = document.getElementById('formProductActive');
        var formProductDescription = document.getElementById('formProductDescription');
        var groupCostPrice = document.getElementById('groupCostPrice');
        var noticeCostPriceHidden = document.getElementById('noticeCostPriceHidden');

        var deactivateConfirmModal = document.getElementById('deactivateConfirmModal');
        var deactivateModalContent = document.getElementById('deactivateModalContent');
        var btnCloseDeactivateModal = document.getElementById('btnCloseDeactivateModal');
        var btnCancelDeactivate = document.getElementById('btnCancelDeactivate');
        var btnConfirmDeactivate = document.getElementById('btnConfirmDeactivate');

        var priceBookModal = document.getElementById('priceBookModal');
        var priceBookForm = document.getElementById('priceBookForm');
        var btnOpenCreatePriceBookModal = document.getElementById('btnOpenCreatePriceBookModal');
        var btnClosePriceBookModal = document.getElementById('btnClosePriceBookModal');
        var btnCancelPriceBookModal = document.getElementById('btnCancelPriceBookModal');
        var priceBookGrid = document.getElementById('priceBookGrid');
        var priceBookLinesTbody = document.getElementById('priceBookLinesTbody');

        // Format tiền tệ VNĐ
        function formatVND(amount) {
            if (amount == null || isNaN(amount)) return '—';
            return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
        }

        function escapeHtml(str) {
            if (str == null) return '';
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        async function readResponse(response) {
            try { return await response.json(); }
            catch (error) { throw new Error('Máy chủ chưa trả về dữ liệu hợp lệ. Vui lòng thử lại sau.'); }
        }
        function errorMessage(error) {
            return error instanceof TypeError ? 'Không thể kết nối máy chủ. Vui lòng thử lại.' : error.message;
        }

        // Thông báo
        function showSuccessAlert(msg) {
            globalSuccessMessage.textContent = msg;
            globalSuccessAlert.style.display = 'flex';
            globalErrorAlert.style.display = 'none';
            setTimeout(function () {
                globalSuccessAlert.style.display = 'none';
            }, 4500);
        }

        function showErrorAlert(msg) {
            globalErrorMessage.textContent = msg;
            globalErrorAlert.style.display = 'flex';
            globalSuccessAlert.style.display = 'none';
        }

        function hideAlerts() {
            globalSuccessAlert.style.display = 'none';
            globalErrorAlert.style.display = 'none';
        }

        // Cập nhật thống kê nhanh
        function updateStats() {
            badgeProductsTabCount.textContent = state.total == null ? '—' : state.total;
            badgePriceBooksTabCount.textContent = state.priceBooksLoaded ? state.priceBooks.length : '—';
        }

        // Lọc sản phẩm
        function getFilteredProducts() {
            var kw = (state.keyword || '').trim().toLowerCase();
            return state.products.filter(function (p) {
                var matchKw = true;
                if (kw) {
                    var codeMatch = (p.code || '').toLowerCase().indexOf(kw) !== -1;
                    var nameMatch = (p.name || '').toLowerCase().indexOf(kw) !== -1;
                    matchKw = codeMatch || nameMatch;
                }
                var matchType = true;
                if (state.filterType) {
                    matchType = (p.type || p.category) === state.filterType;
                }
                var matchActive = true;
                if (state.filterActive !== '') {
                    var actBool = state.filterActive === 'true';
                    matchActive = p.active === actBool;
                }
                return matchKw && matchType && matchActive;
            });
        }

        // Render bảng sản phẩm
        function renderProductTable() {
            var filtered = getFilteredProducts();
            var isDirector = canManageCost;

            document.getElementById('thCostPrice').hidden = !isDirector;
            if (filtered.length === 0) {
                productTableBody.innerHTML = '';
                productEmptyState.style.display = 'block';
                productPaginationInfo.textContent = 'Hiển thị 0 trên 0 sản phẩm';
                productPaginationControls.innerHTML = '';
                return;
            }

            productEmptyState.style.display = 'none';
            productTableBody.innerHTML = '';

            filtered.forEach(function (p, index) {
                var tr = document.createElement('tr');
                if (!p.active) {
                    tr.className = 'row-inactive';
                }

                // Cột Loại hình
                var typeBadgeHtml = (p.type || p.category) === 'SUBSCRIPTION'
                    ? '<span class="badge-type badge-type-subscription"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M21.5 2v6h-6"></path><path d="M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"></path></svg> Thuê bao</span>'
                    : '<span class="badge-type badge-type-onetime"><svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg> Một lần</span>';

                if ((p.type || p.category) !== 'SUBSCRIPTION' && (p.type || p.category) !== 'ONE_TIME') {
                    typeBadgeHtml = '<span class="badge-type crm-badge">' + escapeHtml(p.type || p.category || 'Chưa phân loại') + '</span>';
                }

                // Cột Trạng thái
                var statusBadgeHtml = p.active
                    ? '<span class="badge-status badge-status-active"><span style="width:6px;height:6px;border-radius:50%;background:#10b981;display:inline-block;"></span> Đang sử dụng</span>'
                    : '<span class="badge-status badge-status-inactive"><span style="width:6px;height:6px;border-radius:50%;background:#94a3b8;display:inline-block;"></span> Ngừng sử dụng</span>';

                // Cột Giá vốn (AC 3: Phân quyền xem)
                var costPriceHtml = '';
                if (isDirector) {
                    costPriceHtml = '<span class="price-cost">' + formatVND(p.costPrice) + '</span>';
                } else {
                    costPriceHtml = '<span class="cost-masked" title="Chỉ Giám đốc kinh doanh có quyền xem giá vốn">••••••••</span>';
                }

                // Cột Báo giá
                var quoteCount = p.quoteCount;
                var quoteCountHtml = quoteCount > 0
                    ? '<span class="quote-count-badge" title="Đã có ' + quoteCount + ' báo giá sử dụng sản phẩm này">' + quoteCount + ' báo giá</span>'
                    : '<span class="quote-count-badge quote-count-zero">' + (quoteCount == null ? '—' : quoteCount) + '</span>';

                tr.innerHTML =
                    '<td><strong>' + (index + 1) + '</strong></td>' +
                    '<td><span class="col-code" title="' + escapeHtml(p.code) + '">' + escapeHtml(p.code) + '</span></td>' +
                    '<td>' +
                        '<div class="product-name-cell">' +
                            '<span class="product-title" title="' + escapeHtml(p.name) + '">' + escapeHtml(p.name) + '</span>' +
                            (p.description ? '<span class="product-desc-sub" title="' + escapeHtml(p.description) + '">' + escapeHtml(p.description) + '</span>' : '') +
                        '</div>' +
                    '</td>' +
                    '<td>' + typeBadgeHtml + '</td>' +
                    '<td><span style="color:#475569; font-weight:500;">' + escapeHtml(p.unit) + '</span></td>' +
                    '<td style="text-align: right;"><span class="price-value">' + formatVND(p.listPrice) + '</span></td>' +
                    '<td style="text-align: right;">' +
                        '<span class="price-value price-floor" title="Giá sàn tối thiểu để duyệt chiết khấu báo giá">' +
                            formatVND(p.floorPrice) +
                        '</span>' +
                        '<span class="price-floor-hint">Ngưỡng tối thiểu</span>' +
                    '</td>' +
                    (isDirector ? '<td style="text-align: right;">' + costPriceHtml + '</td>' : '') +
                    '<td style="text-align: center;">' + quoteCountHtml + '</td>' +
                    '<td style="text-align: center;">' + statusBadgeHtml + '</td>' +
                    '<td>' +
                        '<div class="action-cell" style="justify-content: center;">' +
                            '<button type="button" class="btn-icon" data-action="edit" data-id="' + p.id + '" aria-label="Sửa sản phẩm" title="Chỉnh sửa sản phẩm">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>' +
                                    '<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>' +
                                '</svg>' +
                            '</button>' +
                            '<button type="button" class="btn-icon" data-action="toggle-active" data-id="' + p.id + '" title="' + (p.active ? 'Chuyển sang Ngừng sử dụng' : 'Kích hoạt lại kinh doanh') + '">' +
                                (p.active
                                    ? '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>'
                                    : '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>'
                                ) +
                            '</button>' +
                            '<button type="button" class="btn-icon btn-icon-danger" data-action="delete" data-id="' + p.id + '" aria-label="Xóa sản phẩm" title="Xóa hoặc Ngừng sử dụng">' +
                                '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
                                    '<polyline points="3 6 5 6 21 6"></polyline>' +
                                    '<path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>' +
                                '</svg>' +
                            '</button>' +
                        '</div>' +
                    '</td>';

                productTableBody.appendChild(tr);
            });

            productPaginationInfo.textContent = 'Hiển thị ' + filtered.length + ' sản phẩm trong trang ' + state.page + ' / ' + (state.totalPages || 1) + ' · Tổng ' + state.total + ' sản phẩm';
            productPaginationControls.innerHTML = '<button type="button" class="crm-btn crm-btn-secondary" data-page="' + (state.page - 1) + '"' + (state.page <= 1 ? ' disabled' : '') + '>Trước</button><span>' + state.page + '</span><button type="button" class="crm-btn crm-btn-secondary" data-page="' + (state.page + 1) + '"' + (state.page >= (state.totalPages || 1) ? ' disabled' : '') + '>Sau</button>';
        }

        productPaginationControls.addEventListener('click', function(event) {
            var button = event.target.closest('button[data-page]');
            if (button && !button.disabled) { state.page = Number(button.dataset.page); fetchProducts(); }
        });

        // Render Bảng giá niêm yết
        function renderPriceBooks() {
            priceBookGrid.innerHTML = '';
            if (state.priceBooks.length === 0) {
                priceBookGrid.innerHTML = '<div style="grid-column: 1/-1; padding: 40px; text-align: center; color: #64748b;">Chưa có bảng giá niêm yết nào được tạo.</div>';
                return;
            }

            state.priceBooks.forEach(function (pb) {
                var card = document.createElement('div');
                card.className = 'pricebook-card';
                card.innerHTML =
                    '<div class="pricebook-card-header">' +
                        '<div>' +
                            '<h4 class="pricebook-title">' + escapeHtml(pb.name) + '</h4>' +
                            '<span class="pricebook-date">' +
                                '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>' +
                                'Hiệu lực từ: ' + escapeHtml(pb.effectiveFrom) +
                            '</span>' +
                        '</div>' +
                        '<span class="badge-status badge-status-active">Bảng giá</span>' +
                    '</div>' +
                    '<div class="pricebook-card-body">' +
                        '<p class="pricebook-desc">' + escapeHtml(pb.description || 'Chưa có mô tả') + '</p>' +
                        '<ul class="pricebook-meta-list">' +
                            '<li class="pricebook-meta-item">' +
                                '<span>Số lượng mặt hàng:</span>' +
                                '<strong>' + (Array.isArray(pb.lines) ? pb.lines.length : (pb.itemCount == null ? '—' : pb.itemCount)) + ' sản phẩm</strong>' +
                            '</li>' +
                            '<li class="pricebook-meta-item">' +
                                '<span>Trạng thái:</span>' +
                                '<span>' + (pb.active ? 'Đang sử dụng' : 'Ngừng sử dụng') + '</span>' +
                            '</li>' +
                        '</ul>' +
                    '</div>' +
                    '<div class="pricebook-card-footer">' +
                    '</div>';
                priceBookGrid.appendChild(card);
            });
        }

        // Tải danh sách sản phẩm từ backend (GET /api/products)
        var productsRequest = 0;
        async function fetchProducts() {
            var requestId = ++productsRequest;
            productLoadingOverlay.style.display = 'flex';
            var queryParams = new URLSearchParams({keyword: state.keyword, active: state.filterActive, page: state.page, size: state.size});
            try {
                var response = await fetch(contextPath + '/api/products?' + queryParams, {headers: {'Accept': 'application/json'}});
                var json = await readResponse(response);
                if (requestId !== productsRequest) return;
                if (!response.ok || json.success === false) throw new Error(json.message || 'Không thể tải danh sách sản phẩm.');
                var data = json.data || json;
                var list = Array.isArray(data.items) ? data.items : (Array.isArray(data) ? data : null);
                if (!list) throw new Error('Phản hồi danh sách sản phẩm không hợp lệ.');
                state.products = list;
                state.total = Number.isFinite(data.total) ? data.total : list.length;
                state.page = data.page || state.page;
                state.totalPages = data.totalPages || 1;
                renderProductTable();
            } catch (err) {
                if (requestId !== productsRequest) return;
                state.products = [];
                state.total = null;
                productTableBody.innerHTML = '';
                productEmptyState.style.display = 'none';
                productPaginationInfo.textContent = 'Không thể tải dữ liệu';
                productPaginationControls.innerHTML = '';
                showErrorAlert(errorMessage(err) || 'Không thể kết nối máy chủ. Vui lòng thử lại.');
            } finally {
                if (requestId !== productsRequest) return;
                productLoadingOverlay.style.display = 'none';
                updateStats();
            }
        }

        // Tải danh sách bảng giá (GET /api/price-books)
        async function fetchPriceBooks() {
            priceBookGrid.textContent = 'Đang tải bảng giá…';
            try {
                var res = await fetch(contextPath + '/api/price-books', {headers: {'Accept': 'application/json'}});
                if (!res.ok) throw new Error('Không thể tải bảng giá. Vui lòng thử lại sau.');
                var json = await readResponse(res);
                if (json.success === false) throw new Error(json.message || 'Không thể tải bảng giá.');
                var data = json.data || json;
                var list = Array.isArray(data) ? data : data.items;
                if (!Array.isArray(list)) throw new Error('Phản hồi bảng giá không hợp lệ.');
                state.priceBooks = list;
                state.priceBooksLoaded = true;
                renderPriceBooks();
            } catch (err) {
                state.priceBooks = [];
                state.priceBooksLoaded = false;
                priceBookGrid.innerHTML = '<div class="crm-empty-state" role="status"><h2 class="crm-card-title">Chưa tải được bảng giá</h2><p>' + escapeHtml(errorMessage(err)) + '</p><button type="button" class="crm-btn crm-btn-secondary" id="btnRetryPriceBooks">Thử lại</button></div>';
            }
            updateStats();
        }
        priceBookGrid.addEventListener('click', function (event) {
            if (event.target.closest('#btnRetryPriceBooks')) fetchPriceBooks();
        });

        // Chuyển đổi Tab
        window.switchTab = function (tabName) {
            state.currentTab = tabName;
            var tabBtnProducts = document.getElementById('tabBtnProducts');
            var tabBtnPriceBooks = document.getElementById('tabBtnPriceBooks');
            var tabContentProducts = document.getElementById('tabContentProducts');
            var tabContentPriceBooks = document.getElementById('tabContentPriceBooks');

            if (tabName === 'products') {
                tabBtnProducts.classList.add('active');
                tabBtnPriceBooks.classList.remove('active');
                tabContentProducts.style.display = 'block';
                tabContentPriceBooks.style.display = 'none';
            } else {
                tabBtnProducts.classList.remove('active');
                tabBtnPriceBooks.classList.add('active');
                tabContentProducts.style.display = 'none';
                tabContentPriceBooks.style.display = 'block';
                fetchPriceBooks();
            }
        };

        // Tìm kiếm & Bộ lọc
        productSearchInput.addEventListener('input', function () {
            state.keyword = productSearchInput.value.trim();
            state.page = 1;
            fetchProducts();
        });

        filterProductType.addEventListener('change', function () {
            state.filterType = filterProductType.value;
            state.page = 1;
            fetchProducts();
        });

        filterProductActive.addEventListener('change', function () {
            state.filterActive = filterProductActive.value;
            state.page = 1;
            fetchProducts();
        });

        btnResetProductFilter.addEventListener('click', function () {
            productSearchInput.value = '';
            filterProductType.value = '';
            filterProductActive.value = '';
            state.keyword = '';
            state.filterType = '';
            state.filterActive = '';
            state.page = 1;
            fetchProducts();
        });

        // Mở Modal Thêm mới sản phẩm
        btnOpenCreateProductModal.addEventListener('click', function () {
            hideAlerts();
            productForm.reset();
            formProductId.value = '';
            productModalHeading.textContent = 'Thêm sản phẩm mới';

            // Xử lý quyền xem/sửa giá vốn
            var isDirector = canManageCost;
            if (isDirector) {
                groupCostPrice.style.display = 'flex';
                formCostPrice.disabled = false;
                noticeCostPriceHidden.style.display = 'none';
            } else {
                groupCostPrice.style.display = 'none';
                formCostPrice.disabled = true;
                noticeCostPriceHidden.style.display = 'flex';
            }

            clearFormErrors();
            productModal.style.display = 'flex';
        });

        // Đóng Modal Sản phẩm
        function closeProductModal() {
            productModal.style.display = 'none';
            clearFormErrors();
        }

        btnCloseProductModal.addEventListener('click', closeProductModal);
        btnCancelProductModal.addEventListener('click', closeProductModal);

        // Mở Modal Sửa sản phẩm
        function openEditProductModal(id) {
            var p = state.products.find(function (item) { return item.id === Number(id); });
            if (!p) return;

            hideAlerts();
            clearFormErrors();
            formProductId.value = p.id;
            formProductCode.value = p.code;
            formProductName.value = p.name;
            formProductType.value = p.type || p.category || '';
            formProductUnit.value = p.unit;
            formListPrice.value = p.listPrice;
            formFloorPrice.value = p.floorPrice;
            formProductActive.value = p.active ? 'true' : 'false';
            formProductDescription.value = p.description || '';

            // AC 3: Kiểm tra quyền xem/sửa giá vốn
            var isDirector = canManageCost;
            if (isDirector) {
                groupCostPrice.style.display = 'flex';
                formCostPrice.disabled = false;
                formCostPrice.value = p.costPrice != null ? p.costPrice : '';
                noticeCostPriceHidden.style.display = 'none';
            } else {
                groupCostPrice.style.display = 'none';
                formCostPrice.disabled = true;
                formCostPrice.value = '';
                noticeCostPriceHidden.style.display = 'flex';
            }

            productModalHeading.textContent = 'Chỉnh sửa sản phẩm: ' + p.code;
            productModal.style.display = 'flex';
        }

        function clearFormErrors() {
            document.getElementById('productFormError').textContent = '';
            document.querySelectorAll('.form-feedback').forEach(function (el) { el.textContent = ''; });
            document.querySelectorAll('.form-control').forEach(function (el) { el.classList.remove('is-invalid'); });
        }

        // Validate Form Sản phẩm (AC 1, AC 2, AC 3)
        function validateProductForm() {
            clearFormErrors();
            var valid = true;

            var code = formProductCode.value.trim();
            if (!code) {
                document.getElementById('feedbackProductCode').textContent = 'Vui lòng nhập mã sản phẩm.';
                formProductCode.classList.add('is-invalid');
                valid = false;
            }

            var name = formProductName.value.trim();
            if (!name) {
                document.getElementById('feedbackProductName').textContent = 'Vui lòng nhập tên sản phẩm.';
                formProductName.classList.add('is-invalid');
                valid = false;
            }

            var unit = formProductUnit.value.trim();
            if (!unit) {
                document.getElementById('feedbackProductUnit').textContent = 'Vui lòng nhập đơn vị tính.';
                formProductUnit.classList.add('is-invalid');
                valid = false;
            }

            var listPrice = parseFloat(formListPrice.value);
            if (isNaN(listPrice) || listPrice <= 0) {
                document.getElementById('feedbackListPrice').textContent = 'Giá niêm yết phải lớn hơn 0.';
                formListPrice.classList.add('is-invalid');
                valid = false;
            }

            var floorPrice = parseFloat(formFloorPrice.value);
            if (isNaN(floorPrice) || floorPrice <= 0) {
                document.getElementById('feedbackFloorPrice').textContent = 'Giá sàn phải lớn hơn 0.';
                formFloorPrice.classList.add('is-invalid');
                valid = false;
            }

            // AC 2: Giá sàn không được vượt quá Giá niêm yết
            if (!isNaN(listPrice) && !isNaN(floorPrice) && floorPrice > listPrice) {
                document.getElementById('feedbackFloorPrice').textContent = 'Giá sàn (' + formatVND(floorPrice) + ') không được lớn hơn Giá niêm yết (' + formatVND(listPrice) + '). Đây là ngưỡng tối thiểu duyệt chiết khấu!';
                formFloorPrice.classList.add('is-invalid');
                valid = false;
            }

            // AC 3: Nếu là Giám đốc kinh doanh thì kiểm tra giá vốn
            if (canManageCost) {
                var costPrice = parseFloat(formCostPrice.value);
                if (isNaN(costPrice) || costPrice < 0) {
                    document.getElementById('feedbackCostPrice').textContent = 'Giá vốn không được để trống và phải >= 0.';
                    formCostPrice.classList.add('is-invalid');
                    valid = false;
                }
            }

            return valid;
        }

        // Xử lý Submit Form Sản phẩm (POST/PUT /api/products)
        productForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            if (!validateProductForm()) return;

            var id = formProductId.value;
            var isEdit = Boolean(id);

            var payload = {
                code: formProductCode.value.trim().toUpperCase(),
                name: formProductName.value.trim(),
                type: formProductType.value,
                unit: formProductUnit.value.trim(),
                listPrice: parseFloat(formListPrice.value),
                floorPrice: parseFloat(formFloorPrice.value),
                active: formProductActive.value === 'true',
                description: formProductDescription.value.trim()
            };

            if (canManageCost) {
                payload.costPrice = parseFloat(formCostPrice.value);
            }

            var endpoint = isEdit ? (contextPath + '/api/products/' + id) : (contextPath + '/api/products');
            var method = isEdit ? 'PUT' : 'POST';
            var saveButton = document.getElementById('btnSaveProduct');
            saveButton.disabled = true;
            try {
                var res = await fetch(endpoint, {method: method, headers: {'Content-Type': 'application/json', 'Accept': 'application/json'}, body: JSON.stringify(payload)});
                var responseBody = await readResponse(res);
                if (!res.ok || responseBody.success === false) throw new Error(responseBody.message || 'Không thể lưu sản phẩm.');
                closeProductModal();
                await fetchProducts();
                showSuccessAlert(isEdit ? 'Đã cập nhật sản phẩm.' : 'Đã thêm sản phẩm.');
            } catch (err) {
                document.getElementById('productFormError').textContent = errorMessage(err) || 'Không thể kết nối máy chủ để lưu sản phẩm.';
            } finally {
                saveButton.disabled = false;
            }
        });

        // Thao tác dòng: Sửa, Đổi trạng thái, Xóa/Ngừng sử dụng
        productTableBody.addEventListener('click', function (e) {
            var btn = e.target.closest('button[data-action]');
            if (!btn) return;

            var action = btn.getAttribute('data-action');
            var id = Number(btn.getAttribute('data-id'));

            if (action === 'edit') {
                openEditProductModal(id);
            } else if (action === 'toggle-active') {
                toggleProductActive(id);
            } else if (action === 'delete') {
                handleDeleteProduct(id);
            }
        });

        // Đổi trạng thái nhanh Active / Inactive
        async function toggleProductActive(id) {
            var p = state.products.find(function (item) { return item.id === id; });
            if (!p) return;

            var newStatus = !p.active;
            var endpoint = contextPath + '/api/products/' + id;

            try {
                var response = await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify({ active: newStatus })
                });

                var mutationResult = await readResponse(response);
                if (!response.ok || mutationResult.success === false) {
                    showErrorAlert('Không thể đổi trạng thái sản phẩm trên máy chủ (Mã lỗi: ' + response.status + '). Trạng thái chưa thay đổi.');
                    return;
                }
            } catch (e) {
                console.error('Lỗi khi đổi trạng thái sản phẩm:', e);
                showErrorAlert('Không thể kết nối máy chủ để đổi trạng thái sản phẩm. Trạng thái chưa thay đổi.');
                return;
            }

            p.active = newStatus;
            showSuccessAlert('Đã chuyển trạng thái sản phẩm [' + p.code + '] sang: ' + (newStatus ? 'Đang sử dụng' : 'Ngừng sử dụng'));
            updateStats();
            renderProductTable();
        }

        // Xử lý Xóa / Ngừng sử dụng (AC 4: Sản phẩm đã có trong báo giá không xóa được)
        function handleDeleteProduct(id) {
            var p = state.products.find(function (item) { return item.id === id; });
            if (!p) return;

            // AC 4: Nếu sản phẩm đã xuất hiện trong báo giá thì KHÔNG ĐƯỢC XÓA, chỉ được phép ngừng sử dụng
            if (p.quoteCount && p.quoteCount > 0) {
                state.pendingDeactivateProduct = p;
                deactivateModalContent.innerHTML =
                    'Sản phẩm <strong>[' + escapeHtml(p.code) + ' - ' + escapeHtml(p.name) + ']</strong> đã xuất hiện trong <strong>' +
                    p.quoteCount + ' báo giá</strong>. Để bảo vệ dữ liệu, hệ thống <span style="color:#ef4444;font-weight:700;">không cho phép xóa bỏ hoàn toàn</span> để bảo đảm tính toàn vẹn dữ liệu hợp đồng/báo giá lịch sử.';
                deactivateConfirmModal.style.display = 'flex';
                return;
            }

            // Nếu chưa có báo giá nào, cho phép xóa hoặc xác nhận
            if (confirm('Bạn có chắc chắn muốn xóa sản phẩm [' + p.code + '] không?')) {
                deleteProductDirectly(id);
            }
        }

        async function deleteProductDirectly(id) {
            var endpoint = contextPath + '/api/products/' + id;
            try {
                var response = await fetch(endpoint, {
                    method: 'DELETE',
                    headers: { 'Accept': 'application/json' }
                });

                var mutationResult = await readResponse(response);
                if (!response.ok || mutationResult.success === false) {
                    showErrorAlert('Không thể xóa sản phẩm trên máy chủ (Mã lỗi: ' + response.status + '). Dữ liệu vẫn được giữ nguyên.');
                    return;
                }
            } catch (e) {
                console.error('Lỗi khi xóa sản phẩm:', e);
                showErrorAlert('Không thể kết nối máy chủ để xóa sản phẩm. Dữ liệu vẫn được giữ nguyên.');
                return;
            }
            state.products = state.products.filter(function (p) { return p.id !== id; });
            showSuccessAlert('Đã xóa sản phẩm thành công!');
            updateStats();
            renderProductTable();
        }

        // Xác nhận chuyển sang Ngừng sử dụng
        btnConfirmDeactivate.addEventListener('click', async function () {
            if (!state.pendingDeactivateProduct) return;
            var p = state.pendingDeactivateProduct;

            var endpoint = contextPath + '/api/products/' + p.id;
            try {
                var response = await fetch(endpoint, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify({ active: false })
                });

                var mutationResult = await readResponse(response);
                if (!response.ok || mutationResult.success === false) {
                    showErrorAlert('Không thể ngừng sử dụng sản phẩm trên máy chủ (Mã lỗi: ' + response.status + '). Trạng thái chưa thay đổi.');
                    return;
                }
            } catch (e) {
                console.error('Lỗi khi ngừng sử dụng sản phẩm:', e);
                showErrorAlert('Không thể kết nối máy chủ để ngừng sử dụng sản phẩm. Trạng thái chưa thay đổi.');
                return;
            }

            p.active = false;
            deactivateConfirmModal.style.display = 'none';
            state.pendingDeactivateProduct = null;
            showSuccessAlert('Đã chuyển sản phẩm [' + p.code + '] sang trạng thái [Ngừng sử dụng]!');
            updateStats();
            state.page = 1;
            fetchProducts();
        });

        btnCloseDeactivateModal.addEventListener('click', function () {
            deactivateConfirmModal.style.display = 'none';
            state.pendingDeactivateProduct = null;
        });
        btnCancelDeactivate.addEventListener('click', function () {
            deactivateConfirmModal.style.display = 'none';
            state.pendingDeactivateProduct = null;
        });

        // Modal Tạo Bảng giá mới
        btnOpenCreatePriceBookModal.addEventListener('click', function () {
            priceBookForm.reset();
            document.getElementById('priceBookFormError').textContent = '';
            var today = new Date().toISOString().substring(0, 10);
            document.getElementById('formPriceBookEffectiveFrom').value = today;

            // Render danh sách sản phẩm đang kinh doanh để chọn vào bảng giá
            priceBookLinesTbody.innerHTML = '';
            var activeProducts = state.products.filter(function (p) { return p.active; });
            activeProducts.forEach(function (p) {
                var row = document.createElement('tr');
                row.innerHTML =
                    '<td><input type="checkbox" class="pb-check-item" data-code="' + escapeHtml(p.code) + '" checked></td>' +
                    '<td><span class="col-code">' + escapeHtml(p.code) + '</span></td>' +
                    '<td>' + escapeHtml(p.name) + '</td>' +
                    '<td style="text-align: right;"><input type="number" class="form-control crm-input pb-price-input" aria-label="Đơn giá áp dụng" style="width: 140px; display: inline-block; text-align: right;" value="' + p.listPrice + '" min="0" step="1000"></td>';
                priceBookLinesTbody.appendChild(row);
            });

            priceBookModal.style.display = 'flex';
        });

        function closePriceBookModal() {
            priceBookModal.style.display = 'none';
        }
        btnClosePriceBookModal.addEventListener('click', closePriceBookModal);
        btnCancelPriceBookModal.addEventListener('click', closePriceBookModal);

        // Submit Tạo Bảng giá (POST /api/price-books)
        priceBookForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            var name = document.getElementById('formPriceBookName').value.trim();
            var effectiveFrom = document.getElementById('formPriceBookEffectiveFrom').value;
            var description = document.getElementById('formPriceBookDescription').value.trim();

            if (!name) {
                document.getElementById('feedbackPriceBookName').textContent = 'Vui lòng nhập tên bảng giá.';
                return;
            }
            if (!effectiveFrom) {
                document.getElementById('feedbackPriceBookEffective').textContent = 'Vui lòng chọn ngày có hiệu lực.';
                return;
            }

            var lines = [];
            document.querySelectorAll('.pb-check-item:checked').forEach(function (chk) {
                var row = chk.closest('tr');
                var code = chk.getAttribute('data-code');
                var priceInput = row.querySelector('.pb-price-input');
                var price = parseFloat(priceInput.value) || 0;
                lines.push({ productCode: code, price: price });
            });

            if (lines.length === 0) {
                document.getElementById('priceBookFormError').textContent = 'Vui lòng chọn ít nhất một sản phẩm vào bảng giá.';
                return;
            }

            var payload = {
                name: name,
                effectiveFrom: effectiveFrom,
                description: description,
                lines: lines
            };

            var endpoint = contextPath + '/api/price-books';
            var saveButton = document.getElementById('btnSavePriceBook');
            saveButton.disabled = true;
            try {
                var response = await fetch(endpoint, {method:'POST', headers:{'Content-Type':'application/json','Accept':'application/json'}, body:JSON.stringify(payload)});
                var result = await readResponse(response);
                if (!response.ok || result.success === false) throw new Error(result.message || 'Không thể lưu bảng giá.');
                closePriceBookModal();
                await fetchPriceBooks();
                showSuccessAlert('Đã tạo bảng giá.');
            } catch (err) {
                document.getElementById('priceBookFormError').textContent = errorMessage(err) || 'Không thể kết nối máy chủ để lưu bảng giá.';
            } finally {
                saveButton.disabled = false;
            }
        });

        // Check All items in Price Book modal
        var checkAllPb = document.getElementById('checkAllPriceBookLines');
        if (checkAllPb) {
            checkAllPb.addEventListener('change', function () {
                var checked = checkAllPb.checked;
                document.querySelectorAll('.pb-check-item').forEach(function (chk) {
                    chk.checked = checked;
                });
            });
        }

        // Dialog keyboard/focus handling is presentation-only.
        var dialogs = [productModal, priceBookModal, deactivateConfirmModal];
        var activeDialog = null;
        var trigger = null;
        var inertState = [];
        function synchronizeDialogs() {
            var next = dialogs.find(function (dialog) { return dialog.style.display === 'flex'; });
            dialogs.forEach(function (dialog) { dialog.setAttribute('aria-hidden', dialog === next ? 'false' : 'true'); });
            if (next === activeDialog) return;
            if (next) {
                if (!activeDialog) {
                    trigger = document.activeElement;
                    inertState = Array.from(document.querySelectorAll('.crm-header, .crm-main-layout')).map(function (node) {
                        var item = {node: node, inert: node.inert}; node.inert = true; return item;
                    });
                    document.body.style.overflow = 'hidden';
                }
                activeDialog = next;
                var first = next.querySelector('input:not([type="hidden"]):not(:disabled), button:not(:disabled)');
                if (first) first.focus();
            } else {
                activeDialog = null;
                inertState.forEach(function (item) { item.node.inert = item.inert; });
                document.body.style.overflow = '';
                if (trigger && trigger.isConnected) trigger.focus();
            }
        }
        dialogs.forEach(function (dialog) {
            new MutationObserver(synchronizeDialogs).observe(dialog, {attributes: true, attributeFilter: ['style']});
        });
        document.addEventListener('keydown', function (event) {
            if (!activeDialog) return;
            if (event.key === 'Escape' && !activeDialog.querySelector('button[type="submit"]:disabled')) {
                activeDialog.querySelector('.crm-modal-close').click();
                event.preventDefault();
            }
            if (event.key === 'Tab') {
                var focusable = Array.from(activeDialog.querySelectorAll('button:not(:disabled), input:not([type="hidden"]):not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex="0"]')).filter(function (node) { return node.getClientRects().length; });
                var first = focusable[0], last = focusable[focusable.length - 1];
                if (event.shiftKey && document.activeElement === first) { last.focus(); event.preventDefault(); }
                else if (!event.shiftKey && document.activeElement === last) { first.focus(); event.preventDefault(); }
            }
        });
        synchronizeDialogs();
        // Khởi động
        fetchProducts();
        fetchPriceBooks();

    })();
