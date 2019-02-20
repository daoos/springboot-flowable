package priv.gitonlie.flowable.engine;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;
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
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import priv.gitonlie.flowable.engine.util.Tools;


@Service
public class WorkflowEngineService {
	
	
	private static Logger Log = LoggerFactory.getLogger(WorkflowEngineService.class);
	
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private IdentityService identityService;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private ProcessEngine processEngine;
	@Autowired
	private TaskService taskService;
	@Autowired
	private HistoryService historyService;
	
	/**
	 *   <p>初始化流程</p>
	 * @param requestMap
	 * @return
	 */
	public Map<String,Object> init(Map<String,Object> requestMap) {
		//结果返回
		Map<String,Object> result = new HashMap<String, Object>();
		String userId = requestMap.get("userId")==null?"":String.valueOf(requestMap.get("userId"));
		String processDefinitionKey = requestMap.get("processKey")==null?"":String.valueOf(requestMap.get("processKey"));
		String businessKey = requestMap.get("businessKey")==null?"":String.valueOf(requestMap.get("businessKey"));
		String global = requestMap.get("global")==null?"{}":String.valueOf(requestMap.get("global"));
		Log.info("userId:{},processKey:{},businessKey:{},global:{}",userId,processDefinitionKey,businessKey,global);
		if(!Tools.isJson(global)) {
			Log.info("全局变量不合法global:{}",global);
			result.put("messsage", "参数错误");
			return result;
		}
		Map<String,Object> variables = new HashMap<String, Object>();
		variables.putAll(Tools.transform(global));
		variables.put("userId", userId);//会覆盖global相同key的值
		
		ProcessInstance processInstance = null;
		String processInstanceId = null;
		try {
			//设置流程发起人
			identityService.setAuthenticatedUserId(userId);
			processInstance = runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey, variables);
			processInstanceId = processInstance.getId();
			result.put("processInstanceId", processInstanceId);
			Log.info("[{}]发起流程ID:{}",userId,processInstanceId);
		}finally {
			identityService.setAuthenticatedUserId(null);
		}	
		return result;
	}
	
	/**
	 * <p>查询任务</p>
	 * @param map
	 * @return
	 */
	public Map<String,Object> queryTask(Map<String,Object> map) {
		//需要userId，流程实例Id
		Map<String,Object> result = new HashMap<String,Object>();
		String userId = map.get("userId")==null?"":String.valueOf(map.get("userId"));
		String processInstanceId = map.get("processInstanceId")==null?"":String.valueOf(map.get("processInstanceId"));//流程实例Id
		String userType = map.get("userType")==null?"":String.valueOf(map.get("userType"));
		Log.info("userId:{},processInstanceId:{},userType:{}",userId,processInstanceId,userType);
		if(StringUtils.isEmpty(processInstanceId) ||StringUtils.isEmpty(userId)) {
			result.put("message", "参数错误");
			return result;
		}
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
				result.put("taskName", task.getName());
			}
		}	
		return result;
	}
	
	/**
     * <p>申领任务</p>
	 * @param map
	 * @return
	 */
	public Map<String,Object> claimTask(Map<String,Object> map) {
			//任务Id、流程实例Id、其他参数
			Map<String,Object> result = new HashMap<String,Object>();
			String taskId = map.get("taskId")==null?"":String.valueOf(map.get("taskId"));
			String userId = map.get("userId")==null?"":String.valueOf(map.get("userId"));
			Log.info("taskId:{},userId:{}");
			Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
			if(task==null) {
				result.put("messsage", "任务不存在");
				return result;
			}
			taskService.claim(taskId, userId);
			result.put("message", "任务领取成功");
		return result;
		
	}
	
	/**
	 * <p>驱动任务</p>
	* @param requestMap
	* @return
	*/
	public Map<String,Object> completeTask(Map<String,Object> map) {
			//任务Id、流程实例Id、其他参数
			Map<String,Object> result = new HashMap<String,Object>();
			String processInstanceId = map.get("processInstanceId")==null?"":String.valueOf(map.get("processInstanceId"));//流程实例Id
			String taskId = map.get("taskId")==null?"":String.valueOf(map.get("taskId"));//任务Id
			String assignee = map.get("userId")==null?"":String.valueOf(map.get("userId"));//代理人
			String flag = map.get("flag")==null?"":String.valueOf(map.get("flag"));//驱动前进或者回退
			String global = map.get("global")==null?"{}":String.valueOf(map.get("global"));//全局额外字段
			String remark = map.get("remark")==null?"":String.valueOf(map.get("remark"));//任务备注
			String local = map.get("local")==null?"{}":String.valueOf(map.get("local"));//任务额外字段
			Log.info("processInstanceId:{},taskId:{},userId:{},flag:{},global:{},remark:{},local:{}"
					,processInstanceId,taskId,assignee,flag,global,remark,local);
			if(!Tools.isJson(global)) {
				Log.info("全局变量不合法global:{}",global);
				result.put("messsage", "参数错误");
				return result;
			}
			if(!Tools.isJson(local)) {
				Log.info("局部变量不合法local:{}",local);
				result.put("messsage", "参数错误");
				return result;
			}
			Map<String,Object> var = new HashMap<String,Object>();//全局变量
			var.putAll(Tools.transform(global));
			var.put("flag", flag);//会覆盖global相同key的值
			
			Map<String,Object> localVar = new HashMap<String,Object>();//绑定任务Id的局部变量
			localVar.putAll(Tools.transform(local));
			localVar.put("remark", remark);//会覆盖local相同key的值
			
			if(!StringUtils.isEmpty(processInstanceId) && StringUtils.isEmpty(taskId)) {//按照processInstanceId查询任务
				List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
				if(taskList.size()>0) {
					for(int i=0;i<taskList.size();i++) {
						Task task = taskList.get(i);
						if(!StringUtils.isEmpty(assignee)) {
							taskService.setAssignee(task.getId(), assignee);//设定代理人							
						}
						taskService.setVariables(task.getId(), var);//设定全局变量
						taskService.setVariablesLocal(task.getId(), localVar);//绑定局部变量
						taskService.complete(task.getId());
					}
					result.put("message", "任务提交成功");
				}else {
					result.put("message", "任务不存在");
				}
			}else if(StringUtils.isEmpty(processInstanceId) && !StringUtils.isEmpty(taskId)) {//按照taskId查询任务
				Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
				if(task==null) {
					result.put("messsage", "任务不存在");
					return result;
				}
				if(!StringUtils.isEmpty(assignee)) {
					taskService.setAssignee(task.getId(), assignee);//设定代理人							
				}
				taskService.setVariables(task.getId(), var);//设定全局变量
				taskService.setVariablesLocal(task.getId(), localVar);//绑定局部变量
				taskService.complete(task.getId());
				result.put("message", "任务提交成功");
			}else {
				result.put("messsage", "参数错误");
			}
			
		return result;
	}
	
	/**
	 * <p>开始流程</p>
	 * <p>初始化流程并且驱动到第一个节点，直接开始流程</p>
	 * @param map
	 * @return
	 */
	public Map<String,Object> startProcess(Map<String,Object> requestMap) {
		Map<String,Object> processMap = init(requestMap);
		requestMap.putAll(processMap);
		completeTask(requestMap);
		return processMap;
	}
	
	/**
	 * <p>流程历史任务记录</p>
	 * @param map
	 * @return
	 */
	public List<Map<String,Object>> historyNode(Map<String,Object> map){
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
		String processInstanceId = map.get("processInstanceId")==null?"":String.valueOf(map.get("processInstanceId"));//流程实例Id
		Log.info("processInstanceId:{}",processInstanceId);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).orderByHistoricTaskInstanceStartTime().asc().list();
		for(HistoricTaskInstance task:taskList) {
			Map<String,Object> result = new HashMap<String,Object>();
			result.put("taskId", task.getId());//任务Id
			result.put("taskName", task.getName());//任务名
			result.put("userId", task.getAssignee()==null?"":task.getAssignee());//操作人
			result.put("startTime", sdf.format(task.getCreateTime()));//任务开始时间
			result.put("endTime", task.getEndTime()==null?"":sdf.format(task.getEndTime()));//任务结束时间
			List<HistoricVariableInstance> varList = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).taskId(task.getId()).list();
			for(HistoricVariableInstance var:varList) {
				result.put(var.getVariableName(), var.getValue());//绑定任务Id的局部变量
			}
			list.add(result);
		}
		return list;
	}
	
	/**
	 * <p>流程当前任务节点</p>
	 * @param map
	 * @return
	 */
	public Map<String,Object> currentNode(Map<String,Object> map){
		Map<String,Object> result = new HashMap<String,Object>();		
		String processInstanceId = map.get("processInstanceId")==null?"":String.valueOf(map.get("processInstanceId"));//流程实例Id
		Log.info("processInstanceId:{}",processInstanceId);
		if(this.isFinished(processInstanceId)) {
			result.put("message", "流程已结束");
		}else {
			ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(); 
			 if(pi==null) {
				 result.put("message", "该流程不存在");
			 }else {
				 Task task = taskService.createTaskQuery().orderByTaskCreateTime().desc().list().get(0);
				 result.put("taskId", task.getId());
				 result.put("taskName", task.getName());
			 }			 
		}
		return result;	
	}
	
	/**
	 * <p>流程所有节点信息</p>
	 * @param map
	 * @return
	 */
	public List<Map<String,Object>> allNode(Map<String,Object> map){
		List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
		String processDefinitionKey = map.get("processKey")==null?"":String.valueOf(map.get("processKey"));//流程图ID
		Log.info("processKey:{}",processDefinitionKey);
		String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).latestVersion().singleResult().getId();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
		org.flowable.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
		List<UserTask> userTaskList = process.findFlowElementsOfType(UserTask.class);
		for(int i=0;i<userTaskList.size();i++) {
			Map<String,Object> result = new HashMap<String,Object>();
			UserTask userTask = userTaskList.get(i);
			result.put("taskId",userTask.getId());
			result.put("taskName",userTask.getName());
			list.add(result);
		}
		return list;		
	}
	
	/**
	 * <p>检查流程是否结束</p>
	 * @param map
	 * @return
	 */
	public Map<String,Object> finished(Map<String,Object> map){
		String processInstanceId = map.get("processInstanceId")==null?"":String.valueOf(map.get("processInstanceId"));//流程实例Id
		Log.info("processInstanceId:{}",processInstanceId);
		Map<String,Object> result = new HashMap<String,Object>();
		if(this.isFinished(processInstanceId)) {
			result.put("finished", true);
		}else {
			result.put("finished", false);
		}
		return result;
	}
	
	/**
	 * <p>上下节点</p>
	 * @param taskId
	 * @return
	 */
	public Object frontAndNextNode(Map<String,Object> map) {
		Object result = null;
		List<Map<String,Object>> resultList = new ArrayList<Map<String,Object>>();
		Map<String,Object> resultMap = new HashMap<String,Object>();
		String processInstanceId = map.get("processInstanceId")==null?"":String.valueOf(map.get("processInstanceId"));//流程实例Id
		String taskId = map.get("taskId")==null?"":String.valueOf(map.get("taskId"));//任务Id
		if(!StringUtils.isEmpty(processInstanceId) && StringUtils.isEmpty(taskId)) {
			List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
			for(int i=0;i<taskList.size();i++) {
				Task task = taskList.get(i);
				Map<String,Object> taskMap =new HashMap<String,Object>();
				taskMap.put("taskId", task.getId());
				Map<String,Object> taskResult = this.taskFrontNextNode(task.getId());
				taskMap.putAll(taskResult);
				resultList.add(taskMap);
			}
			result = resultList;
		}else if(StringUtils.isEmpty(processInstanceId) && !StringUtils.isEmpty(taskId)) {
			resultMap.put("taskId", taskId);
			resultMap.putAll(this.taskFrontNextNode(taskId));
			result = resultMap;
		}else {
			resultMap.put("message", "参数错误");
			result = resultMap;
		}
		return result;
	}
	
	/**
	 * <p>根据任务Id查询上下节点</p>
	 * <p>基于frontAndNextNode的子方法</p>
	 * @param taskId
	 * @return
	 */
	private Map<String,Object> taskFrontNextNode(String taskId){
		Map<String,Object> result = new HashMap<String,Object>();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if(task==null) {
			result.put("message", "不存在任务");
			return result;
		}
		Execution execute = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
		BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
		FlowElement currentFlowElement = bpmnModel.getFlowElement(execute.getActivityId());
		Log.info("currentNodeId:{},currentNodeName:{}",currentFlowElement.getId(),currentFlowElement.getName());
		Map<String,Object> current = new HashMap<String,Object>();
		current.put("currentNodeId", currentFlowElement.getId());
		current.put("currentNodeName", currentFlowElement.getName());
		result.put("currentNode", current);
		//输出
		List<Map<String,Object>> next = this.extractUserTask(currentFlowElement, "out");
		result.put("nextNode",next);
		//输入
		List<Map<String,Object>> front = this.extractUserTask(currentFlowElement, "in");
		result.put("frontNode",front);
		return result;
	}
	
	/**
	 * <p>根据序列流元素递归获取用户任务</p>
	 * <p>基于taskFrontNextNode(String)</p>
	 * @param source
	 * @param status
	 * @return
	 */
	private List<Map<String,Object>> extractUserTask(FlowElement flowElement,String status) {
		List<Map<String,Object>> resultList = new ArrayList<Map<String,Object>>();
		FlowNode flowNode = (FlowNode)flowElement;
		List<SequenceFlow> gateway = null;
		if("out".equals(status)) {
			gateway = flowNode.getOutgoingFlows();//输出
		}else if("in".equals(status)) {
			gateway = flowNode.getIncomingFlows();//输入
		}
		for(int i=0;i<gateway.size();i++) {
			SequenceFlow gateFlow = gateway.get(i);
			FlowElement ts = null;
			if("out".equals(status)) {
				ts = gateFlow.getTargetFlowElement();//输出
			}else if("in".equals(status)) {
				ts = gateFlow.getSourceFlowElement();//输入
			}
			if(!(ts instanceof UserTask)) {//非用户任务
				Log.info("{}:{}非用户任务,将进行递归查询用户任务",status,ts.getId());
				List<Map<String,Object>> temp = extractUserTask(ts, status);//递归查询出用户任务
				resultList.addAll(temp);
			}else {
				if("out".equals(status)) {
					Log.info("{}:nextNodeId:{},nextNodeName:{}",i,ts.getId(),ts.getName());
					Map<String,Object> out = new HashMap<String,Object>();
					out.put("nextNodeId", ts.getId());
					out.put("nextNodeName", ts.getName());
					resultList.add(out);
				}else if("in".equals(status)) {
					Log.info("{}:frontNodeId:{},frontNodeName:{}",i,ts.getId(),ts.getName());
					Map<String,Object> front = new HashMap<String,Object>();
					front.put("frontNodeId", ts.getId());
					front.put("frontNodeName", ts.getName());
					resultList.add(front);
				}
			}
		}
		return resultList;		
	}
	
	/**
	 * <p>生成流程图(Base64)</p>
	 * @param requestMap
	 * @return
	 * @throws Exception
	 */
	public Map<String,Object> processDiagram(Map<String,Object> requestMap) throws Exception {
		Map<String,Object> result = new HashMap<String,Object>();
		String processInstanceId = requestMap.get("processInstanceId")==null?"":(String) requestMap.get("processInstanceId");
		String flag = requestMap.get("flag")==null?"":(String) requestMap.get("flag");
		String processKey = requestMap.get("processKey")==null?"":(String) requestMap.get("processKey");
		String pis = "";
		List<String> activityIds = new ArrayList<String>();
		List<String> flows = new ArrayList<String>();
		if(!StringUtils.isEmpty(processKey) && StringUtils.isEmpty(processInstanceId)) {//所有流程节点的流程图
			pis = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey).latestVersion().singleResult().getId();
		}else if(StringUtils.isEmpty(processKey) && !StringUtils.isEmpty(processInstanceId)){//实时流程节点的流程图
			if(this.isFinished(processInstanceId) || "true".equals(flag)) {//已结束流程
				HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
				pis = hpi.getProcessDefinitionId();
				List<HistoricActivityInstance> activityList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
				for(HistoricActivityInstance act:activityList) {
					activityIds.add(act.getActivityId());
					if(act.getActivityType().equals("sequenceFlow")) {
						flows.add(act.getActivityId());
					}
				}
			}else {//未结束流程
				 ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(); 
				 if(pi==null) {
					 Log.info("该流程不存在");
					 result.put("message", "流程不存在");
					 return result;
				 }
				  pis=pi.getProcessDefinitionId();
				  List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
				  for (Execution exe : executions) { 
						  List<String> ids = runtimeService.getActiveActivityIds(exe.getId()); 
						  activityIds.addAll(ids); 
				  }
			}
		}else {
			 Log.info("参数错误");
			 result.put("message", "参数错误");
			 return result;
		}
		// 获取流程图
		BpmnModel bpmnModel = repositoryService.getBpmnModel(pis);
		ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
		ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
		InputStream is = diagramGenerator.generateDiagram(bpmnModel, "jpg", activityIds, flows,
				"宋体", "宋体", "宋体",null, 1.0,false);
		byte[] buf = new byte[is.available()];
		is.read(buf);
		String diagramBase64Str = Base64.getEncoder().encodeToString(buf);
		result.put("diagram", diagramBase64Str);
		is.close();
		return result;
	}

	
	/**
	 * <p>生成流程图</p>
	 * @param httpServletResponse
	 * @param requestMap
	 * @throws Exception
	 */
	public void genProcessDiagram(HttpServletResponse httpServletResponse,Map<String,Object> requestMap) throws Exception {
		String processInstanceId = requestMap.get("processInstanceId")==null?"":(String) requestMap.get("processInstanceId");
		String flag = requestMap.get("flag")==null?"":(String) requestMap.get("flag");
		String processKey = requestMap.get("processKey")==null?"":(String) requestMap.get("processKey");
		String pis = "";
		List<String> activityIds = new ArrayList<String>();
		List<String> flows = new ArrayList<String>();
		if(!StringUtils.isEmpty(processKey) && StringUtils.isEmpty(processInstanceId)) {
			pis = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey).latestVersion().singleResult().getId();
		}else if(StringUtils.isEmpty(processKey) && !StringUtils.isEmpty(processInstanceId)) {
			if(this.isFinished(processInstanceId) || "true".equals(flag)) {//已结束流程
				HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
				pis = hpi.getProcessDefinitionId();
				List<HistoricActivityInstance> activityList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list();
				for(HistoricActivityInstance act:activityList) {
					activityIds.add(act.getActivityId());
					if(act.getActivityType().equals("sequenceFlow")) {
						flows.add(act.getActivityId());
					}
				}
			}else {//未结束流程
				 ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult(); 
				 if(pi==null) {
					 Log.info("该流程不存在");
					 return;
				 }
				  pis=pi.getProcessDefinitionId();
				  List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
				  for (Execution exe : executions) { 
						  List<String> ids = runtimeService.getActiveActivityIds(exe.getId()); 
						  activityIds.addAll(ids); 
				  }
			}
		}else {
			Log.info("参数错误");
			return;
		}
		
		// 获取流程图
		BpmnModel bpmnModel = repositoryService.getBpmnModel(pis);
		ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
		ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
		InputStream in = diagramGenerator.generateDiagram(bpmnModel, "jpg", activityIds, flows,
				"宋体", "宋体", "宋体",null, 1.0,false);
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
	
	
	/**
	 *   <p>判断流程是否结束</p>
	 * @param processInstanceId
	 * @return
	 */
	private boolean isFinished(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery().finished()
                .processInstanceId(processInstanceId).count() > 0;
	}
}
