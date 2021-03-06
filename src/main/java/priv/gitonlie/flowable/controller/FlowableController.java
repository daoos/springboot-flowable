package priv.gitonlie.flowable.controller;

import java.io.InputStream;
import java.io.OutputStream;
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
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONObject;

import priv.gitonlie.flowable.engine.ControlCenterService;
import priv.gitonlie.flowable.engine.WorkflowEngineService;
import priv.gitonlie.flowable.exception.BusinessException;
import priv.gitonlie.flowable.model.FlowableConsant;
import priv.gitonlie.flowable.model.ReturnParam;
import priv.gitonlie.flowable.service.FlowableService;

/**
 * 所有流程控制入口
 * @author Administrator
 *
 */
//@RestController
//@RequestMapping("/flowable")
public class FlowableController {
	
	private static Logger Log = LoggerFactory.getLogger(FlowableController.class);
	
	@Autowired
	private ControlCenterService service;
	
//	@RequestMapping("/process")
	public ReturnParam process(@RequestParam Map<String,Object> requestMap) throws BusinessException, Exception {
		ReturnParam returnParam = ReturnParam.getInstance(FlowableConsant.REQ_FAIL_CODE, FlowableConsant.REQ_FAIL_MSG);
		try {
			Object res = service.process(requestMap);
			returnParam = ReturnParam.getInstance(FlowableConsant.REQ_SUCCESS_CODE, FlowableConsant.REQ_SUCCESS_MSG,res);
			Log.info("=======FlowableController, process，return: {}",JSONObject.toJSONString(returnParam));
		}catch(Exception e) {
			Log.error("=======FlowableController, process，error: " + e);
			throw new BusinessException(FlowableConsant.REQ_FAIL_CODE, FlowableConsant.REQ_FAIL_MSG);
		}
		return returnParam;
	}
	
//	@RequestMapping("/processDiagram")
	public void processDiagram(HttpServletResponse httpServletResponse, @RequestParam Map<String,Object> requestMap) throws BusinessException, Exception {
		try {
//			service.genProcessDiagram(httpServletResponse, requestMap);
			Log.info("=======FlowableController, processDiagram");
		} catch (Exception e) {
			Log.error("=======FlowableController, processDiagram，error: " + e);
			throw new BusinessException(FlowableConsant.REQ_FAIL_CODE, FlowableConsant.REQ_FAIL_MSG);
		}
	}
	
}
