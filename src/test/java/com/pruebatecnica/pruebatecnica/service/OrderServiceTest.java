package com.pruebatecnica.pruebatecnica.service;

import com.pruebatecnica.pruebatecnica.dto.CreateOrderRequest;
import com.pruebatecnica.pruebatecnica.dto.OrderItemRequest;
import com.pruebatecnica.pruebatecnica.model.Order;
import com.pruebatecnica.pruebatecnica.model.Product;
import com.pruebatecnica.pruebatecnica.repository.OrderRepository;
import com.pruebatecnica.pruebatecnica.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    // --- Tests de Regla de Negocio (Descuento) ---

    @Test
    void testCreateOrderWithoutDiscount_ShouldNotApplyVarietyDiscount() {
        // Escenario: 3 productos diferentes (Límite exacto para NO aplicar descuento)
        // Total base: 10 + 20 + 30 = 60. Esperado: 60.00

        // 1. Mock Data
        Product p1 = createMockProduct(1L, "P1", new BigDecimal("10.00"));
        Product p2 = createMockProduct(2L, "P2", new BigDecimal("20.00"));
        Product p3 = createMockProduct(3L, "P3", new BigDecimal("30.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(productRepository.findById(3L)).thenReturn(Optional.of(p3));

        // Simulamos que el guardado devuelve la misma orden que le pasamos
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        // 2. Request
        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1),
                new OrderItemRequest(2L, 1),
                new OrderItemRequest(3L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest("Cliente Test", "test@mail.com", items);

        Order result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("60.00").compareTo(result.getTotalAmount()),
                "El total debería ser 60.00 (Sin descuento por variedad)");
    }

    @Test
    void testCreateOrderWithDiscount_ShouldApplyVarietyDiscount() {
        // Escenario: 4 productos diferentes (Supera el límite de 3) -> Aplica 10%
        // Total base: 10 + 10 + 10 + 10 = 40.
        // Descuento: 40 * 0.10 = 4.
        // Total Final: 36.00.

        // 1. Mock Data
        Product p1 = createMockProduct(1L, "A", new BigDecimal("10.00"));
        Product p2 = createMockProduct(2L, "B", new BigDecimal("10.00"));
        Product p3 = createMockProduct(3L, "C", new BigDecimal("10.00"));
        Product p4 = createMockProduct(4L, "D", new BigDecimal("10.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));
        when(productRepository.findById(3L)).thenReturn(Optional.of(p3));
        when(productRepository.findById(4L)).thenReturn(Optional.of(p4));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1),
                new OrderItemRequest(2L, 1),
                new OrderItemRequest(3L, 1),
                new OrderItemRequest(4L, 1)
        );
        CreateOrderRequest request = new CreateOrderRequest("Cliente VIP", "vip@mail.com", items);

        Order result = orderService.createOrder(request);

        // 36.00 con descuento
        assertEquals(0, new BigDecimal("36.00").compareTo(result.getTotalAmount()),
                "El total debería tener 10% de descuento (40 - 4 = 36)");
    }

    @Test
    void testCreateOrderWithSameProductMultipleTimes_ShouldNotApplyDiscount() {
        // Escenario: 10 items, pero todos del MISMO producto ID.
        // Variedad = 1. NO aplica descuento.
        // Total: 10 unidades * 10.00 = 100.00.

        // 1. Mock Data
        Product p1 = createMockProduct(1L, "Manzana", new BigDecimal("10.00"));
        // Stock suficiente para 10 unidades
        p1.setStock(20);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        // 2. Request (10 unidades del mismo producto)
        List<OrderItemRequest> items = Collections.singletonList(
                new OrderItemRequest(1L, 10)
        );
        CreateOrderRequest request = new CreateOrderRequest("Cliente Mayorista", "bulk@mail.com", items);

        Order result = orderService.createOrder(request);

        assertEquals(0, new BigDecimal("100.00").compareTo(result.getTotalAmount()),
                "El total debe ser 100.00 sin descuento (solo 1 tipo de producto)");
    }

    @Test
    void testCreateBasicOrder() {
        // Test básico para asegurar que no rompimos la funcionalidad original
        Product product1 = createMockProduct(1L, "Test Product", BigDecimal.valueOf(10.00));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);

        OrderItemRequest item = new OrderItemRequest(1L, 2);
        CreateOrderRequest request = new CreateOrderRequest("John Doe", "john@test.com", List.of(item));

        assertDoesNotThrow(() -> {
            Order result = orderService.createOrder(request);
            assertNotNull(result);
            assertEquals("John Doe", result.getCustomerName());
            // 2 items * 10.00 = 20.00
            assertEquals(0, BigDecimal.valueOf(20.00).compareTo(result.getTotalAmount()));
        });
    }

    private Product createMockProduct(Long id, String name, BigDecimal price) {
        Product p = new Product(name, price, 100); // 100 stock default
        p.setId(id);
        return p;
    }
}