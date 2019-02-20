package priv.gitonlie.flowable.process;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentDiscardDelegate implements JavaDelegate {
	
	private Logger Log = LoggerFactory.getLogger(PaymentDiscardDelegate.class);
	
	@Override
	public void execute(DelegateExecution execution) {
		Log.info("生成代付废单");
	}

}
