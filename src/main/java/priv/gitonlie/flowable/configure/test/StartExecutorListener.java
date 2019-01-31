package priv.gitonlie.flowable.configure.test;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import priv.gitonlie.flowable.service.LeaveService;

public class StartExecutorListener implements ExecutionListener {
	
	private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -7651814351056791863L;

	@Override
	public void notify(DelegateExecution execution) {
		Log.info("********************************启动请假工作流引擎");
	}

}
