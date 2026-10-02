package com.crm.service.customfields;

import com.crm.dao.customfields.CustomFieldDAO;
import com.crm.model.CustomField;
import com.crm.model.CustomFieldValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomFieldFormIntegrationTest {

    @Mock
    private CustomFieldDAO dao;

    @Mock
    private Connection connection;

    private CustomFieldService service;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(connection.getAutoCommit()).thenReturn(true);
        service = new CustomFieldService(dao, () -> connection);
    }

    @Test
    @DisplayName("Retrieve form definitions for CUSTOMER and OPPORTUNITY separately")
    void testRetrieveDefinitionsByEntity() throws SQLException {
        CustomField custField = new CustomField();
        custField.setId(1L);
        custField.setEntityType("CUSTOMER");
        custField.setFieldName("tax_id");
        custField.setFieldLabel("Mã số thuế");
        custField.setFieldType("TEXT");
        custField.setActive(true);
        custField.setInForm(true);

        CustomField oppField = new CustomField();
        oppField.setId(2L);
        oppField.setEntityType("OPPORTUNITY");
        oppField.setFieldName("deal_source");
        oppField.setFieldLabel("Nguồn cơ hội");
        oppField.setFieldType("SELECT");
        oppField.setOptions(List.of("Inbound", "Outbound"));
        oppField.setActive(true);
        oppField.setInForm(true);

        when(dao.findAll(connection, "CUSTOMER")).thenReturn(List.of(custField));
        when(dao.findAll(connection, "OPPORTUNITY")).thenReturn(List.of(oppField));

        List<CustomField> customerDefs = service.getDefinitions("CUSTOMER");
        assertEquals(1, customerDefs.size());
        assertEquals("tax_id", customerDefs.get(0).getFieldName());

        List<CustomField> oppDefs = service.getDefinitions("OPPORTUNITY");
        assertEquals(1, oppDefs.size());
        assertEquals("deal_source", oppDefs.get(0).getFieldName());
    }

    @Test
    @DisplayName("Process form submission with 4 data types (TEXT, NUMBER, DATE, SELECT) successfully")
    void testSaveFormValuesAllFourTypes() throws SQLException {
        CustomField textField = createField(1L, "CUSTOMER", "tax_code", "Mã số thuế", "TEXT", true, null);
        CustomField numField = createField(2L, "CUSTOMER", "annual_revenue", "Doanh thu năm", "NUMBER", false, null);
        CustomField dateField = createField(3L, "CUSTOMER", "established_date", "Ngày thành lập", "DATE", false, null);
        CustomField selectField = createField(4L, "CUSTOMER", "customer_tier", "Hạng khách hàng", "SELECT", false, List.of("VIP", "Standard"));

        when(dao.recordExists(connection, "CUSTOMER", 100L)).thenReturn(true);
        when(dao.findActive(connection, "CUSTOMER")).thenReturn(List.of(textField, numField, dateField, selectField));
        when(dao.findValues(connection, "CUSTOMER", 100L)).thenReturn(Map.of());

        Map<String, String> formSubmission = new HashMap<>();
        formSubmission.put("tax_code", "0102030405");
        formSubmission.put("annual_revenue", "5000000000");
        formSubmission.put("established_date", "2020-05-15");
        formSubmission.put("customer_tier", "VIP");

        CustomFieldValues result = service.saveValues("CUSTOMER", 100L, formSubmission);

        assertNotNull(result);
        assertEquals("0102030405", result.getValues().get("tax_code"));
        assertEquals("5000000000", result.getValues().get("annual_revenue"));
        assertEquals("2020-05-15", result.getValues().get("established_date"));
        assertEquals("VIP", result.getValues().get("customer_tier"));

        verify(dao).upsertValue(connection, 1L, 100L, "0102030405");
        verify(dao).upsertValue(connection, 2L, 100L, "5000000000");
        verify(dao).upsertValue(connection, 3L, 100L, "2020-05-15");
        verify(dao).upsertValue(connection, 4L, 100L, "VIP");
        verify(connection).commit();
    }

    @Test
    @DisplayName("Form submission fails when a required custom field is missing")
    void testFormFailsWhenRequiredFieldMissing() throws SQLException {
        CustomField requiredField = createField(1L, "CUSTOMER", "tax_code", "Mã số thuế", "TEXT", true, null);

        when(dao.recordExists(connection, "CUSTOMER", 100L)).thenReturn(true);
        when(dao.findActive(connection, "CUSTOMER")).thenReturn(List.of(requiredField));
        when(dao.findValues(connection, "CUSTOMER", 100L)).thenReturn(Map.of());

        Map<String, String> formSubmission = new HashMap<>();
        formSubmission.put("tax_code", "");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.saveValues("CUSTOMER", 100L, formSubmission));

        assertTrue(ex.getMessage().contains("bắt buộc"));
        verify(dao, never()).upsertValue(any(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Form submission fails when SELECT value is not in defined options")
    void testFormFailsOnInvalidSelectOption() throws SQLException {
        CustomField selectField = createField(4L, "OPPORTUNITY", "deal_source", "Nguồn", "SELECT", false, List.of("Direct", "Partner"));

        when(dao.recordExists(connection, "OPPORTUNITY", 200L)).thenReturn(true);
        when(dao.findActive(connection, "OPPORTUNITY")).thenReturn(List.of(selectField));
        when(dao.findValues(connection, "OPPORTUNITY", 200L)).thenReturn(Map.of());

        Map<String, String> formSubmission = Map.of("deal_source", "HackedOption");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.saveValues("OPPORTUNITY", 200L, formSubmission));

        assertTrue(ex.getMessage().contains("không thuộc danh sách tùy chọn"));
        verify(dao, never()).upsertValue(any(), anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Form submission fails when submitting unknown or deactivated field name")
    void testFormFailsOnUnknownField() throws SQLException {
        when(dao.recordExists(connection, "CUSTOMER", 100L)).thenReturn(true);
        when(dao.findActive(connection, "CUSTOMER")).thenReturn(List.of());

        Map<String, String> formSubmission = Map.of("unregistered_field", "some_value");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.saveValues("CUSTOMER", 100L, formSubmission));

        assertTrue(ex.getMessage().contains("không tồn tại hoặc đã ngừng kích hoạt"));
    }

    @Test
    @DisplayName("Edit form preserves existing custom field values and updates only submitted delta")
    void testEditFormPreservesExistingValues() throws SQLException {
        CustomField field1 = createField(1L, "CUSTOMER", "tax_code", "MST", "TEXT", true, null);
        CustomField field2 = createField(2L, "CUSTOMER", "note", "Ghi chú", "TEXT", false, null);

        when(dao.recordExists(connection, "CUSTOMER", 100L)).thenReturn(true);
        when(dao.findActive(connection, "CUSTOMER")).thenReturn(List.of(field1, field2));
        when(dao.findValues(connection, "CUSTOMER", 100L)).thenReturn(Map.of("tax_code", "0102030405", "note", "Old note"));

        Map<String, String> editSubmission = Map.of("note", "New updated note");

        CustomFieldValues result = service.saveValues("CUSTOMER", 100L, editSubmission);

        assertEquals("0102030405", result.getValues().get("tax_code"));
        assertEquals("New updated note", result.getValues().get("note"));
        verify(dao).upsertValue(connection, 2L, 100L, "New updated note");
    }

    private CustomField createField(long id, String entityType, String name, String label, String type, boolean required, List<String> options) {
        CustomField field = new CustomField();
        field.setId(id);
        field.setEntityType(entityType);
        field.setFieldName(name);
        field.setFieldLabel(label);
        field.setFieldType(type);
        field.setRequired(required);
        field.setActive(true);
        field.setInForm(true);
        if (options != null) {
            field.setOptions(options);
        }
        return field;
    }
}
