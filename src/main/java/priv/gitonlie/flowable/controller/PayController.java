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
import org.flowable.engine.IdentityService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
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
	public String modify(String taskId,String msg) {
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
	/**
	 * 删除任务
	 * @return
	 */
	@RequestMapping("/deltask")
	public String deleteTask(String taskId) {
		taskService.deleteTask(taskId);
		return "删除任务";
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
	 * 生成流程图
	 *
	 * @param processId 任务ID
	 */
	@RequestMapping(value = "processDiagram")
	public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
		ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

		// 流程走完的不显示图
		if (pi == null) {
			Log.info("实例不存在");
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
