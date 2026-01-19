package com.pruebatecnica.pruebatecnica.service;

import com.pruebatecnica.pruebatecnica.dto.CreateOrderRequest;
import com.pruebatecnica.pruebatecnica.dto.OrderItemRequest;
import com.pruebatecnica.pruebatecnica.exception.InsufficientStockException;
import com.pruebatecnica.pruebatecnica.exception.ProductNotFoundException;
import com.pruebatecnica.pruebatecnica.model.Order;
import com.pruebatecnica.pruebatecnica.model.OrderItem;
import com.pruebatecnica.pruebatecnica.model.OrderStatus;
import com.pruebatecnica.pruebatecnica.model.Product;
import com.pruebatecnica.pruebatecnica.repository.OrderRepository;
import com.pruebatecnica.pruebatecnica.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final int MIN_VARIETY_FOR_DISCOUNT = 3;
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * Crea una nueva orden aplicando validaciones, gestión de stock y reglas de negocio.
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        validateOrderRequest(request);

        Order order = initializeOrder(request);
        List<OrderItem> items = processOrderItems(request.getItems(), order);

        order.setItems(items);
        calculateFinalTotal(order);

        return orderRepository.save(order);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    private void validateOrderRequest(CreateOrderRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (request.getCustomerEmail() == null || request.getCustomerEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items are required");
        }
    }

    private Order initializeOrder(CreateOrderRequest request) {
        Order order = new Order(request.getCustomerName(), request.getCustomerEmail());
        order.setStatus(OrderStatus.CONFIRMED);
        return order;
    }

    private List<OrderItem> processOrderItems(List<OrderItemRequest> itemRequests, Order order) {
        return itemRequests.stream()
                .map(request -> createOrderItemAndUpdateStock(request, order))
                .collect(Collectors.toList());
    }

    private OrderItem createOrderItemAndUpdateStock(OrderItemRequest itemRequest, Order order) {
        validateItemRequest(itemRequest);

        Product product = getProductOrThrow(itemRequest.getProductId());
        validateAndDeductStock(product, itemRequest.getQuantity());

        // Guardamos el producto actualizado (stock reducido)
        productRepository.save(product);

        return buildOrderItem(product, itemRequest.getQuantity(), order);
    }

    private void validateItemRequest(OrderItemRequest itemRequest) {
        if (itemRequest.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void validateAndDeductStock(Product product, Integer quantity) {
        if (product.getStock() < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStock());
        }
        product.setStock(product.getStock() - quantity);
    }

    private OrderItem buildOrderItem(Product product, Integer quantity, Order order) {
        OrderItem orderItem = new OrderItem(product, quantity);
        orderItem.setOrder(order);
        return orderItem;
    }

    /**
     * Aplica la lógica de cálculo de total y la regla de negocio del descuento.
     */
    private void calculateFinalTotal(Order order) {
        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (hasVarietyDiscount(order)) {
            BigDecimal discount = subtotal.multiply(DISCOUNT_RATE);
            order.setTotalAmount(subtotal.subtract(discount));
        } else {
            order.setTotalAmount(subtotal);
        }
    }

    /**
     * Regla de Negocio: Si un pedido contiene más de 3 TIPOS de productos diferentes,
     * aplica descuento.
     */
    private boolean hasVarietyDiscount(Order order) {
        long uniqueProductTypes = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .distinct()
                .count();

        return uniqueProductTypes > MIN_VARIETY_FOR_DISCOUNT;
    }
}