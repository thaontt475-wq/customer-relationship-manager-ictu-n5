package com.crm.service.customers;

import com.crm.dto.customers.CustomerSearchFilter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerSearchFilterTest {
    @Test void trimsCriteriaAndValidatesTypesAndLimits() {
        var filter = new CustomerSearchFilter("  ABC  ", " ", null, null, " MB ", 10L);
        assertEquals("ABC", filter.search()); assertNull(filter.status()); assertEquals("MB", filter.region());
        assertNull(CustomerSearchFilter.id(" ", "ownerId"));
        assertEquals(Long.MAX_VALUE, CustomerSearchFilter.id(Long.toString(Long.MAX_VALUE), "ownerId"));
        for (String value : new String[]{"0", "-1", "1.0", "abc", "9223372036854775808"}) {
            assertThrows(IllegalArgumentException.class, () -> CustomerSearchFilter.id(value, "ownerId"));
        }
        assertThrows(IllegalArgumentException.class, () -> new CustomerSearchFilter("x".repeat(256), null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CustomerSearchFilter(null, "DA_GOP", null, null, null, null));
        assertEquals("Hà Nội", new CustomerSearchFilter(null, null, null, null, "Hà Nội", null).region());
        assertThrows(IllegalArgumentException.class, () -> new CustomerSearchFilter(null, null, -1L, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CustomerSearchFilter(null, null, null, 0L, null, null));
    }
}
