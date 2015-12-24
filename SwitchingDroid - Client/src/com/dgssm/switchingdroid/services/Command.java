package com.dgssm.switchingdroid.services;

public class Command {
	public int command = -1;
	public int length = 0;
	public byte[] data = null;
	
	public Command(int cmd, int l, byte[] d) {
		command = cmd;
		length = l;
		data = d;
	}
}
