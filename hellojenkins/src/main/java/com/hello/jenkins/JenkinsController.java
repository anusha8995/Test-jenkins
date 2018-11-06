package com.hello.jenkins;

import java.util.ArrayList;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JenkinsController {
	
	@RequestMapping("/getAll")
	public Information getValues() {
		Information InformationBean=new Information();
		InformationBean.setId(1);
		InformationBean.setName("Anusha");
		InformationBean.setAge(22);
		return 	InformationBean;
		
	}

}
