package priv.gitonlie.flowable.configure.test;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import priv.gitonlie.flowable.service.LeaveService;

public class UserTaskListener implements TaskListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6694596993861321586L;
	
	private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	@Override
	public void notify(DelegateTask delegateTask) {
		delegateTask.setAssignee("人事");
	}

}
