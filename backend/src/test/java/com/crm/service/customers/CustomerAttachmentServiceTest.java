package com.crm.service.customers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerAttachmentServiceTest {
    @TempDir Path temp;
    @Test void resolvesOnlyFilesInsidePrivateDirectory() throws Exception {
        Path root=Files.createDirectory(temp.resolve("private"));
        Path file=Files.writeString(root.resolve("valid.txt"),"private content");
        Files.writeString(temp.resolve("outside.txt"),"must not leak");
        assertEquals(file.toRealPath(),CustomerAttachmentService.resolvePrivateFile(root,"valid.txt"));
        assertThrows(NoSuchFileException.class,()->CustomerAttachmentService.resolvePrivateFile(root,"../outside.txt"));
        assertThrows(NoSuchFileException.class,()->CustomerAttachmentService.resolvePrivateFile(root,temp.resolve("outside.txt").toString()));
        assertThrows(NoSuchFileException.class,()->CustomerAttachmentService.resolvePrivateFile(root,"missing.txt"));
        assertThrows(NoSuchFileException.class,()->CustomerAttachmentService.resolvePrivateFile(root,"."));
    }
}
