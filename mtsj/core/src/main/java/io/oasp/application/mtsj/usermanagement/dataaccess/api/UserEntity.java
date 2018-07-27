package io.oasp.application.mtsj.usermanagement.dataaccess.api;

import io.oasp.application.mtsj.bookingmanagement.dataaccess.api.BookingEntity;
import io.oasp.application.mtsj.general.dataaccess.api.ApplicationPersistenceEntity;
import io.oasp.application.mtsj.usermanagement.common.api.User;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "User")
public class UserEntity extends ApplicationPersistenceEntity implements User {

  private String username;

  private String password;

  private String email;

  private UserRoleEntity userRole;

  private List<BookingEntity> bookings;

  private List<Long> favourites;

  private static final long serialVersionUID = 1L;

  /**
   * @return username
   */
  @Override
  public String getUsername() {

    return this.username;
  }

  /**
   * @param username new value of {@link #getusername}.
   */
  @Override
  public void setUsername(String username) {

    this.username = username;
  }

  /**
   * @return password
   */
  @Override
  public String getPassword() {

    return this.password;
  }

  /**
   * @param password new value of {@link #getpassword}.
   */
  @Override
  public void setPassword(String password) {

    this.password = password;
  }

  /**
   * @return email
   */
  @Override
  public String getEmail() {

    return this.email;
  }

  /**
   * @param email new value of {@link #getemail}.
   */
  @Override
  public void setEmail(String email) {

    this.email = email;
  }

  /**
   * @return userRole
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "idRole")
  public UserRoleEntity getUserRole() {

    return this.userRole;
  }

  /**
   * @param userRole new value of {@link #getuserRole}.
   */
  public void setUserRole(UserRoleEntity userRole) {

    this.userRole = userRole;
  }

  /**
   * @return favourites
   */
  @ElementCollection
  @CollectionTable(name = "UserFavourite", joinColumns = @JoinColumn(name = "idUser"))
  @Column(name = "idDish")
  public List<Long> getFavourites() {

    return this.favourites;
  }

  /**
   * @param favourites new value of {@link #getfavourites}.
   */
  public void setFavourites(List<Long> favourites) {

    this.favourites = favourites;
  }

  /**
   * @return bookings
   */
  @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
  public List<BookingEntity> getBookings() {

    return this.bookings;
  }

  /**
   * @param bookings new value of {@link #getbookings}.
   */
  public void setBookings(List<BookingEntity> bookings) {

    this.bookings = bookings;
  }

  @Override
  @Transient
  public Long getUserRoleId() {

    if (this.userRole == null) {
      return null;
    }
    return this.userRole.getId();
  }

  @Override
  public void setUserRoleId(Long userRoleId) {

    if (userRoleId == null) {
      this.userRole = null;
    } else {
      UserRoleEntity userRoleEntity = new UserRoleEntity();
      userRoleEntity.setId(userRoleId);
      this.userRole = userRoleEntity;
    }
  }

}
