package picoded.core.conv;

import org.junit.Test;

public class ArrayConv_test {
	
	//
	// Expected exception testing
	//
	
	/// Invalid constructor test
	@Test(expected = IllegalAccessError.class)
	public void invalidConstructor() throws Exception {
		new ArrayConv();
	}
	
}
