package activiti;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.identity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings("deprecation")
@RunWith(value=SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:activiti/spring.activiti.xml"})
@TransactionConfiguration(transactionManager="transactionManager",defaultRollback=false)
@Transactional
public class UserTest extends AbstractTransactionalJUnit4SpringContextTests{
	@Autowired
	private ProcessEngine processEngine;
	
	@Test
	public void testUser() throws Exception {
		IdentityService identityService = processEngine.getIdentityService();
		List<User> users = identityService.createUserQuery().list();
		for (Iterator iterator = users.iterator(); iterator.hasNext();) {
			User user = (User) iterator.next();
			System.out.println(user.getId());
		}
	}
}
