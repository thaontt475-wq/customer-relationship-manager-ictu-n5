import com.crm.dto.profile.ProfileUpdateRequest;
import com.crm.service.profile.ProfileService;
import com.crm.util.JsonUtil;
import com.google.gson.JsonParseException;

/** DEV-only checks: every service input is invalid and must fail before DAO access. */
public final class ProfileValidationChecks {
    public static void main(String[] args) throws Exception {
        ProfileService service = new ProfileService();
        invalidProfile(service, null, null, "Họ tên là bắt buộc");
        invalidProfile(service, "   ", "", "Họ tên là bắt buộc");
        invalidProfile(service, "N".repeat(151), null, "Họ tên không được vượt quá 150 ký tự");
        invalidProfile(service, "  " + "N".repeat(151) + "  ", null,
                "Họ tên không được vượt quá 150 ký tự");
        invalidProfile(service, "DEV validation", "invalid-phone", "Số điện thoại không hợp lệ");
        invalidProfile(service, "DEV validation", "1234567", "Số điện thoại không hợp lệ");
        invalidProfile(service, "DEV validation", "1".repeat(21), "Số điện thoại không hợp lệ");
        invalidJson("{");
        invalidJson("[]");
        invalidJson("{\"fullName\": []}");
        System.out.println("PASS 10 DEV Profile validation checks; no DAO access or account mutation.");
    }

    private static void invalidProfile(ProfileService service, String fullName, String phone,
                                       String expectedMessage) throws Exception {
        try {
            service.update(0, fullName, phone);
        } catch (IllegalArgumentException e) {
            if (expectedMessage.equals(e.getMessage())) return;
            throw new AssertionError("Unexpected Profile validation result");
        }
        throw new AssertionError("Invalid Profile input was accepted");
    }

    private static void invalidJson(String json) {
        try {
            JsonUtil.getGson().fromJson(json, ProfileUpdateRequest.class);
        } catch (JsonParseException e) {
            return;
        }
        throw new AssertionError("Malformed Profile JSON was accepted");
    }
}
