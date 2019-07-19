package toys.test;

import com.toys.dao.UserDao;
import com.toys.entity.User;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.type.TypeHandler;
import org.junit.Test;

public class MyTest {

  @Test
  public void findUserById() {
    SqlSession sqlSession = getSessionFactory().openSession();
    UserDao userMapper = sqlSession.getMapper(UserDao.class);
    User user = userMapper.findUserById(2);
    System.out.println(userMapper);
  }

  //Mybatis 通过SqlSessionFactory获取SqlSession, 然后才能通过SqlSession与数据库进行交互
  private static SqlSessionFactory getSessionFactory() {
    SqlSessionFactory sessionFactory = null;
    String resource = "configuration.xml";
    try {
      sessionFactory = new SqlSessionFactoryBuilder().build(Resources
          .getResourceAsReader(resource));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sessionFactory;
  }
}
