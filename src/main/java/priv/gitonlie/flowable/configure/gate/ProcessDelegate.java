package priv.gitonlie.flowable.configure.gate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDelegate implements JavaDelegate {
	
	private Logger Log = LoggerFactory.getLogger(ProcessDelegate.class);
	
	@Override
	public void execute(DelegateExecution execution) {
		Log.info("测试归档");
	}

}
