/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package eu.stratosphere.sopremo.pact;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.fs.FileInputSplit;
import eu.stratosphere.pact.common.io.FileInputFormat;
import eu.stratosphere.pact.common.io.statistics.BaseStatistics;
import eu.stratosphere.pact.common.type.KeyValuePair;
import eu.stratosphere.pact.common.type.base.PactLong;
import eu.stratosphere.sopremo.io.JsonParseException;
import eu.stratosphere.sopremo.io.JsonParser;
import eu.stratosphere.sopremo.jsondatamodel.JsonNode;
import eu.stratosphere.sopremo.jsondatamodel.NullNode;
import eu.stratosphere.sopremo.jsondatamodel.ObjectNode;

/**
 * Reads json files with Jackson. The resulting key/value pair consists of an id and a {@link PactJsonObject}. The id is
 * an auto-incremented {@link PactLong}.
 * 
 * @author Arvid Heise
 */
public class JsonInputFormat extends FileInputFormat<JsonNode, JsonNode> {

//	private boolean array;

	private boolean end;

	private JsonParser parser;

	private Charset encoding;

	public static final String PARAMETER_ENCODING = "Encoding";

	private void checkEnd() throws IOException, JsonParseException {
//		if (this.array && this.parser.nextToken() == JsonToken.END_ARRAY || !this.array
//			&& this.parser.nextToken() == null)
			this.end = this.parser.checkEnd();
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.parser.close();
	}

	@Override
	public void configure(final Configuration parameters) {
		super.configure(parameters);

		final String encoding = parameters.getString(PARAMETER_ENCODING, null);
		if (encoding != null)
			this.encoding = Charset.forName(encoding);
	}

	@Override
	public KeyValuePair<JsonNode, JsonNode> createPair() {
		return new KeyValuePair<JsonNode, JsonNode>(NullNode.getInstance(), new ObjectNode());
	}

	@Override
	public boolean nextRecord(final KeyValuePair<JsonNode, JsonNode> pair) throws IOException {
		if (!this.end) {
			pair.setValue(this.parser.readValueAsTree());
			this.checkEnd();
			return true;
		}

		return false;
	}

	@Override
	public void open(final FileInputSplit split) throws JsonParseException, IOException {
		super.open(split);

		this.end = false;
		if (this.encoding != null)
			this.parser = new JsonParser(new InputStreamReader(this.stream, this.encoding));
		else
			this.parser = new JsonParser(this.stream);
//		this.parser.setCodec(JsonUtil.OBJECT_MAPPER);
//		this.parser.enable(eu.stratosphere.sopremo.io.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
//		this.parser.enable(eu.stratosphere.sopremo.io.Feature.INTERN_FIELD_NAMES);
//		if (this.array = this.parser.nextToken() == JsonToken.START_ARRAY)
//			this.parser.clearCurrentToken();
		this.checkEnd();
	}

	@Override
	public boolean reachedEnd() {
		return this.end;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.stratosphere.pact.common.io.InputFormat#getStatistics()
	 */
	@Override
	public BaseStatistics getStatistics(final BaseStatistics cachedStatistics) {
		return null;
	}

}