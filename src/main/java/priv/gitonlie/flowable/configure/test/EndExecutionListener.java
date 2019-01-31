package priv.gitonlie.flowable.configure.test;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import priv.gitonlie.flowable.service.LeaveService;

public class EndExecutionListener implements ExecutionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4431483593450068083L;
	
	private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	
	@Override
	public void notify(DelegateExecution execution) {
		Log.info("********************************结束请假流程");
	}

}
