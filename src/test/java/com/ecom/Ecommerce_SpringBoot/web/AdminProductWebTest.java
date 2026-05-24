package com.ecom.Ecommerce_SpringBoot.web;

import com.ecom.Ecommerce_SpringBoot.controller.AdminController;
import com.ecom.Ecommerce_SpringBoot.entities.Category;
import com.ecom.Ecommerce_SpringBoot.entities.Product;
import com.ecom.Ecommerce_SpringBoot.service.*;
import com.ecom.Ecommerce_SpringBoot.service.implement.CloudinaryService;
import com.ecom.Ecommerce_SpringBoot.support.TestFixtures;
import com.ecom.Ecommerce_SpringBoot.util.CommonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UC1 — web-slice tests for the admin product endpoints in {@link AdminController}.
 * Mocked services, no DB, no Thymeleaf rendering, no Spring Security filters.
 */
@WebMvcTest(controllers = AdminController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Tag("web")
class AdminProductWebTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CategoryService categoryService;
    @MockBean private ProductService productService;
    @MockBean private UserService userService;
    @MockBean private OrderService orderService;
    @MockBean private CommonUtil commonUtil;
    @MockBean private CloudinaryService cloudinaryService;

    private static final MockMultipartFile EMPTY_IMG =
            new MockMultipartFile("file", "", MediaType.IMAGE_JPEG_VALUE, new byte[0]);

    @BeforeEach
    void seedCommonMocks() {
        // AdminController's @ModelAttribute hits this for every request
        when(categoryService.getAllActiveCategory()).thenReturn(List.of());
    }

    @Test
    void list_returnsProductsViewWithProductsAttribute() throws Exception {
        Product a = TestFixtures.product("A"); a.setId(1);
        Product b = TestFixtures.product("B"); b.setId(2);
        when(productService.getAllProducts()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"))
                .andExpect(model().attribute("products", hasSize(2)));
        verify(productService).getAllProducts();
    }

    @Test
    void editForm_returnsEditViewWithProductAndCategories() throws Exception {
        Product p = TestFixtures.product("Widget"); p.setId(42);
        when(productService.getProductById(42)).thenReturn(p);
        when(categoryService.getAllCategory()).thenReturn(List.of(new Category()));

        mockMvc.perform(get("/admin/editProduct/{id}", 42))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/edit_product"))
                .andExpect(model().attribute("product", hasProperty("id", is(42))))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    void addForm_returnsAddViewWithEmptyProductAndCategories() throws Exception {
        when(categoryService.getAllCategory()).thenReturn(List.of());

        mockMvc.perform(get("/admin/loadAddProduct"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/add_product"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    void saveProduct_validInput_redirectsToProductList() throws Exception {
        Product saved = TestFixtures.product("New"); saved.setId(1);
        when(productService.saveProduct(any(Product.class))).thenReturn(saved);

        mockMvc.perform(multipart("/admin/saveProduct").file(EMPTY_IMG)
                        .param("title", "New Widget")
                        .param("description", "A useful widget")
                        .param("category", "Gadgets")
                        .param("price", "199.99")
                        .param("stock", "10")
                        .param("discount", "0")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));

        verify(productService).saveProduct(any(Product.class));
        verify(cloudinaryService, never()).uploadImage(any(), any()); // empty file → no Cloudinary call
    }

    @Test
    void saveProduct_validationError_returnsAddViewWithoutSaving() throws Exception {
        mockMvc.perform(multipart("/admin/saveProduct").file(EMPTY_IMG)
                        .param("title", "")          // @NotBlank
                        .param("description", "")
                        .param("category", "")
                        .param("price", "0.0"))      // @DecimalMin("0.01")
                .andExpect(status().isOk())
                .andExpect(view().name("admin/add_product"));

        verify(productService, never()).saveProduct(any());
    }

    @Test
    void updateProduct_redirectsBackToEditScreen() throws Exception {
        Product existing = TestFixtures.product("Original"); existing.setId(7);
        Product updated = TestFixtures.product("Updated"); updated.setId(7);
        when(productService.getProductById(7)).thenReturn(existing);
        when(productService.updateProduct(any(Product.class))).thenReturn(updated);

        mockMvc.perform(multipart("/admin/updateProduct").file(EMPTY_IMG)
                        .param("id", "7")
                        .param("title", "Updated")
                        .param("description", "x")
                        .param("category", "Cat")
                        .param("price", "120.0")
                        .param("stock", "3")
                        .param("discount", "10")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/editProduct/7"));
        verify(productService).updateProduct(any(Product.class));
    }

    @Test
    void updateProduct_withEmptyImage_preservesExistingImageUrl() throws Exception {
        // The edit form does not echo the current image URL back, so without server-side
        // fallback the product's image would get wiped to null on every non-image edit.
        Product existing = TestFixtures.product("Original"); existing.setId(8);
        existing.setImage("https://cdn.example.com/keep-me.jpg");
        when(productService.getProductById(8)).thenReturn(existing);
        when(productService.updateProduct(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(multipart("/admin/updateProduct").file(EMPTY_IMG)
                        .param("id", "8")
                        .param("title", "Renamed")
                        .param("description", "x")
                        .param("category", "Cat")
                        .param("price", "100.0")
                        .param("stock", "5")
                        .param("discount", "0")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/editProduct/8"));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).updateProduct(captor.capture());
        assertThat(captor.getValue().getImage()).isEqualTo("https://cdn.example.com/keep-me.jpg");
    }

    @Test
    void updateProduct_missingProduct_redirectsToList() throws Exception {
        when(productService.getProductById(404)).thenReturn(null);

        mockMvc.perform(multipart("/admin/updateProduct").file(EMPTY_IMG)
                        .param("id", "404")
                        .param("title", "Ghost")
                        .param("description", "x")
                        .param("category", "Cat")
                        .param("price", "1.0")
                        .param("stock", "0")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));
        verify(productService, never()).updateProduct(any());
    }

    @Test
    void deleteProduct_success_redirectsToList() throws Exception {
        when(productService.deleteProduct(5)).thenReturn(true);

        mockMvc.perform(get("/admin/deleteProduct/{id}", 5))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));
        verify(productService).deleteProduct(5);
    }

    @Test
    void deleteProduct_serviceFailure_stillRedirectsToList() throws Exception {
        when(productService.deleteProduct(anyInt())).thenReturn(false);

        mockMvc.perform(get("/admin/deleteProduct/{id}", 99))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"));
    }
}
