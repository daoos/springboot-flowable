package priv.gitonlie.flowable.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import priv.gitonlie.flowable.controller.FlowableController;

/**
 *  <p>流程图服务</p>
 * @author Administrator
 *
 */
@Service
public class ProcessDiagramService {
	
	private static Logger Log = LoggerFactory.getLogger(FlowableController.class);
	
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
	
	
	/**
	   * 判断流程是否结束
	 * @param processInstanceId
	 * @return
	 */
	private boolean isFinished(String processInstanceId) {
        return historyService.createHistoricProcessInstanceQuery().finished()
                .processInstanceId(processInstanceId).count() > 0;
	}
	
	/**
	 * 生成流程图
	 *
	 * @param processId 任务ID
	 */
	@RequestMapping(value = "processDiagram")
	public void genProcessDiagram(HttpServletResponse httpServletResponse, String processInstanceId,String flag) throws Exception {
		String pis = "";
		List<String> activityIds = new ArrayList<String>();
		List<String> flows = new ArrayList<String>();
		
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
