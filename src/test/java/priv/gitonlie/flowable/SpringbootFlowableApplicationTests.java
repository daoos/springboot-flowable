package priv.gitonlie.flowable;


import java.util.List;

import org.flowable.task.api.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import priv.gitonlie.flowable.service.MyDemoService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringbootFlowableApplicationTests {
//	@Autowired
//	private LeaveService service;
	@Autowired
	private MyDemoService service;
	@Test
	public void contextLoads() throws Exception {
		//开启流程
//		service.start();
//		service.queryTask("123456");
		//人事审批
//		service.approval("人事", true);
		//领导审批
		//service.leaderApproval("人事", true);
		//归档
//		service.archive("人事");
//		service.queryHistory("123456");
//		service.queryNode();
//		service.queryActHistory2("7dc0cefa-23c8-11e9-895a-507b9dc37214");//703c102f-23c3-11e9-bd38-507b9dc37214
//		service.queryActHistory("703c102f-23c3-11e9-bd38-507b9dc37214");//703c102f-23c3-11e9-bd38-507b9dc37214
		
		//启动
//		String p = service.startUp("123");
//		System.out.println(p);
		//查询待办
//		String  taskList = service.query("244235235");
//		System.out.println("--------->"+taskList);
//[Task[id=87c68231-2483-11e9-87aa-507b9dc37214, name=请假], Task[id=91856f0c-2482-11e9-ba7a-507b9dc37214, name=请假]]
		//同意请假
//		String msg = service.agree("87c68231-2483-11e9-87aa-507b9dc37214");
//		System.out.println(msg);
		//驳回请假
//		String msg = service.reject("91856f0c-2482-11e9-ba7a-507b9dc37214");
//		System.out.println(msg);
		//归档
	service.archive("b5bf2db7-248b-11e9-960d-507b9dc37214");
//		service.queryProcessHistory();
	}

}

