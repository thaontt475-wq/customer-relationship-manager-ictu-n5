package com.crm.service.leads;

import com.crm.dto.leads.WebFormLeadRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeadCaptureValidationTest {
    private WebFormLeadRequest valid() {
        WebFormLeadRequest r = new WebFormLeadRequest();
        r.setFullName("Lead QA"); r.setEmail("qa@example.invalid"); r.setPhone("0901234567");
        r.setSource("WEB_FORM"); r.setFormId("00000000-0000-0000-0000-000000000001");
        r.setRequestId("1234567890abcdef"); return r;
    }
    @Test void missingSourceIsRejectedExplicitly() {
        var r=valid(); r.setSource(null);
        assertTrue(assertThrows(IllegalArgumentException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1")).getMessage().contains("Nguồn Lead"));
    }
    @Test void arbitrarySourceCannotBeSpoofed() {
        var r=valid(); r.setSource("EVENT");
        assertThrows(IllegalArgumentException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1"));
    }
    @Test void malformedEmailAndPunctuationOnlyPhoneAreRejected() {
        var r=valid(); r.setEmail("bad");
        assertThrows(IllegalArgumentException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1"));
        r.setEmail("qa@example.invalid"); r.setPhone("++++++++");
        assertThrows(IllegalArgumentException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1"));
    }
    @Test void missingFormAndRetryIdAreRejected() {
        var r=valid(); r.setFormId(null);
        assertThrows(IllegalArgumentException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1"));
        r.setFormId("00000000-0000-0000-0000-000000000001"); r.setRequestId(null);
        assertThrows(IllegalArgumentException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1"));
    }
    @Test void overlongInputIsRejectedInsteadOfTruncated() {
        var r=valid(); r.setFullName("x".repeat(151));
        assertThrows(IllegalArgumentException.class,()->new LeadService().createWebFormLead(r,"127.0.0.1"));
    }
}
