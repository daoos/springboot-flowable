package priv.gitonlie.flowable.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
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
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
public class MyDemoService {
	
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
	//启动流程
	public String startUp(String userId) throws Exception {
		String processId = "demo";
		Map<String, Object> map = new HashMap<String, Object>();       
    	map.put("userId", userId);
        map.put("status", "0");
        //流程发起人
    	ProcessInstance pro = null; 	
    	try {
    		  identityService.setAuthenticatedUserId(userId);
    		  pro = runtimeService.startProcessInstanceByKey(processId, map);
    		  Log.info("流程ID:{}",pro.getId());
    		  
    	}catch (Exception e) {
    		e.printStackTrace();
    		throw new Exception(e);
		}finally {
    		  identityService.setAuthenticatedUserId(null);
    	}
    	String msg = userId+"启动请假工作流成功";
		return msg;		
	}
	
	//查询待办
	public String query(String userId) {
		String msg = "";
		List<Task> taskList = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
		for(Task task:taskList) {
			if(msg!="") {
				msg = msg+",";
			}
			msg = msg+task.getId()+","+task.getName();
		}
		Log.info(msg);
		return msg;		
	}
	
	//同意请假
	public String agree(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			throw new RuntimeException("流程不存在");
		}
		// 通过审核
		HashMap<String, Object> map = new HashMap<>();
		map.put("status", "1");
		taskService.complete(taskId, map);
		String msg = "同意请假";
		Log.info(msg);
		archive(taskId);
		return msg;
	}
	
	//同意请假
	public String reject(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			throw new RuntimeException("流程不存在");
		}
		// 通过审核
		HashMap<String, Object> map = new HashMap<>();
		map.put("status", "-1");
		taskService.complete(taskId, map);
		String msg = "驳回请假";
		Log.info(msg);
		return msg;
	}
	
	//归档
	public void archive(String taskId) {
		TaskQuery q = taskService.createTaskQuery().taskAssignee("personnal").taskId(taskId);
		Task task = q.singleResult();
		if (task == null) {
			throw new RuntimeException("流程不存在");
		}
    	taskService.complete(task.getId());
    }
	//流程活动
	 public void queryActHistory(String processId) {
	    	Log.info("======================================================================");
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
	    	HistoricActivityInstanceQuery activity = historyService.createHistoricActivityInstanceQuery();
	    	List<HistoricActivityInstance> act = activity.processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();
	    	for(HistoricActivityInstance actIns:act) {
	    		Log.info("{},{},{},{},{}",sdf.format(actIns.getStartTime()),actIns.getActivityName(),actIns.getActivityType(),actIns.getAssignee(),actIns.getActivityId());			
	    	}	    	
	  }
	 //流程变量
	 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	 public void queryVars(String processId) {
		 	HistoricVariableInstanceQuery varQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(processId);
	    	List<HistoricVariableInstance> varList = varQuery.list();
	    	Log.info("已完成流程的变量实例Variable：{},",varList.size());
	    	for(HistoricVariableInstance vars:varList) {
	    		Log.info("创建时间:{},变量ID：{},{},{},{},变量任务ID:{}",sdf.format(vars.getCreateTime()),vars.getId(),vars.getValue(),vars.getVariableName(),vars.getVariableTypeName(),vars.getTaskId());
	    	}
	 }
	 //流程历史
	 public void queryProcessHistory() {
		//HistoricProcessInstanceQuery 查询历史流程实例
	    	HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
	    	//历史实例
	    	List<HistoricProcessInstance> list = query.processDefinitionKey("demo").list();
	    	Log.info("历史流程实例process:{}个",list.size());
	    	for(HistoricProcessInstance instance:list) {
	    		Log.info("{}",instance.getId());
	    		queryActHistory(instance.getId());
	    		queryVars(instance.getId());
	    	}
	 }
	 
	 public void queryProcessHistory(String userId) {
			//HistoricProcessInstanceQuery 查询历史流程实例
		    	HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
		    	//历史实例
		    	List<HistoricProcessInstance> list = query.processDefinitionKey("demo").list();
		    	//
//		    	runtimeService.createProcessInstanceQuery().startedBy(userId).orderByStartTime().asc().list();
		    	
		    	Log.info("历史流程实例process:{}个",list.size());
		    	for(HistoricProcessInstance instance:list) {
		    		Log.info("{}",instance.getId());
		    		queryActHistory(instance.getId());
		    		queryVars(instance.getId());
		    	}
		 }
	
	/**
	 * 生成流程图
	 *
	 * @param processId 任务ID
	 */
	@RequestMapping(value = "processDiagram")
	public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
		ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

		// 流程走完的不显示图
		if (pi == null) {
			return;
		}
		Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
		// 使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
		String InstanceId = task.getProcessInstanceId();
		List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(InstanceId).list();

		// 得到正在执行的Activity的Id
		List<String> activityIds = new ArrayList<>();
		List<String> flows = new ArrayList<>();
		for (Execution exe : executions) {
			List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
			activityIds.addAll(ids);
		}

		// 获取流程图
		BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
		ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
		ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
		InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows,
				engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(),
				engconf.getClassLoader(), 1.0, false);
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
