package com.crm.service.customers;

import com.crm.dao.customers.CustomerAttachmentDAO;
import java.io.IOException;
import java.nio.file.*;
import java.util.NoSuchElementException;

public class CustomerAttachmentService {
    public record Download(String name, Path path) {}
    public Download download(long user, long id) throws Exception {
        var item=new CustomerAttachmentDAO().find(id);
        if(item==null || item.location()==null || new CustomerService().getById(user,item.customerId())==null)
            throw new NoSuchElementException("Không tìm thấy tệp");
        String configured=System.getenv("CRM_ATTACHMENT_DIR");
        Path directory=Path.of(configured==null ? System.getProperty("catalina.base",".")+"/data/customer-attachments" : configured);
        return new Download(item.name(),resolvePrivateFile(directory,item.location()));
    }
    public static Path resolvePrivateFile(Path directory, String location) throws IOException {
        Path root=directory.toRealPath();
        Path candidate=root.resolve(location).normalize();
        if(!candidate.startsWith(root) || !Files.isRegularFile(candidate)) throw new NoSuchFileException("Attachment");
        Path real=candidate.toRealPath();
        if(!real.startsWith(root)) throw new NoSuchFileException("Attachment");
        return real;
    }
}
