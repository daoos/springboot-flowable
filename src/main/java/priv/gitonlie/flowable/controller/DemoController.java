package priv.gitonlie.flowable.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import priv.gitonlie.flowable.service.MyDemoService;

@RestController
public class DemoController {
	@Autowired
	private MyDemoService demoService;
	
	@RequestMapping("/startup")
	public String startUp(String userId) throws Exception{
		return demoService.startUp(userId);		
	}
	
	@RequestMapping("/queryTaskList")
	public String query(String userId){
		return demoService.query(userId);	
	}
	
	@RequestMapping("/agree")
	public String agree(String taskId) {
		return demoService.agree(taskId);
	}
	
	@RequestMapping("/reject")
	public String reject(String taskId) {
		return demoService.reject(taskId);
	}
	
	@RequestMapping("/diagram")
	public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception{
		demoService.genProcessDiagram(httpServletResponse, processId);
	}
}
