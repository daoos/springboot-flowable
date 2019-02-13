package priv.gitonlie.flowable.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import priv.gitonlie.flowable.service.LeaveService;

@RestController
@RequestMapping("/parallel")
public class ParallelGatewayController {
	
private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	
	@Autowired
	private IdentityService identityService;
	@Autowired
    private RuntimeService runtimeService;
	@Autowired
    private TaskService taskService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private HistoryService historyService;
	
	@RequestMapping("/start")
	public String start(String sel,double amount) {
		Map<String, Object> map = new HashMap<String, Object>();       
    	map.put("name", "火箭配件");
    	map.put("condition",sel);
    	map.put("count", amount);
        //流程发起人
    	ProcessInstance pro = null;   	
    	try {
    		  identityService.setAuthenticatedUserId("上级领导");
    		  pro = runtimeService.startProcessInstanceByKey("myProcess", map);
    		  Log.info("流程ID:{}",pro.getId());   		  
    	} finally {
    		  identityService.setAuthenticatedUserId(null);
    	}
		return "火箭生产流程{"+pro.getId()+"}开始";	
	}
	
	@RequestMapping("/puttask")
	public String puttask() {
		List<Task> taskList = taskService.createTaskQuery().taskAssignee("上级领导").orderByTaskCreateTime().desc().list();
		for(Task task:taskList) {
    		Log.info("任务:{}",task.toString());
    		taskService.complete(task.getId());
    	}  
		return "上级领导提出任务";	
	}
	
	@RequestMapping("/event")
	public String event() {
		runtimeService.signalEventReceived("alert");
		return "发送信号";		
	}
	
	@RequestMapping("/pulltask")
	public String pulltask(String pull) {
		String assignee = "";
		if("A组".equals(pull)) {
			assignee = "A组";
		}else if("B组".equals(pull)) {
			assignee = "B组";
		}else if("组装".equals(pull)) {
			assignee = "组装车间";
		}else if("分部".equals(pull)) {
			assignee = "分部";
		}else if("主管".equals(pull)) {
			assignee = "主管";
		}else if("经理".equals(pull)) {
			assignee = "经理";
		}else if("总工".equals(pull)) {
			assignee = "总工";
		}else if("老板".equals(pull)) {
			assignee = "老板";
		}else if("定时".equals(pull)) {
			assignee = "定时";
		}else if("消息".equals(pull)) {
			assignee = "消息";
		}else if("事件".equals(pull)) {
			assignee = "事件";
		}else if("定时边界".equals(pull)) {
			assignee = "定时边界";
		}else if("补偿".equals(pull)) {
			assignee = "补偿";
		}
		List<Task> taskList = taskService.createTaskQuery().taskAssignee(assignee).orderByTaskCreateTime().desc().list();
		for(Task task:taskList) {
    		Log.info("任务:{}",task.toString());
    		taskService.complete(task.getId());
    	}  
		return assignee+"处理任务";	
	}
	
	 public boolean isFinished(String processInstanceId) {
	        return historyService.createHistoricProcessInstanceQuery().finished()
	                .processInstanceId(processInstanceId).count() > 0;
	  }
	/**
	 * 生成流程图
	 *
	 * @param processId 任务ID
	 */
	@RequestMapping(value = "processDiagram")
	public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId,String flag) throws Exception {
		String pis = "";
		List<String> activityIds = new ArrayList<String>();
		List<String> flows = new ArrayList<String>();
		
		if(this.isFinished(processId) || "1".equals(flag)) {//已结束流程
			HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processId).singleResult();
			pis = hpi.getProcessDefinitionId();
			List<HistoricActivityInstance> activityList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processId).list();
			for(HistoricActivityInstance act:activityList) {
				activityIds.add(act.getActivityId());
				if(act.getActivityType().equals("sequenceFlow")) {
					flows.add(act.getActivityId());
				}
			}
		}else {//未结束流程
			 ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult(); 
			  pis=pi.getProcessDefinitionId();
			  List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processId).list();
			  for (Execution exe : executions) { 
					  List<String> ids = runtimeService.getActiveActivityIds(exe.getId()); 
					  activityIds.addAll(ids); 
			  }
		}
		 	
		// 获取流程图
		BpmnModel bpmnModel = repositoryService.getBpmnModel(pis);
		ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
		ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
		InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows,
				engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(),
				engconf.getClassLoader(), 1.0, true);
		OutputStream out = null;
		byte[] buf = new byte[1024];
		int legth = 0;
		try {
			out = httpServletResponse.getOutputStream();
			while ((legth = in.read(buf)) != -1) {
				out.write(buf, 0, legth);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}
}
