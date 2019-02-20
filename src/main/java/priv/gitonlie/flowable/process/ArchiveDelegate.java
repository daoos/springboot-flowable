package priv.gitonlie.flowable.process;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import priv.gitonlie.flowable.service.LeaveService;

public class ArchiveDelegate implements JavaDelegate {
	
	private Logger Log = LoggerFactory.getLogger(LeaveService.class);
	@Override
	public void execute(DelegateExecution execution) {
		Log.info("{}:已归档",execution.getId());
	}

}
