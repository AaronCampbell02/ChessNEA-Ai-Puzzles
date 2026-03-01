package main;

import java.util.HashMap;

public class ColMap {
	
	static HashMap<String,String> numToCol = new HashMap<String,String>();
	
	static{
		numToCol.put("1", "A");
		numToCol.put("2", "B");
		numToCol.put("3", "C");
		numToCol.put("4", "D");
		numToCol.put("5", "E");
		numToCol.put("6", "F");
		numToCol.put("7", "G");
		numToCol.put("8", "H");
	}
	
	public static String getColLet(String colNum) {
		return(numToCol.getOrDefault(colNum, ""));
	}
	
}

