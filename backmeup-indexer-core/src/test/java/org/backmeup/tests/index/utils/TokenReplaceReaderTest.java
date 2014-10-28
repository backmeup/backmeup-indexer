package org.backmeup.tests.index.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.backmeup.index.utils.tokenreader.MapTokenResolver;
import org.backmeup.index.utils.tokenreader.TokenReplaceReader;
import org.junit.Assert;
import org.junit.Test;

public class TokenReplaceReaderTest {

	@Test
	public void replaceTokensInStringsByMapTokenResolver() throws IOException {

		Map<String, String> tokens = new HashMap<>();
		tokens.put("token1", "value1");
		tokens.put("token2", "JJ ROCKS!!!");

		MapTokenResolver resolver = new MapTokenResolver(tokens);

		Reader source = new StringReader(
				"1234567890${token1}abcdefg${token2}XYZ$000");

		try (Reader reader = new TokenReplaceReader(source, resolver)) {
		    
		    String result = "";
		    String expresult = "1234567890value1abcdefgJJ ROCKS!!!XYZ$000";

	        int data = reader.read();
	        while (data != -1) {
	            result += (char) data;
	            data = reader.read();
	        }
	        Assert.assertEquals(result, expresult);
		}

	}
}
