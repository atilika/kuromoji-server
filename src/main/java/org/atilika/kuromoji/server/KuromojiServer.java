/**
 * 
 */
package org.atilika.kuromoji.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;
import org.atilika.kuromoji.Tokenizer.Mode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/tokenizer")
public class KuromojiServer {

	private static Logger log = LoggerFactory.getLogger(KuromojiServer.class);

	private static Tokenizer normalTokenizer;

	private static Tokenizer searchTokenizer;

	private static Tokenizer extendedTokenizer;

	public KuromojiServer() {
		this.normalTokenizer = Tokenizer.builder().mode(Mode.NORMAL).build();
		this.searchTokenizer = Tokenizer.builder().mode(Mode.SEARCH).build();
		this.extendedTokenizer = Tokenizer.builder().mode(Mode.EXTENDED).build();
	}
	
	@GET
    @Path("/tokenize")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenize(@QueryParam("text") String text,
    		                 @DefaultValue("utf-8") @QueryParam("encoding") String encoding,
    		                 @DefaultValue("0") @QueryParam("mode") int mode)
		throws JSONException, UnsupportedEncodingException {
		
		Tokenizer tokenizer = getTokenizer(mode);
		
		log.info("Got text: " + new String(text.getBytes("utf-8")));
		log.info("Got encoding: " + encoding);
		
		String decodedText = URLDecoder.decode(text, encoding);
		log.info("Decoded text: " + new String(decodedText.getBytes("utf-8")));
		
		JSONArray jsonTokens = new JSONArray();
		for (Token token : tokenizer.tokenize(decodedText)) {
			JSONObject jsonToken = new JSONObject();
			jsonToken.put("surface", token.getSurfaceForm());
			jsonToken.put("features", token.getAllFeatures());
			jsonTokens.put(jsonToken);
		}
		
		return Response.ok(jsonTokens).build();
    }
    
    private Tokenizer getTokenizer(int mode) {
    	if (mode == 0) {
    		return this.normalTokenizer;
    	} else if (mode == 1) {
    		return this.searchTokenizer;
    	} else if (mode == 2) {
    		return this.extendedTokenizer;
    	} else {
    		log.error("Illegal mode " + mode + ". Using normal tokenizer.");
    		return this.normalTokenizer;
    	}
    }
}