package com.hu.easy.service;

@com.hu.easy.annotation.Service
public class TestService implements Service{

	@Override
	public void add() {
		System.out.println("������Serviceʵ�����add����");
	}

}
