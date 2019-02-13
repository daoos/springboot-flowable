package priv.gitonlie.flowable.controller;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {
	
	@RequestMapping("/payIndex")
	public String payIndex() {
		return "payIndex";		
	}
	
	@RequestMapping(value = "/diagramPNG")
	public String processDIA(String processId,String flag,Model model) {
		model.addAttribute("processId",processId);
		model.addAttribute("flag", flag);
		return "diagram";		
	}
	
	@RequestMapping("/go")
	@ResponseBody
	public String go(HttpServletResponse response) throws IOException {
		String msg ="body";
		OutputStream ops = response.getOutputStream();
		ops.write("good".getBytes());
		ops.flush();
		ops.close();
		return msg;		
	}
}
