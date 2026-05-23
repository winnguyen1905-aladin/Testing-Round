package com.ecom.Ecommerce_SpringBoot.unit;

import com.ecom.Ecommerce_SpringBoot.entities.Cart;
import com.ecom.Ecommerce_SpringBoot.entities.Product;
import com.ecom.Ecommerce_SpringBoot.entities.ProductOrder;
import com.ecom.Ecommerce_SpringBoot.entities.UserDtls;
import com.ecom.Ecommerce_SpringBoot.repository.CartRepository;
import com.ecom.Ecommerce_SpringBoot.repository.OrderRepository;
import com.ecom.Ecommerce_SpringBoot.service.implement.OrderServiceImpl;
import com.ecom.Ecommerce_SpringBoot.support.TestFixtures;
import com.ecom.Ecommerce_SpringBoot.util.CommonUtil;
import com.ecom.Ecommerce_SpringBoot.util.StatusOrder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderServiceImpl}.
 * <ul>
 *   <li>UC2 — {@code saveOrder} builds a {@code ProductOrder} per cart line.</li>
 *   <li>UC3 — admin listing + status update.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CommonUtil commonUtil;
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void saveOrder_persistsOneProductOrderPerCartLine_andSendsMail() throws Exception {
        UserDtls u = TestFixtures.user("buyer@example.com", "Buyer"); u.setId(7);
        Product p1 = TestFixtures.product("A", "Cat", 50.0); p1.setId(101);
        Product p2 = TestFixtures.product("B", "Cat", 20.0); p2.setId(102);

        List<Cart> carts = List.of(TestFixtures.cart(u, p1, 2), TestFixtures.cart(u, p2, 1));
        when(cartRepository.findByUserId(7)).thenReturn(carts);
        when(orderRepository.save(any(ProductOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.saveOrder(7, TestFixtures.checkoutRequest());

        ArgumentCaptor<ProductOrder> captor = ArgumentCaptor.forClass(ProductOrder.class);
        verify(orderRepository, times(2)).save(captor.capture());

        List<ProductOrder> saved = captor.getAllValues();
        assertThat(saved).hasSize(2).allSatisfy(o -> {
            assertThat(o.getOrderId()).isNotBlank();
            assertThat(o.getOrderDate()).isNotNull();
            assertThat(o.getStatus()).isEqualTo(StatusOrder.IN_PROGRESS.getName());
            assertThat(o.getPaymentType()).isEqualTo("COD");
            assertThat(o.getAddressOrder()).isNotNull();
            assertThat(o.getAddressOrder().getCity()).isEqualTo("Hanoi");
        });
        assertThat(saved.get(0).getProduct().getId()).isEqualTo(101);
        assertThat(saved.get(0).getQuantity()).isEqualTo(2);
        assertThat(saved.get(1).getProduct().getId()).isEqualTo(102);

        verify(commonUtil, times(2)).sendOrderMail(any(ProductOrder.class), eq("Success"));
    }

    @Test
    void saveOrder_emptyCart_savesNothingAndSendsNoMail() throws Exception {
        when(cartRepository.findByUserId(9)).thenReturn(List.of());

        orderService.saveOrder(9, TestFixtures.checkoutRequest());

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(commonUtil);
    }

    @Test
    void getAllOrdersByUser_returnsAllOrders() {
        ProductOrder a = new ProductOrder(); a.setId(1);
        ProductOrder b = new ProductOrder(); b.setId(2);
        when(orderRepository.findAll()).thenReturn(List.of(a, b));

        assertThat(orderService.getAllOrdersByUser())
                .extracting(ProductOrder::getId)
                .containsExactly(1, 2);
    }

    @Test
    void getOrdersByUser_filtersByUserId() {
        ProductOrder o = new ProductOrder(); o.setId(5);
        when(orderRepository.findByUserId(42)).thenReturn(List.of(o));

        List<ProductOrder> result = orderService.getOrdersByUser(42);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(5);
    }

    @Test
    void orderStatusUpdate_setsStatusWhenOrderExists() {
        ProductOrder existing = new ProductOrder();
        existing.setId(10);
        existing.setStatus(StatusOrder.IN_PROGRESS.getName());
        when(orderRepository.findById(10)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);

        ProductOrder updated = orderService.orderStatusUpdate(10, StatusOrder.DELIVERED.getName());

        assertThat(updated.getStatus()).isEqualTo(StatusOrder.DELIVERED.getName());
        verify(orderRepository).save(existing);
    }

    @Test
    void orderStatusUpdate_returnsNullWhenOrderMissing() {
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        assertThat(orderService.orderStatusUpdate(999, StatusOrder.DELIVERED.getName())).isNull();
        verify(orderRepository, never()).save(any());
    }
}
