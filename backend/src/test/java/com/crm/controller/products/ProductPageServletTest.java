package com.crm.controller.products;
import com.crm.service.products.ProductService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
class ProductPageServletTest {
    @Test void missingActiveFilterRendersAllProductsInsteadOfUnboxingNull() throws Exception {
        var service = mock(ProductService.class);
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        var session = mock(HttpSession.class);
        var dispatcher = mock(RequestDispatcher.class);
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(req.getRequestDispatcher("/jsp/products/product-list.jsp")).thenReturn(dispatcher);
        when(service.searchProducts(null,null,null,1,20,List.of())).thenReturn(
            new ProductService.ProductSearchResult(List.of(),0,1,20,1));
        new ProductPageServlet(service).doGet(req,res);
        verify(service).searchProducts(null,null,null,1,20,List.of());
        verify(dispatcher).forward(req,res);
    }
}
