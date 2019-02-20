package priv.gitonlie.flowable.engine;

import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson.JSONObject;
import priv.gitonlie.flowable.engine.ControlCenterService;
import priv.gitonlie.flowable.exception.BusinessException;
import priv.gitonlie.flowable.model.FlowableConsant;
import priv.gitonlie.flowable.model.ReturnParam;

/**
 * 所有流程控制入口
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/flowable")
public class FlowableController {
	
	private static Logger Log = LoggerFactory.getLogger(FlowableController.class);
	
	@Autowired
	private ControlCenterService service;
	@Autowired
	private WorkflowEngineService EngineService;
	
	@RequestMapping("/process")
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
	
	@RequestMapping("/processDiagram")
	public void processDiagram(HttpServletResponse httpServletResponse, @RequestParam Map<String,Object> requestMap) throws BusinessException, Exception {
		try {
			EngineService.genProcessDiagram(httpServletResponse, requestMap);
			Log.info("=======FlowableController, processDiagram");
		} catch (Exception e) {
			Log.error("=======FlowableController, processDiagram，error: " + e);
			throw new BusinessException(FlowableConsant.REQ_FAIL_CODE, FlowableConsant.REQ_FAIL_MSG);
		}
	}
	
}
