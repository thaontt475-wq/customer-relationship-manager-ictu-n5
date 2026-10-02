package com.crm.service.products;

import com.crm.model.QuoteItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuotePricingPolicyTest {
    private QuoteItem item() {
        return new QuoteItem(1L, 1L, "P", "Sản phẩm", "cái", BigDecimal.ONE,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("90"));
    }

    @Test
    @DisplayName("Discount pushing net unit price below floor price requires approval")
    void belowFloorRequiresApproval() {
        assertTrue(QuotePricingService.belowFloor(List.of(item()), new BigDecimal("11")));
    }

    @Test
    @DisplayName("Discount exactly at floor price does not require approval")
    void exactlyFloorDoesNotRequireApproval() {
        assertFalse(QuotePricingService.belowFloor(List.of(item()), new BigDecimal("10")));
    }

    @Test
    @DisplayName("Computes currency rounding correctly for net unit price")
    void computesCurrencyRounding() {
        assertEquals(new BigDecimal("89.99"), QuotePricingService.netUnitPrice(new BigDecimal("99.99"), new BigDecimal("10")));
    }
}
