/**
 * Copyright © 2011-2012 Atilika Inc.  All rights reserved.
 *
 * Atilika Inc. licenses this file to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  A copy of the License is distributed with this work in the
 * LICENSE.txt file.  You may also obtain a copy of the License from
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.atilika.kuromoji.server;

import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.atilika.kuromoji.DebugTokenizer;
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

    private static final String DOT_COMMAND = "dot -Tsvg";

    private static final int MAX_INPUT_LENGTH = 512;

    private static final int MAX_INPUT_VITERBI_LENGTH = 32;

    private static final Logger log = LoggerFactory.getLogger(KuromojiServer.class);

    private static Tokenizer normalTokenizer = Tokenizer.builder().mode(Mode.NORMAL).build();

    private static Tokenizer searchTokenizer = Tokenizer.builder().mode(Mode.SEARCH).build();

    private static Tokenizer extendedTokenizer = Tokenizer.builder().mode(Mode.EXTENDED).build();

    private static DebugTokenizer debugTokenizer = DebugTokenizer.builder().mode(Mode.NORMAL).build();

    @GET
    @Path("/tokenize")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenizeGet(@QueryParam("text") String text,
                                @DefaultValue("utf-8") @QueryParam("encoding") String encoding,
                                @DefaultValue("0") @QueryParam("mode") int mode)
    throws JSONException, UnsupportedEncodingException {
        log.debug("GET request with text: " + text + ", encoding: " + encoding + ", mode: " + mode);
        return Response.ok(tokenize(text, encoding, mode)).build();
    }


    @POST
    @Path("/tokenize")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response tokenizePost(@FormParam("text") String text,
                                 @DefaultValue("utf-8") @FormParam("encoding") String encoding,
                                 @DefaultValue("0") @FormParam("mode") int mode) throws JSONException, IOException {
        log.debug("POST request with text: " + text + ", encoding: " + encoding + ", mode: " + mode);
        return Response.ok(tokenize(text, encoding, mode)).build();
    }

    private JSONObject tokenize(String text, String encoding, int mode) throws JSONException, UnsupportedEncodingException {
        text = URLDecoder.decode(text, encoding);
        log.info("Tokenizing text " + text + " using mode " + mode);
        
        if (mode == 3) {
            text = trimInputText(text, MAX_INPUT_VITERBI_LENGTH);
        } else {
            text = trimInputText(text, MAX_INPUT_LENGTH);
        }
        
        Tokenizer tokenizer = getTokenizer(mode);
        List<Token> tokens = tokenizer.tokenize(text);
        return makeResponse(text, mode, tokens);
    }
    
    private String trimInputText(String text, int maxLength) {
        int length = text.length();
        if (length > maxLength) {
            text = text.substring(0, maxLength);
            log.warn("Input length " + length + " exceeds max length.  Trimming to max length of " + maxLength);
        }
        return text;
    }

    private JSONObject makeResponse(String text, int mode, List<Token> tokens) throws JSONException {

        JSONObject json = new JSONObject();
        JSONArray jsonTokens = new JSONArray();

        for (Token token : tokens) {
            JSONObject jsonToken = new JSONObject();
            jsonToken.put("surface", token.getSurfaceForm());
            jsonToken.put("base", token.getBaseForm());
            jsonToken.put("pos", token.getPartOfSpeech());

            if (token.isUnknown()) {
                jsonToken.put("reading", "?");
            } else {
                jsonToken.put("reading", token.getReading());
            }

            if (token.isUser() || token.isUnknown()) {
                jsonToken.put("pronunciation", "?");
            } else {
                assert token.getAllFeaturesArray().length == 9;
                jsonToken.put("pronunciation", token.getAllFeaturesArray()[8]);
            }
            jsonTokens.put(jsonToken);
        }
        json.put("input", text);
        json.put("tokens", jsonTokens);
        json.put("mode", mode);

        if (mode == 3) {
            json.put("viterbi", getViterbiSVG(debugTokenizer.debugTokenize(text)));
        }
        return json;
    }

    private Tokenizer getTokenizer(int mode) {
        if (mode == 0 || mode == 3) {
            return normalTokenizer;
        } else if (mode == 1) {
            return searchTokenizer;
        } else if (mode == 2) {
            return extendedTokenizer;
        } else {
            log.error("Illegal mode " + mode + ". Using normal tokenizer.");
            return this.normalTokenizer;
        }
    }

    private String getViterbiSVG(String dot) {
        Process process = null;
        try {
            log.info("Running " + DOT_COMMAND);
            process = Runtime.getRuntime().exec(DOT_COMMAND);
            process.getOutputStream().write(dot.getBytes("utf-8"));
            process.getOutputStream().close();

            InputStream input = process.getInputStream();
            String svg = new Scanner(input, "utf-8").useDelimiter("\\A").next();

            int exitValue = process.exitValue();

            log.debug("Read " + svg.getBytes("utf-8").length + " bytes of SVG output");
            log.info("Process exited with exit value " + exitValue);
            return svg;
        } catch (IOException e) {
            log.error("Error running process " + process, e.getCause());
            return null;
        } finally {
            if (process != null) {
                log.info("Destroying process " + process);
                process.destroy();
            }
        }
    }
}