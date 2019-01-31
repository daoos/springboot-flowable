package priv.gitonlie.flowable.configure.pro;

import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import priv.gitonlie.flowable.controller.ExpenseController;

public class PaymentSlipDelegate implements JavaDelegate {
	
	private Logger Log = LoggerFactory.getLogger(ExpenseController.class);
	
	@Override
	public void execute(DelegateExecution execution) {
		/*
		 * Map<String, Object> exe = execution.getVariables(); String userId =
		 * String.valueOf(exe.get("userId")); String createDate =
		 * String.valueOf(exe.get("createDate"));
		 */
		Log.info("生成代付单");
	}

}
