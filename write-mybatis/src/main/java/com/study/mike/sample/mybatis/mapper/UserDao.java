package com.study.mike.sample.mybatis.mapper;

import java.util.List;

import com.study.mike.mybatis.annotations.Insert;
import com.study.mike.mybatis.annotations.Mapper;
import com.study.mike.mybatis.annotations.Select;
import com.study.mike.sample.mybatis.model.User;

@Mapper
public interface UserDao {

	@Insert("insert into t_user(id,name,sex,age) values(?,?,?,?)")
	void addUser(User user);

	@Select("select id,name,sex,age,address from t_user where sex = #{sex} order by #{orderColumn}")
	List<User> query(String sex, String orderColumn);

	void doSomething();
}
