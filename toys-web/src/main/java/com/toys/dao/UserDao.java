package com.toys.dao;

import com.toys.entity.User;
import java.util.List;


public interface UserDao {

  public void insertUser (User user);

  public void updateUser (User user);

  public void deleteUser (User user);

  public User findUserById (int userId);

  public List<User> findAllUsers();

}