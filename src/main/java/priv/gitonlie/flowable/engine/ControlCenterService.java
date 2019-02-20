package priv.gitonlie.flowable.engine;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ControlCenterService {
	
	@Autowired
	private WorkflowEngineService service;
	
	public Object process(Map<String,Object> requestMap) throws Exception {
		Object result = null;
		String processName = requestMap.get("processName")==null?"":String.valueOf(requestMap.get("processName"));
		requestMap.remove("processName");
		switch (processName) {
		case "init":
			result = service.init(requestMap);//初始化流程
			break;
		case "start":
			result = service.startProcess(requestMap);//发起流程(开启并且直接完成任务)
			break;
		case "queryTask":
			result = service.queryTask(requestMap);//查询任务流程
			break;
		case "claimTask":
			result = service.claimTask(requestMap);//申领任务流程
			break;
		case "go":
			result = service.completeTask(requestMap);//驱动任务流程
			break;
		case "currentNode":
			result = service.currentNode(requestMap);//当前任务流程节点
			break;
		case "history":
			result = service.historyNode(requestMap);//任务历史流程节点
			break;
		case "allNode":
			result = service.allNode(requestMap);//所有流程节点
			break;
		case "frontAndNextNode":
			result = service.frontAndNextNode(requestMap);//上下节点
			break;
		case "finished":
			result = service.finished(requestMap);//流程是否结束
			break;
		case "diagram":
			result = service.processDiagram(requestMap);//流程图
			break;
		default:
			Map<String,Object> res = new HashMap<String, Object>();
			res.put("message", "流程名称不存在");
			break;
		}
		return result;		
	}
}
