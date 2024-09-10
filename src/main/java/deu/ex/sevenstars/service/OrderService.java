package deu.ex.sevenstars.service;

import deu.ex.sevenstars.dto.OrderDTO;
import deu.ex.sevenstars.dto.PageRequestDTO;
import deu.ex.sevenstars.dto.ProductDTO;
import deu.ex.sevenstars.entity.OrderItem;
import deu.ex.sevenstars.entity.Orders;
import deu.ex.sevenstars.entity.Product;
import deu.ex.sevenstars.exception.OrderException;
import deu.ex.sevenstars.exception.ProductException;
import deu.ex.sevenstars.repository.OrderItemRepository;
import deu.ex.sevenstars.repository.OrderRepository;
import deu.ex.sevenstars.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderDTO insert(OrderDTO orderDTO) {
        try {
            Orders orders = orderDTO.toEntity();

            //
            List<OrderItem> orderItems = orderDTO.getOrderItems().stream()
                    .map(productDTO -> {

                        Product product = productRepository.findById(productDTO.getProductId())
                                .orElseThrow(() -> new RuntimeException("Product not found"));


                        OrderItem orderItem = OrderItem.builder()
                                .product(product)
                                .price(product.getPrice())
                                .category(product.getCategory())
                                .quantity(1)
                                .build();


                        orderItem.changeOrder(orders);

                        return orderItem;
                    })
                    .collect(Collectors.toList());

            orders.changeOrderItems(orderItems);

            Orders savedOrder = orderRepository.save(orders);
            return new OrderDTO(savedOrder); //주문과동시에 orderitmes
        } catch (DataIntegrityViolationException e) {
            throw OrderException.NOT_FOUND.get();
        } catch (Exception e) {
            log.error("예외 발생 코드 : " + e.getMessage());
            throw OrderException.NOT_REGISTERED.get();
        }
    }


    public OrderDTO read(Long orderId){
        Orders orders = orderRepository.findById(orderId).orElseThrow(OrderException.NOT_FOUND::get);
        log.info(orders);
        log.info(orders.getOrderItems());
        return new OrderDTO(orders);
    }

    public OrderDTO update(OrderDTO orderDTO){
        Orders orders = orderRepository.findById(orderDTO.getOrderId()).orElseThrow(OrderException.NOT_FOUND::get);

        try {
            orders.changeAddress(orderDTO.getAddress());
            orders.changePostcode(orders.getPostcode());

            return new OrderDTO(orders);
        } catch (Exception e){
            log.error("예외 발생 코드 : "+e.getMessage());
            throw OrderException.NOT_MODIFIED.get();
        }
    }

    public void delete(Long orderId){
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(OrderException.NOT_FOUND::get);
        try {
            orderRepository.delete(orders);
        } catch (Exception e){
            log.error("예외 발생 코드 : " + e.getMessage());
            throw OrderException.NOT_REMOVED.get();
        }
    }

    public Page<OrderDTO> page(PageRequestDTO pageRequestDTO){
        try {
            Sort sort = Sort.by("orderId").ascending();
            Pageable pageable = pageRequestDTO.getPageable(sort);
            return orderRepository.listDTO(pageable);
        }catch (Exception e){
            log.error("예외 코드 : " + e.getMessage());
            throw OrderException.NOT_FOUND.get();
        }
    }
}