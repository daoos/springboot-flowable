package priv.gitonlie.flowable.process;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentSlipDelegate implements JavaDelegate {
	
	private Logger Log = LoggerFactory.getLogger(PaymentSlipDelegate.class);
	
	@Override
	public void execute(DelegateExecution execution) {
		Log.info("生成代付单");
	}

}
