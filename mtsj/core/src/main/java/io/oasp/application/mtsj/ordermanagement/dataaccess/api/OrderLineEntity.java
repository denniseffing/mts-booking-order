package io.oasp.application.mtsj.ordermanagement.dataaccess.api;

import io.oasp.application.mtsj.general.dataaccess.api.ApplicationPersistenceEntity;
import io.oasp.application.mtsj.ordermanagement.common.api.OrderLine;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link ApplicationPersistenceEntity Entity} that represents a single {@link OrderLine} of an {@link OrderEntity}.
 */
@Entity
@Table(name = "OrderLine")
public class OrderLineEntity extends ApplicationPersistenceEntity implements OrderLine {

  private OrderEntity order;

  private Long dishId;

  private List<Long> extras;

  private Integer amount;

  private String comment;

  private static final long serialVersionUID = 1L;

  /**
   * @return order
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "idOrder")
  public OrderEntity getOrder() {

    return this.order;
  }

  /**
   * @param order new value of {@link #getOrder}.
   */
  public void setOrder(OrderEntity order) {

    this.order = order;
  }

  /**
   * @return extras
   */
  @ElementCollection
  @CollectionTable(name = "OrderDishExtraIngredient", joinColumns = @JoinColumn(name = "idOrderLine"))
  @Column(name = "idIngredient")
  public List<Long> getExtras() {

    return this.extras;
  }

  /**
   * @param extras new value of {@link #getExtras}.
   */
  public void setExtras(List<Long> extras) {

    this.extras = new LinkedList<>(extras);
  }

  /**
   * @return amount
   */
  @Override
  public Integer getAmount() {

    return this.amount;
  }

  /**
   * @param amount new value of {@link #getAmount}.
   */
  @Override
  public void setAmount(Integer amount) {

    this.amount = amount;
  }

  /**
   * @return comment
   */
  @Override
  public String getComment() {

    return this.comment;
  }

  /**
   * @param comment new value of {@link #getComment}.
   */
  @Override
  public void setComment(String comment) {

    this.comment = comment;
  }

  @Override
  @Transient
  public Long getOrderId() {

    if (this.order == null) {
      return null;
    }
    return this.order.getId();
  }

  @Override
  public void setOrderId(Long orderId) {

    if (orderId == null) {
      this.order = null;
    } else {
      OrderEntity orderEntity = new OrderEntity();
      orderEntity.setId(orderId);
      this.order = orderEntity;
    }
  }

  @Override
  @Column(name = "idDish")
  public Long getDishId() {

    if (this.dishId == null) {
      return null;
    }
    return this.dishId;
  }

  @Override
  public void setDishId(Long dishId) {

    if (dishId == null) {
      this.dishId = null;
    } else {
      this.dishId = dishId;
    }
  }
}
