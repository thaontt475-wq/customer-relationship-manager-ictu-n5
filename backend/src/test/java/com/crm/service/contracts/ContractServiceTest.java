package com.crm.service.contracts;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ContractServiceTest {

    @Test
    void createContractRequiresTitle() {
        ContractService service = new ContractService();
        assertThrows(IllegalArgumentException.class, () ->
                service.create(1L, "HD-001", "", 1L, null, null,
                        BigDecimal.valueOf(1000000), LocalDate.now(), LocalDate.now(), null, "NET 30", "DRAFT", "Note"));
    }

    @Test
    void createContractRequiresCustomerId() {
        ContractService service = new ContractService();
        assertThrows(IllegalArgumentException.class, () ->
                service.create(1L, "HD-002", "Hợp đồng test", null, null, null,
                        BigDecimal.valueOf(1000000), LocalDate.now(), LocalDate.now(), null, "NET 30", "DRAFT", "Note"));
    }

    @Test
    void createContractInvalidDateRange() {
        ContractService service = new ContractService();
        LocalDate start = LocalDate.of(2026, 12, 31);
        LocalDate end = LocalDate.of(2026, 1, 1);
        assertThrows(IllegalArgumentException.class, () ->
                service.create(1L, "HD-003", "Hợp đồng test", 1L, null, null,
                        BigDecimal.valueOf(1000000), LocalDate.now(), start, end, "NET 30", "DRAFT", "Note"));
    }
}
