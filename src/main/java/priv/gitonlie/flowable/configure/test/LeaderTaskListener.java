package priv.gitonlie.flowable.configure.test;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import priv.gitonlie.flowable.service.LeaveService;

public class LeaderTaskListener implements TaskListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6992889100696073124L;
	private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	
	@Override
	public void notify(DelegateTask delegateTask) {
		Log.info("###############领导审批");
		delegateTask.setAssignee("领导");
		
	}
	
}
