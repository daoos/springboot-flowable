package priv.gitonlie.flowable.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ActivityInstanceQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import priv.gitonlie.flowable.entity.PayProcessEntity;

@RestController
@RequestMapping(value = "/pay")
public class PayController {
	
	private Logger Log = LoggerFactory.getLogger(ExpenseController.class);
	
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
	//1.开启流程 -> 2.查询录入任务 -> 3.录入数据提交 -> 4.查询待审核任务 -> 5.审核  -> 6.a 通过  -> 7.a 生成代付单 -> 8.a 结束
//																    -> 6.b 不通过 -> 7.b 打回修改  -> 8.b 生成废单 -> 9.b 生成代付数据废单 -> 10.b 结束
//																							  -> 8.c 继续提交 -> 9.c==>4?
//																		
	//开启流程															
	@RequestMapping(value = "/start")
	public String startProcess(String userId) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
		String date = sdf.format(new Date());
		// 启动流程
		HashMap<String, Object> map = new HashMap<>();
		map.put("userId", userId);
		map.put("createDate", date);
		ProcessInstance processInstance = null;
		String processInstanceId = null;
		try {
			//设置流程发起人
			identityService.setAuthenticatedUserId(userId);
			processInstance = runtimeService.startProcessInstanceByKey("pay000001", map);
			processInstanceId = processInstance.getId();
			Log.info("{}>userId:{}开启流程ID:{}",date,userId,processInstanceId);
		}finally {
			  identityService.setAuthenticatedUserId(null);
		}	
		return "提交成功.流程Id为：" + processInstanceId;
	}
	//查询任务
	@RequestMapping("/list")
	public Object queryTask(String userId) {
		JSONArray arry = new JSONArray();
		List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
		for(Task task:tasks) {
			JSONObject object = new JSONObject();
			object.put("taskId", task.getId());
			object.put("taskName", task.getName());
			arry.add(object);
		}
		return arry;
	}
	
	//提交代付录入任务
	@RequestMapping("/record")
	public String record(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if(task==null) {
			return "任务不存在";
		}
		taskService.complete(taskId);
		return "任务"+task+"已提交";	
	}
	
	//审核
	@RequestMapping("/review")
	public String review(String status,String taskId) {
		if(("通过".equals(status) || "不通过".equals(status)) && !StringUtils.isEmpty(taskId)) {
			HashMap<String, Object> map = new HashMap<>();
			map.put("flag", status);
//			List<Task> tasks = taskService.createTaskQuery().taskAssignee("review").orderByTaskCreateTime().desc().list();
//			for(Task task:tasks) {
//				taskService.complete(task.getId(), map);
//				Log.info("任务 {}:{}>{}",task.getName(),task.getId(),status);
//			}
			Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
			if(task==null) {
				return "待审核任务不存在";
			}
			taskService.complete(taskId,map);
			String msg = "任务"+taskId+"{"+status+"}";
			if("通过".equals(status)) {
				msg = msg +",并生成代付单成功";
			}else {
				msg = msg +",打回修改";
			}
			return msg;
		}
		return "审核状态错误";
	}
	
	//打回修改
	@RequestMapping("/modify")
	public String modify(String msg,String taskId) {
		if(("生成废单".equals(msg)||"提交审核".equals(msg))||!StringUtils.isEmpty(taskId)) {
			Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
			if(task==null) {
				return "任务不存在";
			}
			HashMap<String, Object> map = new HashMap<>();
			map.put("t", msg);
			taskService.complete(taskId,map);
			msg = "任务："+taskId+","+msg;
			return msg;
		}		
		return "打回修改参数错误";
	}
	
	//查询流程是否完成
	@RequestMapping("/queryInstance")
	public String queryProcessInstance(String processInstanceId) {
		HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("pay000001").processInstanceId(processInstanceId);
		if(query.count()==0) {
			return "流程不存在";
		}
		long c = query.finished().count();
		if(c>0) {
			return "该流程已完成";
		}
		return "该流程未完成";
	}
	
	@RequestMapping("/node")
	public String node(String processId) {
		String currentNode="",nextNode="",previousNode="";
		if(this.isFinished(processId)) {
			List<HistoricActivityInstance> list = historyService.createHistoricActivityInstanceQuery().processInstanceId(processId).orderByHistoricActivityInstanceStartTime().desc().list();
			HistoricActivityInstance CURRENT = list.get(0);
		}else {
			ActivityInstanceQuery aiq = runtimeService.createActivityInstanceQuery().processInstanceId(processId);
			List<ActivityInstance> list = aiq.orderByActivityInstanceStartTime().desc().list();
			ActivityInstance current = list.get(0);
			currentNode=current.getActivityId();
			BpmnModel bpmnModel = repositoryService.getBpmnModel(current.getProcessDefinitionId());
			FlowElement e = bpmnModel.getFlowElement(currentNode);
			FlowNode flowNode = (FlowNode) e;
			List<SequenceFlow> outFlows = flowNode.getOutgoingFlows();
			for(SequenceFlow sequenceFlow:outFlows) {
				// 下一个审批节点
	            FlowElement targetFlow = sequenceFlow.getTargetFlowElement();
	            if(targetFlow instanceof SequenceFlow) {
	            	Log.info("序列流信息~~");
	            	FlowNode f = (FlowNode) targetFlow;
	            }

			}
		}				
		return "das";		
	}
	/**
	 * 总控制
	 * @param payProcess
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping("/interface")
	public Object pay(PayProcessEntity payProcess,HttpServletResponse httpServletResponse) throws Exception {
		Object msg = "不存在该流程名称";
		if("启动流程".equals(payProcess.getProcessName())) {//启动流程
			msg = startProcess(payProcess.getUserId());
		}else if("查询任务".equals(payProcess.getProcessName())) {//查询任务列表
			msg = queryTask(payProcess.getUserId());
		}else if("录入数据".equals(payProcess.getProcessName())) {//录入任务
			msg = record(payProcess.getTaskId());
		}else if("审核数据".equals(payProcess.getProcessName())) {//审核任务
			msg = review(payProcess.getStatus(), payProcess.getTaskId());
		}else if("打回修改".equals(payProcess.getProcessName())) {//打回修改
			msg = modify(payProcess.getStatus(),payProcess.getTaskId());
		}else if("流程图".equals(payProcess.getProcessName())){//显示流程图
			genProcessDiagram(httpServletResponse, payProcess.getProcessId(),payProcess.getStatus());
			msg = "不存在流程实例";
		}else if("流程是否完成".equals(payProcess.getProcessName())) {//查询流程是否完成
			msg = queryProcessInstance(payProcess.getProcessId());
		}else if("任务流程ID".equals(payProcess.getProcessName())) {//根据任务Id查询流程Id
			msg = queryInstance(payProcess.getTaskId());
		}else if("删除流程".equals(payProcess.getProcessName())) {//删除流程实例
			msg = delInstance(payProcess.getProcessId());
		}else if("用户流程明细".equals(payProcess.getProcessName())) {//删除流程实例
			msg = queryExistProcess(payProcess.getUserId());
		}
		return msg;
	}
	@RequestMapping("/api")
	public String api() {
		StringBuffer sb = new StringBuffer();
		sb.append("================pay000001.bpmn20.xml=============").append("<br/>")
		.append("http://127.0.0.1:8081/pay/interface?").append("<br/>")
		.append("&nbsp;").append("processName=启动流程&userId=?").append("<br/>")
		.append("&nbsp;").append("processName=查询任务&userId=?").append("<br/>")
		.append("&nbsp;").append("processName=录入数据&taskId=?").append("<br/>")
		.append("&nbsp;").append("processName=审核数据&status=通过|不通过&taskId=?").append("<br/>")
		.append("&nbsp;").append("processName=打回修改&status=提交审核|生成废单&taskId=?").append("<br/>")
		.append("&nbsp;").append("processName=流程图&processId=?&status=null|流程历史").append("<br/>")
		.append("&nbsp;").append("processName=流程是否完成&processId=?").append("<br/>")
		.append("&nbsp;").append("processName=任务流程ID&taskId=?").append("<br/>")
		.append("&nbsp;").append("processName=删除流程&processId=?").append("<br/>")
		.append("&nbsp;").append("processName=用户流程明细&userId=?");
		return sb.toString();
	}
	
	public String queryExistProcess(String userId) {		
		StringBuffer sb = new StringBuffer();
		List<HistoricProcessInstance> ufhp = historyService.createHistoricProcessInstanceQuery().startedBy(userId).unfinished().orderByProcessInstanceStartTime().desc().list();
		sb.append(userId).append("未完成流程").append(ufhp.size()).append("个").append("<br/>")
		.append("=======================================").append("<br/>");
		for(HistoricProcessInstance h:ufhp) {
			sb.append(h.getId()).append("<br/>");
		}
		sb.append("=======================================").append("<br/>");
		List<HistoricProcessInstance> fhp = historyService.createHistoricProcessInstanceQuery().startedBy(userId).finished().orderByProcessInstanceEndTime().desc().list();
		sb.append(userId).append("已完成流程").append(fhp.size()).append("个").append("<br/>")
		.append("=======================================").append("<br/>");
		for(HistoricProcessInstance h:fhp) {
			sb.append(h.getId()).append("<br/>");
		}
		sb.append("=======================================").append("<br/>");
		return sb.toString();		
	}
	
	/**
	 * 查询任务流程实例
	 * @return
	 */
	@RequestMapping("/queryProcessId")
	public String queryInstance(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if(task==null) {
			return "不存在该任务";
		}
		String msg = "processId:"+task.getProcessInstanceId();
		return msg;	
	}
	
	/**
	 * 删除实例
	 * @return
	 */
	@RequestMapping("/delInstance")
	public String delInstance(String processId) {
		runtimeService.deleteProcessInstance(processId, "废弃");
		return "删除实例"+processId+"成功";	
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
	@RequestMapping(value = "processDiagram")
	public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId,String flag) throws Exception {
		String pis = "";
		List<String> activityIds = new ArrayList<String>();
		List<String> flows = new ArrayList<String>();
		
		if(this.isFinished(processId) || "流程历史".equals(flag)) {//已结束流程
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
