package io.oasp.application.mtsj.ordermanagement.logic.impl;

import io.oasp.application.mtsj.bookingmanagement.common.api.datatype.BookingType;
import io.oasp.application.mtsj.bookingmanagement.logic.api.Bookingmanagement;
import io.oasp.application.mtsj.bookingmanagement.logic.api.to.*;
import io.oasp.application.mtsj.dishmanagement.logic.api.to.DishEto;
import io.oasp.application.mtsj.dishmanagement.logic.api.to.IngredientEto;
import io.oasp.application.mtsj.dishmanagement.service.rest.DishServiceClient;
import io.oasp.application.mtsj.general.logic.base.AbstractComponentFacade;
import io.oasp.application.mtsj.ordermanagement.common.api.exception.*;
import io.oasp.application.mtsj.ordermanagement.dataaccess.api.OrderEntity;
import io.oasp.application.mtsj.ordermanagement.dataaccess.api.OrderLineEntity;
import io.oasp.application.mtsj.ordermanagement.dataaccess.api.dao.OrderDao;
import io.oasp.application.mtsj.ordermanagement.dataaccess.api.dao.OrderLineDao;
import io.oasp.application.mtsj.ordermanagement.logic.api.Ordermanagement;
import io.oasp.application.mtsj.ordermanagement.logic.api.to.*;
import io.oasp.module.jpa.common.api.to.PaginatedListTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of component interface of ordermanagement
 */
@Named
@Transactional
public class OrdermanagementImpl extends AbstractComponentFacade implements Ordermanagement {

  /**
   * Logger instance.
   */
  private static final Logger LOG = LoggerFactory.getLogger(OrdermanagementImpl.class);

  /**
   * @see #getOrderDao()
   */
  @Inject
  private OrderDao orderDao;

  /**
   * @see #getOrderLineDao()
   */
  @Inject
  private OrderLineDao orderLineDao;

  @Inject
  private Bookingmanagement bookingManagement;

  @Inject
  private DishServiceClient dishServiceClient;

  @Value("${client.port}")
  private int clientPort;

  @Value("${server.context-path}")
  private String serverContextPath;

  @Value("${mythaistar.hourslimitcancellation}")
  private int hoursLimit;

  /**
   * The constructor.
   */
  public OrdermanagementImpl() {

    super();
  }

  @Override
  public OrderCto findOrder(Long id) {

    LOG.debug("Get Order with id {} from database.", id);
    OrderEntity entity = getOrderDao().findOne(id);
    OrderCto cto = new OrderCto();
    cto.setBooking(getBeanMapper().map(entity.getBooking(), BookingEto.class));
    cto.setHost(getBeanMapper().map(entity.getHost(), BookingEto.class));
    cto.setOrderLines(getBeanMapper().mapList(entity.getOrderLines(), OrderLineCto.class));
    cto.setOrder(getBeanMapper().map(entity, OrderEto.class));
    cto.setInvitedGuest(getBeanMapper().map(entity.getInvitedGuest(), InvitedGuestEto.class));
    return cto;
  }

  public PaginatedListTo<OrderCto> findOrdersByPost(OrderSearchCriteriaTo criteria) {

    return findOrderCtos(criteria);
  }

  @Override
  public PaginatedListTo<OrderCto> findOrderCtos(OrderSearchCriteriaTo criteria) {

    return findOrderCtos(criteria, true);
  }

  private PaginatedListTo<OrderCto> findOrderCtos(OrderSearchCriteriaTo criteria, boolean fetchOrderLines) {

    criteria.limitMaximumPageSize(MAXIMUM_HIT_LIMIT);
    List<OrderCto> ctos = new ArrayList<>();
    PaginatedListTo<OrderEntity> orders = getOrderDao().findOrders(criteria);
    for (OrderEntity order : orders.getResult()) {
      OrderCto cto = new OrderCto();
      cto.setBooking(getBeanMapper().map(order.getBooking(), BookingEto.class));
      cto.setHost(getBeanMapper().map(order.getHost(), BookingEto.class));
      cto.setInvitedGuest(getBeanMapper().map(order.getInvitedGuest(), InvitedGuestEto.class));
      cto.setOrder(getBeanMapper().map(order, OrderEto.class));
      cto.setOrderLines(getBeanMapper().mapList(order.getOrderLines(), OrderLineCto.class));
      List<OrderLineCto> orderLinesCto = new ArrayList<>();
      for (OrderLineEntity orderLine : order.getOrderLines()) {
        OrderLineCto orderLineCto = new OrderLineCto();

        if (fetchOrderLines) {
            try {
                orderLineCto.setDish(getBeanMapper().map(this.dishServiceClient.getDish(orderLine.getDishId()), DishEto.class));
            } catch (Exception e) {
                orderLineCto.setDish(null);
            }
            try {
                orderLineCto.setExtras(getBeanMapper().mapList(this.dishServiceClient.getIngredients(orderLine.getExtras()), IngredientEto.class));
            } catch (Exception e) {
                orderLineCto.setExtras(null);
            }
        }
        orderLineCto.setOrderLine(getBeanMapper().map(orderLine, OrderLineEto.class));
        orderLinesCto.add(orderLineCto);
      }
      cto.setOrderLines(orderLinesCto);
      ctos.add(cto);
    }

    return new PaginatedListTo<>(ctos, orders.getPagination());
  }

  @Override
  public boolean deleteOrder(Long orderId) {

    OrderEntity order = getOrderDao().find(orderId);

    if (!cancellationAllowed(order)) {
      throw new CancelNotAllowedException();
    }
    OrderLineSearchCriteriaTo criteria = new OrderLineSearchCriteriaTo();
    criteria.setOrderId(order.getId());
    List<OrderLineEntity> orderLines = getOrderLineDao().findOrderLines(criteria).getResult();

    for (OrderLineEntity orderLine : orderLines) {
      getOrderLineDao().delete(orderLine.getId());
    }
    getOrderDao().delete(order);
    LOG.debug("The order with id '{}' has been deleted.", orderId);

    return true;
  }

  @Override
  public OrderEto saveOrder(OrderCto order) {

    Objects.requireNonNull(order, "order");
    List<OrderLineCto> linesCto = order.getOrderLines();
    List<OrderLineEntity> orderLineEntities = new ArrayList<>();
    for (OrderLineCto lineCto : linesCto) {
      OrderLineEntity orderLineEntity = getBeanMapper().map(lineCto, OrderLineEntity.class);
      orderLineEntity.setExtras(getBeanMapper().mapList(lineCto.getExtras(), Long.class));
      orderLineEntity.setDishId(lineCto.getOrderLine().getDishId());
      orderLineEntity.setAmount(lineCto.getOrderLine().getAmount());
      orderLineEntity.setComment(lineCto.getOrderLine().getComment());
      orderLineEntities.add(orderLineEntity);
    }

    OrderEntity orderEntity = getBeanMapper().map(order, OrderEntity.class);
    String token = orderEntity.getBooking().getBookingToken();
    // initialize, validate orderEntity here if necessary
    orderEntity = getValidatedOrder(orderEntity.getBooking().getBookingToken(), orderEntity);
    orderEntity.setOrderLines(orderLineEntities);
    OrderEntity resultOrderEntity = getOrderDao().save(orderEntity);
    LOG.debug("Order with id '{}' has been created.", resultOrderEntity.getId());

    for (OrderLineEntity orderLineEntity : orderLineEntities) {
      orderLineEntity.setOrderId(resultOrderEntity.getId());
      OrderLineEntity resultOrderLine = getOrderLineDao().save(orderLineEntity);
      LOG.info("OrderLine with id '{}' has been created.", resultOrderLine.getId());
    }

//    sendOrderConfirmationEmail(token, resultOrderEntity);

    return getBeanMapper().map(resultOrderEntity, OrderEto.class);
  }

  /**
   * Returns the field 'orderDao'.
   *
   * @return the {@link OrderDao} instance.
   */
  public OrderDao getOrderDao() {

    return this.orderDao;
  }

  @Override
  public OrderLineEto findOrderLine(Long id) {

    LOG.debug("Get OrderLine with id {} from database.", id);
    return getBeanMapper().map(getOrderLineDao().findOne(id), OrderLineEto.class);
  }

  @Override
  public PaginatedListTo<OrderLineCto> findOrderLineCtos(OrderLineSearchCriteriaTo criteria) {

    criteria.limitMaximumPageSize(MAXIMUM_HIT_LIMIT);
    PaginatedListTo<OrderLineEntity> orderlines = getOrderLineDao().findOrderLines(criteria);
    List<OrderLineCto> orderLinesCto = new ArrayList<>();
    for (OrderLineEntity orderline : orderlines.getResult()) {
      OrderLineCto orderLineCto = new OrderLineCto();
      orderLineCto.setOrderLine(getBeanMapper().map(this.orderLineDao.findOne(orderline.getId()), OrderLineEto.class));
      orderLineCto.setExtras(getBeanMapper().mapList(orderline.getExtras(), IngredientEto.class));
      orderLinesCto.add(orderLineCto);
    }
    return new PaginatedListTo<>(orderLinesCto, orderlines.getPagination());
  }

  @Override
  public boolean deleteOrderLine(Long orderLineId) {

    OrderLineEntity orderLine = getOrderLineDao().find(orderLineId);
    getOrderLineDao().delete(orderLine);
    LOG.debug("The orderLine with id '{}' has been deleted.", orderLineId);
    return true;
  }

  @Override
  public OrderLineEto saveOrderLine(OrderLineEto orderLine) {

    Objects.requireNonNull(orderLine, "orderLine");
    OrderLineEntity orderLineEntity = getBeanMapper().map(orderLine, OrderLineEntity.class);

    // initialize, validate orderLineEntity here if necessary
    OrderLineEntity resultEntity = getOrderLineDao().save(orderLineEntity);
    LOG.debug("OrderLine with id '{}' has been created.", resultEntity.getId());

    return getBeanMapper().map(resultEntity, OrderLineEto.class);
  }

  /**
   * Returns the field 'orderLineDao'.
   *
   * @return the {@link OrderLineDao} instance.
   */
  public OrderLineDao getOrderLineDao() {

    return this.orderLineDao;
  }

  private OrderEntity getValidatedOrder(String token, OrderEntity orderEntity) {

    // BOOKING VALIDATION
    if (getOrderType(token) == BookingType.COMMON) {
      BookingCto booking = getBooking(token);
      if (booking == null) {
        throw new NoBookingException();
      }
      List<OrderCto> currentOrders = getBookingOrders(booking.getBooking().getId());
      if (!currentOrders.isEmpty()) {
        throw new OrderAlreadyExistException();
      }
      orderEntity.setBookingId(booking.getBooking().getId());

      // GUEST VALIDATION
    } else if (getOrderType(token) == BookingType.INVITED) {

      InvitedGuestEto guest = getInvitedGuest(token);
      if (guest == null) {
        throw new NoInviteException();
      }
      List<OrderCto> currentGuestOrders = getInvitedGuestOrders(guest.getId());
      if (!currentGuestOrders.isEmpty()) {
        throw new OrderAlreadyExistException();
      }
      orderEntity.setBookingId(guest.getBookingId());
      orderEntity.setInvitedGuestId(guest.getId());
    }

    return orderEntity;
  }

  private BookingType getOrderType(String token) throws WrongTokenException {

    if (token.startsWith("CB_")) {
      return BookingType.COMMON;
    } else if (token.startsWith("GB_")) {
      return BookingType.INVITED;
    } else {
      throw new WrongTokenException();
    }
  }

  private BookingCto getBooking(String token) {

    BookingSearchCriteriaTo criteria = new BookingSearchCriteriaTo();
    criteria.setBookingToken(token);
    PaginatedListTo<BookingCto> booking = this.bookingManagement.findBookingCtos(criteria);
    return booking.getResult().isEmpty() ? null : booking.getResult().get(0);
  }

  private List<OrderCto> getBookingOrders(Long idBooking) {

    OrderSearchCriteriaTo criteria = new OrderSearchCriteriaTo();
    criteria.setBookingId(idBooking);
    return findOrderCtos(criteria, false).getResult();
  }

  private InvitedGuestEto getInvitedGuest(String token) {

    InvitedGuestSearchCriteriaTo criteria = new InvitedGuestSearchCriteriaTo();
    criteria.setGuestToken(token);
    PaginatedListTo<InvitedGuestEto> guest = this.bookingManagement.findInvitedGuestEtos(criteria);
    return guest.getResult().isEmpty() ? null : guest.getResult().get(0);
  }

  private List<OrderCto> getInvitedGuestOrders(Long idInvitedGuest) {

    OrderSearchCriteriaTo criteria = new OrderSearchCriteriaTo();
    criteria.setInvitedGuestId(idInvitedGuest);
    return findOrderCtos(criteria).getResult();
  }

  private String getContentFormatedWithCost(OrderEntity order) {
    return "null";

//    OrderLineSearchCriteriaTo criteria = new OrderLineSearchCriteriaTo();
//    criteria.setOrderId(order.getId());
//    List<OrderLineEntity> orderLines = this.orderLineDao.findOrderLines(criteria).getResult();

// TODO: Implement microservice communication

//    StringBuilder sb = new StringBuilder();
//    sb.append("\n");
//    BigDecimal finalPrice = BigDecimal.ZERO;
//    for (OrderLineEntity orderLine : orderLines) {
//      // 1. Get dish from mts-dishes
//      // 2.
//
//      DishCto dishCto = this.dishManagement.findDish(orderLine.getDishId());
//      List<IngredientEto> extras = dishCto.getExtras();
//      Set<IngredientEto> set = new HashSet<>();
//      set.addAll(extras);
//      extras.clear();
//      extras.addAll(set);
//      // dish name
//      BigDecimal linePrice = BigDecimal.ZERO;
//      sb.append(dishCto.getDish().getName()).append(", x").append(orderLine.getAmount());
//      // dish cost
//      BigDecimal dishCost = dishCto.getDish().getPrice().multiply(new BigDecimal(orderLine.getAmount()));
//      linePrice = dishCost;
//      // dish selected extras
//      sb.append(". Extras: ");
//      for (Ingredient extra : extras) {
//        for (Ingredient selectedExtra : orderLine.getExtras()) {
//          if (extra.getId().equals(selectedExtra.getId())) {
//            sb.append(extra.getName()).append(",");
//            linePrice = linePrice.add(extra.getPrice());
//            break;
//          }
//        }
//      }
//
//      // dish cost
//      sb.append(" ==>").append(". Dish cost: ").append(linePrice.toString());
//      sb.append("\n");
//      // increase the finalPrice of the order
//      finalPrice = finalPrice.add(linePrice);
//    }
//
//    return sb.append("Total Order cost: ").append(finalPrice.toString()).toString();
  }

  private String getBookingOrGuestEmail(String token) {

    // Get the Host email
    if (getOrderType(token) == BookingType.COMMON) {
      BookingCto booking = getBooking(token);
      if (booking == null) {
        throw new NoBookingException();
      }
      return booking.getBooking().getEmail();

      // Get the Guest email
    } else if (getOrderType(token) == BookingType.INVITED) {

      InvitedGuestEto guest = getInvitedGuest(token);
      if (guest == null) {
        throw new NoInviteException();
      }
      return guest.getEmail();
    } else {
      return null;
    }
  }

  private boolean cancellationAllowed(OrderEntity order) {

    BookingCto booking = this.bookingManagement.findBooking(order.getBookingId());
    Timestamp bookingTime = booking.getBooking().getBookingDate();
    Long bookingTimeMillis = bookingTime.getTime();
    Long cancellationLimit = bookingTimeMillis - (3600000 * this.hoursLimit);
    Long now = Timestamp.from(Instant.now()).getTime();

    return (now > cancellationLimit) ? false : true;
  }
}
