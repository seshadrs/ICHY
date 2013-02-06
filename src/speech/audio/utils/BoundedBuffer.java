/* 
 * AUTHOR: seshadrs
 * Feb 2 2013
 * 
 * */

package speech.audio.utils;

import java.util.*;


public class BoundedBuffer <Type> {
	//A first in - first out buffer data structure
	
		  private ArrayList<Type> data = new ArrayList<Type>();		//buffer datastructure
		  private int capacity;										//the maximum number of items allowed in the buffer

		  public BoundedBuffer(Integer number) 
		  {
			  //Initializes buffer to the passed capacity. Memory is not allocated immediately 
			  capacity = number;
		  }

		  public boolean store(Type value) 
		  {
			  //Value gets added to the buffer at the end
		    if (!bufferFull()) 
		    {
		    	data.add(value);
		    	return true;
		      
		    } 
		  else 
		  	{
		      return false;
		    }
		    
		  }

		  public Type read() {
			  //Value at the beginning of the buffer is deleted and returned
			  
			  if(!bufferEmpty())
			  {
				  Type item = data.remove(0);
				  return item;
				  
				  }
			  else
				  return null;
		  }
		  
		  public Type peek() {
			  //Value at the beginning of buffer is returned. It is not deleted.
			  
			  if(!bufferEmpty())
			  {
				  Type item = data.get(0);
				  return item;
				  
				  }
			  else
				  return null;
		  }

		  public boolean bufferFull() {
			  //Returns true if buffer is full. false otherwise.
		    
			  if (data.size()==capacity)
				  return true;
			  else
				  return false;
		  }
		  
		  public boolean bufferEmpty() {
			  //Returns true if buffer is empty. false otherwise.
			    
			  if (data.size()>0)
				  return false;
			  else
				  return false;
		  }
		  
		  public static void main(String[] args)
		  {
			  //testing the Bounded buffer class  
			  
			  BoundedBuffer<Integer> b1 = new BoundedBuffer<Integer>(3);
			  b1.store(2);
			  b1.store(1);
			  System.out.println(b1.read());
			  System.out.println(b1.peek());
			  b1.store(7);
			  System.out.println(b1.read());
			  b1.store(8);
			  System.out.println(b1.peek());
			  System.out.println(b1.read());
		  }

}
