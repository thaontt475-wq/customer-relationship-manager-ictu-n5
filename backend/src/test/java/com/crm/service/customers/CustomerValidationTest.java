package com.crm.service.customers;

import com.crm.dto.customers.CustomerWriteRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CustomerValidationTest {
    private CustomerWriteRequest request() {
        CustomerWriteRequest request = new CustomerWriteRequest();
        request.setCompanyName("  Công ty ABC  ");
        return request;
    }

    @Test void aliasDefaultStatusAndOptionalTaxAreRetained() {
        var request = request();
        request.setTaxCode("  ");
        CustomerValidation.normalize(request);
        assertEquals("Công ty ABC", request.getName());
        assertNull(request.getTaxCode());
        assertEquals("TIEM_NANG", request.getStatus());
    }

    @ParameterizedTest @ValueSource(strings = {"0123456789", "0123456789001", " 0123456789-001 "})
    void acceptedTaxCodesPreserveLeadingZerosAndNormalizeBranch(String tax) {
        var request = request(); request.setTaxCode(tax);
        CustomerValidation.normalize(request);
        assertEquals(tax.trim().replace("-", ""), request.getTaxCode());
    }

    @ParameterizedTest @ValueSource(strings = {"123", "012345678X", "01234 56789", "0123456789--001"})
    void invalidTaxCodesAreRejected(String tax) {
        var request = request(); request.setTaxCode(tax);
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
    }

    @ParameterizedTest @ValueSource(strings = {"TIEM_NANG", "DANG_GIAO_DICH", "CHINH_THUC", "NGUNG_HOP_TAC"})
    void allExistingBusinessStatusCodesAreAccepted(String status) {
        var request = request(); request.setStatus(status);
        CustomerValidation.normalize(request); assertEquals(status, request.getStatus());
    }

    @Test void mergeStatusCannotBeWrittenThroughCrud() {
        var request = request(); request.setStatus("DA_GOP");
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
    }

    @Test void validatesNameLengthsReferencesAndContactFormats() {
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(null));
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(new CustomerWriteRequest()));
        var request = request(); request.setName("x".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
        request.setName("ABC"); request.setIndustryId(-1L);
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
        request.setIndustryId(null); request.setEmail("wrong@");
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
        request.setEmail(null); request.setPhone("abc123456");
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
        request.setPhone(null); request.setWebsite("javascript:alert(1)");
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
        request.setWebsite("https://user:password@example.com");
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
        request.setWebsite("https://example.com"); request.setAddress("x".repeat(501));
        assertThrows(IllegalArgumentException.class, () -> CustomerValidation.normalize(request));
    }
}
