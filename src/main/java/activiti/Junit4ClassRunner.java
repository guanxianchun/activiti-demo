package activiti;

import java.io.FileNotFoundException;

import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Log4jConfigurer;

@SuppressWarnings("deprecation")
public class Junit4ClassRunner extends SpringJUnit4ClassRunner {
	
	static{
		try {
			Log4jConfigurer.initLogging("classpath:log4j.properties");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public Junit4ClassRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		// TODO Auto-generated constructor stub
	}

}
