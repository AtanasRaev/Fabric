package bg.tshirt.database.repository;

import bg.tshirt.database.dto.order.OrderPageDTO;
import bg.tshirt.database.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT new bg.tshirt.database.dto.order.OrderPageDTO(" +
            "o.id, o.firstName, o.lastName, o.status, o.totalPrice, SUM(i.quantity), o.createdAt) " +
            "FROM Order o LEFT JOIN o.items i " +
            "WHERE o.status = :status " +
            "GROUP BY o.id, o.firstName, o.lastName, o.status, o.totalPrice, o.createdAt")
    Page<OrderPageDTO> findAllByStatusDto(@Param("status") String status, Pageable pageable);


    @Query("SELECT new bg.tshirt.database.dto.order.OrderPageDTO(o.id, o.firstName, o.lastName, o.status, o.totalPrice, SUM(i.quantity), o.createdAt) " +
            "FROM Order o LEFT JOIN o.items i " +
            "WHERE o.user.email = :userEmail " +
            "GROUP BY o.id, o.firstName, o.lastName, o.status, o.totalPrice, o.createdAt")
    Page<OrderPageDTO> findOrdersByUserDto(@Param("userEmail") String userEmail, Pageable pageable);


    @Query("SELECT new bg.tshirt.database.dto.order.OrderPageDTO(o.id, o.firstName, o.lastName, o.status, o.totalPrice, SUM(i.quantity), o.createdAt) " +
            "FROM Order o LEFT JOIN o.items i " +
            "GROUP BY o.id, o.firstName, o.lastName, o.status, o.totalPrice, o.createdAt")
    Page<OrderPageDTO> findAllOrderPageDTO(Pageable pageable);


}
