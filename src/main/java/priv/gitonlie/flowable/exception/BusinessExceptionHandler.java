package priv.gitonlie.flowable.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import priv.gitonlie.flowable.model.FlowableConsant;
import priv.gitonlie.flowable.model.ReturnParam;

@ControllerAdvice
public class BusinessExceptionHandler {

	private Logger log = LoggerFactory.getLogger(BusinessExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	@ResponseBody
	public ReturnParam exceptionHandler(BusinessException bex) {
		log.info("=======BusinessExceptionHandler：{}({})", bex.getMsg(), bex.getCode());
		ReturnParam returnParam = ReturnParam.getInstance(bex.getCode(), bex.getMsg());
		return returnParam;
	}

	@ExceptionHandler(Exception.class)
	@ResponseBody
	public ReturnParam exceptionHandler(Exception e) {
		log.error("=======ExceptionHandler：", e);
		ReturnParam returnParam = ReturnParam.getInstance(FlowableConsant.SYS_ERROR_CODE, FlowableConsant.SYS_ERROR_MSG);
		return returnParam;
	}
}
