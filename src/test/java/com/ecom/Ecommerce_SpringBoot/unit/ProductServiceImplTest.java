package com.ecom.Ecommerce_SpringBoot.unit;

import com.ecom.Ecommerce_SpringBoot.entities.Product;
import com.ecom.Ecommerce_SpringBoot.persistence.ProductDAO;
import com.ecom.Ecommerce_SpringBoot.repository.ProductRepository;
import com.ecom.Ecommerce_SpringBoot.service.implement.ProductServiceImpl;
import com.ecom.Ecommerce_SpringBoot.support.TestFixtures;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UC1 — Unit tests for {@link ProductServiceImpl}. Pure Mockito, no Spring
 * context. Verifies DAO delegation + the discount-price calculation that
 * lives in the service layer.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ProductServiceImplTest {

    @Mock private ProductDAO productDAO;
    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductServiceImpl productService;

    @Test
    void saveProduct_delegatesToDao() {
        Product input = TestFixtures.product();
        Product persisted = TestFixtures.product(); persisted.setId(1);
        when(productDAO.saveProduct(input)).thenReturn(persisted);

        assertThat(productService.saveProduct(input)).isSameAs(persisted);
        verify(productDAO).saveProduct(input);
    }

    @Test
    void getAllProducts_returnsListFromDao() {
        List<Product> products = List.of(TestFixtures.product("A"), TestFixtures.product("B"));
        when(productDAO.getAllProducts()).thenReturn(products);

        assertThat(productService.getAllProducts())
                .hasSize(2)
                .containsExactlyElementsOf(products);
    }

    @Test
    void getProductById_returnsProductFromDao() {
        Product p = TestFixtures.product(); p.setId(42);
        when(productDAO.getProductById(42)).thenReturn(p);
        assertThat(productService.getProductById(42)).isSameAs(p);
    }

    @Test
    void getProductById_returnsNullWhenMissing() {
        when(productDAO.getProductById(404)).thenReturn(null);
        assertThat(productService.getProductById(404)).isNull();
    }

    @Test
    void deleteProduct_returnsTrueWhenDaoSucceeds() {
        when(productDAO.deleteProduct(1)).thenReturn(true);
        assertThat(productService.deleteProduct(1)).isTrue();
    }

    @Test
    void deleteProduct_returnsFalseWhenDaoFails() {
        when(productDAO.deleteProduct(404)).thenReturn(false);
        assertThat(productService.deleteProduct(404)).isFalse();
    }

    @Test
    void updateProduct_appliesDiscountPercentageToPrice() {
        Product p = TestFixtures.product();
        p.setPrice(200.0);
        p.setDiscount(25); // 25% off → 150
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.updateProduct(p);

        assertThat(result.getDiscountPrice()).isEqualTo(150.0);
        verify(productRepository).save(p);
    }

    @Test
    void updateProduct_zeroDiscount_keepsPrice() {
        Product p = TestFixtures.product();
        p.setPrice(80.0);
        p.setDiscount(0);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(productService.updateProduct(p).getDiscountPrice()).isEqualTo(80.0);
    }

    @Test
    void updateProduct_nullDiscount_setsDiscountPriceToPrice() {
        Product p = TestFixtures.product();
        p.setPrice(80.0);
        p.setDiscount(null);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(productService.updateProduct(p).getDiscountPrice()).isEqualTo(80.0);
    }

    @Test
    void getAllActiveProducts_passesCategoryThrough() {
        List<Product> filtered = List.of(TestFixtures.product("Book", "Books", 10.0));
        when(productDAO.getAllActiveProducts("Books")).thenReturn(filtered);

        assertThat(productService.getAllActiveProducts("Books")).isEqualTo(filtered);
        verify(productDAO).getAllActiveProducts("Books");
    }

    @Test
    void searchProduct_delegatesToDao() {
        List<Product> hits = List.of(TestFixtures.product());
        when(productDAO.searchProduct("widget")).thenReturn(hits);
        assertThat(productService.searchProduct("widget")).isEqualTo(hits);
    }
}
