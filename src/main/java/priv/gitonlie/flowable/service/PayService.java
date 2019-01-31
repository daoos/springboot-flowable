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
	
	public void queryProcessInstance(String userId) {
//		runtimeService.createProcessInstanceQuery()
		//发起流程
		HistoricProcessInstanceQuery instance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001");
		List<HistoricProcessInstance> list = instance.startedBy(userId).list();
		System.out.println(userId+"发起"+list.size()+"项流程");
		//已完成流程
		HistoricProcessInstanceQuery finishedq = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001");
		List<HistoricProcessInstance> unfinishedl = instance.startedBy(userId).unfinished().list();
		System.out.println(unfinishedl.size());
		for(HistoricProcessInstance hinstance:unfinishedl) {
			hinstance.getId();
			Map<String, Object> vars = hinstance.getProcessVariables();
		}
	}
}
