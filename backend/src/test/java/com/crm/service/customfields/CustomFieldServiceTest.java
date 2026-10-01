package com.crm.service.customfields;

import com.crm.dao.customfields.CustomFieldDAO;
import com.crm.model.CustomField;
import com.crm.model.CustomFieldValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomFieldServiceTest {
    @Mock CustomFieldDAO dao;
    @Mock Connection connection;
    private CustomFieldService service;

    @BeforeEach
    void setUp() throws Exception {
        when(connection.getAutoCommit()).thenReturn(true);
        service = new CustomFieldService(dao, () -> connection);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEXT", "NUMBER", "DATE", "SELECT"})
    void createsAllFourSupportedTypes(String type) throws Exception {
        CustomField field = definition(type, false);
        if ("SELECT".equals(type)) field.setOptions(List.of("Gold", "Silver"));
        when(dao.findAll(connection, "CUSTOMER")).thenReturn(List.of());
        when(dao.insert(connection, field)).thenReturn(11L);
        when(dao.findById(connection, 11L)).thenReturn(field);

        CustomField result = service.createDefinition(field);

        assertSame(field, result);
        assertEquals(type, field.getStorageType());
        verify(dao).replaceOptions(connection, 11L, field.getOptions());
        verify(connection).commit();
    }

    @Test
    void rejectsDuplicateCodeWithinEntity() throws Exception {
        CustomField field = definition("TEXT", false);
        when(dao.existsByName(connection, "CUSTOMER", "field_code", null)).thenReturn(true);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.createDefinition(field));

        assertTrue(error.getMessage().contains("tồn tại"));
        verify(dao, never()).insert(any(), any());
        verify(connection).rollback();
    }

    @Test
    void requiredValueCannotBeMissing() throws Exception {
        CustomField required = definition("TEXT", true);
        required.setId(1L);
        prepareValues(required, Map.of());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveValues("CUSTOMER", 10L, Map.of()));

        assertTrue(error.getMessage().contains("bắt buộc"));
    }

    @Test
    void rejectsInvalidNumber() throws Exception {
        assertInvalidValue(definition("NUMBER", false), "not-a-number", "NUMBER");
    }

    @Test
    void rejectsInvalidDate() throws Exception {
        assertInvalidValue(definition("DATE", false), "31/12/2026", "yyyy-MM-dd");
    }

    @Test
    void rejectsInvalidSelectOption() throws Exception {
        CustomField field = definition("SELECT", false);
        field.setOptions(List.of("Gold", "Silver"));
        assertInvalidValue(field, "Bronze", "SELECT");
    }

    @Test
    void savesAndUpdatesAValueWithUpsert() throws Exception {
        CustomField field = definition("NUMBER", false);
        field.setId(9L);
        prepareValues(field, Map.of("field_code", "10"));

        CustomFieldValues result = service.saveValues(
                "customer", 77L, Map.of("field_code", "12.50"));

        assertEquals("12.5", result.getValues().get("field_code"));
        verify(dao).upsertValue(connection, 9L, 77L, "12.5");
        verify(connection).commit();
    }

    @Test
    void deleteInUseDefinitionDeactivatesWithoutDataLoss() throws Exception {
        CustomField field = definition("TEXT", false);
        field.setId(5L);
        field.setUsageCount(3);
        when(dao.findById(connection, 5L)).thenReturn(field);

        assertEquals(CustomFieldService.DeleteOutcome.DEACTIVATED, service.deleteDefinition(5L));

        verify(dao).deactivate(connection, 5L);
        verify(dao, never()).delete(any(), anyLong());
    }

    private void assertInvalidValue(CustomField field, String value, String expectedMessage) throws Exception {
        field.setId(2L);
        prepareValues(field, Map.of());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.saveValues("CUSTOMER", 10L, Map.of("field_code", value)));
        assertTrue(error.getMessage().contains(expectedMessage));
        verify(dao, never()).upsertValue(any(), anyLong(), anyLong(), anyString());
    }

    private void prepareValues(CustomField field, Map<String, String> existing) throws Exception {
        when(dao.recordExists(eq(connection), eq("CUSTOMER"), anyLong())).thenReturn(true);
        when(dao.findActive(connection, "CUSTOMER")).thenReturn(List.of(field));
        when(dao.findValues(eq(connection), eq("CUSTOMER"), anyLong())).thenReturn(existing);
    }

    private CustomField definition(String type, boolean required) {
        CustomField field = new CustomField();
        field.setEntityType("CUSTOMER");
        field.setFieldName("field_code");
        field.setFieldLabel("Field label");
        field.setFieldType(type);
        field.setRequired(required);
        field.setActive(true);
        field.setInForm(true);
        field.setInFilter(true);
        field.setInExport(true);
        return field;
    }
}
