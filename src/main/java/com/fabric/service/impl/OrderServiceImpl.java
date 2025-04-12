package com.fabric.service.impl;

import com.fabric.database.dto.clothes.ClothingDetailsPageDTO;
import com.fabric.database.dto.order.OrderDTO;
import com.fabric.database.dto.order.OrderItemDTO;
import com.fabric.database.dto.order.OrderPageDTO;
import com.fabric.database.dto.order.OrdersDetailsDTO;
import com.fabric.database.dto.user.UserDTO;
import com.fabric.database.entity.Clothing;
import com.fabric.database.entity.Order;
import com.fabric.database.entity.OrderItem;
import com.fabric.database.entity.User;
import com.fabric.database.repository.ClothingRepository;
import com.fabric.database.repository.OrderRepository;
import com.fabric.database.repository.UserRepository;
import com.fabric.exceptions.BadRequestException;
import com.fabric.exceptions.NotFoundException;
import com.fabric.service.ClothingService;
import com.fabric.service.EmailService;
import com.fabric.service.OrderService;
import com.fabric.utils.PhoneNumberUtils;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ClothingRepository clothRepository;
    private final UserRepository userRepository;
    private final ClothingService clothingService;
    private final EmailService emailService;
    private final PhoneNumberUtils phoneNumberUtils;
    private final ModelMapper modelMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ClothingRepository clothRepository,
                            UserRepository userRepository,
                            ClothingService clothingService,
                            EmailService emailService,
                            PhoneNumberUtils phoneNumberUtils,
                            ModelMapper modelMapper) {
        this.orderRepository = orderRepository;
        this.clothRepository = clothRepository;
        this.userRepository = userRepository;
        this.clothingService = clothingService;
        this.emailService = emailService;
        this.phoneNumberUtils = phoneNumberUtils;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public void createOrder(OrderDTO orderDTO, UserDTO userDTO) {
        if (orderDTO == null) {
            throw new BadRequestException("Order data is invalid: order cannot be null");
        }

        User user = validateUser(userDTO);
        Order order = buildOrder(orderDTO, user);

        user.getOrders().add(order);
        order.setUser(user);

        this.orderRepository.save(order);
        this.emailService.sendOrderEmail(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public void createOrder(OrderDTO orderDTO) {
        if (orderDTO == null) {
            throw new BadRequestException("Order data is invalid: order cannot be null");
        }

        Optional<User> optional = this.userRepository.findByEmail(orderDTO.getEmail());
        User user = optional.orElse(null);
        Order order = buildOrder(orderDTO, user);

        this.orderRepository.save(order);

        this.emailService.sendOrderEmail(order);
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public boolean updateStatus(Long id, String status) {
        Optional<Order> byId = this.orderRepository.findById(id);

        if (byId.isEmpty()) {
            throw new NotFoundException("Order with id: " + id + " was not found");
        }
        Order order = byId.get();

        if (order.getStatus().equalsIgnoreCase(status)) {
            return false;
        }

        order.setStatus(setStatus(status));

        if ("confirm".equalsIgnoreCase(status)) {
            this.clothingService.setTotalSales(order.getItems());
        }

        this.orderRepository.save(order);
        return true;
    }

    @Override
    @Cacheable(value = "orders", key = "'findOrderById_' + #id")
    public OrdersDetailsDTO findOrderById(Long id) {
        return this.orderRepository.findById(id)
                .map(order -> {
                    OrdersDetailsDTO dto = this.modelMapper.map(order, OrdersDetailsDTO.class);
                    dto.setCustomer(order.getFirstName() + " " + order.getLastName());
                    dto.setItems(
                            dto.getItems().stream().peek(itemDTO -> {
                                ClothingDetailsPageDTO clothing = this.clothingService.findById(itemDTO.getClothingId());
                                if (clothing == null) {
                                    throw new NotFoundException("Clothing with id " + itemDTO.getClothingId() + " not found");
                                }
                                itemDTO.setName(getTypeOnBulgarian(clothing, itemDTO.getType()) + " " + clothing.getName());
                                itemDTO.setModel(clothing.getModel());
                            }).collect(Collectors.toList())
                    );
                    return dto;
                })
                .orElse(null);
    }

    @Override
    @Cacheable(value = "orders", key = "'getAllOrdersByStatus_' + #status + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderPageDTO> getAllOrdersByStatus(Pageable pageable, String status) {
        return this.orderRepository.findAllByStatusDto(status, pageable);
    }

    @Override
    @Cacheable(value = "orders", key = "'getAllOrders_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderPageDTO> getAllOrders(Pageable pageable) {
        return this.orderRepository.findAllOrderPageDTO(pageable);
    }

    @Override
    @Cacheable(value = "orders", key = "'findOrdersByUser_' + #userEmail + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<OrderPageDTO> findOrdersByUser(String userEmail, Pageable pageable) {
        return this.orderRepository.findOrdersByUserDto(userEmail, pageable);
    }

    private String setStatus(String status) {
        return switch (status.toLowerCase()) {
            case "confirm" -> "Confirmed";
            case "reject" -> "Rejected";
            default -> "Pending";
        };
    }

    private User validateUser(UserDTO userDTO) {
        if (userDTO == null) {
            throw new NotFoundException("User not found");
        }

        return userRepository.findByEmail(userDTO.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Order buildOrder(OrderDTO orderDTO, User user) {
        Order order = setOrderDetails(orderDTO, user);
        List<OrderItem> cart = buildOrderItems(orderDTO, order);

        order.setItems(cart);
        return order;
    }

    private Order setOrderDetails(OrderDTO orderDTO, User user) {
        this.phoneNumberUtils.validateBulgarianPhoneNumber(orderDTO.getPhoneNumber());
        return new Order(
                orderDTO.getFirstName(),
                orderDTO.getLastName(),
                orderDTO.getEmail(),
                orderDTO.getSelectedOffice() != null,
                orderDTO.getDeliveryCost(),
                orderDTO.getFinalPrice(),
                user,
                orderDTO.getSelectedOffice() != null ? orderDTO.getSelectedOffice() : orderDTO.getCity() + " (" + orderDTO.getRegion() + ") " + orderDTO.getAddress().trim(),
                this.phoneNumberUtils.formatPhoneNumber(orderDTO.getPhoneNumber()),
                orderDTO.getTotalPrice(),
                "Pending"
        );
    }

    private List<OrderItem> buildOrderItems(OrderDTO orderDTO, Order order) {
        List<Long> clothesIds = orderDTO.getCart().stream()
                .map(OrderItemDTO::getId)
                .toList();

        Map<Long, Clothing> clothesMap = this.clothRepository.findAllById(clothesIds)
                .stream()
                .collect(Collectors.toMap(Clothing::getId, Function.identity()));

        return orderDTO.getCart()
                .stream()
                .map(itemDTO -> {
                    Clothing clothing = clothesMap.get(itemDTO.getId());

                    if (clothing == null) {
                        throw new NotFoundException("Cloth with id: " + itemDTO.getId() + " not found");
                    }

                    OrderItem item = setOrderItemDetails(order, itemDTO, clothing);
                    order.getItems().add(item);

                    return item;
                })
                .toList();
    }

    private static OrderItem setOrderItemDetails(Order order, OrderItemDTO itemDTO, Clothing cloth) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setClothing(cloth);
        item.setSize(extractSize(itemDTO.getSize()));
        item.setGender(itemDTO.getGender());
        item.setType(extractType(itemDTO.getType()));
        item.setPrice(itemDTO.getPrice());
        item.setQuantity(itemDTO.getQuantity());
        return item;
    }

    protected static String extractSize(String input) {
        int index = input.indexOf(' ');

        if (index == -1) {
            index = input.indexOf('(');
        }

        return (index == -1) ? input : input.substring(0, index);
    }

    public static String extractType(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        int index = input.indexOf(" (");

        return (index == -1) ? input : input.substring(0, index);
    }

    protected static String getTypeOnBulgarian(ClothingDetailsPageDTO byId, String type) {
        String name;
        switch (byId.getType()) {
            case T_SHIRT -> {
                name = "Тениска с къс ръкав".equals(type) ? "Тениска" : "Блуза с дълъг ръкав";
            }
            case KIT -> name = "Комплект";
            case LONG_T_SHIRT -> name = "Блуза с дълъг ръкав";
            case SWEATSHIRT -> name = "Суитчър";
            case TOWELS -> name = "Плажна кърпа";
            case BANDANAS -> name = "Бандана";
            default -> name = "Къси панталони";
        }
        return name;
    }
}
