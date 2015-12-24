package com.dgssm.switchingdroid.services;

import java.util.LinkedList;

public class NetworkCommands {
	
	private LinkedList<Command> mCommandList = new LinkedList<Command>();
	
	
	/*****************************************************
	*		Initializing methods
	******************************************************/
	public NetworkCommands() {
	}
	
	/*****************************************************
	*		Public methods
	******************************************************/
	public void addCommand(Command cmd) {
		mCommandList.add(cmd);
	}
	
	public void clearCommandList() {
		mCommandList.clear();
	}
	
	public int size() {
		return mCommandList.size();
	}
	
	public Command getIndex(int index) {
		if(index < 0 || index > mCommandList.size() - 1) {
			return null;
		}
		return mCommandList.get(index);
	}
	
	
	/*****************************************************
	*		Sub classes
	******************************************************/

}
