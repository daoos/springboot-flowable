package priv.gitonlie.flowable.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 废弃
 * @author Administrator
 *
 */
@RestController
//@RequestMapping("flowable")
public class FlowableTemplateController {
	
	private Logger Log = LoggerFactory.getLogger(FlowableTemplateController.class);
	
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private IdentityService identityService;
	@Autowired
	private HistoryService historyService;
	
//	@RequestMapping("process")
	public Map<String,Object> forwardCenter(@RequestParam Map<String,Object> map) {
		Map<String,Object> result = null;
		String processName = (String) map.get("processName");
		//根据传入的流程名称进行下一步操作
		switch (processName) {
		case "start":
			result = startUp(map);//开启流程
			break;
		case "initAndsub":
			result = initiate(map);//开启流程并驱动第一个流程节点
			break;
		case "queryTask":
			result = queryTask(map);//查询任务
			break;
		case "claimTask":
			result = claimTask(map);//申领任务
			break;
		case "completeTask":
			result = completeTask(map);//驱动任务
			break;
		case "delInst":
			result = deleteProcessInstance(map);//删除实例
			break;
		case "allNode":
			result = allNode(map);//所有流程节点
			break;
		case "historyNode":
			result = historyNode(map);//历史流程节点
			break;
		case "currentNode":
			result = currentNode(map);//当前流程节点
			break;
		default:
			break;
		}
		
		return result;
	}
	
	/**
	 *  启动流程
	 * @param map
	 * @return
	 */
	public Map<String,Object> startUp(Map<String,Object> map) {
		//需要userId、流程Id
		Map<String,Object> result = new HashMap<String,Object>();
		String userId = (String) map.get("userId");
		String processDefinitionKey = (String) map.get("processKey");
		String businessKey = (String) map.get("businessKey");
		//提出多余字段保留必要字段
		map.remove("processKey");
		map.remove("businessKey");
		
		ProcessInstance processInstance = null;
		String processInstanceId = null;
		try {
			//设置流程发起人
			identityService.setAuthenticatedUserId(userId);
//			processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, map);
			processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, map);
			processInstanceId = processInstance.getId();
			Log.info("userId:{}>开启流程ID:{}",userId,processInstanceId);
			result.put("processInstanceId", processInstanceId);
		}finally {
			  identityService.setAuthenticatedUserId(null);
		}	
		return result;
	}
	
	/**
	 * 开启流程并驱动第一个流程节点
	 * @param map
	 * @return
	 */
	public Map<String,Object> initiate(Map<String,Object> map){
		Map<String,Object> startMap = startUp(map);
		completeTask(startMap);
		return startMap;
		
	}
	
	/**
	 * 	查询任务
	 * @param map
	 * @return
	 */
	public Map<String,Object> queryTask(Map<String,Object> map) {
		//需要userId，流程实例Id
		Map<String,Object> result = new HashMap<String,Object>();
		String userId = (String) map.get("userId");
		String processInstanceId = (String) map.get("processInstanceId");
		String userType = (String) map.get("userType");
		TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstanceId);
		if("assignee".equals(userType)) {
			taskQuery = taskQuery.taskAssignee(userId);//代理人
		}else if("candidate".equals(userType)) {
			taskQuery = taskQuery.taskCandidateUser(userId);//候选人
		}else if("group".equals(userType)) {
			taskQuery = taskQuery.taskCandidateGroup(userId);//候选组
		}else {
			taskQuery = taskQuery.taskAssignee(userId);//代理人(默认)
		}
		if(taskQuery==null) {
			result.put("message", "没有对应任务列表");
		}else {
			List<Task> tasks = taskQuery.orderByTaskCreateTime().desc().list();
			for(Task task:tasks) {
				result.put("taskId", task.getId());
			}
		}	
		return result;
	}
	
	/**
     *    申领任务
	 * @param map
	 * @return
	 */
	public Map<String,Object> claimTask(Map<String,Object> map) {
			//任务Id、流程实例Id、其他参数
			Map<String,Object> result = new HashMap<String,Object>();
			String taskId = (String) map.get("taskId");
			String userId = (String) map.get("userId");
			Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
			if(task==null) {
				result.put("messsage", "任务不存在");
				return result;
			}
			taskService.claim(taskId, userId);
			result.put("message", "任务领取成功");
		return map;
		
	}
	
	/**
     *    驱动任务
	 * @param map
	 * @return
	 */
	public Map<String,Object> completeTask(Map<String,Object> map) {
			//任务Id、流程实例Id、其他参数
			Map<String,Object> result = new HashMap<String,Object>();
			String taskId = (String) map.get("taskId");
			Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
			if(task==null) {
				result.put("messsage", "任务不存在");
				return result;
			}
			map.remove("taskId");
			Map<String,Object> variables = map;
			taskService.complete(task.getId(), variables);
			result.put("message", "任务提交成功");
		return result;
	}
	
	/**
	 * 流程历史记录
	 * @param map
	 * @return
	 */
	public Map<String,Object> historyNode(Map<String,Object> map){
		Map<String,Object> result = new HashMap<String,Object>();
		
		String processInstanceId = (String) map.get("processInstanceId");
		List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
		List<Map<String,Object>> actList = new ArrayList<Map<String,Object>>();
		for(HistoricActivityInstance h:list) {
			Map<String,Object> act = new HashMap<String,Object>();
			act.put("actId", h.getActivityId());
			act.put("actName", h.getActivityName());
			act.put("actType", h.getActivityType());
			actList.add(act);
		}
		result.put("historyNode", actList);
		return result;		
	}
	
	/**
	 * 当前流程节点
	 * @param map
	 * @return
	 */
	public Map<String,Object> currentNode(Map<String,Object> map){
		Map<String,Object> result = new HashMap<String,Object>();
		
		String processInstanceId = (String) map.get("processInstanceId");
		if(this.isFinished(processInstanceId)) {
			result.put("message", "流程已结束");
		}else {
			ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(); 
			 if(pi==null) {
				 result.put("message", "该流程不存在");
			 }else {
				 List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
				  for (Execution exe : executions) {
						 if(exe.getParentId()!=null) {//不为空说明是任务节点
							 result.put("message",exe.getActivityId());
						 }
				  }
			 }			 
		}
		return result;	
	}
	/**
	 * 流程所有节点信息
	 * @param map
	 * @return
	 */
	public Map<String,Object> allNode(Map<String,Object> map){
		Map<String,Object> result = new HashMap<String,Object>();
		
		String processDefinitionKey = (String) map.get("processKey");
		String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).latestVersion().singleResult().getId();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
		org.flowable.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
		List<UserTask> userTaskList = process.findFlowElementsOfType(UserTask.class);
		for(int i=0;i<userTaskList.size();i++) {
			UserTask userTask = userTaskList.get(i);
			result.put(userTask.getId(), userTask.getName());
		}
		return result;		
	}
	
	/**
	 * 删除实例
	 * @param map
	 * @return
	 */
	public Map<String,Object> deleteProcessInstance(Map<String,Object> map){
		String processInstanceId = (String) map.get("processInstanceId");
		Map<String,Object> result = new HashMap<String,Object>();
		if(this.isFinished(processInstanceId)) {		
//			historyService.deleteHistoricProcessInstance(processInstanceId);
			result.put("message", "流程已结束");
		}else {		
			runtimeService.deleteProcessInstance(processInstanceId, "作废");
			result.put("message", "运行时实例删除成功");
		}
		return result;		
	}
	
	/**
	 * 判断流程是否结束
	 * @param processInstanceId
	 * @return
	 */
	public boolean isFinished(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery().finished()
                .processInstanceId(processInstanceId).count() > 0;
	}
	
	/**
	 * 生成流程图
	 *
	 * @param processId 任务ID
	 */
//	@RequestMapping(value = "processDiagram")
	public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId,String flag) throws Exception {
		String pis = "";
		List<String> activityIds = new ArrayList<String>();
		List<String> flows = new ArrayList<String>();
		
		if(this.isFinished(processId) || "history".equals(flag)) {//已结束流程
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
			 if(pi==null) {
				 Log.info("该流程不存在");
				 return;
			 }
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
		InputStream in = diagramGenerator.generateDiagram(bpmnModel, "bmp", activityIds, flows,
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
