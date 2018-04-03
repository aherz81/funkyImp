/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.javac.util;

/**
 *
 * @author aherz
 */

import com.sun.tools.javac.tree.JCTree;

@SuppressWarnings("serial")
public class CodePath extends java.util.ArrayList<JCTree>{

	public JCTree getLast()
	{
		return get(size()-1);
	}

	public JCTree getFirst()
	{
		return get(0);
	}

	public String toString()
	{
		String res="[";
		for(JCTree t:this)
			if(t!=this.get(0))
				res=res+","+t.toFlatString();
			else
				res=res+t.toFlatString();
		return res+"]";
	}
}
