package priv.gitonlie.flowable;


import java.util.List;

import org.flowable.task.api.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import priv.gitonlie.flowable.controller.PayController;
import priv.gitonlie.flowable.service.MyDemoService;
import priv.gitonlie.flowable.service.PayService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringbootFlowableApplicationTests {
//	@Autowired
//	private LeaveService service;
	/*
	 * @Autowired private MyDemoService service;
	 */
	@Autowired
	private PayService service;
	
	@Autowired
	private PayController C;
	
	@Test
	public void test() {
		String userId = "ngh424";
//		 String s = C.startProcess(userId); 
//		 System.out.println(s);
//		
		service.queryProcessInstance(userId);
	}
	//开始
	public void startup(String userId) {
		 String s = C.startProcess(userId); 
		 System.out.println(s);
	}
	//录入
	
}

