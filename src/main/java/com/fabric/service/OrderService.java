package com.fabric.service;

import com.fabric.database.dto.order.OrderDTO;
import com.fabric.database.dto.order.OrderPageDTO;
import com.fabric.database.dto.order.OrdersDetailsDTO;
import com.fabric.database.dto.user.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    void createOrder(OrderDTO dto, UserDTO userDTO);

    void createOrder(OrderDTO dto);

    Page<OrderPageDTO> getAllOrdersByStatus(Pageable pageable, String status);

    Page<OrderPageDTO> getAllOrders(Pageable pageable);

    OrdersDetailsDTO findOrderById(Long id);

    boolean updateStatus(Long id, String status);

    Page<OrderPageDTO> findOrdersByUser(String userEmail, Pageable pageable);
}
