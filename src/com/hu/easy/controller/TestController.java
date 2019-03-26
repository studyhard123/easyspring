package com.hu.easy.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hu.easy.annotation.AutoWired;
import com.hu.easy.annotation.Controller;
import com.hu.easy.annotation.RequestMapping;
import com.hu.easy.annotation.RequestParam;
import com.hu.easy.service.Service;

@Controller
@RequestMapping("/test")
public class TestController {

	@AutoWired
	private Service service;
	
	@RequestMapping("/add")
	public String add(@RequestParam("str") String str , @RequestParam("year") Integer year , @RequestParam("date") Date date , HttpServletRequest request , HttpServletResponse response) throws Exception{
		service.add();
		System.out.println("============================");
		System.out.println(str);
		System.out.println(year);
		System.out.println(date);
		System.out.println("============================");
		return "str=" + str + ",year=" + year + ",date=" + date;
	}
}
