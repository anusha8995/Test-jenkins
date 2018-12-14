package com.canon.teamforge.util;

public class TeamForgeUtilCL extends TeamForgeUtil
{
	public TeamForgeUtilCL(String sUrl, String sUser, String sPass)
	{
		super(sUrl, sUser, sPass);
	}

	void errorMsg(String sMsg)
	{
		System.out.println(sMsg);
	}

	void errorMsg(String sMsg, Throwable t)
	{
		System.out.println(sMsg);
		t.printStackTrace(System.out);
	}
}