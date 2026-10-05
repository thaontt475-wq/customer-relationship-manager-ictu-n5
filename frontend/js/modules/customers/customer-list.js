document.addEventListener("DOMContentLoaded", () => {
    loadCustomers();
});

async function loadCustomers() {

    try {

        const customers =
            await apiRequest("/customers");

        console.log("Customers:", customers);

        renderCustomers(customers);

    } catch (error) {

        console.error(
            "Lỗi lấy danh sách khách hàng:",
            error
        );

    }
}

function renderCustomers(customers) {

    const tbody =
        document.getElementById("customerTableBody");

    if (!tbody) {
        return;
    }

    tbody.innerHTML = "";

    customers.forEach(customer => {

        const row =
            document.createElement("tr");

        row.innerHTML = `
            <td>${customer.id}</td>
            <td>${customer.name}</td>
            <td>${customer.phone || ""}</td>
            <td>${customer.email || ""}</td>
        `;

        tbody.appendChild(row);

    });
}