package priv.gitonlie.flowable.service;

import java.util.List;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PayService {
	
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
	
	public void queryProcessInstance(String userId,String processId) {
//		runtimeService.createProcessInstanceQuery()
		//发起流程
		HistoricProcessInstanceQuery instance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001");
		System.out.println("******************发起流程****************** ");
		List<HistoricProcessInstance> list = instance.startedBy(userId).list();
		System.out.println(userId+"发起"+list.size()+"项流程");
		System.out.println("******************未完成流程****************** ");
		//未完成流程
		HistoricProcessInstanceQuery unfinishedq = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001");
		List<HistoricProcessInstance> unfinishedl = unfinishedq.startedBy(userId).unfinished().list();
		System.out.println("未完成流程"+unfinishedl.size());
		for(HistoricProcessInstance hinstance:unfinishedl) {
			System.out.println(hinstance.getId());
		}
		//完成流程
		System.out.println("******************完成流程****************** ");
		HistoricProcessInstanceQuery finishedq = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001");
		List<HistoricProcessInstance> finishedl = finishedq.startedBy(userId).finished().list();
		System.out.println("完成流程："+finishedl.size());
		for(HistoricProcessInstance hinstance:finishedl) {
			Log.info("{}",hinstance.getId());
		}
		//删除流程
		HistoricProcessInstanceQuery delq = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001");
		List<HistoricProcessInstance> dell = delq.startedBy(userId).deleted().list();
		System.out.println("删除流程："+dell.size());
			for(HistoricProcessInstance hinstance:dell) {
			Log.info("processId:{},删除理由:{}",hinstance.getId(),hinstance.getDeleteReason());
		}
		
		//未完成任务
		System.out.println("******************未完成任务****************** ");
		HistoricProcessInstanceQuery fdq = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001");
		List<HistoricProcessInstance> fdl = fdq.startedBy(userId).finished().list();
		for(HistoricProcessInstance hinstance:fdl) {
			 List<Task> taskList = taskService.createTaskQuery().processInstanceId(hinstance.getId()).list();
			 for(Task task:taskList) {
				 System.out.println("processId:"+hinstance.getId()+">taskId:"+task.getId()+">taskName");
			 }
		}
		
	}
}
