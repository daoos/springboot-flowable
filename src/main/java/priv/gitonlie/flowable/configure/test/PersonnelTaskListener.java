package priv.gitonlie.flowable.configure.test;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import priv.gitonlie.flowable.service.LeaveService;

public class PersonnelTaskListener implements TaskListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6197854117509946373L;
	private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	
	@Override
	public void notify(DelegateTask delegateTask) {
		delegateTask.setAssignee("person");
	}
	
}
