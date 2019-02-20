package priv.gitonlie.flowable.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FlowableService {
	
	private Logger Log = LoggerFactory.getLogger(FlowableService.class);
	
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
	
	public Object process(Map<String,Object> requestMap){
		Object result = null;
		String processName = (String) requestMap.get("processName");
		requestMap.remove("processName");
		switch (processName) {
		case "start":
			result = initiate(requestMap);
			break;
		case "go":
			result = completeTask(requestMap);
			break;
		case "history":
			result = historyNode(requestMap);
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
	public String startUp(Map<String,Object> map) {
		//需要userId、流程Id
		String userId = (String) map.get("userId");
		String processDefinitionKey = (String) map.get("processKey");
		//提出多余字段保留必要字段
		map.remove("processKey");
		
		ProcessInstance processInstance = null;
		String processInstanceId = null;
		try {
			//设置流程发起人
			identityService.setAuthenticatedUserId(userId);
			processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, map);
			processInstanceId = processInstance.getId();
			Log.info("userId:{}>开启流程ID:{}",userId,processInstanceId);
		}finally {
			  identityService.setAuthenticatedUserId(null);
		}	
		return processInstanceId;
	}
	
	/**
             * 驱动任务
	 * @param map
	 * @return
	 */
	public String startTask(String processInstanceId) {
			//任务Id、流程实例Id、其他参数
			String message = "";
			Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
			if(task==null) {
				message = "任务不存在";
				Log.info(message);
				return message;
			}
			taskService.complete(task.getId());
			message = "任务完成";
			Log.info(message);
		return message;
	}
	
	
	/**
	 * 开启流程并驱动第一个流程节点
	 * @param map
	 * @return
	 */
	public Map<String,Object> initiate(Map<String,Object> map){
		Map<String,Object> result = new HashMap<String,Object>();
		String processInstanceId = startUp(map);
		startTask(processInstanceId);
		result.put("processInstanceId", processInstanceId);
		return result;	
	}
	
   /**
	 * 驱动任务
	* @param map
	* @return
	*/
	public Map<String,Object> completeTask(Map<String,Object> map) {
			Map<String,Object> result = new HashMap<String,Object>();
			String processInstanceId = map.get("processInstanceId")==null?"":String.valueOf(map.get("processInstanceId"));
			String taskId = map.get("taskId")==null?"":String.valueOf(map.get("taskId"));
			String assignee = map.get("userId")==null?"":String.valueOf(map.get("userId"));
			String flag = map.get("flag")==null?"":String.valueOf(map.get("flag"));
			String remark = map.get("remark")==null?"":String.valueOf(map.get("remark"));
			if(!StringUtils.isEmpty(processInstanceId)) {
				
			}
			Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
			if(task==null) {
				result.put("messsage", "任务不存在");
				return result;
			}
			System.out.println(task.getId());
			Map<String,Object> var = new HashMap<String,Object>();
			var.put("flag", flag);
			Map<String,Object> localVar = new HashMap<String,Object>();
			localVar.put("remark", remark);
			taskService.setAssignee(task.getId(), assignee);
			taskService.setVariables(task.getId(), var);
			taskService.setVariablesLocal(task.getId(), localVar);
			taskService.complete(task.getId());
			result.put("message", "任务提交成功");
		return result;
	}
	
	/**
	 * 流程历史记录
	 * @param map
	 * @return
	 */
	public List<Map<String,Object>> historyNode(Map<String,Object> map){
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
		String processInstanceId = (String) map.get("processInstanceId");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).orderByHistoricTaskInstanceStartTime().asc().list();
		for(HistoricTaskInstance task:taskList) {
			Map<String,Object> result = new HashMap<String,Object>();		
			result.put("taskName", task.getName());
			result.put("userId", task.getAssignee()==null?"":task.getAssignee());
			result.put("startTime", sdf.format(task.getStartTime()));
			result.put("endTime", task.getEndTime()==null?"":sdf.format(task.getEndTime()));
			List<HistoricVariableInstance> varList = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).taskId(task.getId()).list();
			for(HistoricVariableInstance var:varList) {
				result.put(var.getVariableName(), var.getValue());
			}
			list.add(result);
		}
		return list;
	}
}
