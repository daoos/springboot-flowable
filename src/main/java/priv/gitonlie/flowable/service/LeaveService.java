package priv.gitonlie.flowable.service;

import java.text.SimpleDateFormat;
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
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricDetailQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import priv.gitonlie.flowable.entity.ProcessParams;

@Service
public class LeaveService {
	
	private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	
	@Autowired
	private IdentityService identityService;
	@Autowired
    private RuntimeService runtimeService;
	@Autowired
    private TaskService taskService;
	@Autowired
	private HistoryService historyService;
    public void start() {
    	//启动请假流程
    	String processId = "myProcess";
    	String userId = "123456";
    	ProcessParams process = new ProcessParams();
    	process.setProcessId(processId);
    	process.setUserId(userId);
    	process.setUsername("小王");
    	process.setType("事假");
    	process.setStatus("0");
    	
    	Map<String, Object> map = new HashMap<String, Object>();       
    	map.put("userId", "人事");
        map.put("username", process.getUsername());
        map.put("type", process.getType());
        map.put("status", process.getStatus());
        //流程发起人
    	ProcessInstance pro = null;   	
    	try {
    		  identityService.setAuthenticatedUserId(userId);
    		  pro = runtimeService.startProcessInstanceByKey(processId, map);
    		  Log.info("流程ID:{}",pro.getId());
    		  
    	} finally {
    		  identityService.setAuthenticatedUserId(null);
    	}
    	//查询任务个数
    	queryTask(userId);
    	//审批
//    	approval(userId, true);
    	//查询历史记录
    	
    }
    
    /**
     * 查询任务个数
     * @param userId
     */
    public void queryTask(String userId) {
    	//查询流程列表，待办列表
    	Log.info("查询待审批列表");   	
    	List<Task> taskList = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
    	Log.info("任务个数:{}",taskList.size());
    	for(Task task:taskList) {
    		Log.info("任务:{}",task.toString());
    	}
    }
    
    /**
     * 人事审核
     * @param userId
     * @param s
     */
    public void approval(String userId,boolean s) {
    	//查询流程列表，待办列表
    	Log.info("人事查询待审批列表");
    	List<Task> taskList = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
    	boolean enable = s;
    	for(Task task:taskList) {
    		Log.info("任务:{}",task.toString());
    		
    		//批准
        	//Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    		//if (task == null) {
    		//	throw new RuntimeException("流程不存在");
    		//}
    		// 通过审核
    		String status = "-1";
    		if(enable) {
    			status = "1";
    		}
    		HashMap<String, Object> map = new HashMap<>();
    		map.put("status", status);
    		taskService.complete(task.getId(), map);
    	}    	
    }
    
    
    public void leaderApproval(String userId,boolean s) {
    	//查询流程列表，待办列表
    	Log.info("领导查询待审批列表");
    	List<Task> taskList = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
    	Log.info("任务个数:{}",taskList.size());
    	boolean enable = s;
    	for(Task task:taskList) {
    		Log.info("任务:{}",task.toString());
    		
    		//批准
        	//Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    		//if (task == null) {
    		//	throw new RuntimeException("流程不存在");
    		//}
    		// 通过审核
    		String status = "-1";
    		if(enable) {
    			status = "2";
    			enable = false;
    		}else {
    			enable = true;
    		}
    		HashMap<String, Object> map = new HashMap<>();
    		map.put("status", status);
    		taskService.complete(task.getId(), map);
    	}
    }
    
    public void archive(String userId) {
    	List<Task> taskList = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
    	for(Task task:taskList) {
    		Log.info("归档任务:{}",task.toString());
    		taskService.complete(task.getId());
    	}
    }
    
    public void queryNode() {
//    	ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("myProcess").processInstanceId("7dc0cefa-23c8-11e9-895a-507b9dc37214");
//    	ProcessInstanceQuery q = runtimeService
    	List<Task> taskList = taskService.createTaskQuery().taskAssignee("人事").orderByTaskCreateTime().desc().list();
    	Task task  = taskList.get(0);
    	System.out.println(task);
    }
    
    public void queryHistory(String userId) {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	
    	//HistoricProcessInstanceQuery 查询历史流程实例
    	HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
    	//历史实例
    	List<HistoricProcessInstance> list = query.processDefinitionKey("myProcess").list();
    	Log.info("历史流程实例process:{}个",list.size());
    	for(HistoricProcessInstance p:list) {
    		Log.info("{},{},{},{}",p.getId(),p.getName(),sdf.format(p.getStartTime()),p.getEndTime()==null?null:sdf.format(p.getEndTime()));
    	}
    	//发起人
    	List<HistoricProcessInstance> userProcessList = query.startedBy(userId).orderByProcessInstanceStartTime().desc().list();
    	Log.info("{}发起{}项流程process",userId,userProcessList.size());
    	//未结束实例
    	List<HistoricProcessInstance> unfinishedList = query.unfinished().list();
    	Log.info("未结束的流程实例process:{}个",unfinishedList.size());
    	//已结束实例
    	List<HistoricProcessInstance> finishedList = query.finished().list();
    	Log.info("已结束的流程实例:{}个process",finishedList.size());
    	
    	
    	
    	
    	//HistoricVariableInstanceQuery 查询历史变量
    	HistoricVariableInstanceQuery varQuery = historyService.createHistoricVariableInstanceQuery();
    	List<HistoricVariableInstance> varList = varQuery.list();
    	Log.info("已完成流程的变量实例Variable：{},",varList.size());
    	for(HistoricVariableInstance vars:varList) {
    		Log.info("创建时间:{},变量ID：{},{},{},{},变量任务ID:{}",sdf.format(vars.getCreateTime()),vars.getId(),vars.getValue(),vars.getVariableName(),vars.getVariableTypeName(),vars.getTaskId());
    	}
    	
    	
    	
    	
    	//HistoricActivityInstanceQuery 查询历史活动
    	HistoricActivityInstanceQuery activity = historyService.createHistoricActivityInstanceQuery();
    	List<HistoricActivityInstance> actList = activity.list();
    	Log.info("历史活动:{}",actList.size());
    	Log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    	for(HistoricActivityInstance act:actList) {
    		Log.info("{},{},{},{},{}",act.getActivityId(),act.getActivityName(),act.getActivityType(),act.getAssignee(),act.getTaskId());
    	}
    	Log.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
    	
    	
    	
    	//HistoricDetailQuery 详细查询
    	HistoricDetailQuery detail = historyService.createHistoricDetailQuery();
    	List<HistoricDetail> detailList = detail.list();
    	Log.info("HistoricDetail:{}",detailList.size());
    	for(HistoricDetail d:detailList) {
    		Log.info("{},{},{}",d.getExecutionId(),d.getId(),d.getTaskId());
    	}
    	
    	
    	//HistoricTaskInstanceQuery
    	List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery().orderByTaskCreateTime().desc().list();
    	Log.info("任务实例:{}",taskList);
    	for(HistoricTaskInstance task:taskList) {
    		Log.info("{}>{}>{}",task.getId(),task.getName(),task);
    	}
    }
    
    
    public void queryActHistory(String processId) {
    	Log.info("======================================================================");
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    	HistoricActivityInstanceQuery activity = historyService.createHistoricActivityInstanceQuery();
    	List<HistoricActivityInstance> act = activity.processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();
    	for(HistoricActivityInstance actIns:act) {
    		Log.info("{},{},{},{},{}",sdf.format(actIns.getStartTime()),actIns.getActivityName(),actIns.getActivityType(),actIns.getAssignee(),actIns.getActivityId());			
    	}
    	
    }
    
    
    public void queryActHistory2(String processId) {
    	Log.info("======================================================================");
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    	HistoricActivityInstanceQuery activity = historyService.createHistoricActivityInstanceQuery();
    	List<HistoricActivityInstance> act = activity.processInstanceId(processId).orderByHistoricActivityInstanceStartTime().asc().list();
    	for(HistoricActivityInstance actIns:act) {
    		Log.info("{},{},{},{},{}",sdf.format(actIns.getStartTime()),actIns.getActivityName(),actIns.getActivityType(),actIns.getAssignee(),actIns.getActivityId());			
    	}
    	
    }
    
    public void taskQueryAAAHistory() {
    	HistoricTaskInstanceQuery taskQ = historyService.createHistoricTaskInstanceQuery();
    	List<HistoricTaskInstance> taskList = taskQ.list();
    	Log.info("======================================{}",taskList.size());
    	for(HistoricTaskInstance task:taskList) { 
    		Log.info("{}>{}>{}",task.getId(),task.getName(),task); 
    		}
		/*
		 * List<HistoricTaskInstance> taskList =
		 * historyService.createHistoricTaskInstanceQuery().orderByTaskCreateTime().desc
		 * ().list(); Log.info("任务实例:{}",taskList); for(HistoricTaskInstance
		 * task:taskList) { Log.info("{}>{}>{}",task.getId(),task.getName(),task); }
		 */
	}
}
